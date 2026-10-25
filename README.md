<div align="center">

<img src="docs/images/banner.svg" alt="Light Launcher — A quieter home for your TV" width="100%" />

# 轻桌面 · M13 Light Launcher

**看电影、听音乐，让天气和明日课程留在屏保里。**
A minimal TV home for video, music, casting and a crayon weather & class calendar.

[![Release](https://img.shields.io/github/v/release/QQ-L-XX/m13-light-launcher?color=13bdeb)](https://github.com/QQ-L-XX/m13-light-launcher/releases/latest)
![Android](https://img.shields.io/badge/Android-5.0%2B-43b883)
![Native Java](https://img.shields.io/badge/UI-native%20Java-7289ff)
[![License](https://img.shields.io/badge/license-AGPL--3.0-ea487e)](LICENSE)

[下载安装 / Download](https://github.com/QQ-L-XX/m13-light-launcher/releases/latest) · [安装说明 / Installation](docs/INSTALL.md) · [构建 / Build](docs/BUILD.md) · [问题反馈 / Issues](https://github.com/QQ-L-XX/m13-light-launcher/issues)

</div>

## 一个更安静的客厅 / A calmer living room

为天猫魔盒 M13 改造的原生 Android 桌面：蓝红渐变、遥控器操作，把影视、轻音乐和投屏放在同一张首页。待机时，蜡笔日历显示福州今明天气及横向明日课程，并按日出日落切换配色。电脑关机后仍能独立运行；在线影视、音乐和天气需要盒子联网。

A native Java launcher developed on a MagicBox M13: video, music and casting on the home screen, with a crayon calendar, two-day weather and tomorrow's classes during idle time. It runs on the box without a PC server; online media and weather require internet. This is an independent community adaptation, not an official LibreTV, Apple, NetEase or Tmall product.

<img src="docs/images/home-2.33.png" alt="2.33 实机首页：清晰焦点与极简两行卡片 / M13 home with a clear focus border" width="100%" />

*以下截图均为 2026-10-25 的 2.33 M13 实机。课程编辑与日夜屏保均使用示例课表，不含姓名、班级、私人课程、账号或登录二维码。All screenshots below show 2.33 on M13. Timetables use demo data. Third-party app icons belong to their owners.*

## 2.33：更顺手，也更好管理 / Better TV controls

- **遥控器与画面**：清晰焦点、长按排序、返回后保留位置，四档电视边距减少裁字。Clear focus, home ordering, retained navigation and overscan margins.
- **轻音乐**：极简全屏、四档歌词字号、顺序／单曲循环／随机播放、睡眠定时和重试。Fullscreen music, lyric sizes, playback modes, sleep pause and retries.
- **课程与屏保**：遥控器编辑课表，支持指定日期调课和放假；天气缓存标注时间。Remote-editable courses, date exceptions and labelled weather cache.
- **设置与更新**：本机备份、恢复与撤销；盒子直接检查 GitHub 更新并保留安装包。Local settings backup/restore and public GitHub update discovery.

从首页“设置 → 画面 / 课程 / 备份 / 更新”进入。在线升级仅接受较新的正式版，下载后检查大小、SHA-256、包名、签名和版本号，再交由系统确认安装。完整的在线下载升级到更高版本尚待后续版本验证。此 M13 无系统文件选择器，当前验证的是本机设置备份；APK 保留不代表能绕过 Android 的降级限制。

Open Settings → Display / Courses / Backup / Updates. The updater validates package integrity, identity and version before requesting installation. Update discovery and installer opening are verified; downloading and installing a future newer release is still pending. Local backup works on this M13; file export requires a system document picker.

[设置使用说明 / Settings guide](docs/SETTINGS.md) · [验证记录 / Validation](docs/VALIDATION.md)

<table><tr><td width="50%"><img src="docs/images/apps-2.33.png" alt="2.33 应用页遥控器焦点 / Apps navigation" /></td><td width="50%"><img src="docs/images/settings-2.33.png" alt="2.33 画面、课程、备份与更新入口 / On-box settings" /></td></tr><tr><td align="center">应用与焦点 / Apps & focus</td><td align="center">盒子上的设置 / On-box settings</td></tr></table>

## 轻音乐 / Music on the box

从首页打开轻音乐，直接搜歌曲或歌单，查看行歌词、收藏、播放历史和续听。用网易云 App 扫码授权后，按账号实际权限获取歌曲；播放区提供逐曲歌词时间校准、四档字号与全屏歌词，支持顺序播放、单曲循环、随机播放和睡眠定时。搜索、歌词和音频都由盒子获取，不需要电脑常开。

Search tracks and playlists, follow line lyrics, save favorites and resume listening directly on the box. NetEase QR login uses your account's actual rights; per-track lyric timing and size can be adjusted. Fullscreen mode, sequential/repeat/shuffle playback and sleep pause timers are included. Availability is controlled by the provider. See [music features and limits](docs/MUSIC.md).

<table><tr><td width="50%"><img src="docs/images/music-2.33.png" alt="2.33 轻音乐搜索与播放 / Music search and playback" /></td><td width="50%"><img src="docs/images/music-fullscreen-2.33.png" alt="2.33 极简全屏音乐 / Minimal fullscreen music" /></td></tr><tr><td align="center">列表播放 / Music library</td><td align="center">全屏播放 / Fullscreen player</td></tr></table>

## 明天有安排，屏保看得见 / Tomorrow at a glance

日历和天气区上移，底部七节课程横向排列，区分上午与下午；明日日期按北京时间自动更新。日间是浅色纸张，夜间是深蓝底色，支持日出日落自动切换。以下课表仅用于展示，公开安装包不附带个人课程。

A seven-lesson row separates morning and afternoon beneath the calendar and weather. The calendar uses Asia/Shanghai; day and night palettes can follow sunrise and sunset. Both images below use the same sample timetable. [Configure your own classes](docs/TIMETABLE.md).

<table><tr><td width="50%"><img src="docs/images/timetable-day-demo-2.33.png" alt="日间课程屏保，示例课表 / Day screensaver with demo classes" /></td><td width="50%"><img src="docs/images/timetable-night-demo-2.33.png" alt="夜间课程屏保，示例课表 / Night screensaver with demo classes" /></td></tr><tr><td align="center">日间 · 示例课程 / Day · Demo classes</td><td align="center">夜间 · 示例课程 / Night · Demo classes</td></tr></table>

## 功能 / Features

| 模块 / Module | 体验 / Experience |
|---|---|
| 极简桌面 / Home | 蓝红配色、大卡片、遥控器排序、位置保留和电视边距。TV focus, ordering, preserved navigation and safe margins. |
| 影视 / Video | 海报首页、分类、中文模糊搜索、拼音与首字母搜索（如 `lldq`）、独立分页、观看记录。Posters, filters, fuzzy/pinyin/initial search, pagination and local history. |
| 片源合并 / Grouping | 同名同年作品合并为海报，进入后选择片源；不同年份、续集等保留区分。Group compatible titles while retaining provider variants. |
| 轻音乐 / Music | 独立搜歌、歌单、网易云扫码登录、账号权限播放、行歌词与校准、收藏及续听。On-device music and NetEase QR account login; see [music scope](docs/MUSIC.md). |
| 视频投屏 / Casting | 同一局域网中的 DLNA/UPnP 视频 App 投屏，设备名“轻桌面 M13”。Video casting; **not AirPlay screen mirroring**. |
| 蜡笔屏保 / Screensaver | 日历、福州今明天气、横向明日课程，按日出日落切换日夜配色。Calendar, two-day weather, tomorrow's classes and sunrise/sunset themes. |
| 后台管理 / Background | 一键后台清理；桌面管理权限下可对指定应用按需启用，检测到媒体活动时保留后台。Playback-aware cleanup and optional on-demand app policies. |

日出日落在盒子离线计算，默认福州（26.0745°N, 119.2965°E），使用北京时间。天气通过 Open-Meteo 获取；太阳时刻为当地估算值，不能作为精密观测时间。Solar switching works offline; weather does not.

屏保底部可显示课程：**周一至周四显示明天，周五至周日直接显示下周一**，标题明确标为“下周一课程”，并显示周一的实际日期。课程表保存在盒子应用私有区，默认安装包不包含个人课程、姓名或班级。支持在盒子上直接编辑每周课表、指定日期调课与放假。[课程配置 / Timetable configuration](docs/TIMETABLE.md). Monday–Thursday shows tomorrow; Friday–Sunday shows the upcoming Monday with an explicit label and date. Personal timetables stay on-device.

当前公开版本 **2.33** 包含 2.32 的首页排序、位置保留与全屏音乐，以及本次设置、课程与播放优化。保留现有影视网格与可选语音独立包联动；语音引擎不包含在此 APK 中。Version 2.33 includes the unpublished 2.32 improvements. The speech engine remains an optional separate companion.

<details><summary>遥控器编辑课程 · 示例 / Remote course editor · Demo</summary>

<img src="docs/images/course-editor-demo-2.33.png" alt="2.33 使用示例科目的课程编辑页 / Course editor with demo lessons" width="100%" />

</details>

## 开始使用 / Quick start

1. 在 [Releases](https://github.com/QQ-L-XX/m13-light-launcher/releases/latest) 下载完整 APK 和校验文件。Download the full APK and checksum.
2. 在盒子安装 APK，打开“轻桌面”，先验证遥控器导航、播放和返回。Install and test navigation and playback first.
3. 如需默认主页、开机进入与屏保系统配置，阅读 [安装与恢复](docs/INSTALL.md)。Managed-home behavior requires device support and additional provisioning.

```powershell
adb devices -l
adb -s <DEVICE_SERIAL> install -r .\M13-Light-Launcher-2.33.apk
adb -s <DEVICE_SERIAL> shell am start -n local.m13.home/.MainActivity
```

**仅验证过特定 M13 设备环境；不是通刷固件。** Android 5.0+ 是 API 最低要求，不代表已适配所有品牌和新版系统。无需为普通安装获取 Root；本项目不包含 Root 工具、CEC 解锁、原厂固件或开机片头替换。

The minimum API is Android 5.0. Compatibility outside the development M13 is unverified. This APK does not flash firmware or grant root. System boot animations and vendor restrictions remain device-specific.

## 源码与构建 / Source & build

```text
app/AndroidManifest.xml     Package, permissions and components
app/src/local/m13/home/    Native launcher, player, casting and screensaver
app/res/                  Android resources
app/assets/               Title index, pinyin, fonts, CA roots and notices
tools/build.py            Python + Android SDK build; no private key in source
tools/test.py             Offline Java solar/schedule regression checks
tests/                    Host-side checks
docs/                     Installation, build and validation notes
```

需要 Python 3.11+、JDK 21、Android API 22 和 Build Tools 35.0.0。首次构建生成仅供开发的本地签名，私钥保存在忽略目录；正式包使用私有签名。具体环境变量见 [构建说明](docs/BUILD.md)。Python orchestrates the standard Android compiler, D8, aapt, zipalign and apksigner.

```powershell
python tools/test.py
python tools/build.py
```

## 边界与验证 / Scope & validation

- 本次公开版本执行源码构建、签名与 APK 完整性检查、太阳时刻边界回归测试。结果见 [验证记录](docs/VALIDATION.md)。Public build checks are documented separately from device acceptance.
- 历史上 iPhone 视频 App 投屏曾由用户确认画面与声音正常；公开包未重新在每款手机上验收。DLNA reception depends on the sending app and LAN multicast.
- “语音搜片”是可选独立包 `local.m13.voice` 的入口；**本仓库不包含语音识别引擎和模型**。未安装时会提示缺少组件，普通文本搜索可用。Voice is an optional companion integration, not bundled recognition.
- 默认片源是外部服务，可能变化或失效；本项目不托管媒体文件，使用者需确认访问内容的授权。Catalog and stream availability are not guaranteed.
- 片名索引只包含名称和拼音键，不包含影片、播放 URL、剧情或海报。运行时历史保存在盒子本地。Title seed metadata and provenance: [title-seed-info.json](app/assets/title-seed-info.json).
- 投屏没有配对认证，只在可信局域网使用，勿映射端口到公网。Cast control is intended for a trusted LAN.

## 许可证与鸣谢 / License & credits

本地改编源码按 **GNU AGPL-3.0** 发布，保留 [LibreSpark/LibreTV](https://github.com/LibreSpark/LibreTV) 的改编说明。拼音数据来自 [mozillazg/pinyin-data](https://github.com/mozillazg/pinyin-data)（MIT）；Patrick Hand 字体采用 SIL OFL 1.1；Mozilla CA 数据采用 MPL-2.0；天气数据署名 Open-Meteo（CC BY 4.0）。完整说明见 [第三方声明](app/assets/THIRD-PARTY-NOTICES.txt)。这些许可证不授予外部视频内容或第三方商标的使用权。

Adaptation code is distributed under AGPL-3.0; bundled third-party materials retain their licenses. See [LICENSE](LICENSE) and the included notices. No signing private keys, device backups, accounts or viewing histories are included.

[版本记录 / Changelog](CHANGELOG.md) · [反馈与贡献 / Contributing](CONTRIBUTING.md)
