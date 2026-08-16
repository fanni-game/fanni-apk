# 凡逆RPG 安卓APK原生壳

APK纯壳，全屏WebView加载线上地址 `http://124.156.183.73/`。
游戏资源全部在服务器，服务器更新后玩家杀掉APP重开即时生效，无需重装。

## 技术要求对照

| 要求 | 实现位置 |
|---|---|
| 全屏WebView无地址栏 | `MainActivity.applyImmersiveFullscreen()` + Theme.Material.NoActionBar.Fullscreen |
| 不缓存（LOAD_NO_CACHE） | `WebSettings.setCacheMode(LOAD_NO_CACHE)` |
| 允许HTTP明文 | Manifest `android:usesCleartextTraffic="true"` |
| 竖屏锁定 | Manifest `screenOrientation="portrait"` |
| 禁用下拉刷新 | `setOverScrollMode(OVER_SCROLL_NEVER)` |
| 返回键后退/首页退出确认 | `onBackPressed()` |
| 启动画面+10秒慢网提示 | splash_overlay + Handler 10s Runnable |
| 断网友好错误页 | `onReceivedError` 主帧判断 + error_overlay 重试按钮 |
| JS/DOM存储/数据库 | `setJavaScriptEnabled / setDomStorageEnabled / setDatabaseEnabled` |
| UA标识 FanNiApp/1.0 | `setUserAgentString(原UA + " FanNiApp/1.0")` |
| targetSdk 30 / minSdk 21 | `app/build.gradle` |
| debug + release 双包 | `gradlew assembleDebug assembleRelease` |
| release混淆+压缩 | `minifyEnabled true` + `shrinkResources true` |

## 编译环境（VPS：Ubuntu 22.04）

- JDK 17（已装）
- Android SDK：`/home/ubuntu/android-sdk`（platform-34 + build-tools 34.0.0，已装）
- Gradle 8.4：`/opt/gradle-8.4`（脚本自动安装）
- AGP 8.2.2

## 在VPS上编译

```bash
cd /opt/fanni/build/fanni-apk
export ANDROID_HOME=/home/ubuntu/android-sdk
./gradlew assembleDebug assembleRelease
# 产物：
# app/build/outputs/apk/debug/app-debug.apk
# app/build/outputs/apk/release/app-release.apk
# 发布到下载路径：
cp app/build/outputs/apk/release/app-release.apk /opt/fanni/web/download/fanni-release.apk
```

## 签名（铁律：所有后续版本必须同一签名，否则玩家无法覆盖安装）

- keystore文件：`fanni-release.keystore`（VPS `/opt/fanni/build/fanni-apk/` + 探索者本地备份）
- 别名：`fanni`
- 密码：见交付时告知，已单独发给探索者备份

## 玩家安装

手机浏览器打开 `http://124.156.183.73/download/fanni-release.apk` 下载，
安装时允许"未知来源应用"，装完桌面出现"凡逆"图标（金色四角星）。
