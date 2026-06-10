package com.example.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallets")
    fun getAllWalletsFlow(): Flow<List<WalletEntity>>

    @Query("SELECT * FROM wallets")
    suspend fun getAllWallets(): List<WalletEntity>

    @Query("SELECT * FROM wallets WHERE address = :address LIMIT 1")
    suspend fun getWalletByAddress(address: String): WalletEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallet(wallet: WalletEntity)

    @Update
    suspend fun updateWallet(wallet: WalletEntity)

    @Query("UPDATE wallets SET balance = :balance WHERE address = :address")
    suspend fun updateBalance(address: String, balance: Double)

    @Delete
    suspend fun deleteWallet(wallet: WalletEntity)
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY createdAt DESC")
    fun getAllPaymentsFlow(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE id = :id LIMIT 1")
    suspend fun getPaymentById(id: String): PaymentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)

    @Update
    suspend fun updatePayment(payment: PaymentEntity)

    @Query("UPDATE payments SET status = :status, txHash = :txHash WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, txHash: String)

    @Query("UPDATE payments SET escrowStatus = :escrowStatus WHERE id = :id")
    suspend fun updateEscrowStatus(id: String, escrowStatus: String)
}

@Dao
interface LicenseDao {
    @Query("SELECT * FROM licenses ORDER BY purchasedAt DESC")
    fun getAllLicensesFlow(): Flow<List<LicenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLicense(license: LicenseEntity)
}

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions ORDER BY nextBillingDate ASC")
    fun getAllSubscriptionsFlow(): Flow<List<SubscriptionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: SubscriptionEntity)

    @Update
    suspend fun updateSubscription(subscription: SubscriptionEntity)
}

@Dao
interface EventLogDao {
    @Query("SELECT * FROM event_logs ORDER BY timestamp DESC")
    fun getAllEventLogsFlow(): Flow<List<EventLogEntity>>

    @Query("SELECT * FROM event_logs ORDER BY id DESC LIMIT 1")
    suspend fun getLastLog(): EventLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: EventLogEntity)

    @Query("DELETE FROM event_logs")
    suspend fun clearLogs()
}
