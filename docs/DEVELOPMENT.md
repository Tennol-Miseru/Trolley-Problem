# 开发与维护说明

## 环境

- Minecraft Java 1.21.1
- NeoForge 21.1.244
- Java 21
- Windows 全局 Gradle 8.14.6 / GitHub CI Gradle 8.14.3 / ModDevGradle 2.0.143
- 编码：UTF-8

## 主要模块

| 模块 | 路径 |
|---|---|
| 物品/实体注册 | `src/main/java/cn/blockforge/generated/mode539c988/GeneratedMod.java` |
| 电车逻辑与 BOSS | `entity/TrainEntity.java`，`dimension/TerminalBoss.java` |
| 伦理难题 | `dilemma/` |
| 白色终点站 | `dimension/` |
| 物品使用逻辑 | `item/` |
| 客户端模型 | `client/`、`src/main/resources/assets/mod_e539c988/models/` |
| 多人输入同步 | `network/` |

## 构建和测试

```bash
gradle --no-daemon clean build
gradle runClient
```

Windows 可使用仓库根目录的 `一键编译_JAR.bat`，无需提交下载好的 `tools/`。在 Linux/macOS 上先安装 Java 21 和 Gradle 8.14.3。

CI 首先运行 `python3 scripts/check_repository.py` 静态检查，再用 Gradle 8.14.3 和 JDK 21 构建。若 CI 构建失败，应按日志修复，不应仅因 JSON/Java 基本校验通过而宣称可运行。

## 发布新版本

1. 修改 `gradle.properties` 中 `mod_version`，保持 Mod ID 不变。
2. 更新 `CHANGELOG.md` 和 README 中版本号。
3. 运行 `gradle clean build` 并在游戏内验证。
4. 上传 `build/libs/` 下实际生成的主 JAR 到 GitHub Releases / 模组平台。
5. 不要将 `.gradle/`、`build/`、`dist/`、`tools/`、世界存档或 `build.log` 提交到仓库。

## 兼容性注意

修改物品/实体/维度注册名称可能影响旧世界；保存旧存档备份后再测试。电车维度结构在玩家接近区域时增量生成，变动世界生成逻辑前应同时验证新旧存档。
