package dev.mechrev.evmwallet

import org.junit.Assert.assertEquals
import org.junit.Test

class WalletDerivationTest {
    @Test
    fun derivesHardhatDefaultAccount() {
        val mnemonic = "test test test test test test test test test test test junk"
        assertEquals(
            "0xf39fd6e51aad88f6f4ce6ab8827279cfffb92266",
            WalletDerivation.address(mnemonic)
        )
    }

    @Test
    fun defaultChainsUseExpectedIds() {
        assertEquals("0x1", DefaultChains.ethereum.hexChainId)
        assertEquals("0xaa36a7", DefaultChains.sepolia.hexChainId)
    }
}
