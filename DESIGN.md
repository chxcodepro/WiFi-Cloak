---
name: WiFi Cloak
description: 原生 Android 上按应用管理可复用的 WiFi 身份，呈现真实框架与授权状态。
colors:
  light-primary: "#275D4E"
  dark-primary: "#A9D0B8"
  light-primary-container: "#D8E9DD"
  dark-primary-container: "#284D3B"
  light-secondary: "#526557"
  dark-secondary: "#BBCBB8"
  light-background: "#F6F3EA"
  dark-background: "#111812"
  light-surface: "#FFFEF8"
  dark-surface: "#192019"
  light-on-surface: "#202720"
  dark-on-surface: "#E1E8DC"
  light-on-surface-variant: "#465345"
  dark-on-surface-variant: "#C1CCB9"
  light-surface-variant: "#E5E8DC"
  dark-surface-variant: "#3B473A"
  light-outline: "#738174"
  dark-outline: "#8B998A"
  light-outline-variant: "#C5CDBC"
  dark-outline-variant: "#465342"
  light-error: "#A33830"
  dark-error: "#FFB4A8"
  light-error-container: "#FFDAD4"
  dark-error-container: "#74332A"
typography:
  headline-small:
    fontFamily: "Roboto, sans-serif"
    fontSize: "24sp"
    fontWeight: 400
    lineHeight: "32sp"
  headline-medium:
    fontFamily: "Roboto, sans-serif"
    fontSize: "28sp"
    fontWeight: 400
    lineHeight: "36sp"
  title-large:
    fontFamily: "Roboto, sans-serif"
    fontSize: "22sp"
    fontWeight: 400
    lineHeight: "28sp"
  title-medium:
    fontFamily: "Roboto, sans-serif"
    fontSize: "16sp"
    fontWeight: 500
    lineHeight: "24sp"
  body-large:
    fontFamily: "Roboto, sans-serif"
    fontSize: "16sp"
    fontWeight: 400
    lineHeight: "24sp"
  body-medium:
    fontFamily: "Roboto, sans-serif"
    fontSize: "14sp"
    fontWeight: 400
    lineHeight: "20sp"
  body-small:
    fontFamily: "Roboto, sans-serif"
    fontSize: "12sp"
    fontWeight: 400
    lineHeight: "16sp"
  label-large:
    fontFamily: "Roboto, sans-serif"
    fontSize: "14sp"
    fontWeight: 500
    lineHeight: "20sp"
  label-medium:
    fontFamily: "Roboto, sans-serif"
    fontSize: "12sp"
    fontWeight: 500
    lineHeight: "16sp"
rounded:
  content: "16dp"
spacing:
  xs: "4dp"
  sm: "8dp"
  md: "12dp"
  lg: "16dp"
  lg-plus: "20dp"
  xl: "24dp"
  section: "48dp"
  control: "56dp"
components:
  button-primary:
    backgroundColor: "{colors.light-primary}"
    textColor: "#FFFFFF"
    typography: "{typography.label-large}"
    padding: "16dp 24dp"
    height: "48dp"
  button-tonal:
    backgroundColor: "{colors.light-primary-container}"
    textColor: "{colors.light-on-surface}"
    typography: "{typography.label-large}"
    padding: "16dp 24dp"
    height: "48dp"
  field-outlined:
    backgroundColor: "{colors.light-surface}"
    textColor: "{colors.light-on-surface}"
    typography: "{typography.body-large}"
    padding: "16dp"
    height: "56dp"
  chip-assist:
    backgroundColor: "{colors.light-surface}"
    textColor: "{colors.light-on-surface}"
    typography: "{typography.label-large}"
    padding: "8dp 12dp"
    height: "40dp"
  card-current-profile:
    backgroundColor: "{colors.light-primary-container}"
    textColor: "{colors.light-on-surface}"
    rounded: "{rounded.content}"
    padding: "24dp"
  nav-item:
    backgroundColor: "{colors.light-surface}"
    textColor: "{colors.light-on-surface-variant}"
    typography: "{typography.label-large}"
    padding: "12dp"
    height: "80dp"
---

# 松绿手帐 · WiFi Cloak 1.1.0

## 视觉与平台

沿用用户确认的松绿、暖白 Material 3 视觉。Android 原生组件负责导航、选择、权限、状态与反馈；BSSID、MAC 使用系统等宽字体。浅色和深色角色来自 `ui/Theme.kt`。不增加装饰阴影、纹理或渐变。宽度 600dp 以下使用导航栏，以上使用导航轨；WiFi 工作区最大 720dp，设置最大 640dp。

## 首页结构

WiFi / 应用 / 设置三目的地。WiFi 页依次显示框架服务连接状态、已选 WiFi 身份、主操作“扫描 WiFi”、次要操作“手动添加”、附近与备用两个标签页。服务连接不推断模块启用或目标应用注入状态；设置页分别呈现框架服务与模块开关，后者显示“请在 LSPosed 查看”。当前身份使用 16dp 圆角、24dp 内边距、16dp 分组间距，SSID 使用 headlineSmall，BSSID/MAC 使用 bodyMedium。

附近列表显示 SSID、BSSID、频段、信号强度；备用列表显示 SSID、BSSID、最近使用日期与次数。整行通过原生 RadioButton 与 selectable 语义选择，行内间隔 12dp，数据间隔 6dp，外层左右间隔 24dp。备用行保留移除操作，当前或被引用项须先切换 WiFi。列表不使用嵌套卡片。

## 交互

扫描显示进度并接收系统扫描广播；受限或超时时，缓存结果标明“最近扫描结果”。选择附近 WiFi 时自动采集设备 MAC，进度显示“采集设备 MAC”；成功后保存为备用并应用。同 SSID/BSSID 的重复选用更新同一项；不同 BSSID 分别保存。失败时保留上一选择并提示重试。

选择备用 WiFi 使用原始 SSID、BSSID、MAC，保持离线复用，不发起扫描或 Root 读取。所有受控应用统一跟随所选身份。应用页只有搜索、筛选、作用域勾选和身份状态，不提供逐应用配置选择。

手动添加使用 Material AlertDialog，三个必填字段分别为 SSID、BSSID 和 MAC。字段保留草稿，显示格式校验错误，支持键盘“下一项”和“完成”；表单正文可滚动。确认按钮明确标为“保存并使用”，保存失败保留内容供重试；成功后显示备用标签页。相同 SSID/BSSID 更新已有身份及 MAC，保留历史次数和扫描元数据。手动添加不请求扫描权限或 Root；当前请求不增加独立编辑页。旧版备用数据保留，旧逐应用分配统一到当前选择。

## 状态与反馈

空扫描、无结果、空备用、扫描中、读取 MAC 中、权限拒绝、定位关闭、Root 拒绝或超时、MAC 受限、保存失败、框架断开、授权申请中或拒绝、同步失败均有简短状态和恢复入口。采集设备 MAC 时禁止冲突选择。使用 Snackbar 返回瞬时结果，移除确认使用 Material AlertDialog。

扫描获取接入点 MAC，即 BSSID；设备 MAC 优先系统接口读取，受限制时请求 Root 读取当前无线接口地址。无法读取时不保存占位值，也不把 BSSID 当作设备 MAC。

## 原生保证与证据

触摸目标遵循 Material 48dp 最小规范；列表可滚动，长 SSID 可换行，使用 window insets 与 edge-to-edge。没有可用原生截图和设备，因此文档描述的是源码约束，不代表视觉、Root 或 LSPosed 运行验收。受控 WiFi 对象合成失败返回空值或空列表，保留上一版的异常保护。
