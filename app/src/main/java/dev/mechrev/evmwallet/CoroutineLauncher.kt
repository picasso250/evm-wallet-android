package dev.mechrev.evmwallet

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object CoroutineLauncher {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun launch(onError: (Throwable) -> Unit, block: suspend () -> Unit) {
        scope.launch {
            runCatching { block() }.onFailure(onError)
        }
    }
}
