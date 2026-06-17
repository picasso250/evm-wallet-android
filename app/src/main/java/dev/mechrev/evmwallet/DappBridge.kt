package dev.mechrev.evmwallet

import android.webkit.WebView
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import org.json.JSONArray
import org.json.JSONObject
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric
import java.math.BigInteger

class DappBridge(
    private val webView: WebView,
    private val chain: () -> ChainConfig,
    private val account: () -> WalletAccount?,
    private val isOriginAllowed: (String) -> Boolean,
    private val requestConnect: (String, (Boolean) -> Unit) -> Unit,
    private val requestPersonalSign: (String, String, (Boolean) -> Unit) -> Unit,
    private val requestTransaction: (String, DappTransaction, (Boolean) -> Unit) -> Unit,
    private val sendTransaction: suspend (DappTransaction) -> String
) {
    fun install(): Boolean {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) return false
        WebViewCompat.addWebMessageListener(
            webView,
            BRIDGE_NAME,
            setOf("*")
        ) { _, message, sourceOrigin, _, replyProxy ->
            handleMessage(sourceOrigin.toString(), message, replyProxy)
        }
        webView.evaluateJavascript(PROVIDER_SCRIPT, null)
        return true
    }

    private fun handleMessage(origin: String, message: WebMessageCompat, reply: JavaScriptReplyProxy) {
        val request = JSONObject(message.data ?: "{}")
        val id = request.opt("id")
        val method = request.optString("method")
        val params = request.optJSONArray("params") ?: JSONArray()
        fun ok(result: Any?) = reply.postMessage(json(id, result = result).toString())
        fun fail(code: Int, text: String) = reply.postMessage(json(id, error = JSONObject().put("code", code).put("message", text)).toString())

        when (method) {
            "eth_chainId" -> ok(chain().hexChainId)
            "eth_accounts" -> {
                val wallet = account()
                ok(if (wallet != null && isOriginAllowed(origin)) JSONArray().put(wallet.address) else JSONArray())
            }
            "eth_requestAccounts" -> {
                val wallet = account() ?: return fail(4100, "Wallet is not initialized")
                if (isOriginAllowed(origin)) return ok(JSONArray().put(wallet.address))
                requestConnect(origin) { accepted ->
                    if (accepted) ok(JSONArray().put(wallet.address)) else fail(4001, "User rejected request")
                }
            }
            "personal_sign" -> {
                val wallet = account() ?: return fail(4100, "Wallet is not initialized")
                if (!isOriginAllowed(origin)) return fail(4100, "Origin is not connected")
                val messageToSign = params.optString(0).ifBlank { params.optString(1) }
                requestPersonalSign(origin, messageToSign) { accepted ->
                    if (!accepted) return@requestPersonalSign fail(4001, "User rejected request")
                    val bytes = if (messageToSign.startsWith("0x")) Numeric.hexStringToByteArray(messageToSign) else messageToSign.toByteArray()
                    val signature = Sign.signPrefixedMessage(bytes, wallet.credentials.ecKeyPair)
                    ok(Numeric.toHexString(signature.r + signature.s + signature.v))
                }
            }
            "eth_sendTransaction" -> {
                if (account() == null) return fail(4100, "Wallet is not initialized")
                if (!isOriginAllowed(origin)) return fail(4100, "Origin is not connected")
                val tx = DappTransaction.from(params.optJSONObject(0) ?: JSONObject())
                requestTransaction(origin, tx) { accepted ->
                    if (!accepted) fail(4001, "User rejected request") else CoroutineLauncher.launch(
                        onError = { fail(-32000, it.message ?: "Transaction failed") },
                        block = { ok(sendTransaction(tx)) }
                    )
                }
            }
            "wallet_switchEthereumChain", "wallet_addEthereumChain" -> fail(4200, "$method is declared but not implemented in v1 UI")
            else -> fail(4200, "Unsupported method: $method")
        }
    }

    private fun json(id: Any?, result: Any? = null, error: JSONObject? = null): JSONObject {
        val obj = JSONObject().put("jsonrpc", "2.0").put("id", id)
        if (error != null) obj.put("error", error) else obj.put("result", result)
        return obj
    }

    companion object {
        private const val BRIDGE_NAME = "EvmWalletBridge"
        const val PROVIDER_SCRIPT = """
(() => {
  if (window.ethereum) return;
  let nextId = 1;
  const pending = new Map();
  const listeners = {};
  function emit(name, value) { (listeners[name] || []).forEach(fn => { try { fn(value); } catch (_) {} }); }
  window.EvmWalletBridge.onmessage = event => {
    const msg = JSON.parse(event.data);
    const p = pending.get(msg.id);
    if (!p) return;
    pending.delete(msg.id);
    if (msg.error) p.reject(Object.assign(new Error(msg.error.message), { code: msg.error.code }));
    else p.resolve(msg.result);
  };
  window.ethereum = {
    request(args) {
      const id = nextId++;
      window.EvmWalletBridge.postMessage(JSON.stringify({ jsonrpc: '2.0', id, method: args.method, params: args.params || [] }));
      return new Promise((resolve, reject) => pending.set(id, { resolve, reject }));
    },
    on(name, fn) { listeners[name] = listeners[name] || []; listeners[name].push(fn); },
    removeListener(name, fn) { listeners[name] = (listeners[name] || []).filter(x => x !== fn); },
    _emit: emit
  };
})();
"""
    }
}

data class DappTransaction(
    val to: String,
    val valueWei: BigInteger,
    val gasLimit: BigInteger,
    val data: String
) {
    companion object {
        fun from(json: JSONObject): DappTransaction = DappTransaction(
            to = json.optString("to"),
            valueWei = json.optString("value", "0x0").hexToBigInt(),
            gasLimit = json.optString("gas", json.optString("gasLimit", "0x5208")).hexToBigInt(),
            data = json.optString("data", "0x")
        )
    }
}

private fun String.hexToBigInt(): BigInteger {
    val clean = removePrefix("0x").ifBlank { "0" }
    return BigInteger(clean, 16)
}
