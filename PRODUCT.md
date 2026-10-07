# WiFi Cloak

<!-- impeccable:product-schema 1 -->

## Platform

android

## Stack

用户确认 Kotlin + Jetpack Compose，原生 APK，最低 Android 10。

## Users

已安装 LSPosed 的 Android 用户，按应用控制对 WiFi 标识信息的读取。

## Product Purpose

扫描附近 WiFi 并选择使用，自动保存为备用 WiFi。管理受控应用，离开 WiFi 覆盖范围后从备用列表选择已保存的身份。

## Capabilities and Constraints

用户于 2026-10-07 明确要求移除手动新建和编辑，仅保留附近 WiFi 扫描与备用 WiFi 选择。扫描自动取得 SSID、BSSID、频率、信号强度；选用时自动读取设备 WiFi MAC，优先系统接口，受限制时使用用户授权的 Root 读取无线接口地址。Root 拒绝、超时或 MAC 无效时保留已有选择并提示重试，不填入伪造值。选用会自动保存完整身份供离线复用，同 SSID 的不同 BSSID 分别保留；重复选用同一身份更新备用项。切换 WiFi 时统一更新受控应用的身份。应用列表保留搜索、勾选与实际授权状态，通过官方现代 LSPosed service API 申请或移除作用域。框架授权由用户在提示中完成。版本不兼容时提供框架管理入口。

## Operating Context

原生 Android 管理端与 LSPosed 注入模块包含于同一 APK。仅控制目标应用读取的 WiFi 信息，不改变系统无线连接。无可用设备连接的情况下，编译验证与真实 LSPosed 验证分开报告。

## Product Principles

扫描和选择即可完成；采集成功后自动保存；备用 WiFi 无需重新扫描或 Root 即可复用；授权和采集状态明确；界面仅保留必要标签与操作反馈，不添加说明性文本注释。
