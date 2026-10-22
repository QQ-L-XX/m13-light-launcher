# Changelog / 版本记录

## 2.24 — Tomorrow's classes in the screensaver

- Move and slightly reduce the calendar and weather region to make room for a horizontal seven-lesson row.
- Show tomorrow's date, weekday, morning/afternoon groups and locally configured courses; empty days have a no-classes message.
- Calculate tomorrow in Asia/Shanghai across midnight, weekends, month/year boundaries and leap days.
- Keep personal timetable data in private app storage, configured through the existing shell-permission channel; no personal timetable is embedded in the APK or repository.
- Host date checks and seven-day device data checks passed. Day/night rendering inspected on M13; solar appearance and NetEase account preserved.

## 2.23.4 — Lyric timing controls

- Refresh only lyric display every 100 ms, with unchanged-text updates skipped; retain the main player refresh at 500 ms.
- Add per-song lyric calibration in 250 ms steps, with a reset and a 10-second limit in either direction.
- Verify timing boundaries in host tests and advance/reset buttons on M13; account authorization survived the update and member track playback continued.
- Audible synchronization remains subject to user listening verification; no guessed default offset is applied.

## 2.23.3 — Desktop QR endpoint and bounded errors (device validated)

- Use form POST with type 3 on interface.music.163.com for QR creation and polling.
- Confine account requests to music.163.com and interface.music.163.com over HTTPS.
- Stop when status 8821 lacks verification metadata; do not present it as an available second QR.
- Limit consecutive request failures and display other unsupported business codes explicitly.
- Installed on M13; QR authorization returned 803 and account session was saved privately.
- Account playback of NetEase track 167876 (fee 1) succeeded with duration 241867 ms; playback advanced beyond a seek to 95000 ms without player error. User confirmation of this track's audible output remains pending.

## 2.23.1 — NetEase additional verification (verification pending)

- Handle login status 8821 by requesting and showing the provider's additional verification QR.
- Use the standard encrypted web request envelope for the verification endpoint.
- Keep verification tokens in memory and out of diagnostics.
- Installed on M13; fresh login QR responds with 801. Phone verification and member playback still await validation.

## 2.23 — NetEase account authorization (verification pending)

- Add on-device QR login and local logout for NetEase accounts.
- Use the private account session only for HTTPS requests to music.163.com.
- Generate QR images locally with the MIT-licensed Nayuki QR encoder.
- Keep provider preview restrictions and existing full-track checks.
- Use response files for long Java build commands on Windows.
- Installed on M13; user authorization and member-track playback remain pending.

## 2.22.2 — Standalone music module (device validated)

- Add the native 轻音乐 entry, direct provider search, playlists and line lyrics.
- Store favorites/history locally and support resume, background playback and audio focus.
- Integrate Music-TV lyric-parser attribution and Apache-2.0 license.
- Wake the music activity from the screensaver and apply D-pad SeekBar changes.
- Forward valid HTTP Content-Range metadata and extend casting relay diagnostics.
- See docs/MUSIC.md for scope and docs/MUSIC-VALIDATION.md for actual validation.

## 2.21.1 — Public source release

- Publish the existing 2.21 native launcher source, assets and third-party notices.
- Remove the developer-specific LAN server address; retain standalone default.
- Add portable SDK build tooling, local debug signing and offline solar checks.
- Add bilingual installation, recovery and validation documentation.
- No root, firmware or device-state changes are performed by this release process.

此前版本逐步加入极简桌面、原生影视、拼音与首字母搜索、片源归并、DLNA 投屏、蜡笔天气屏保、后台管理和日出日落自动配色。旧版本未作为本仓库的独立发行历史上传。
