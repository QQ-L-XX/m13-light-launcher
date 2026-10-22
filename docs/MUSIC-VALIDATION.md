# 轻音乐 2.22.2 实机检查 / Device validation

日期：2026-10-22。设备：已核对的 MagicBox M13，Android API 22。APK 已安装，versionCode 32。用户此前已确认音乐声音正常、遥控器能操作。

本轮完成 12 项核验：原有影视条目及进度保留、屏保选择保留、屏保仍启用、15 万条以上片名索引可用、搜索从 25 首分页至 50 首、进入歌单、歌单完整音轨播放、收藏添加与撤销、试听限制提示、跨更新续听、遥控器进度跳转、投屏解码时音乐自动暂停。

Additional checks: screen preview yields back to the music activity; standalone music resumes after video is stopped; loopback HTTP Range metadata/body, full GET and HEAD protocol checks passed against a local fixture. Existing solar/schedule and lyric-boundary checks previously passed; neither parser changed during the final fixes.

## 音频与视频验证边界

音乐播放直接连接公网音乐源，运行时观察到 music.126.net CDN，播放器为盒子本地 MediaPlayer。电脑不提供音乐服务。

视频焦点测试使用自行生成的 480×270 H.264 baseline / AAC 兼容短片，通过投屏路径完成真实解码并观察音乐暂停。短片测试临时使用电脑 HTTP 服务，该服务已关闭；投屏接收状态也恢复到测试前的关闭状态，用户进入投屏页时仍可开启。

Two external public MP4 samples did not decode on this old player. Forwarding valid Content-Range response metadata was corrected and protocol-tested, but this does not establish compatibility with every external MP4 stream. No new cold-boot, full-day soak or new phone-device casting acceptance was performed.

## 保留的数据

原始记录中的 2 条影视条目及进度都完整保留，当前历史共有 6 条；新增条目不视为数据丢失。收藏与最近音乐记录跨更新保留，续听从已保存的非零进度开始。屏保仍为蜡笔日历服务，片名索引可用。

本轮只更新轻桌面应用；未进行系统刷写或 ESP32 操作。

## 网易云账号 / 2.23.3

同日安装 versionCode 36。用户用网易云 App 扫码确认后，盒子返回授权成功状态 803，应用私有存储中的登录会话检查为已授权。此前旧登录路径返回 8821 且没有验证参数；改用 interface.music.163.com 的 type 3 表单 POST 后本次授权成功。

许嵩《有何不可》（网易歌曲 ID 167876，搜索权限字段 fee=1）在登录前曾无法获取完整播放地址，本次账号播放成功。播放器实际时长 241867 ms，元数据时长 241840 ms，包含 54 行歌词。跳转至 95000 ms 后，实际播放进度继续推进至 109105 ms，状态 playing，播放器 error 为空，standalone=true。电脑没有提供音频服务。

这验证了本次授权与该曲目超过试听区间的播放；没有连续听完全部四分钟，也没有验证全部会员曲库、无损音质、重启后的会话有效期。该首歌的电视声音和歌词视觉效果仍待用户确认。文档及诊断不包含账号凭据或二维码会话参数。
