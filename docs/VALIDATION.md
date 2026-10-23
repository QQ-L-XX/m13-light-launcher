# 验证范围 / Validation scope

## 2.28

2026-10-23 在 M13 安装 versionCode 42。周五的实际屏保显示“下周一课程 / 周一 · 10/26”和七节课，界面已检查。原有七天私人课表回读一致，保留现有影视网格和语音联动源码；这次没有重新验收全部语音流程。公开截图另用示例课程拍摄，拍摄后已恢复私人课表。

Automated checks: 3,660 solar + 1,022 lyric + 44 timetable assertions passed (4,726 total), including Friday/Saturday/Sunday selection, ordinary weekdays and year boundaries. The raw daily status remains unchanged; `display` gives the effective screen selection. The speech engine remains an optional separate package.

完整 APK SHA256: `93105f574d0384b37700db6559a67e264c357344a34a86ab3802edff7b054e33`.

## 2.24

2026-10-22 在开发用 MagicBox M13 / Android API 22 安装 versionCode 38。系统实际屏保服务运行并绘制新增课程行；日间、夜间画面均已人工检查。七天课程配置回读匹配，恢复为日出日落自动配色，网易云私有账号会话跨更新仍有效。个人课表、实机课程截图与账号数据不随本仓库分发。

Current 2.24 checks: full APK compilation/signature/ZIP integrity; 3,660 solar assertions; 1,022 lyric boundary assertions; 28 timetable date/rollover assertions. Day/night layout and the actual dream service were checked on M13. See MUSIC-VALIDATION.md for member-track and playback checks. This does not establish compatibility with other boxes or every music/video source.

2.24 完整 APK SHA256: `facfac40ee264180791019770bbb7c55a83e6af31c795381151a410ced77c941`.

## 2.21.1 historical publication checks

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
