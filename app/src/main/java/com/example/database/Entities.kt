package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey val address: String,
    val label: String,
    val type: String, // BUSINESS, CUSTOMER, ESCROW, TREASURY, MARKETPLACE, SETTLEMENT, COLD_STORAGE
    val network: String, // TRON (TRC20), Ethereum (ERC20), Arbitrum (ERC20), BSC (BEP20)
    val balance: Double, // in USDT
    val publicKey: String,
    val privateKeyEncrypted: String, // HD derivation path or mock encrypted payload
    val isMultisig: Boolean,
    val requiredApprovals: Int = 1,
    val coSignersJson: String = "[]", // JSON string list of addresses
    val kycStatus: String = "APPROVED", // APPROVED, PENDING, REJECTED, UNVERIFIED
    val amlRiskScore: Double = 0.02 // 0.0 to 1.0 risk score
)

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey val id: String, // pay_xxxx
    val orderId: String,
    val merchantLabel: String,
    val amount: Double, // in USDT
    val status: String, // CREATED, PENDING, CONFIRMED, REJECTED, EXPIRED, SETTLED
    val network: String,
    val senderAddress: String,
    val recipientAddress: String,
    val txHash: String = "",
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isEscrow: Boolean = false,
    val escrowStatus: String = "NONE", // NONE, HELD, RELEASED, REFUNDED
    val escrowReleaseDate: Long = 0L,
    val splitConfigJson: String = "" // "address1:0.95,address2:0.05"
)

@Entity(tableName = "licenses")
data class LicenseEntity(
    @PrimaryKey val id: String, // lic_xxxx
    val assetName: String,
    val licenseKey: String,
    val buyerAddress: String,
    val paymentId: String,
    val status: String, // ACTIVE, REVOKED, EXPIRED
    val purchasedAt: Long = System.currentTimeMillis(),
    val durationDays: Int = 365,
    val versionHistory: String = "v1.0.0", // comma-separated
    val deploymentInstruction: String = "",
    val digitalSignature: String = ""
)

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey val id: String, // sub_xxxx
    val planName: String,
    val customerAddress: String,
    val pricePerPeriod: Double, // in USDT
    val periodDays: Int = 30,
    val status: String, // ACTIVE, OUTSTANDING, SUSPENDED
    val lastBillingDate: Long = System.currentTimeMillis(),
    val nextBillingDate: Long = System.currentTimeMillis() + (30 * 24 * 3600 * 1000L)
)

@Entity(tableName = "event_logs")
data class EventLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val eventType: String, // PAYMENT_CREATED, PAYMENT_CONFIRMED, ESCROW_RELEASED, etc.
    val timestamp: Long = System.currentTimeMillis(),
    val payloadJson: String, // Arbitrary data for explanation
    val network: String,
    val txHash: String = "",
    val integrityHash: String // SHA256 of index + previous_hash + payload for immutability auditing
)
