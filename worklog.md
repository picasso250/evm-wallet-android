# Worklog

- Created Kotlin/Compose Android project under `C:\Users\MECHREV\projects\evm-wallet-android`.
- Generated Gradle wrapper with Gradle 8.13.
- Fixed Windows/AGP resource merge race by limiting Gradle workers to 1.
- Fixed Kotlin/Javac target mismatch by setting Java 17 compile options and Kotlin JVM toolchain.
- Fixed Web3j Android packaging conflicts by excluding duplicate `META-INF` metadata.
- Pinned Web3j to 4.12.3 because 4.14.0 ships Java 21 class files and breaks local JDK 17 unit tests.
- Lowered `minSdk` from 29 to 28 after the connected MI 6 reported Android 9 / API 28.
- Installed debug APK successfully on device `aa29645` after lowering `minSdk` to 28.
- Added `docs/test-device.md` to record physical test device parameters before making SDK/WebView/install assumptions.
- `.\gradlew.bat assembleDebug --no-daemon` succeeds and produces a debug APK.
