# 2.33 设置使用说明

桌面右上角选择“设置 → 画面 / 课程 / 备份 / 更新”。

- **电视画面边距**：标准、加宽、更宽、最大。整体缩小界面，影视播放画面不裁切。主界面、轻音乐、投屏说明与课程屏保使用同一档位。
- **课程表**：选择星期和课节，可从常用课程中选择或输入名称。保存后立即生效。周末默认显示下周一；指定日期可设置调课、放假或恢复常规安排。不会自动判断法定节假日，需要手动指定放假日期。
- **设置备份**：保存到盒子，修改后可恢复；恢复前自动留下撤销副本。仅包含首页排序、课程与临时安排、配色、边距、歌词字号和播放模式。不包含账号、Cookie、影视记录、固件和设备管理权限。备份有个人课程，请勿公开上传。
- **文件导出**：使用系统文件选择器导入或导出到支持的存储位置。M13 原系统没有 DocumentsUI，实机验证的是本机备份恢复；本机备份不能抵御清除应用数据或设备损坏。
- **检查更新**：读取 GitHub 正式发布和更新说明，只提供高于当前版本的下载。下载校验大小、SHA-256、包名、签名和版本号，之后需在系统安装页确认。网络失败可重新检查，不自动卸载、不清除数据。
- **旧版安装包**：可保留当前安装包，未来更新前也会保留一份。系统可能拒绝降级；应用数据和账号并不包含在安装包里，保留 APK 不等于系统救砖备份。

轻音乐列表左下角“播放设置”或全屏右上角“播放设置”：顺序播放、单曲循环、随机播放；睡眠定时支持关闭、15、30、60、90 分钟。定时按本次播放服务计时，到时暂停音乐，重启设备或服务后需重新设置。失败时可选择重试当前歌曲；搜索可用“重试搜索”。

投屏页显示等待连接、正在连接、正在投屏或暂停。播放中按返回停止本次投屏，回到先前页面，接收器继续等待下次连接。兼容 DLNA 视频 App，仍不包含 AirPlay 整屏镜像。

天气超过有效期仍显示最后一次数据，并明确标注缓存和观测时间；无缓存时提示联网获取。旧预报只在日期仍匹配时显示。

## Validation boundary

Installed on MagicBox M13 (Android 5.1/API 22). On-device checks cover margins, local backup/restore, course editing, holiday selection/removal, background sleep pause, playback mode persistence, DLNA decoding and return-to-home focus, public release discovery and system installer opening. A newer public APK is required to finish an end-to-end updater download and installation test. Provider compatibility and audible output need user acceptance on the television.

GitHub release metadata follows the [official REST release API](https://docs.github.com/en/rest/releases/releases#get-the-latest-release).
