package dev.mechrev.evmwallet

import org.web3j.crypto.Bip32ECKeyPair
import org.web3j.crypto.Keys
import org.web3j.crypto.MnemonicUtils

object WalletDerivation {
    fun keyPair(mnemonic: String): Bip32ECKeyPair {
        val seed = MnemonicUtils.generateSeed(mnemonic, "")
        val master = Bip32ECKeyPair.generateKeyPair(seed)
        return Bip32ECKeyPair.deriveKeyPair(master, DERIVATION_PATH)
    }

    fun address(mnemonic: String): String = "0x" + Keys.getAddress(keyPair(mnemonic))

    private val DERIVATION_PATH = intArrayOf(
        44 or Bip32ECKeyPair.HARDENED_BIT,
        60 or Bip32ECKeyPair.HARDENED_BIT,
        0 or Bip32ECKeyPair.HARDENED_BIT,
        0,
        0
    )
}
