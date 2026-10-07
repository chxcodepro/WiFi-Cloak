# 发布流程

GitHub 仓库：<https://github.com/chxcodepro/WiFi-Cloak>。

## 签名配置

在仓库 Settings → Secrets and variables → Actions 保存以下 Repository secrets：

| Secret | 内容 |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | 与已发行 APK 一致的 keystore 的 Base64 内容 |
| `ANDROID_KEYSTORE_PASSWORD` | keystore 密码 |
| `ANDROID_KEY_ALIAS` | 签名 key alias |
| `ANDROID_KEY_PASSWORD` | 签名 key 密码 |

密钥不能提交到 Git。Actions 在临时目录还原密钥，使用 Gradle 的 `RELEASE_KEYSTORE_PATH`、`RELEASE_KEYSTORE_PASSWORD`、`RELEASE_KEY_ALIAS` 和 `RELEASE_KEY_PASSWORD` 环境变量构建，任务结束后删除临时密钥。

`.github/signing-certificate.sha256` 保存与现有安装包相同的公开证书摘要，发布前强制核对。当前发行沿用最初的本机开发签名；替换密钥会失去旧安装包的直接覆盖安装兼容性。

## 发布新版本

1. 修改 `app/build.gradle.kts` 的 `versionName`，递增 `versionCode`。
2. 在 `CHANGELOG.md` 增加对应版本的 `## X.Y.Z - YYYY-MM-DD` 节，写入发行说明。
3. 提交代码并推送 `main`，再创建与 versionName 一致的注解标签：

```bash
git add app/build.gradle.kts CHANGELOG.md
git commit -m "chore: release vX.Y.Z"
git push origin main
git tag -a vX.Y.Z -m "Release vX.Y.Z"
git push origin vX.Y.Z
```

`.github/workflows/release.yml` 自动安装 JDK 21、Android SDK 36 与 Build Tools 35.0.0，执行 26 项现有单元测试、Lint 和 Release 构建。任何测试、静态检查、版本或签名校验失败都会阻止发布。真实设备验收范围见 `artifacts/VERIFICATION.md`。

成功后，GitHub Release 包含 `WiFiCloak-X.Y.Z.apk` 和本次构建生成的 `checksums.sha256`。Actions 同时保留构建资产和测试报告。源码仓库不提交历史本机 APK，也不使用本机历史校验文件作为 CI APK 的校验值。

若发生临时构建或上传故障，在 Actions 中重跑失败任务；也可通过 CLI 对已存在的标签运行：

```bash
gh workflow run release.yml --ref main -f tag=vX.Y.Z
```

手动运行使用 `main` 上的最新工作流，在 `tag` 输入中指定已存在的版本标签；工作流强制检出该标签的源码并验证版本。这样可以修复 SDK 安装等发布工具问题后构建原标签，无需移动已发布的标签。重跑同一标签可以补齐或更新对应 Release 的资产及校验文件。运行时源码修改应增加新版本并推送新标签。
