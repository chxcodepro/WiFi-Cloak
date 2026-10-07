# 验证记录

## GitHub Actions 1.1.3 首次公开发行

验证日期：2026-10-07。公开仓库：[chxcodepro/WiFi-Cloak](https://github.com/chxcodepro/WiFi-Cloak)。[Actions 运行 37612544367](https://github.com/chxcodepro/WiFi-Cloak/actions/runs/37612544367) 已全部成功，构建现有注解标签 `v1.1.3` 的源码并发布 [WiFi Cloak 1.1.3](https://github.com/chxcodepro/WiFi-Cloak/releases/tag/v1.1.3)。

下载并解析 Actions 报告：26 项测试通过，0 失败、0 错误；Lint 无错误或严重错误。签名证书与旧版一致的流水线校验通过。Release 已公开，非草稿、非预发行，包含 APK 和 SHA-256 校验文件；下载校验文件中的摘要与 GitHub 记录的 APK 资产摘要一致。

CI APK SHA-256：`a7e0a7a657c2d9ebed7de00dd37e2c90d7c45298d21e4cbf2567b38343b2503f`。此摘要对应公开下载包；下方历史记录的摘要对应本机构建包。

首次 SDK 准备运行因 setup-android 默认安装已移除的旧 `tools` 包失败。最新工作流明确安装 `platform-tools`，手动输入既有标签并强制检出标签源码后完成发布，原注解标签没有移动。真机覆盖安装与 LSPosed 注入仍沿用下方的验收边界。

## 1.1.3 覆盖安装后的 WiFi 配置保留

验证日期：2026-10-07，versionCode 5。

配置主副本改为应用私有目录的 `files/wifi_cloak.json`，使用 Android `AtomicFile` 写入并校验落盘内容。保留旧版偏好配置，首次读取时自动迁移。首次连接框架前，本地快照缺失或无法解析时先读取远程快照恢复；恢复失败不会将空配置发布到框架。有效本地空配置和用户新保存的配置优先。框架发布不再清空整个偏好组。解码逐项校验，损坏项停用控制并保留其他有效 WiFi、使用记录和规则。

- `:app:testDebugUnitTest`：26 项通过，0 失败、0 错误。新增 10 项 Robolectric 存储测试和 3 项部分损坏解析测试。覆盖旧版迁移后清空偏好组、重新创建存储对象、重复保存、写入中断、原子备份恢复、远程恢复、主动空配置优先、恢复失败保留原始数据及离线新保存优先。
- 存储测试使用 Robolectric Android 34。在 Windows 下仅模拟 `AtomicFile.rename` 的 Android 覆盖目标文件语义，其余原子文件代码使用 Android 实现；Windows `File.renameTo` 无法替换已有文件。该测试调整不进入 APK。
- `:app:assembleRelease`：成功，R8 与资源压缩完成。
- `:app:lintDebug`：0 错误、0 严重错误，13 个警告。
- `artifacts/WiFiCloak-1.1.3.apk`：v2 签名验证通过。包名仍为 `dev.wificloak`，签名证书 SHA-256 与所有既有 1.0.0–1.1.2 APK 一致：`75cf0b09bdb94c03005ee50f4748f6c20f1d32896941011e6c7b7512cbc210ea`。
- APK SHA-256：`5dcdbb72e26bd887b6fcd414bf9ec4b4670e40ee9c7072c188285e0556fc6ae9`。

当前无已连接设备，未复现用户手机上的覆盖安装过程，不能断言手机上触发清空的具体原因。真实 APK 替换、框架快照升级期间的保留和跨进程恢复仍需 API 101 设备验收。设备验收时在旧版保存两个 WiFi、记录当前项与应用规则，直接覆盖安装 1.1.3，重新打开后核对 SSID/BSSID/MAC、使用记录与规则；再次覆盖安装确认数据持续保留。本地及框架快照均已被删除时无法恢复原 WiFi。

## 1.1.2 框架连接状态语义修复

验证日期：2026-10-07，versionCode 4。核对本机官方 `service:101.0.0` 的 `XposedService` 公共接口，确认没有查询模块启用开关的方法。“模块已连接”改为“框架服务已连接”，设置页单独呈现“模块开关：请在 LSPosed 查看”。读取框架状态失败时清除旧元数据与作用域；刷新结果只提交到同一服务连接，避免服务断开/替换后迟到结果覆盖状态。

`:app:assembleRelease` 成功，`:app:lintDebug` 零错误，现有配置与 WiFi 选择单元测试 13 项通过。`artifacts/WiFiCloak-1.1.2.apk` 的 v2 签名验证通过。尚无可用设备，服务断开、重连及关闭模块后服务连接仍存活的实际表现需真机验证；当前实现不具备模块启用状态查询能力。

## 1.1.1 顶部间距修复

验证日期：2026-10-07，versionCode 3。外层布局在应用 Scaffold 的窗口间距后调用 `consumeWindowInsets`，避免三个页面的标题栏重复预留状态栏高度，也覆盖宽屏导航轨。

`:app:assembleRelease` 成功，`:app:lintDebug` 零错误，`artifacts/WiFiCloak-1.1.1.apk` 的 v2 签名验证通过。本次仅修复布局并更新版本；单元测试结果沿用下方 1.1.0 记录，未重新执行。尚无可用设备，顶部间距的实际显示需真机确认。

## 1.1.0 扫描与备用流程

验证日期：2026-10-07。版本：1.1.0（versionCode 2）。当前验证环境为 Windows、JDK 21、Android SDK 36。

## 已通过

- `:app:assembleRelease`：Kotlin / Compose 管理端与现代 Xposed API 101 模块编译成功，R8 与资源压缩完成。
- `:app:testDebugUnitTest`：13 项测试通过，0 失败、0 错误。原有 6 项覆盖存储模型、无效快照与标识校验；新增 7 项覆盖扫描保存、相同接入点更新、同名不同 BSSID、备用选择同步所有规则、拒绝无效 MAC、Root 输出解析和旧数据迁移。
- `:app:lintDebug`：0 错误。保留的警告包括可选依赖升级、权限属性的旧平台适用性、读取 MAC 的隐私提示和同步偏好写入风格。
- `:app:assembleDebugAndroidTest`：管理端备用选择与持久化仪器测试 APK 编译成功，测试后恢复原数据。
- `apksigner verify --verbose`：APK v2 签名校验通过。
- APK 内包含现代模块的 `META-INF/xposed/java_init.list` 和 `module.prop`，模块入口已保留；合并 Manifest 包含官方 `XposedProvider`。

独立源码复核指出合成 WiFi 对象失败时回退真实数据的风险，已改为在受控分支返回空值或空列表、严格拒绝部分构造失败的对象，并保留一次性失败日志。复核仅覆盖源码，不能代替设备行为验证。

1.0.0 源码修复复核结果：WiFi 身份异常回退、扫描/已配置网络异常回退、主题背景显式设置三项均已解决，结论为源码范围内 `ship`；该结论不代表本次新增扫描流程已通过真机验收。远程同步每次以 `clear + snapshot` 的原子提交发送完整快照，避免远程写入失败后同值重试被本地缓存跳过。

## 未完成的设备验证

本机没有连接 Android 设备。安装官方 Android Emulator 与 Android 34 系统镜像后尝试软件模拟启动，因缺少加速驱动且模拟器启动失败，未获得可用 Android 运行时。Windows Application 日志包含该模拟器 crashpad 进程错误。

因此以下项目没有运行结果，不能作为已验证能力：

- 原生界面截图、深色模式、大字体与宽屏的视觉检查。
- `ManagerFlowTest` 的设备执行。
- WiFi 扫描权限、广播、系统限流、15 秒超时缓存和定位关闭时的实际表现。
- 系统 API、网络接口与 Root 只读路径读取设备 MAC 的成功、拒绝及 25 秒超时表现。
- 支持 API 101 的 rooted 设备上的模块加载、授权接受/拒绝/超时、作用域移除和跨进程配置同步。
- 目标应用通过不同 Java / 原生路径获取 WiFi 标识时的实际兼容性。

## 设备验收步骤

1. 在 API 101 框架设备安装 APK、启用模块，打开管理端并确认“框架服务已连接”；模块开关在 LSPosed 中查看。关闭模块后服务可能仍连接，该显示不得被解释为模块已启用。断开服务或读取失败后确认版本和旧作用域已清除。
2. 开启 WiFi、系统定位，点击扫描并授予精确定位权限，检查附近列表的 SSID/BSSID；选择一项并按需授予 Root，检查设备 MAC。退出重新打开，确认备用项保留。重复选择相同接入点不产生重复项；同名不同 BSSID 分别保留。
3. 勾选目标应用，拒绝授权，检查应用保持未授权；再次勾选并接受，确认框架真实作用域包含目标应用。
4. 勾选两个应用，选择另一个 WiFi，重启两者，读取 SSID、BSSID、MAC、扫描结果，确认两者跟随当前选择。
5. 离开原 WiFi 范围，直接选择备用 WiFi，确认无需重新采集 MAC；重启目标应用并检查受控 API 使用保存的身份。
6. 暂停控制与取消作用域，重启目标应用并确认恢复真实读取。
7. 检查权限拒绝、系统定位关闭、扫描限流、Root 拒绝/超时、当前备用项移除保护与框架断开时的恢复入口；MAC 获取失败不得保存占位值。

APK 使用本机开发签名；正式发行前配置自有 release 签名。支持 Android 10+ 和现代 Xposed API 101，传统 LSPosed 1.9.x 不兼容。
