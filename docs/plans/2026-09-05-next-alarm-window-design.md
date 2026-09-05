# Next Alarm Display Window Design

## Goal

让“下一个闹钟”速览只显示用户指定时间范围内即将响铃的闹钟，范围为 5 分钟、10 分钟、30 分钟、1 小时、3 小时或 6 小时。

## Recommended design

在现有 `GlanceSettingsDialog` 的“下一个闹钟”子菜单中增加一个可点击设置行，使用项目现有的 `BottomSheetMenu` 展示六个选项。偏好值以分钟保存，默认 360 分钟，以保持现有功能在升级后的最大兼容性。

闹钟时间处理拆成两步：先得到真实有效的下一闹钟时间（包括通过 Shizuku 读取小米 `ALARM_ALERT`），再判断剩余时间是否在用户选择的范围内。超过范围只是不显示，不被标记成错误来源，但会在闹钟进入所选范围的时间点安排一次刷新，保证它之后能自动出现。

## Data flow

选择项 → `Preferences.nextAlarmWindow` → `AlarmHelper` 计算剩余时间 → Widget/At a Glance 决定是否显示。修改偏好后立即刷新设置列表和桌面小部件；已有 Shizuku 授权、服务绑定和 Xiaomi 提前提醒过滤逻辑保持不变。

## Error handling

没有闹钟或闹钟超过范围时返回空显示；非时钟来源或无法取得真实 Xiaomi `ALARM_ALERT` 时继续使用现有异常状态。非法或旧偏好值回退到 6 小时。

## Verification boundary

本次只进行编译和 APK 安装核对，不替用户执行手机上的功能测试。用户可自行分别设置各个时间范围并验证速览是否按范围显示。
