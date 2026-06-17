package dev.mechrev.evmwallet

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.web3j.crypto.Credentials
import org.web3j.crypto.Keys
import org.web3j.crypto.MnemonicUtils
import java.security.SecureRandom

class WalletStore(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "wallet",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun hasWallet(): Boolean = prefs.contains(KEY_MNEMONIC)

    fun createWallet(): WalletAccount {
        val entropy = ByteArray(16)
        SecureRandom().nextBytes(entropy)
        val mnemonic = MnemonicUtils.generateMnemonic(entropy)
        prefs.edit().putString(KEY_MNEMONIC, mnemonic).apply()
        return account()
    }

    fun importWallet(mnemonic: String): WalletAccount {
        val normalized = mnemonic.trim().lowercase().replace(Regex("\\s+"), " ")
        require(MnemonicUtils.validateMnemonic(normalized)) { "Invalid BIP39 mnemonic" }
        prefs.edit().putString(KEY_MNEMONIC, normalized).apply()
        return account()
    }

    fun account(): WalletAccount {
        val mnemonic = prefs.getString(KEY_MNEMONIC, null) ?: error("Wallet is not initialized")
        val keyPair = WalletDerivation.keyPair(mnemonic)
        return WalletAccount(
            address = "0x" + Keys.getAddress(keyPair),
            mnemonic = mnemonic,
            credentials = Credentials.create(keyPair)
        )
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_MNEMONIC = "mnemonic"
    }
}

data class WalletAccount(
    val address: String,
    val mnemonic: String,
    val credentials: Credentials
)
