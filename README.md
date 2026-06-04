# 沿语（LORE）

## 简介

**沿语** 是一款基于 Android 平台的智能小说写作助手应用。它集成了 OpenAI 大语言模型，通过 AI Agent 工具系统，为作者提供从章节编辑、角色管理、世界观构建到时间线梳理的全方位写作支持。应用采用 Material 3 设计语言与 Jetpack Compose 构建现代 UI，支持本地数据库持久化存储，并内置写作统计与版本历史功能。

### 主要特性

- **小说与章节管理**：创建多部小说，支持分卷、章节的树形组织结构
- **AI 智能助手**：基于 OpenAI API 的对话式写作辅助，支持流式输出
- **Agent 工具系统**：AI 可调用 20+ 内置工具，包括创建/编辑/删除章节、角色、世界观、笔记，搜索内容，统计字数，检查一致性，润色/重写选中文本，生成大纲等
- **角色管理**：建立角色档案，包含姓名、性别、年龄、性格、背景故事等详细属性
- **世界观构建**：管理世界观设定条目，支持分类与层级关系
- **时间线管理**：记录故事事件的时间脉络，确保叙事一致性
- **笔记与灵感**：随时记录写作灵感和创作思路
- **草稿系统**：支持章节多版本草稿保存
- **内容历史**：记录每次编辑的历史版本，支持内容回溯
- **写作统计**：字数统计、日写作量追踪
- **数据导出/导入**：支持 TXT 格式导出、项目完整备份与恢复
- **安全锁**：支持应用级别的密码保护

---

## 如何构建

### 环境要求

| 工具/环境 | 版本要求 |
|-----------|---------|
| Android Studio | Ladybug 及以上 |
| JDK | 11 或更高 |
| Gradle | 8.x（使用项目自带 wrapper） |
| Android SDK | API 34（compileSdk） |
| Kotlin | 2.0.21 |

### 构建步骤

1. **克隆项目**
   ```bash
   git clone <仓库地址>
   cd NovelEditor
   ```

2. **配置 Android SDK**
   
   在 `local.properties` 中指定 Android SDK 路径（如不存在请手动创建）：
   ```properties
   sdk.dir=E\:\\build_tools\\android_sdk
   ```

3. **使用 Android Studio 打开项目**

   用 Android Studio 打开项目根目录，等待 Gradle 同步完成。

4. **构建与运行**
   ```bash
   # 调试构建
   ./gradlew assembleDebug

   # 发布构建（已开启混淆与资源压缩）
   ./gradlew assembleRelease
   ```

   或直接在 Android Studio 中点击 **Run** 按钮运行到设备/模拟器。

### 关键构建配置

- **minSdk**: 24（Android 7.0）
- **targetSdk**: 34（Android 14）
- **混淆**：Release 模式已开启 R8 混淆与资源压缩
- **版本号**：`Bicy V1.260603`

---

## 详细介绍

### 技术栈

| 类别 | 技术选择 |
|------|---------|
| 语言 | Kotlin |
| UI 框架 | Jetpack Compose + Material 3 |
| 架构模式 | MVVM（ViewModel + Repository） |
| 依赖注入 | Hilt（Dagger） |
| 本地数据库 | Room（SQLite 抽象层） |
| 数据存储 | DataStore Preferences |
| 导航 | Navigation Compose |
| 异步处理 | Kotlin Coroutines + Flow |
| AI 集成 | OpenAI API（兼容接口） |
| 序列化 | Kotlinx Serialization |
| Markdown 渲染 | Compose Markdown |
| 动画 | Lottie Compose |
| 安全加密 | Security Crypto |

### 项目结构

```
NovelEditor/
├── app/
│   ├── src/main/
│   │   ├── java/com/bicy/novel/
│   │   │   ├── data/
│   │   │   │   ├── agent/          # AI Agent 工具集（20+ 工具定义）
│   │   │   │   ├── ai/             # AI 服务层（OpenAI Provider）
│   │   │   │   ├── export/         # 导出/导入服务
│   │   │   │   ├── local/          # Room 数据库（Entity、DAO、Database）
│   │   │   │   ├── preferences/    # DataStore 偏好设置
│   │   │   │   └── repository/     # Repository 实现层
│   │   │   ├── di/                 # Hilt 依赖注入模块
│   │   │   ├── domain/
│   │   │   │   ├── model/          # 领域模型（Chapter、Novel、Character 等）
│   │   │   │   └── repository/     # Repository 接口定义
│   │   │   ├── service/            # Android Service（AI 请求）
│   │   │   ├── ui/
│   │   │   │   ├── components/     # 可复用 UI 组件
│   │   │   │   ├── navigation/     # 导航图与路由定义
│   │   │   │   ├── screens/        # 各功能页面（Screen + ViewModel）
│   │   │   │   └── theme/          # 主题与排版定义
│   │   │   └── util/               # 工具类（日志、加密、文件、网络等）
│   │   └── res/                    # 资源文件
│   └── build.gradle.kts            # App 模块构建脚本
├── gradle/
│   ├── libs.versions.toml          # 版本目录（统一依赖管理）
│   └── wrapper/                    # Gradle Wrapper
├── build.gradle.kts                # 根项目构建脚本
├── settings.gradle.kts             # 项目设置
├── gradle.properties               # Gradle 全局属性
├── gradlew / gradlew.bat           # Gradle Wrapper 脚本
└── local.properties                # 本地 SDK 配置
```

### 核心功能详解

#### 1. AI Agent 工具系统

应用内置了一套完整的 Agent 工具系统，AI 助手可以自主选择并调用以下工具：

- **novel_info** — 获取当前小说基本信息
- **search** — 在章节、角色、世界观、笔记中搜索关键词
- **view** — 查看指定内容的详细信息
- **create** — 创建新章节、角色、世界观条目、笔记、时间线事件
- **edit** — 编辑已有内容
- **delete** — 删除指定条目
- **move_chapter** — 移动章节位置
- **merge_chapters** — 合并多个章节
- **split_chapter** — 拆分章节
- **list_*** — 列出各类型的全部条目
- **count_words** — 统计字数
- **check_consistency** — 检查内容一致性
- **rewrite_selection** — 重写选中文段
- **polish_selection** — 润色选中文段
- **generate_outline** — 生成写作大纲

#### 2. 数据持久化

使用 Room 数据库管理以下实体：
- `NovelEntity` — 小说信息
- `VolumeEntity` — 卷信息
- `ChapterEntity` — 章节内容
- `CharacterEntity` — 角色档案
- `WorldviewEntity` — 世界观条目
- `NoteEntity` — 笔记
- `TimelineEntity` — 时间线事件
- `DraftEntity` — 草稿
- `AIOperationEntity` — AI 操作记录
- `ChatEntity` — 聊天会话与消息

#### 3. AI 对话

- 支持自定义 API 端点和 API Key
- 流式输出，实时显示 AI 生成内容
- 对话历史本地持久化存储
- 支持 Foreground Service 保证后台 AI 请求不中断

### 许可证

内部项目，保留所有权利。
