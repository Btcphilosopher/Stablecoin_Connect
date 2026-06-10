package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.blockchain.BlockchainSimulator
import com.example.cryptography.CryptoEngine
import com.example.database.AppDatabase
import com.example.database.EventLogEntity
import com.example.database.LicenseEntity
import com.example.database.PaymentEntity
import com.example.database.SubscriptionEntity
import com.example.database.WalletEntity
import com.example.repository.SettlementRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppScreen {
    DASHBOARD,
    WALLETS,
    SETTLEMENT_ESCROW,
    BROWSER_COMMERCE,
    BLOCK_EXPLORER,
    EVENT_LOGS,
    COMPLIANCE_SECURITY
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    
    private val repository = SettlementRepository(
        walletDao = database.walletDao(),
        paymentDao = database.paymentDao(),
        licenseDao = database.licenseDao(),
        subscriptionDao = database.subscriptionDao(),
        eventLogDao = database.eventLogDao()
    )

    // Reactive screens
    private val _currentScreen = MutableStateFlow(AppScreen.DASHBOARD)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Database core stateflows
    val wallets: StateFlow<List<WalletEntity>> = repository.allWallets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val payments: StateFlow<List<PaymentEntity>> = repository.allPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val licenses: StateFlow<List<LicenseEntity>> = repository.allLicenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subscriptions: StateFlow<List<SubscriptionEntity>> = repository.allSubscriptions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val events: StateFlow<List<EventLogEntity>> = repository.allEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Blockchain Explorer state block bindings
    val ethBlocks = BlockchainSimulator.ethereumBlocks
    val tronBlocks = BlockchainSimulator.tronBlocks
    val arbBlocks = BlockchainSimulator.arbitrumBlocks
    val bscBlocks = BlockchainSimulator.bscBlocks

    // State indicators for current selected details
    private val _selectedWalletAddress = MutableStateFlow<String?>(null)
    val selectedWalletAddress: StateFlow<String?> = _selectedWalletAddress.asStateFlow()

    // Searching and filtering event logs
    private val _eventSearchQuery = MutableStateFlow("")
    val eventSearchQuery: StateFlow<String> = _eventSearchQuery.asStateFlow()

    val filteredEvents: StateFlow<List<EventLogEntity>> = combine(events, _eventSearchQuery) { list, query ->
        if (query.isEmpty()) list else {
            list.filter {
                it.eventType.contains(query, ignoreCase = true) ||
                it.payloadJson.contains(query, ignoreCase = true) ||
                it.network.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Live Payment Flow states (Sandbox Checkout simulation)
    private val _activeCheckoutPayment = MutableStateFlow<PaymentEntity?>(null)
    val activeCheckoutPayment: StateFlow<PaymentEntity?> = _activeCheckoutPayment.asStateFlow()

    private val _checkoutStep = MutableStateFlow<CheckoutStep>(CheckoutStep.IDLE)
    val checkoutStep: StateFlow<CheckoutStep> = _checkoutStep.asStateFlow()

    private val _checkoutError = MutableStateFlow<String?>(null)
    val checkoutError: StateFlow<String?> = _checkoutError.asStateFlow()

    // Multisig multi-step approval
    private val _multisigApprovals = MutableStateFlow<List<String>>(emptyList()) // Signer names who approved
    val multisigApprovals: StateFlow<List<String>> = _multisigApprovals.asStateFlow()

    // Rotating keys
    private val _hsmKeyVersion = MutableStateFlow(1)
    val hsmKeyVersion: StateFlow<Int> = _hsmKeyVersion.asStateFlow()

    init {
        // Bootstrap template enterprise configuration with dummy data so it shines immediately!
        viewModelScope.launch {
            repository.bootstrapDemoDataIfEmpty()
            // Set first business wallet as initial selected
            val initialWallets = database.walletDao().getAllWallets()
            if (initialWallets.isNotEmpty()) {
                _selectedWalletAddress.value = initialWallets.first().address
            }
        }
    }

    fun setScreen(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun setSelectedWallet(address: String) {
        _selectedWalletAddress.value = address
    }

    fun updateSearchQuery(query: String) {
        _eventSearchQuery.value = query
    }

    // Interactive Action: Create custom Wallet
    fun generateMerchantWallet(label: String, type: String, network: String) {
        viewModelScope.launch {
            val mnemonic = CryptoEngine.generateMnemonic()
            val walletPath = when (type) {
                "CUSTOMER" -> "m/44'/60'/0'/0/${(10..99).random()}"
                "BUSINESS" -> "m/44'/60'/0'/0/${(1..9).random()}"
                "ESCROW" -> "m/44'/195'/0'/0/${(1..9).random()}"
                else -> "m/44'/60'/1'/0/${(1..9).random()}"
            }
            val initialBalance = if (type == "CUSTOMER") 1000.0 else 0.0
            
            repository.createWallet(
                label = label,
                type = type,
                network = network,
                mnemonic = mnemonic,
                path = walletPath,
                isMultisig = (type == "TREASURY"),
                coSigners = if (type == "TREASURY") listOf("0x83e2CoSigner...", "0x12a0CoSigner...") else emptyList(),
                initialBalance = initialBalance
            )
        }
    }

    // Trigger block mining on all simulated blockchains to keep balances indexing healthy
    fun triggerBlockMinedForAllNetworks() {
        viewModelScope.launch {
            BlockchainSimulator.mineNextBlock("Ethereum (ERC20)")
            BlockchainSimulator.mineNextBlock("TRON (TRC20)")
            BlockchainSimulator.mineNextBlock("Arbitrum (ERC20)")
            BlockchainSimulator.mineNextBlock("Binance Smart Chain")
            repository.createEventLog(
                eventType = "BLOCK_MINED_SIM",
                payload = "Dynamic multi-chain background node verification block added successfully across all active pools.",
                network = "GLOBAL"
            )
        }
    }

    // Rotate simulated vault HSM Cryptographic root master key
    fun rotateMasterHsmKey() {
        viewModelScope.launch {
            _hsmKeyVersion.value = _hsmKeyVersion.value + 1
            repository.createEventLog(
                eventType = "HSM_KEY_ROTATED",
                payload = "Root HSM master key rotated. Updated key version identifier to v${_hsmKeyVersion.value}.",
                network = "SYSTEM"
            )
        }
    }

    // Escrow Control actions
    fun releaseMerchantEscrow(paymentId: String) {
        viewModelScope.launch {
            val res = repository.releaseEscrow(paymentId)
            if (!res) {
                repository.createEventLog(
                    eventType = "SYSTEM_EXCEPTION",
                    payload = "Failed to release escrow for paymentId=$paymentId. Escrow terms may be pending or locked.",
                    network = "GLOBAL"
                )
            }
        }
    }

    fun refundMerchantEscrow(paymentId: String) {
        viewModelScope.launch {
            val res = repository.refundEscrow(paymentId)
            if (!res) {
                repository.createEventLog(
                    eventType = "SYSTEM_EXCEPTION",
                    payload = "Failed to refund escrow for paymentId=$paymentId. Security limits or missing recipient balance.",
                    network = "GLOBAL"
                )
            }
        }
    }

    // Commercial Commerce simulator
    fun initCheckoutFlow(amount: Double, assetName: String, isEscrow: Boolean = false) {
        viewModelScope.launch {
            _checkoutStep.value = CheckoutStep.IDLE
            _checkoutError.value = null
            _multisigApprovals.value = emptyList()

            val customerWallets = wallets.value.filter { it.type == "CUSTOMER" }
            val businessWallets = wallets.value.filter { it.type == if (isEscrow) "ESCROW" else "BUSINESS" }

            if (customerWallets.isEmpty() || businessWallets.isEmpty()) {
                _checkoutError.value = "Wallet simulation error: Require active customer and merchant treasury nodes."
                _checkoutStep.value = CheckoutStep.ERROR
                return@launch
            }

            val sender = customerWallets.first()
            val recipient = businessWallets.first()
            val network = if (isEscrow) "TRON (TRC20)" else "Ethereum (ERC20)"

            val payment = repository.createPayment(
                orderId = "ord_" + UUID.randomUUID().toString().substring(0, 8),
                merchantLabel = "CONNECT Portal Store",
                amount = amount,
                network = network,
                senderAddress = sender.address,
                recipientAddress = recipient.address,
                description = "Purchase of $assetName",
                isEscrow = isEscrow,
                splitConfig = if (isEscrow) "" else "CONNECT-Treasury:0.02,PartnerAccount:0.08" // 2% marketplace cuts for split demo
            )

            if (payment.status == "REJECTED") {
                _checkoutError.value = "Sovereign AML screening declined transaction: Sender audit score flag."
                _checkoutStep.value = CheckoutStep.ERROR
                return@launch
            }

            _activeCheckoutPayment.value = payment
            _checkoutStep.value = CheckoutStep.PROMPTING_SIGNATURE
        }
    }

    // Proceed to sign & finalize checkout
    fun executeCheckoutSignature() {
        val payment = _activeCheckoutPayment.value ?: return
        viewModelScope.launch {
            _checkoutStep.value = CheckoutStep.BROADCASTING
            
            // Execute payment process
            val privateKeyHex = "0x546b3f71cde89a2399efea7bbb16ec..."
            val success = repository.executePaymentAndBroadcast(payment.id, privateKeyHex)
            
            if (success) {
                // If it is a software asset, write the license metadata
                if (payment.description.contains("API") || payment.description.contains("Extension") || payment.description.contains("SaaS")) {
                    val key = "LCN-" + UUID.randomUUID().toString().uppercase().replace("-", "").substring(0, 16)
                    val license = LicenseEntity(
                        id = "lic_" + UUID.randomUUID().toString().substring(0, 8),
                        assetName = payment.description.replace("Purchase of ", ""),
                        licenseKey = key,
                        buyerAddress = payment.senderAddress,
                        paymentId = payment.id,
                        status = "ACTIVE",
                        deploymentInstruction = "Deploy binary with API connector. Digital verify key with Stablecoin-CONNECT compiler nodes.",
                        digitalSignature = CryptoEngine.signTransaction("$key|${payment.id}", "0x99281a_marketplace_signing_key_hex")
                    )
                    database.licenseDao().insertLicense(license)
                    repository.createEventLog(
                        eventType = "LICENSE_PURCHASED",
                        payload = "Software license issued to customer: ${payment.senderAddress}, KeyID=$key",
                        network = payment.network
                    )
                }

                _checkoutStep.value = CheckoutStep.COMPLETED
            } else {
                _checkoutError.value = "Transaction failed. Check sender gas configuration or USDT liquidity."
                _checkoutStep.value = CheckoutStep.ERROR
            }
        }
    }

    // Cancel dynamic checkout session
    fun cancelCheckout() {
        _activeCheckoutPayment.value = null
        _checkoutStep.value = CheckoutStep.IDLE
    }

    // Approve Multisig transaction for treasury wallets (co-signer approval mockup)
    fun requestMultisigSignerApproval(signerName: String) {
        val current = _multisigApprovals.value.toMutableList()
        if (!current.contains(signerName)) {
            current.add(signerName)
            _multisigApprovals.value = current
            
            viewModelScope.launch {
                repository.createEventLog(
                    eventType = "OWNERSHIP_TRANSFERRED",
                    payload = "Multisig signing signature collected from operator=$signerName",
                    network = "Arbitrum (ERC20)"
                )
                
                // If threshold met (2-of-3!)
                if (current.size >= 2) {
                    repository.createEventLog(
                        eventType = "ROYALTY_DISTRIBUTED",
                        payload = "Threshold signature met (2/3 approvals). Multisig Treasury withdrawal released.",
                        network = "Arbitrum (ERC20)"
                    )
                }
            }
        }
    }

    fun resetEventLogs() {
        viewModelScope.launch {
            database.eventLogDao().clearLogs()
            repository.bootstrapDemoDataIfEmpty()
        }
    }

    // Status state definitions
    enum class CheckoutStep {
        IDLE,
        PROMPTING_SIGNATURE,
        BROADCASTING,
        COMPLETED,
        ERROR
    }
}
