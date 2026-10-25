# 轻音乐 · Standalone TV music

本模块研究了 Music-TV 的 LX Server 接口与电视交互，将搜索、歌单、歌词、播放地址获取和收藏改为盒子直接执行。为 M13 Android API 22 的兼容与内存占用，界面使用现有轻桌面的原生 Java 组件；没有将 Node.js 服务或完整 Kotlin/Compose 应用塞进盒子。

This is a lightweight in-launcher adaptation, not a full upstream Music-TV fork or an official Music-TV build. The line-based lyric parser is ported from Music-TV under Apache-2.0; upstream license and modification notice are included. Music-TV commit studied: c55b631364e30d4a52b3b7fe18ec0ea29b0e0581.

## 盒子内完成 / On-device responsibilities

- 搜歌、按页加载、网易公开歌单查询；发现页提供分类关键词及榜单入口。
- 网易、酷我直接搜索；网易同步行歌词。酷我歌词暂未接入。
- 原生 MediaPlayer 解码，MediaSession 与音频焦点管理，前台服务支持回桌面继续听。
- 收藏与最近播放记录只保存在盒子，续听保存最近歌曲与进度，不保存短期播放 URL。
- 同一个应用内的回环 TLS 音频桥只监听 127.0.0.1，用来兼容旧 Android 解码器；电脑、NAS、LX Server 都不参与音频传输。

Search, playlist metadata, lyrics and media URL resolution run directly on the box. The loopback media bridge is part of the APK; it is not a PC proxy. Internet remains required for provider data and online music.

## 使用

轻桌面 → **轻音乐**。默认搜索纯音乐，输入歌名或歌手后点“搜歌曲”；“搜歌单”使用网易。右上“发现”提供分类和榜单，“收藏”“最近”“续听”读取本机记录。播放页可暂停、前后切歌、调整进度、收藏与停止。返回桌面可继续听；进入视频播放后由音频焦点协调。

## 范围

2.23.4 将行歌词刷新间隔缩短至 0.1 秒。播放区下方“歌词校准”可按每次 0.25 秒提前或延后：歌词慢了选提前，歌词快了选延后，按返回关闭。设置按来源与歌曲 ID 单独保存在盒子；“恢复原始”清除本曲偏移，音乐速度和进度不变。不同录音版本的时间轴质量仍由歌词源决定。

2.23 增加右上角“网易云账号”：用手机网易云音乐 App 扫描电视二维码，并在手机确认登录。2.23.1 补充状态码 8821 的额外验证流程：如果电视显示安全验证二维码，需再次扫码并在手机完成网易云要求的验证。二维码在盒子本地生成，授权会话仅保存到应用私有存储；可在同一入口选择“退出此盒子”。电脑不参与登录后的播放。请勿公开二维码或复制应用私有账号文件。

2.23.3 使用新版桌面扫码接口。状态 8821 并不总是附带可完成的安全验证入口：如果平台缺少验证参数，程序会停止并说明，避免持续重试。账号网络请求仅允许 music.163.com 与 interface.music.163.com 两个 HTTPS 主机；不把账号会话发送给其他音乐源或媒体 CDN。

登录后，网易搜索、歌单、歌词和播放地址请求使用该账号会话。是否能播放由网易实际返回的账号与歌曲权限决定；会员不保证涵盖单独购买、区域限制或全部版权曲目。当前请求标准音质（128 kbps），未提供无损音质选择。仍会拒绝标记为试听或明显不足完整时长的音频。

这是盒子独立的音乐客户端，不提供全部平台或全部歌曲可播的承诺。直接接口可能变化。酷我在权限字段缺失时也不会擅自认为可免费播放。酷我账号登录、云同步、下载、逐字卡拉 OK 和本地文件浏览不在此版范围。2.23.3 已在 M13 验证扫码授权、私有会话保存及账号歌曲播放；详见 MUSIC-VALIDATION.md。

Full tracks are accepted only when the provider supplies a usable stream without a declared preview restriction. Shortened streams inconsistent with track duration are rejected. Availability is controlled by the provider; this module does not unlock paid music.


## 2.32：歌词字号与全屏 / Lyric size and fullscreen

播放区点击“歌词字号”可选择小、标准、大、超大，选择会保存在盒子上，列表与全屏共用。“全屏播放”保留歌曲名、三行歌词、进度及上一首／暂停／下一首；选择“返回列表”或按返回键可回到搜索列表，播放继续由原有服务负责。

Choose one of four lyric sizes from the player; the preference is stored on the box and shared with fullscreen mode. Fullscreen keeps the title, lyric context, seek bar and playback controls. Returning to the list retains the current playback service.


## 2.33：播放模式与睡眠定时 / Playback modes and sleep pause

列表左下角或全屏右上角选择“播放设置”。顺序播放在列表结束后停止；单曲循环只影响自动播完，手动下一首仍切歌；随机播放在多首歌曲时避开立即重复同一首。睡眠定时支持 15、30、60、90 分钟，到时暂停音乐，不关闭电视。服务或设备重启后定时不保留。失败时可重试当前歌曲或重试搜索。

Playback settings provide sequential, repeat-one and shuffle modes. Sleep timers pause music after 15/30/60/90 minutes; they do not power off the TV and do not persist across service/device restarts. Retry controls are available for a failed track or search.
