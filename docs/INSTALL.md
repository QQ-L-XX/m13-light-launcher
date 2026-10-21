# 安装与恢复 / Install & restore

## 普通安装 / Basic install

从本仓库 Releases 下载完整 APK，核对 SHA256。用 U 盘复制到盒子后，通过盒子的文件管理器安装；允许该安装来源的提示以设备实际界面为准。也可用已经配置好的 ADB 安装。

Download the full APK from Releases and check its SHA256. Install through a supported file manager or an existing authorized ADB connection. No USB cable is included or required by this project.

```powershell
Get-FileHash .\M13-Light-Launcher-2.21.1.apk -Algorithm SHA256
adb devices -l
adb -s <DEVICE_SERIAL> install -r .\M13-Light-Launcher-2.21.1.apk
adb -s <DEVICE_SERIAL> shell am start -n local.m13.home/.MainActivity
```

`<DEVICE_SERIAL>` 请替换为设备列表中的目标序列号；网络 ADB 时通常是你自己盒子的 IP 与端口。不要原样复制尖括号。Select the correct device; do not paste the placeholder literally.

安装后先验证方向键、确定、返回，再测试联网功能。出现主页选择框时可选“轻桌面 / 始终”；各厂商对默认桌面的限制不同。

## 管理默认主页 / Managed home

本应用包含 Device Owner 管理接口。**只有系统允许设置 Device Owner 的设备才可使用**；含已有账号、其他管理器或厂商限制的系统可能拒绝，普通安装也不会自动获得此权限。不要为此擅自清空设备。

On compatible devices, optional Device Owner provisioning enables persistent Home and package hiding. It can hide the known M13 stock launcher, advertising service and stock screensaver for the current user. These system APKs are not erased from firmware. Read `HomePolicy.java` before provisioning on another model.

仅在你理解这一效果且设备支持的情况下执行：

```powershell
adb -s <DEVICE_SERIAL> shell dpm set-device-owner local.m13.home/.HomeAdmin
adb -s <DEVICE_SERIAL> shell pm grant local.m13.home android.permission.WRITE_SECURE_SETTINGS
adb -s <DEVICE_SERIAL> shell am start -n local.m13.home/.MainActivity
```

若命令返回失败，保留原桌面并按普通应用使用；不要通过反复禁用系统包来绕过失败。主页和广告管理逻辑与特定 M13 的包名相关，不是通用电视系统优化器。

## 屏保、投屏与搜索 / Screensaver, cast & search

- 设置中选择蜡笔屏保及日夜模式；日出日落与天气固定为福州，暂未提供任意城市界面。
- 打开“投屏”，手机和盒子连同一局域网，在支持 DLNA 的视频 App 中选择“轻桌面 M13”。苹果控制中心“屏幕镜像”属于 AirPlay，不是这个功能。
- LibreTV 默认独立模式；无需电脑服务。可输入中文、完整拼音或首字母，使用“加载更多”继续搜索。片源接口失效时联网搜索或播放可能失败。
- 可选远程 LibreTV 服务地址要在连接设置中填写；公开版没有预置开发者的内网地址。127.0.0.1 表示盒子自身，不会自动连接电脑。

Choose screensaver options in Settings. Use a DLNA-capable video app for casting. Native standalone mode is the default. Remote-server mode needs your own compatible service address.

## 恢复 / Restore

先在轻桌面设置选择 **“恢复原系统桌面”**，让应用恢复它记录的主页、屏保与按需应用状态，再从设备设置中处理管理权限与卸载。不同系统移除 Device Owner 的方式不同；本项目不承诺通过一条命令解除所有厂商管理限制。不要先卸载一个仍是唯一可用主页或设备管理器的包。

Use the in-app restore action before removing management or uninstalling. It restores policies recorded by the launcher; it is not a firmware restore or an eMMC backup. Keep the stock firmware and recovery procedure appropriate to your own device.

自行构建时使用的开发签名与 Release 签名不同，不能直接覆盖安装正式包；反之亦然。更换签名前需规划导出与恢复自己的数据。仓库不提供生产签名密钥。

Self-built debug APKs cannot update a release-signed installation. Signing keys are deliberately excluded.
