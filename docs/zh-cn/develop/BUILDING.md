# 构建指南

## 环境准备

- 安装 [Eclipse Temurin JDK 25](https://adoptium.net/zh-CN/temurin/releases?version=25)

- 安装 [Android Studio](https://developer.android.com/studio)

- 下载 MAA Core 预编译产物（so 库 + 资源文件）

  ```bash
  python scripts/setup_maa_core.py
  ```

## 构建步骤

- 使用 Android Studio 打开此文件夹，在 Settings - Build, Execution, Deployment - Build Tools - Gradle - Gradle Projects - Gradle JDK 选择此前安装的 temurin-25

- 运行 Sync Project with Gradle Files，Android Studio 将自行安装其他依赖，完成后运行 Assemble app Run Configuration 即可构建apk。

## 使用相同签名覆盖安装

GitHub Actions 在 `main`、`dev` 和 Release 构建中使用同一套发布签名。若需要从 Android Studio 直接覆盖安装这些 APK，请在未提交的 `local.properties` 中配置同一密钥：

```properties
KEYSTORE_PATH=/absolute/path/to/release.jks
KEYSTORE_PASSWORD=your_store_password
KEY_ALIAS=your_key_alias
KEY_PASSWORD=your_key_password
```

配置后，Debug 和 Release 变体都会使用该密钥。未配置时，本地 Debug 与 Pull Request 构建仍使用 Android 默认调试签名。
