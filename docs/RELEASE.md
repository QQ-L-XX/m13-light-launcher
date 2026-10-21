## 轻桌面 · Light Launcher 2.21.1

首次公开源码版本，基于本地 2.21 桌面整理。包含原生桌面、影视搜索与片源归并、DLNA 视频投屏、蜡笔天气日历及福州日出日落主题。

First public source release of the native M13 launcher. Standalone operation requires no PC server. Online video and weather need internet access.

### 下载 / Download

- `M13-Light-Launcher-2.21.1.apk`: 完整安装包 / full APK, includes the offline title index.
- `SHA256SUMS.txt`: SHA256 校验 / integrity checksum.
- Source code archives: GitHub automatically attaches the matching tagged source.

### 本次验证 / Checks

- 3,660 offline solar and schedule assertions passed.
- Clean Java/D8/aapt build, ZIP integrity and APK signature verification passed.
- Release certificate matches the existing local 2.21 release certificate.
- This public build was not installed on a device during publication. Existing device playback/casting evidence is historical; see docs/VALIDATION.md.

### 安装 / Install

See [installation and restore instructions](https://github.com/QQ-L-XX/m13-light-launcher/blob/main/docs/INSTALL.md). Other TV boxes and recent Android versions are unverified. The optional voice recognition companion is not included. DLNA video casting does not provide AirPlay screen mirroring.

普通使用不需要 Root。设置默认主页和系统屏保可能需要设备允许 Device Owner / WRITE_SECURE_SETTINGS。不是固件，不提供救砖、CEC 解锁或原厂开机片头替换。

AGPL-3.0 adaptation; bundled third-party licenses are retained. No private keys, device backups, accounts or viewing histories are published.

SHA256: `1d7e898320414d59f03a160a66431078a59eb812fa8cdc840f469dab504d222f`
