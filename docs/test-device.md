# Test Device Baseline

This file records the physical device used for first-install validation. Update it whenever the primary test phone changes.

## Device

- Model: MI 6
- Device codename: sagit
- Android: 9
- API level: 28
- MIUI: V11
- ABI list: arm64-v8a, armeabi-v7a, armeabi
- Screen: 1080x1920
- Density: 480 dpi
- WebView package: com.google.android.webview
- WebView version: 138.0.7204.179

## Baseline Commands

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb devices -l
& $adb shell getprop ro.product.model
& $adb shell getprop ro.product.device
& $adb shell getprop ro.build.version.release
& $adb shell getprop ro.build.version.sdk
& $adb shell getprop ro.miui.ui.version.name
& $adb shell getprop ro.product.cpu.abilist
& $adb shell wm size
& $adb shell wm density
& $adb shell dumpsys webviewupdate
```

## Lesson

Before choosing Android `minSdk`, WebView bridge strategy, ABI assumptions, or install flow, capture the physical test device profile first. The first install attempt failed because the app declared `minSdk = 29` while the connected MI 6 is API 28.
