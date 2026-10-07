# Agent Note: GitHub Actions APK 发布与签名连续性

Status: implemented

## Problem

项目此前仅在本机构建 APK。新的 GitHub 仓库需要通过 Actions 构建和发布，且已安装旧版的用户必须能覆盖安装。如果直接使用 CI 自动生成的 debug 密钥，证书会与已有版本不同，Android 将拒绝覆盖安装。

## Decision

仓库以项目的 WiFi 身份管理功能命名为 WiFi-Cloak，按用户选择公开。首个公开版本使用已经修复配置持久化的 1.1.3，不增加运行时版本。`v*` 标签触发测试、Lint、构建、签名校验及 GitHub Release 发布。标签版本必须与 Gradle 的 versionName 和更新日志相符。

签名密钥仅保存在 GitHub Secrets，在 runner 临时目录还原；仓库只保存既有证书的公开 SHA-256。CI 发布禁止自动生成替代密钥，发布前验证证书一致。GitHub Release 附带 APK 和本次 CI 构建产生的校验文件。本地历史 APK 与本机校验文件不进入源码仓库。测试、Lint 或签名校验失败均阻止发布；发布先使用草稿，资产上传后再公开。

## Alternatives considered

直接上传本机已有 APK 可以最快取得下载链接，但无法满足用户要求的 Actions 构建发布及后续可重复流程。采用 CI 默认 debug 签名能省去 Secrets 管理，却破坏与现有安装包的签名连续性。因此使用原有密钥的 Secrets 配置。

## Consequences

后续版本在更新 Gradle 版本与 CHANGELOG 后推送对应的版本标签即可发布。维护者必须保留现有密钥与 Secrets；密钥缺失时流水线明确失败。当前证书沿用本机开发签名，保持旧版覆盖安装兼容性。源码公开并不包括密钥、SDK 本机路径、缓存或构建产物。重跑同一标签更新对应 Release 资产与校验文件，不移动已发布的标签。

历史笔记检索未发现既有 `.agents/notes/`，此记录没有重叠项。流水线首次远程执行结果在 GitHub Actions 中核对；APK 的真实设备安装与 LSPosed 注入验收仍保留此前的验证边界。
