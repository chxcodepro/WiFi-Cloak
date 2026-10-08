# WiFi Cloak

原生 Android WiFi 身份管理端与现代 LSPosed 模块。Kotlin、Jetpack Compose、Material 3，Android 10 及以上。

[下载最新版](https://github.com/chxcodepro/WiFi-Cloak/releases/latest) · [自动发布构建](https://github.com/chxcodepro/WiFi-Cloak/actions/workflows/release.yml)

## 使用

1. 从 [GitHub Releases](https://github.com/chxcodepro/WiFi-Cloak/releases/latest) 下载 `WiFiCloak-1.2.0.apk` 并安装，可直接覆盖安装旧版。
2. 在支持 **现代 Xposed API 101** 的 LSPosed 框架中启用 WiFi Cloak，重新打开本应用。
3. 在 WiFi 页点击“扫描 WiFi”，授予精确定位权限并开启 WiFi、系统定位。从附近 WiFi 中选择一项；SSID、BSSID、频率、信号强度自动采集，设备 MAC 自动读取，必要时确认 Root 授权。
4. 在应用页勾选目标应用，确认框架发出的作用域授权请求。取消勾选会调用框架 API 移除作用域。
5. 重启目标应用。所有受控应用跟随当前选择的 WiFi。

选择扫描结果后自动保存为备用 WiFi，相同 SSID/BSSID 更新已有项。下次在“备用 WiFi”中直接选择，即使不在该网络范围内也可复用保存的身份。备用项保留采集的设备 MAC、最近使用时间与使用次数，无需再次扫描或读取 Root。当前使用项不能移除；先选择其他 WiFi 即可移除旧项。新版保留旧版数据，并将全部受控应用统一到当前 WiFi。

也可在 WiFi 页点击“手动添加”，输入 SSID、BSSID 和 MAC，点击“保存并使用”。手动添加无需扫描、定位权限或 Root，保存后自动切换到备用列表，并将受控应用统一到该身份。相同 SSID/BSSID 更新已有项的 MAC，不重复添加。SSID 限制为 1–32 字节；BSSID/MAC 使用冒号分隔的六组十六进制单播地址。MAC 应填写设备地址，BSSID 应填写接入点地址。

系统限制或扫描超时会显示“最近扫描结果”。扫描选用时设备 MAC 未读取成功不会保存该项，可完成 Root 授权后重新选择。

WiFi 配置保存到应用私有目录的原子文件，覆盖安装后继续使用。首次升级自动迁移旧版 `wifi_cloak/snapshot` 偏好配置；本地快照缺失时，连接框架后先尝试恢复框架快照，再发布配置。已有本地配置（包括主动保存的空列表）优先，不会被框架旧数据替换。单条损坏的 WiFi 或应用规则不会清空其余可用 WiFi；检测到损坏时暂停控制。无法读取且无法恢复的快照保留原始数据，并显示同步错误，避免以空列表覆盖框架配置。卸载或清除应用数据会移除私有配置。

## 框架接口

使用 `io.github.libxposed:api:101.0.1` 与 `io.github.libxposed:service:101.0.0`。

| 操作 | 官方接口 |
| --- | --- |
| 连接框架 | `XposedServiceHelper.registerListener` |
| 查询实际作用域 | `XposedService.getScope` |
| 勾选应用 | `XposedService.requestScope` |
| 取消应用 | `XposedService.removeScope` |
| 分享原子配置快照 | `getRemotePreferences("wifi_cloak")` |

申请作用域仍须用户确认框架提示。未授权、被拒绝或申请超时不会建立新受控规则。远程配置仅发布实际作用域内的规则。未连接框架时可以扫描、保存和选用备用 WiFi，不能申请作用域。

传统 LSPosed 1.9.x 的旧 API 不能运行这个现代 API 101 模块；需要支持 API 101 的框架版本。普通管理器安装方式可以从设置页打开，寄生管理器需通过其 LSPosed 快捷方式打开。

“框架服务已连接”仅表示官方服务接口可读取，不代表模块开关已开启，也不证明目标应用已注入。API 101 没有模块启用开关查询接口，设置页的“模块开关”需在 LSPosed 查看。框架读取失败时清除旧版本与作用域显示；已断开的服务不会被其迟到的刷新结果标回已连接。关闭模块后，已注入的目标应用通常需要重启进程才能卸载拦截。

## WiFi 读取覆盖

- `WifiInfo` 的 SSID、BSSID、MAC、WifiSsid、频率、信号强度、网络 ID、连接阶段等读取。
- `WifiManager.getConnectionInfo`、`getScanResults`、`getConfiguredNetworks`。
- 已有 WiFi `NetworkCapabilities.getTransportInfo`。
- `wlan*` / `wifi*` 的 `NetworkInterface.getHardwareAddress`。

仅更换受控应用在这些 Java API 中读取的信息，不建立真实无线连接，不更改系统网络，不保证依赖真实联网、原生代码、驱动接口或服务端校验的功能。BSSID 是接入点 MAC；设备 MAC 是当前无线接口地址，可能是 Android 随机 MAC，并非保证为出厂 MAC。读取依次尝试系统 API、无线网络接口和 Root 只读接口；不会把 `02:00:00:00:00:00` 等占位值当作真实数据保存。

在受控模拟分支内，合成 `WifiInfo` / `WifiSsid` 失败返回空值，合成扫描结果或已配置网络失败返回空列表，并在 LSPosed 日志中记录一次失败；这些分支不会回退到真实 WiFi 身份。依赖非空 WiFi 对象的目标应用可能把该状态当作信息不可用。

## 构建

JDK 17+、Android SDK 36、Gradle 8.11.1。

```powershell
.\gradlew.bat :app:assembleRelease :app:testDebugUnitTest :app:lintDebug
```

`local.properties` 指向本机 Android SDK；换机器时修改或重新生成。GitHub Actions 使用 Secrets 中保存的既有签名密钥，发布前校验证书，保持与旧版 APK 的覆盖安装兼容性。本地未指定签名环境变量时使用本机开发签名。资源与代码已压缩，模块入口有 R8 保留规则。

## 自动发行

推送与 Gradle 版本一致的 `v*` 标签后，GitHub Actions 自动执行单元测试、Lint、签名构建及证书校验，再发布 APK 与 `checksums.sha256`。发行说明取自 `CHANGELOG.md`，配置方法及后续发布命令见 [RELEASING.md](RELEASING.md)。

## 验证

验证范围与结果记录在 `artifacts/VERIFICATION.md`。29 项单元测试覆盖手动输入保存与去重、无效手动输入拒绝、配置持久化、旧版存储迁移、保存中断恢复、框架快照恢复、部分损坏数据保留、无效快照停用、UTF-8 SSID 限制、MAC 格式与 Root 输出解析、扫描选择去重、备用复用和旧数据迁移。存储测试使用 Robolectric 的 Android 34 运行时；尚未在真实设备执行覆盖安装。管理端备用选择及手动添加表单仪器测试已编译，尚未在设备执行；扫描、Root 读取、真实 LSPosed 注入、授权回调、移除作用域与第三方应用兼容性需在支持 API 101 的 rooted 设备上验证。
