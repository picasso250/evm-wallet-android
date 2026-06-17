package dev.mechrev.evmwallet

data class ChainConfig(
    val name: String,
    val chainId: Long,
    val rpcUrl: String,
    val symbol: String
) {
    val hexChainId: String = "0x" + chainId.toString(16)
}

object DefaultChains {
    val ethereum = ChainConfig(
        name = "Ethereum",
        chainId = 1L,
        rpcUrl = "https://ethereum.publicnode.com",
        symbol = "ETH"
    )

    val sepolia = ChainConfig(
        name = "Sepolia",
        chainId = 11155111L,
        rpcUrl = "https://ethereum-sepolia.publicnode.com",
        symbol = "ETH"
    )

    val all = listOf(ethereum, sepolia)
}
