package dev.mechrev.evmwallet

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.web3j.crypto.Credentials
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.http.HttpService
import org.web3j.tx.RawTransactionManager
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import java.math.BigDecimal
import java.math.BigInteger

class RpcClient(private val chain: ChainConfig) {
    private val web3j: Web3j = Web3j.build(HttpService(chain.rpcUrl))

    suspend fun balanceWei(address: String): BigInteger = withContext(Dispatchers.IO) {
        web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send().balance
    }

    suspend fun nativeBalance(address: String): String {
        val eth = Convert.fromWei(BigDecimal(balanceWei(address)), Convert.Unit.ETHER)
        return eth.stripTrailingZeros().toPlainString()
    }

    suspend fun gasPrice(): BigInteger = withContext(Dispatchers.IO) {
        web3j.ethGasPrice().send().gasPrice
    }

    suspend fun nonce(address: String): BigInteger = withContext(Dispatchers.IO) {
        web3j.ethGetTransactionCount(address, DefaultBlockParameterName.PENDING).send().transactionCount
    }

    suspend fun sendNative(
        credentials: Credentials,
        to: String,
        amountEth: BigDecimal
    ): String = withContext(Dispatchers.IO) {
        val manager = RawTransactionManager(web3j, credentials, chain.chainId)
        val result = manager.sendTransaction(
            gasPrice(),
            BigInteger.valueOf(21_000),
            to,
            "",
            Convert.toWei(amountEth, Convert.Unit.ETHER).toBigIntegerExact()
        )
        if (result.hasError()) error(result.error.message)
        result.transactionHash
    }

    suspend fun signAndSendTransaction(
        credentials: Credentials,
        to: String,
        valueWei: BigInteger,
        gasLimit: BigInteger,
        data: String
    ): String = withContext(Dispatchers.IO) {
        val raw = RawTransaction.createTransaction(
            nonce(credentials.address),
            gasPrice(),
            gasLimit,
            to,
            valueWei,
            data.ifBlank { "0x" }
        )
        val signed = TransactionEncoder.signMessage(raw, chain.chainId, credentials)
        val hex = Numeric.toHexString(signed)
        val result = web3j.ethSendRawTransaction(hex).send()
        if (result.hasError()) error(result.error.message)
        result.transactionHash
    }
}
