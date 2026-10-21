# 验证范围 / Validation scope

公开版本 2.21.1 基于现存 2.21 桌面源码整理，仅修改公开版版本号、移除开发者服务器地址、更新源码声明及构建方式。当前已安装设备未被本次发布过程升级或改动。

Public 2.21.1 is a publication build of local 2.21. Packaging changes do not constitute fresh device acceptance. This publication does not operate the ESP32 or attached storage.

自动检查 / Automated checks:

- Clean Java compile, D8 conversion, resource packaging, APK alignment and signature verification.
- Bundled SQLite header and complete ZIP integrity; required title-index and license assets.
- `tools/test.py`: 2024/2026 daily solar bounds, exact sunrise/sunset transitions, leap day, date rollover, timezone independence and manual schedule modes.
- Pre-publication source scan for private key formats, credential patterns and developer LAN addresses.

历史实机证据 / Historical device evidence:

- M13 上运行过桌面、独立影视播放、屏保日夜模式，用户确认过 iPhone 视频 App 的投屏画面和声音。
- 仓库截图为旧版本真实截图，见 README 版本标注；不是当前公开版的新验收。
- 没有证明：其他盒子兼容性、全部 Android 版本、AirPlay 镜像、Root、HDMI-CEC、原厂开机动画替换。
- 本仓库只包含语音独立应用的启动与预热入口，不包含识别引擎；不宣称裸装即可收音或语音搜索。

Release checksums are in the release assets. Passing a desktop build does not prove TV picture, audio or remote-control behavior on a new device.

本次实际结果 / Publication result: **3,660 assertions PASS**, full APK build PASS, signature verification PASS; signer matched local 2.21. Public release APK SHA256: `1d7e898320414d59f03a160a66431078a59eb812fa8cdc840f469dab504d222f`.
