# 宝贝成长星球 / BaoBei Growth Planet

面向 6–10 岁孩子与家长共同使用的离线 Android 成长记录 App。应用依据原始行为周记的分类与计分规则重新设计，原始 Excel 仅作为规则参考，不随公开仓库发布。

## 功能 / Features

- 多宝贝档案与删除/清空记录管理 / Multiple child profiles with data management
- 孩子表情自评、家长确认、可复盘的扣分项 / Child self-rating, parent confirmation, and reflection deductions
- 动态满分星级、积分余额与奖励兑换 / Dynamic stars, points balance, and reward redemption
- 周、月、年度成长报告与最多 10 个本周小目标 / Weekly, monthly, and yearly reports with up to 10 weekly goals
- 成长星球、森林动物、彩虹乐园三套主题 / Three visual themes: Planet, Forest Animals, and Rainbow Park
- 本地轻音乐、可选每日提醒、Android 自动备份 / Local background music, optional reminders, and Android backup
- 完全离线，无账号、广告、分析 SDK 或网络权限 / Fully offline, with no account, ads, analytics SDK, or network permission

## 技术栈 / Tech Stack

- Kotlin 2.1.20、Jetpack Compose、Material 3
- Room、Coroutines/StateFlow、Navigation Compose
- Android Gradle Plugin 8.9.2、Gradle 8.14、JDK 17
- `minSdk 26`、`targetSdk 36`

## 构建 / Build

```powershell
$env:JAVA_HOME='F:\Software\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

Debug APK 输出到 `app/build/outputs/apk/debug/`。已发布 APK 请从 GitHub Releases 获取；当前版本为 `v1.4.1`，属于 debug/unsigned 构建，不代表生产签名版本。

The debug APK is generated under `app/build/outputs/apk/debug/`. Download published APKs from GitHub Releases. The current `v1.4.1` artifact is a debug/unsigned build and is not a production-signed release.

## 权限与隐私 / Permissions & Privacy

应用不申请网络权限。通知权限仅在用户启用提醒时使用，音乐遵循系统音频焦点；业务数据保存在设备本地，并可由 Android 系统自动备份。项目不会提交 `local.properties`、缓存、构建目录或 APK 文件。

The app requests no Internet permission. Notification permission is used only when reminders are enabled, and music respects audio focus. App data stays on-device and may be included in Android system backup. Local properties, caches, build directories, and APK files are excluded from this repository.