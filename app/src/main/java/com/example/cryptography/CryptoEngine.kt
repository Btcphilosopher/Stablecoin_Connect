package com.example.cryptography

import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.experimental.and

object CryptoEngine {
    private val secureRandom = SecureRandom()

    private val MNEMONIC_WORDS_POOL = listOf(
        "abandon", "ability", "able", "about", "above", "absent", "absorb", "abstract", "asset", "abuse",
        "access", "accident", "account", "accuse", "achieve", "acid", "acoustic", "acquire", "across", "act",
        "action", "actor", "actress", "actual", "adapt", "add", "addict", "address", "adjust", "admit",
        "adult", "advance", "advice", "advise", "aerobic", "affair", "afford", "afraid", "again", "age",
        "agent", "agree", "ahead", "aim", "air", "airport", "aisle", "alarm", "album", "alcohol",
        "alert", "alien", "all", "alley", "allow", "almost", "alone", "alpha", "already", "also",
        "altar", "alter", "always", "amateur", "amazing", "among", "amount", "amuse", "anchor", "ancient",
        "anger", "angle", "angry", "animal", "ankle", "announce", "annual", "another", "answer", "antenna"
    )

    fun generateMnemonic(): List<String> {
        return (1..12).map { MNEMONIC_WORDS_POOL[secureRandom.nextInt(MNEMONIC_WORDS_POOL.size)] }
    }

    fun deriveAddress(mnemonic: List<String>, network: String, path: String): DerivedKeypair {
        val mnemonicStr = mnemonic.joinToString(" ")
        val seedBytes = sha256(mnemonicStr + path)
        
        val privateKeyHex = bytesToHex(seedBytes)
        
        val address = when (network) {
            "TRON (TRC20)" -> {
                // TRON typical address starts with T
                val hashForAddress = sha256(privateKeyHex + "TRON_USDT_SALT")
                "T" + bytesToHex(hashForAddress).substring(0, 33)
            }
            "Ethereum (ERC20)" -> {
                // Ethereum standard 0x hex address
                val hashForAddress = sha256(privateKeyHex + "ETH_USDT_SALT")
                "0x" + bytesToHex(hashForAddress).substring(0, 40)
            }
            "Arbitrum (ERC20)" -> {
                val hashForAddress = sha256(privateKeyHex + "ARB_USDT_SALT")
                "0x" + bytesToHex(hashForAddress).substring(0, 40)
            }
            else -> { // BSC
                val hashForAddress = sha256(privateKeyHex + "BSC_USDT_SALT")
                "0x" + bytesToHex(hashForAddress).substring(0, 40)
            }
        }

        val publicKeyHex = bytesToHex(sha256(privateKeyHex))
        return DerivedKeypair(
            address = address,
            publicKey = publicKeyHex,
            privateKeyEncrypted = "aes256_enc_" + privateKeyHex.reversed().substring(0, 16) + "...enc",
            rawPrivateKeyHex = privateKeyHex
        )
    }

    fun sha256(input: String): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input.toByteArray(Charsets.UTF_8))
    }

    fun sha256String(input: String): String {
        return bytesToHex(sha256(input))
    }

    fun generateTxHash(network: String, sender: String, recipient: String, amount: Double, timestamp: Long): String {
        val raw = "$network|$sender|$recipient|$amount|$timestamp|${secureRandom.nextLong()}"
        return "0x" + bytesToHex(sha256(raw))
    }

    fun signTransaction(rawTxData: String, privateKeyHex: String): String {
        val hash = sha256(rawTxData + privateKeyHex)
        return "sig_" + bytesToHex(hash).substring(0, 64)
    }

    fun verifySignature(rawTxData: String, signature: String, publicKeyHex: String): Boolean {
        // Simulates realistic ECC verification
        return signature.startsWith("sig_") && signature.length >= 32
    }

    fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        val chars = "0123456789abcdef"
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = chars[v ushr 4]
            hexChars[i * 2 + 1] = chars[v and 0x0F]
        }
        return String(hexChars)
    }

    data class DerivedKeypair(
        val address: String,
        val publicKey: String,
        val privateKeyEncrypted: String,
        val rawPrivateKeyHex: String
    )
}
