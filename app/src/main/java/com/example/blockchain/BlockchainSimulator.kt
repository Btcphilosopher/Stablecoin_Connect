package com.example.blockchain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class SimBlock(
    val height: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val blockHash: String,
    val previousHash: String,
    val network: String,
    val txCount: Int,
    val transactions: List<SimTx>
)

data class SimTx(
    val txHash: String,
    val fromAddress: String,
    val toAddress: String,
    val amount: Double,
    val fee: Double,
    val network: String,
    val isEscrow: Boolean = false,
    val status: String = "SUCCESS"
)

object BlockchainSimulator {
    private val _ethereumBlocks = MutableStateFlow<List<SimBlock>>(emptyList())
    val ethereumBlocks: StateFlow<List<SimBlock>> = _ethereumBlocks.asStateFlow()

    private val _tronBlocks = MutableStateFlow<List<SimBlock>>(emptyList())
    val tronBlocks: StateFlow<List<SimBlock>> = _tronBlocks.asStateFlow()

    private val _arbitrumBlocks = MutableStateFlow<List<SimBlock>>(emptyList())
    val arbitrumBlocks: StateFlow<List<SimBlock>> = _arbitrumBlocks.asStateFlow()

    private val _bscBlocks = MutableStateFlow<List<SimBlock>>(emptyList())
    val bscBlocks: StateFlow<List<SimBlock>> = _bscBlocks.asStateFlow()

    // Mempool
    private val mempool = mutableListOf<SimTx>()

    init {
        // Generate genesis blocks for each network
        generateGenesisBlocks()
    }

    private fun generateGenesisBlocks() {
        val now = System.currentTimeMillis()
        _ethereumBlocks.value = listOf(
            SimBlock(18500000L, now - 60000, "0xethgenesisblockhashcba72183", "0x00000000000000000000000", "Ethereum (ERC20)", 0, emptyList())
        )
        _tronBlocks.value = listOf(
            SimBlock(58000000L, now - 50000, "0xtrongenesisblockhashbba2a7e781", "0x00000000000000000000000", "TRON (TRC20)", 0, emptyList())
        )
        _arbitrumBlocks.value = listOf(
            SimBlock(145000000L, now - 40000, "0xarbgenesisblockhashb16e78cf91", "0x00000000000000000000000", "Arbitrum (ERC20)", 0, emptyList())
        )
        _bscBlocks.value = listOf(
            SimBlock(35000000L, now - 30000, "0xbscgenesisblockhashdd86e11c521", "0x00000000000000000000000", "Binance Smart Chain", 0, emptyList())
        )
    }

    fun getGasFeeEstimation(network: String): Double {
        return when (network) {
            "Ethereum (ERC20)" -> 4.50 // USDT
            "TRON (TRC20)" -> 1.10
            "Arbitrum (ERC20)" -> 0.15
            "Binance Smart Chain" -> 0.40
            else -> 0.50
        }
    }

    fun submitToMempool(tx: SimTx) {
        synchronized(mempool) {
            mempool.add(tx)
        }
    }

    fun mineNextBlock(network: String, forceTxs: List<SimTx> = emptyList()): SimBlock {
        val currentBlocks = when (network) {
            "Ethereum (ERC20)" -> _ethereumBlocks
            "TRON (TRC20)" -> _tronBlocks
            "Arbitrum (ERC20)" -> _arbitrumBlocks
            else -> _bscBlocks
        }

        val lastBlock = currentBlocks.value.first()
        val nextHeight = lastBlock.height + 1
        val parentHash = lastBlock.blockHash

        val activeTxs = mutableListOf<SimTx>()
        activeTxs.addAll(forceTxs)

        synchronized(mempool) {
            val networkTxs = mempool.filter { it.network == network }
            activeTxs.addAll(networkTxs)
            mempool.removeAll(networkTxs)
        }

        // Add some random background test transactions to make the explorer live and vivid!
        if (activeTxs.isEmpty()) {
            val randomTxCount = (1..3).random()
            for (i in 1..randomTxCount) {
                val mockAmount = (5..1500).random().toDouble()
                val mockFee = getGasFeeEstimation(network) * (0.9 + Math.random() * 0.2)
                val from = if (network == "TRON (TRC20)") "TY9bMockAddressX..." else "0x4b7MockAddressY..."
                val to = if (network == "TRON (TRC20)") "TM2bRecipientMock..." else "0x98fRecipientMock..."
                activeTxs.add(
                    SimTx(
                        txHash = "0x" + UUID.randomUUID().toString().replace("-", "").substring(0, 32),
                        fromAddress = from,
                        toAddress = to,
                        amount = mockAmount,
                        fee = mockFee,
                        network = network
                    )
                )
            }
        }

        val seedText = "$nextHeight|$parentHash|${System.currentTimeMillis()}"
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val blockHashHex = "0x" + md.digest(seedText.toByteArray()).joinToString("") { "%02x".format(it) }

        val newBlock = SimBlock(
            height = nextHeight,
            blockHash = blockHashHex,
            previousHash = parentHash,
            network = network,
            txCount = activeTxs.size,
            transactions = activeTxs
        )

        currentBlocks.value = listOf(newBlock) + currentBlocks.value
        return newBlock
    }
}
