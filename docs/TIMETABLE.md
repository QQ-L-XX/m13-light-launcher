# 明日课程 / Tomorrow's classes

## 2.33 遥控器直接编辑 / Edit with the remote

打开“设置 → 画面 / 课程 / 备份 / 更新 → 课程表与临时调课”，选择星期与课节，再保存。常见科目可直接选择，也可输入自定义名称。指定日期可以设为放假或更换七节课程；放假时屏保跳到下一上课日，周末的临时补课也可显式设置。不会自动导入法定节假日。通过“查看 / 删除临时安排”恢复常规课表。

Edit weekly lessons and date-specific replacements directly on the box. Holiday dates are entered manually; the screensaver skips those dates. Explicit weekend replacements can schedule a make-up school day. [Settings and backup](SETTINGS.md).

以下开发者配置接口仍可使用。所有公开截图使用示例课程。


屏保下方显示横向七节课，1—4 节为上午，5—7 节为下午。2.28 起，周一至周四显示次日课程；周五至周日直接显示下周一，标题为“下周一课程”，同时标出对应日期。按北京时间更新，调休、寒暑假及临时换课不会自动推断。

Seven lessons are displayed below the calendar and weather. Monday through Thursday shows tomorrow; Friday through Sunday shows the upcoming Monday, with its actual date and an explicit label. The calendar uses Asia/Shanghai and a repeating weekly timetable, not a school holiday calendar. Raw timetable status remains available; its `display` field contains the effective screen selection.

## 配置 / Configuration

课程保存在应用私有 SharedPreferences，不随公开 APK 分发。配置入口由 Android DUMP 权限保护，仅供已授权的本机 ADB shell / 系统使用；没有联网同步或远程公开配置端点。当前没有电视内编辑器。

Personal course data stays in app-private storage. The existing installer receiver requires the Android DUMP permission. No account credentials, names or timetable photos are needed. There is currently no on-TV timetable editor.

在已授权 ADB 的电脑上使用以下 Python 模板，将占位课程替换为自己的内容。按星期一到星期日排列；每天七个课程名，或用空数组表示无课。须有 Python 3 与 PATH 中的 adb。

```python
import base64, json, subprocess

serial = "YOUR_DEVICE_SERIAL"
week = [["课程" + str(n) for n in range(1, 8)] for _ in range(5)] + [[], []]
config = base64.b64encode(json.dumps({"week": week}, ensure_ascii=False).encode("utf-8")).decode("ascii")
subprocess.run([
    "adb", "-s", serial, "shell", "am", "broadcast",
    "-n", "local.m13.home/.LibreTvProvision",
    "-a", "local.m13.home.SCREEN_TIMETABLE_CONFIG",
    "--es", "config", config,
], check=True)
```

成功返回 `result=-1` 和明日课程。正在显示的屏保会重新缓存画面。每个课程名最多 32 个字符，不得为空或含换行；长名称会分成两行显示。

The receiver validates all seven days before replacing the stored timetable. Inspect the broadcast result: process exit status alone does not establish application-level success. A running screensaver notices the new revision and refreshes its cached drawing.

```powershell
adb -s YOUR_DEVICE_SERIAL shell am broadcast -n local.m13.home/.LibreTvProvision -a local.m13.home.SCREEN_TIMETABLE_STATUS
```

此状态会包含明日课程文字，分享日志时请自行删去个人课程。不要把私人配置、姓名、照片或授权记录提交到公开仓库。
