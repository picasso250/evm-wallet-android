package dev.mechrev.evmwallet

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WalletApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletApp() {
    val context = LocalContext.current
    val walletStore = remember { WalletStore(context) }
    val permissions = remember { PermissionStore(context) }
    var chain by remember { mutableStateOf(DefaultChains.sepolia) }
    var account by remember { mutableStateOf(runCatching { if (walletStore.hasWallet()) walletStore.account() else null }.getOrNull()) }
    var balance by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Ready") }
    var pendingConnect by remember { mutableStateOf<Pair<String, (Boolean) -> Unit>?>(null) }
    var pendingSign by remember { mutableStateOf<Triple<String, String, (Boolean) -> Unit>?>(null) }
    var pendingTx by remember { mutableStateOf<Triple<String, DappTransaction, (Boolean) -> Unit>?>(null) }

    LaunchedEffect(account?.address, chain) {
        val address = account?.address ?: return@LaunchedEffect
        balance = runCatching { RpcClient(chain).nativeBalance(address) }.getOrElse { "RPC error" }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("EVM Wallet") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WalletPanel(
                account = account,
                chain = chain,
                balance = balance,
                status = status,
                onCreate = {
                    account = walletStore.createWallet()
                    status = "Wallet created"
                },
                onImport = {
                    runCatching { account = walletStore.importWallet(it) }
                        .onSuccess { status = "Wallet imported" }
                        .onFailure { status = it.message ?: "Import failed" }
                },
                onChain = { chain = it },
                onSend = { to, amount ->
                    val wallet = account ?: return@WalletPanel
                    CoroutineLauncher.launch(
                        onError = { status = it.message ?: "Send failed" },
                        block = {
                            val hash = RpcClient(chain).sendNative(wallet.credentials, to, amount)
                            status = "Sent: $hash"
                            balance = RpcClient(chain).nativeBalance(wallet.address)
                        }
                    )
                }
            )
            BrowserPanel(
                account = { account },
                chain = { chain },
                permissions = permissions,
                onStatus = { status = it },
                onConnect = { origin, cb -> pendingConnect = origin to cb },
                onSign = { origin, message, cb -> pendingSign = Triple(origin, message, cb) },
                onTx = { origin, tx, cb -> pendingTx = Triple(origin, tx, cb) },
                onSendTx = { tx ->
                    val wallet = account ?: error("Wallet is not initialized")
                    RpcClient(chain).signAndSendTransaction(wallet.credentials, tx.to, tx.valueWei, tx.gasLimit, tx.data)
                }
            )
        }
    }

    pendingConnect?.let { (origin, cb) ->
        AlertDialog(
            onDismissRequest = { pendingConnect = null; cb(false) },
            title = { Text("Connect DApp") },
            text = { Text(origin) },
            confirmButton = {
                Button(onClick = {
                    permissions.allow(origin)
                    pendingConnect = null
                    cb(true)
                }) { Text("Allow") }
            },
            dismissButton = { TextButton(onClick = { pendingConnect = null; cb(false) }) { Text("Reject") } }
        )
    }
    pendingSign?.let { (origin, message, cb) ->
        AlertDialog(
            onDismissRequest = { pendingSign = null; cb(false) },
            title = { Text("Sign Message") },
            text = { Text("$origin\n\n$message", fontFamily = FontFamily.Monospace) },
            confirmButton = { Button(onClick = { pendingSign = null; cb(true) }) { Text("Sign") } },
            dismissButton = { TextButton(onClick = { pendingSign = null; cb(false) }) { Text("Reject") } }
        )
    }
    pendingTx?.let { (origin, tx, cb) ->
        AlertDialog(
            onDismissRequest = { pendingTx = null; cb(false) },
            title = { Text("Send Transaction") },
            text = {
                Text(
                    "$origin\n\nto: ${tx.to}\nvalueWei: ${tx.valueWei}\ngas: ${tx.gasLimit}\ndata: ${tx.data}",
                    fontFamily = FontFamily.Monospace
                )
            },
            confirmButton = { Button(onClick = { pendingTx = null; cb(true) }) { Text("Send") } },
            dismissButton = { TextButton(onClick = { pendingTx = null; cb(false) }) { Text("Reject") } }
        )
    }
}

@Composable
fun WalletPanel(
    account: WalletAccount?,
    chain: ChainConfig,
    balance: String,
    status: String,
    onCreate: () -> Unit,
    onImport: (String) -> Unit,
    onChain: (ChainConfig) -> Unit,
    onSend: (String, BigDecimal) -> Unit
) {
    var mnemonic by remember { mutableStateOf("") }
    var to by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onCreate) { Text("Create") }
            Button(onClick = { menuOpen = true }) { Text(chain.name) }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DefaultChains.all.forEach {
                    DropdownMenuItem(text = { Text(it.name) }, onClick = { onChain(it); menuOpen = false })
                }
            }
        }
        Text("Address: ${account?.address ?: "not initialized"}", fontFamily = FontFamily.Monospace)
        Text("Balance: ${if (balance.isBlank()) "-" else balance} ${chain.symbol}")
        Text("Status: $status")
        if (account == null) {
            OutlinedTextField(mnemonic, { mnemonic = it }, modifier = Modifier.fillMaxWidth(), label = { Text("BIP39 mnemonic") })
            Button(onClick = { onImport(mnemonic) }) { Text("Import") }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(to, { to = it }, modifier = Modifier.weight(1f), label = { Text("To") })
                OutlinedTextField(amount, { amount = it }, modifier = Modifier.width(120.dp), label = { Text("ETH") })
                Button(onClick = { runCatching { BigDecimal(amount) }.onSuccess { onSend(to, it) } }) { Text("Send") }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserPanel(
    account: () -> WalletAccount?,
    chain: () -> ChainConfig,
    permissions: PermissionStore,
    onStatus: (String) -> Unit,
    onConnect: (String, (Boolean) -> Unit) -> Unit,
    onSign: (String, String, (Boolean) -> Unit) -> Unit,
    onTx: (String, DappTransaction, (Boolean) -> Unit) -> Unit,
    onSendTx: suspend (DappTransaction) -> String
) {
    var url by remember { mutableStateOf("https://app.uniswap.org/") }
    var webView by remember { mutableStateOf<WebView?>(null) }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(url, { url = it }, modifier = Modifier.weight(1f), singleLine = true, label = { Text("DApp URL") })
            Button(onClick = { webView?.loadUrl(normalizeUrl(url)) }) { Text("Go") }
        }
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(480.dp),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, pageUrl: String) {
                            view.evaluateJavascript(DappBridge.PROVIDER_SCRIPT, null)
                        }
                    }
                    val bridgeInstalled = DappBridge(
                        webView = this,
                        chain = chain,
                        account = account,
                        isOriginAllowed = permissions::isAllowed,
                        requestConnect = onConnect,
                        requestPersonalSign = onSign,
                        requestTransaction = onTx,
                        sendTransaction = onSendTx
                    ).install()
                    if (!bridgeInstalled) onStatus("This WebView does not support WebMessageListener")
                    loadUrl(normalizeUrl(url))
                    webView = this
                }
            }
        )
    }
}

private fun normalizeUrl(input: String): String {
    val trimmed = input.trim()
    return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "https://$trimmed"
}
