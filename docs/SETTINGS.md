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


## 2.34 本地更新 / Local update

LibreTV 播放时，左右键每次快退／快进 10 秒，长按后可加速；连续输入按目标位置累加，底部显示“跳转到”的时间。确认键暂停／继续，上下键重新显示进度条。暂停和跳转时进度条保持显示，正常播放约 4.5 秒后自动隐藏。

搜索框确认后打开应用内蓝红键盘；可直接输入拼音、首字母或数字，本地片名联想使用盒子索引。选择片名直接搜索，返回取消且不覆盖原搜索词。“中文输入”保留原系统输入法，未替换系统键盘。

设置页新增“系统切换动画”：快速档把动画时长设为 0.5，并保存原值供恢复。它影响系统切换动画，不等于提高第三方应用的帧率；需要系统已授予 WRITE_SECURE_SETTINGS。

此次另在开发盒子的云视听小电视中开启了“延迟开机自动播放”，重开后可见自动播放倒计时。这是设备上的应用设置，不随轻桌面 APK 安装而自动修改；账号、画质设置和数据没有清除。未测得足以量化 B 站整体帧率提升的证据，实际滑动体验仍需用户确认。


## 2.35：精简预装应用 / Optional preinstalled apps

进入“设置 → 画面 / 课程 / 备份 / 更新 → 精简预装应用”。可停用少儿大厅、原厂音乐、原厂照片，取消勾选再保存即可恢复。功能只处理这三项，不开放任意包名操作。

使用设备管理权限隐藏应用，入口和后台不可用，原数据保留；原厂 APK 仍在系统分区，所以不等同于释放系统分区空间，也不保证明显提高帧率。停用清单不放入普通显示偏好备份；恢复系统桌面前会自动恢复这些应用。此功能需要轻桌面已获本地设备管理权限，不需要 Root。

The allowlist covers only the stock education, music and photo apps. Device-owner hiding preserves app data and the original visibility, without changing firmware partitions. Uncheck an app to restore it. This policy is separate from ordinary display-preference backups.


## 2.36：B 站短时保留与后台海报暂停

云视听小电视设为按需启动时，从轻桌面打开后再回首页，会保留约两分钟，期间再次打开可复用进程。到期且没有正在播放的媒体时再限制后台；系统内存不足仍可能提前回收。其他按需应用继续使用原有的 20 秒缓冲。该策略由盒子自行执行，不需要电脑常驻。

影视页和轻音乐页退到后台时，停止新增海报加载、清空待处理队列、取消海报连接，并释放解码图片缓存；回来后重新加载可见封面。此变更不取消音乐服务的音频播放，也不修改 B 站账号、画质或系统固件。
