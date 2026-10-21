# 从源码构建 / Build from source

环境 / Requirements: Python 3.11+, JDK 21, Android SDK Platform 22, Android Build Tools 35.0.0. Install SDK components from the official Android SDK distribution; toolchains are not vendored.

Windows PowerShell example（按本机路径修改）：

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-21'
$env:ANDROID_HOME = 'C:\path\to\Android\Sdk'
python tools/test.py
python tools/build.py
```

SDK 默认从 `$env:ANDROID_HOME/platforms/android-22/android.jar` 与 `build-tools/35.0.0` 查找。如你的 SDK 使用不同目录结构，可显式指定：

```powershell
$env:ANDROID_JAR = 'C:\path\to\android.jar'
$env:ANDROID_BUILD_TOOLS = 'C:\path\to\build-tools\35.0.0'
```

On macOS/Linux, export the same variables and run `python3 tools/build.py`. The script selects platform tool suffixes automatically; cross-platform builds have not been device-validated.

## 签名 / Signing

默认生成 `.signing/debug.jks`，仅适合开发，不可更新正式签名包。`dist/` 下输出完整 APK 及 SHA256。忽略目录中含私钥，不要上传。

For your own release key, set `M13_KEYSTORE`, `M13_KEY_ALIAS`, `M13_STORE_PASSWORD` and `M13_KEY_PASSWORD`. Passwords are passed to apksigner via environment references, never embedded in source. Keep all four values outside Git. Missing release credentials fail the build; there is no silent fallback to debug signing.

APK 含完整 SQLite 片名索引，aapt 将 `title-index.db.gz` 解压为 APK 内的 `title-index.db`。首次运行会复制至应用数据库，需预留约 160 MB 以上空间，另留缓存余量。该事实名称索引和别名用于离线拼音匹配；来源与覆盖范围见 assets 的元数据文件。

No Gradle or Maven dependencies are required: source is compiled with javac, dexed with D8, packaged by aapt and signed by apksigner. The optional voice companion is not part of this build.
