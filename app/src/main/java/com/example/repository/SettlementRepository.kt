package com.example.repository

import com.example.blockchain.BlockchainSimulator
import com.example.blockchain.SimTx
import com.example.cryptography.CryptoEngine
import com.example.database.*
import kotlinx.coroutines.flow.Flow
import java.security.MessageDigest
import java.util.UUID

class SettlementRepository(
    private val walletDao: WalletDao,
    private val paymentDao: PaymentDao,
    private val licenseDao: LicenseDao,
    private val subscriptionDao: SubscriptionDao,
    private val eventLogDao: EventLogDao
) {
    val allWallets: Flow<List<WalletEntity>> = walletDao.getAllWalletsFlow()
    val allPayments: Flow<List<PaymentEntity>> = paymentDao.getAllPaymentsFlow()
    val allLicenses: Flow<List<LicenseEntity>> = licenseDao.getAllLicensesFlow()
    val allSubscriptions: Flow<List<SubscriptionEntity>> = subscriptionDao.getAllSubscriptionsFlow()
    val allEvents: Flow<List<EventLogEntity>> = eventLogDao.getAllEventLogsFlow()

    suspend fun getWallet(address: String): WalletEntity? = walletDao.getWalletByAddress(address)

    suspend fun createWallet(
        label: String,
        type: String,
        network: String,
        mnemonic: List<String>,
        path: String,
        isMultisig: Boolean = false,
        coSigners: List<String> = emptyList(),
        initialBalance: Double = 0.0
    ): WalletEntity {
        val derived = CryptoEngine.deriveAddress(mnemonic, network, path)
        
        // Simulates KYC / AML verification during enterprise generation
        val amlScore = if (type == "CUSTOMER") (0.01 + Math.random() * 0.15) else 0.01
        val kycState = if (amlScore > 0.1) "PENDING" else "APPROVED"

        val wallet = WalletEntity(
            address = derived.address,
            label = label,
            type = type,
            network = network,
            balance = initialBalance,
            publicKey = derived.publicKey,
            privateKeyEncrypted = derived.privateKeyEncrypted,
            isMultisig = isMultisig,
            requiredApprovals = if (isMultisig) 2 else 1,
            coSignersJson = coSigners.toString(),
            kycStatus = kycState,
            amlRiskScore = amlScore
        )

        walletDao.insertWallet(wallet)
        createEventLog(
            eventType = "WALLET_CREATED",
            payload = "Wallet generated successfully for address=${wallet.address}, label=${wallet.label}, type=${wallet.type}, multisig=${wallet.isMultisig}",
            network = network
        )

        return wallet
    }

    suspend fun createPayment(
        orderId: String,
        merchantLabel: String,
        amount: Double,
        network: String,
        senderAddress: String,
        recipientAddress: String,
        description: String = "",
        isEscrow: Boolean = false,
        splitConfig: String = "" // e.g. "comissionAddr:0.05,merchantAddr:0.95"
    ): PaymentEntity {
        // AML Check before billing
        val senderWallet = walletDao.getWalletByAddress(senderAddress)
        if (senderWallet != null && senderWallet.amlRiskScore > 0.15) {
            val failedPayment = PaymentEntity(
                id = "pay_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10),
                orderId = orderId,
                merchantLabel = merchantLabel,
                amount = amount,
                status = "REJECTED",
                network = network,
                senderAddress = senderAddress,
                recipientAddress = recipientAddress,
                description = "AML Risk score too high: ${senderWallet.amlRiskScore}",
                isEscrow = isEscrow,
                escrowStatus = if (isEscrow) "REJECTED" else "NONE",
                createdAt = System.currentTimeMillis()
            )
            paymentDao.insertPayment(failedPayment)
            createEventLog(
                eventType = "PAYMENT_FAILED",
                payload = "Payment rejected prior to broadcast: AML block on senderAddress=$senderAddress",
                network = network
            )
            return failedPayment
        }

        val payment = PaymentEntity(
            id = "pay_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10),
            orderId = orderId,
            merchantLabel = merchantLabel,
            amount = amount,
            status = "PENDING",
            network = network,
            senderAddress = senderAddress,
            recipientAddress = recipientAddress,
            description = description,
            isEscrow = isEscrow,
            escrowStatus = if (isEscrow) "HELD" else "NONE",
            createdAt = System.currentTimeMillis(),
            splitConfigJson = splitConfig
        )

        paymentDao.insertPayment(payment)
        createEventLog(
            eventType = if (isEscrow) "ESCROW_CREATED" else "PAYMENT_CREATED",
            payload = "Payment created with id=${payment.id}, amount=$amount, escrow=$isEscrow",
            network = network
        )
        return payment
    }

    suspend fun executePaymentAndBroadcast(paymentId: String, privateKeyHex: String): Boolean {
        val payment = paymentDao.getPaymentById(paymentId) ?: return false
        val sender = walletDao.getWalletByAddress(payment.senderAddress) ?: return false
        val recipient = walletDao.getWalletByAddress(payment.recipientAddress) ?: return false

        if (sender.balance < payment.amount) {
            paymentDao.updateStatus(paymentId, "FAILED", "")
            createEventLog("PAYMENT_FAILED", "Insufficient balance in sender account: ${sender.address}", payment.network)
            return false
        }

        // 1. Core Sign transaction
        val rawTxData = "${payment.network}|${payment.senderAddress}|${payment.recipientAddress}|${payment.amount}|${payment.createdAt}"
        val signature = CryptoEngine.signTransaction(rawTxData, privateKeyHex)

        // 2. Estimate gas fee and charge
        val estimatedGas = BlockchainSimulator.getGasFeeEstimation(payment.network)
        val totalCost = payment.amount + estimatedGas

        if (sender.balance < totalCost) {
            paymentDao.updateStatus(paymentId, "FAILED", "")
            createEventLog("PAYMENT_FAILED", "Sender has enough for USDT but cannot pay transaction gas fee ($estimatedGas USDT)", payment.network)
            return false
        }

        // 3. Submit transaction to local blockchain node simulator mempool
        val txHash = CryptoEngine.generateTxHash(payment.network, payment.senderAddress, payment.recipientAddress, payment.amount, payment.createdAt)
        val simTx = SimTx(
            txHash = txHash,
            fromAddress = payment.senderAddress,
            toAddress = payment.recipientAddress,
            amount = payment.amount,
            fee = estimatedGas,
            network = payment.network,
            isEscrow = payment.isEscrow,
            status = "SUCCESS"
        )
        
        BlockchainSimulator.submitToMempool(simTx)

        // 4. Update memory structures & execute block mining
        BlockchainSimulator.mineNextBlock(payment.network, listOf(simTx))

        // 5. Update local wallet balances and payment state
        val updatedSenderBalance = sender.balance - totalCost
        
        walletDao.updateBalance(sender.address, updatedSenderBalance)

        if (payment.isEscrow) {
            // Funds move into escrow pool address
            val escrowWallet = walletDao.getWalletByAddress(payment.recipientAddress)
            if (escrowWallet != null) {
                walletDao.updateBalance(escrowWallet.address, escrowWallet.balance + payment.amount)
            }
            paymentDao.updatePayment(payment.copy(status = "CONFIRMED", txHash = txHash, escrowStatus = "HELD"))
            createEventLog("ESCROW_CREATED", "Funds held securely in Escrow contract address: ${payment.recipientAddress}", payment.network, txHash)
        } else {
            // Direct deposit
            val updatedRecipientBalance = recipient.balance + payment.amount
            walletDao.updateBalance(recipient.address, updatedRecipientBalance)
            paymentDao.updatePayment(payment.copy(status = "CONFIRMED", txHash = txHash))
            
            // Check for potential revenue splitting!
            if (payment.splitConfigJson.isNotEmpty()) {
                handleRevenueSplitting(payment, totalCost)
            }

            createEventLog("PAYMENT_CONFIRMED", "Payment of ${payment.amount} USDT cleared to recipient ${recipient.address}", payment.network, txHash)
            createEventLog("PAYMENT_SETTLED", "Platform instant payout settled for paymentId=${payment.id}", payment.network, txHash)
        }

        return true
    }

    private suspend fun handleRevenueSplitting(payment: PaymentEntity, totalCost: Double) {
        // Simple demo of split payouts based on splitConfigJson e.g., "0xabc:0.05,0xdef:0.95"
        val recipient = walletDao.getWalletByAddress(payment.recipientAddress) ?: return
        try {
            val splits = payment.splitConfigJson.split(",")
            var originalBalanceLeft = recipient.balance // already includes payment.amount
            val baseAmountAdded = payment.amount
            
            // If we have split configurations, we re-allocate balances
            var splitAmountDistributed = 0.0
            for (split in splits) {
                val parts = split.split(":")
                if (parts.size == 2) {
                    val addr = parts[0].trim()
                    val pct = parts[1].trim().toDoubleOrNull() ?: 0.0
                    val allocated = baseAmountAdded * pct
                    
                    val bWallet = walletDao.getWalletByAddress(addr)
                    if (bWallet != null && bWallet.address != recipient.address) {
                        walletDao.updateBalance(bWallet.address, bWallet.balance + allocated)
                        splitAmountDistributed += allocated
                        createEventLog("ROYALTY_DISTRIBUTED", "Revenue split: Transferred $allocated USDT (or ${pct*100}%) to royalty partner wallet: ${bWallet.address}", payment.network)
                    }
                }
            }
            if (splitAmountDistributed > 0.0) {
                walletDao.updateBalance(recipient.address, recipient.balance - splitAmountDistributed)
            }
        } catch (e: Exception) {
            // fallback
        }
    }

    suspend fun releaseEscrow(paymentId: String): Boolean {
        val payment = paymentDao.getPaymentById(paymentId) ?: return false
        if (payment.isEscrow && payment.escrowStatus == "HELD") {
            val escrowWallet = walletDao.getWalletByAddress(payment.recipientAddress) ?: return false
            
            // Escrow is usually held in an escrow intermediary wallet or contract simulator address.
            // When released, the funds are sent to the actual final merchant checkout wallet.
            // In our system, the recipient wallet of the escrow is the escrow intermediate wallet itself,
            // so we route the funds to the business merchant master checkout address.
            val merchantWallets = walletDao.getAllWallets().filter { it.type == "BUSINESS" }
            if (merchantWallets.isEmpty()) return false
            val destinationMerchant = merchantWallets.first()

            if (escrowWallet.balance < payment.amount) return false

            walletDao.updateBalance(escrowWallet.address, escrowWallet.balance - payment.amount)
            walletDao.updateBalance(destinationMerchant.address, destinationMerchant.balance + payment.amount)

            paymentDao.updatePayment(payment.copy(escrowStatus = "RELEASED", status = "SETTLED"))
            
            val txHash = "0x" + UUID.randomUUID().toString().replace("-", "").substring(0, 32)
            createEventLog("ESCROW_RELEASED", "Escrow settled: ${payment.amount} USDT released into Master Treasury wallet ${destinationMerchant.address}", payment.network, txHash)
            createEventLog("PAYMENT_SETTLED", "Final commercial settlement completed for ${payment.amount} USDT", payment.network, txHash)
            return true
        }
        return false
    }

    suspend fun refundEscrow(paymentId: String): Boolean {
        val payment = paymentDao.getPaymentById(paymentId) ?: return false
        if (payment.isEscrow && payment.escrowStatus == "HELD") {
            val escrowWallet = walletDao.getWalletByAddress(payment.recipientAddress) ?: return false
            val customerWallet = walletDao.getWalletByAddress(payment.senderAddress) ?: return false

            if (escrowWallet.balance < payment.amount) return false

            walletDao.updateBalance(escrowWallet.address, escrowWallet.balance - payment.amount)
            walletDao.updateBalance(customerWallet.address, customerWallet.balance + payment.amount)

            paymentDao.updatePayment(payment.copy(escrowStatus = "REFUNDED", status = "REJECTED"))

            val txHash = "0x" + UUID.randomUUID().toString().replace("-", "").substring(0, 32)
            createEventLog("PAYMENT_FAILED", "Escrow payment refunded to customer: ${payment.amount} USDT sent back to ${customerWallet.address}", payment.network, txHash)
            return true
        }
        return false
    }

    suspend fun purchaseMarketplaceAsset(
        assetName: String,
        amount: Double,
        network: String,
        senderAddress: String,
        licenseDays: Int = 365,
        versionInfo: String = "v1.0.0",
        instruction: String = "Run standard Docker/Binary deploy scripts. Requires Kubernetes or local runtime."
    ): LicenseEntity? {
        // Creates and completes a transaction automatically for simple simulation
        val merchantWallets = walletDao.getAllWallets().filter { it.type == "BUSINESS" }
        if (merchantWallets.isEmpty()) return null
        val merchant = merchantWallets.first()

        val payment = createPayment(
            orderId = "ord_" + UUID.randomUUID().toString().substring(0, 8),
            merchantLabel = "Stablecoin Marketplace",
            amount = amount,
            network = network,
            senderAddress = senderAddress,
            recipientAddress = merchant.address,
            description = "Browser purchase of $assetName",
            isEscrow = false
        )

        if (payment.status == "REJECTED") return null

        val success = executePaymentAndBroadcast(payment.id, "0x123abcde_simulated_private_key")
        if (success) {
            // Generate software license with secure cryptographical signature
            val licenseKey = "LCN-" + UUID.randomUUID().toString().uppercase().replace("-", "").substring(0, 16)
            val digitalSig = CryptoEngine.signTransaction("$assetName|$licenseKey|${payment.id}", "0x987_marketplace_signing_key_hex")
            
            val license = LicenseEntity(
                id = "lic_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8),
                assetName = assetName,
                licenseKey = licenseKey,
                buyerAddress = senderAddress,
                paymentId = payment.id,
                status = "ACTIVE",
                durationDays = licenseDays,
                versionHistory = versionInfo,
                deploymentInstruction = instruction,
                digitalSignature = digitalSig
            )

            licenseDao.insertLicense(license)
            createEventLog(
                eventType = "LICENSE_PURCHASED",
                payload = "Software License created for buyerAddress=$senderAddress, assetName=$assetName, key=$licenseKey",
                network = network
            )
            createEventLog(
                eventType = "DIGITAL_ASSET_PURCHASED",
                payload = "Ownership of digital asset '$assetName' securely stored in Sovereign browser context",
                network = network
            )
            return license
        }
        return null
    }

    suspend fun createEventLog(eventType: String, payload: String, network: String, txHash: String = "") {
        val lastLog = eventLogDao.getLastLog()
        val prevHash = lastLog?.integrityHash ?: "00000000000000000000000000000000"
        
        val sequenceIndex = (lastLog?.id ?: 0L) + 1L
        val dataToHash = "$sequenceIndex|$prevHash|$eventType|$payload|$timestampString"
        val integrity = CryptoEngine.bytesToHex(CryptoEngine.sha256(dataToHash))

        val log = EventLogEntity(
            eventType = eventType,
            payloadJson = payload,
            network = network,
            txHash = txHash,
            integrityHash = integrity
        )
        eventLogDao.insertLog(log)
    }

    suspend fun bootstrapDemoDataIfEmpty() {
        val wallets = walletDao.getAllWallets()
        if (wallets.isEmpty()) {
            val phrase = listOf("stablecoin", "connect", "enterprise", "settlement", "payouts", "multi", "chain", "safe", "secured", "cryptography", "ledger", "audit")
            
            // 1. Business Merchant Wallet
            createWallet(
                label = "CONNECT Merchant Treasury",
                type = "BUSINESS",
                network = "Ethereum (ERC20)",
                mnemonic = phrase,
                path = "m/44'/60'/0'/0/0",
                initialBalance = 125000.00
            )

            // 2. Customer Personal Wallet
            createWallet(
                label = "Corporate Payer Client",
                type = "CUSTOMER",
                network = "Ethereum (ERC20)",
                mnemonic = phrase,
                path = "m/44'/60'/0'/0/1",
                initialBalance = 45000.00
            )

            // 3. Escrow Securing Pool Smart Vault
            createWallet(
                label = "Tron Escrow Contract Account",
                type = "ESCROW",
                network = "TRON (TRC20)",
                mnemonic = phrase,
                path = "m/44'/195'/0'/0/0",
                initialBalance = 3500.00
            )

            // 4. Multisig Security Treasury Group
            createWallet(
                label = "Treasury Vault (2-of-3 Multisig)",
                type = "TREASURY",
                network = "Arbitrum (ERC20)",
                mnemonic = phrase,
                path = "m/44'/60'/1'/0/0",
                isMultisig = true,
                coSigners = listOf("0x9837CoSigner1...", "0x82a9CoSigner2..."),
                initialBalance = 500000.00
            )

            // 5. Some historical logs, payments, and standard subscriptions
            val demoBusiness = walletDao.getAllWallets().first { it.type == "BUSINESS" }
            val demoCustomer = walletDao.getAllWallets().first { it.type == "CUSTOMER" }
            val demoEscrow = walletDao.getAllWallets().first { it.type == "ESCROW" }

            // Historic Payments standard
            val pay1 = createPayment(
                orderId = "ord_7716a",
                merchantLabel = "SaaS Cloud License",
                amount = 2500.0,
                network = "Ethereum (ERC20)",
                senderAddress = demoCustomer.address,
                recipientAddress = demoBusiness.address,
                description = "Annual recurring billing for cloud database storage proxy"
            )
            // Complete it
            paymentDao.updatePayment(pay1.copy(status = "CONFIRMED", txHash = "0x81bf6d7a11de9cfca2a862cd219eef6cc92e"))
            
            val pay2 = createPayment(
                orderId = "ord_8825c",
                merchantLabel = "Escrow B2B Shipment #980",
                amount = 12500.0,
                network = "TRON (TRC20)",
                senderAddress = demoCustomer.address,
                recipientAddress = demoEscrow.address,
                description = "B2B Hardware import escrow contract hold",
                isEscrow = true
            )
            // Kept HELD in escrow
            paymentDao.updatePayment(pay2.copy(status = "CONFIRMED", txHash = "0xtxdf81cc7bb16da8df8cf9c18da2c710"))

            // Standard Subscriptions
            subscriptionDao.insertSubscription(
                SubscriptionEntity(
                    id = "sub_" + UUID.randomUUID().toString().substring(0, 8),
                    planName = "Enterprise AI-Api Sandbox Plan",
                    customerAddress = demoCustomer.address,
                    pricePerPeriod = 350.00,
                    periodDays = 30,
                    status = "ACTIVE"
                )
            )

            subscriptionDao.insertSubscription(
                SubscriptionEntity(
                    id = "sub_" + UUID.randomUUID().toString().substring(0, 8),
                    planName = "Sovereign Node Proxy Hosting",
                    customerAddress = demoCustomer.address,
                    pricePerPeriod = 1500.00,
                    periodDays = 30,
                    status = "ACTIVE"
                )
            )
        }
    }

    private val timestampString: String
        get() = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())
}
