# EVM Wallet Android

Kotlin/Compose Android EVM wallet prototype for personal small-value use.

## What works in v1

- Local BIP39 wallet creation/import with `m/44'/60'/0'/0/0`.
- Android API 28+ so it can run on the connected MI 6 test phone.
- Android Keystore-backed encrypted wallet storage.
- Ethereum Mainnet and Sepolia RPC defaults.
- Native ETH balance and simple native ETH send.
- Single-tab WebView DApp browser.
- Standard EIP-1193 provider injection through AndroidX WebKit WebMessage.
- Per-origin DApp account permission.
- `eth_requestAccounts`, `eth_accounts`, `eth_chainId`, `personal_sign`, and `eth_sendTransaction`.
- Web3j 4.12.3 is pinned so local JVM tests run on JDK 17.

## Explicitly out of scope

- MetaMask compatibility flags such as `isMetaMask`.
- EIP-712 typed data signing.
- Tokens, NFTs, swaps, multi-account, MPC, cloud backup, and public-audit readiness.

## Build

Before changing SDK/API assumptions, capture the current physical test device profile:

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb devices -l
& $adb shell getprop ro.build.version.sdk
& $adb shell getprop ro.product.model
& $adb shell dumpsys webviewupdate
```

```powershell
.\gradlew.bat assembleDebug --no-daemon
```

The debug APK is written to `app\build\outputs\apk\debug\app-debug.apk`.

Current baseline device notes are in `docs/test-device.md`.
