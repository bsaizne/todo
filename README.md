# 高效Todo (纯净离线版) - Todo Offline

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](#)
[![Platform](https://img.shields.io/badge/Platform-Android%205.0%2B%20(API%2021%2B)-green.svg)](#)
[![Size](https://img.shields.io/badge/APK%20Size-~46%20KB-brightgreen.svg)](#)
[![Network](https://img.shields.io/badge/Network-0%20Requests%20(Offline)-orange.svg)](#)

一款极致轻量（安装包仅 **46 KB**）、极速冷启动、**100% 纯本地离线** 的专业时间管理与待办效率应用。

项目旨在解决原版 Todo 应用体积臃肿（20MB+）、塞满各类第三方推送/定位/支付 SDK、频繁后台常驻与闪退崩溃的问题。通过原生轻量重构，还原了最经典的 **2×2 四象限法则矩阵** 与 **垂直时间轴**，并将 **便签备忘录** 彻底解耦独立，提供纯粹、专注、安全的时间管理体验。

---

## 🌟 核心功能模块

### 1. 经典 2×2 四象限法则（专注待办）
基于艾森豪威尔法则（Eisenhower Matrix）设计：
* **第一象限（左上）**：重要且紧急（浅红底色 `#FFDEDE`，高亮深红字）
* **第二象限（右上）**：重要不紧急（浅黄底色 `#FDF4C3`，高亮深黄字）
* **第三象限（左下）**：紧急不重要（浅蓝底色 `#CAEFFF`，高亮深蓝字）
* **第四象限（右下）**：不重要不紧急（浅绿底色 `#E0F4BE`，高亮深绿字）
* **智能自动归档**：四象限专用于呈现“当前待处理事项”。勾选任务完成时提供 **250ms 划线视觉反馈**，随后自动从四象限中隐去，右上角计数同步更新，保持工作台干净聚焦，无需手动删除。

### 2. 垂直时间轴模块（Timeline）
* **时间节点与连接线**：左侧展示垂直轴线，并在对应时刻打上圆点节点，圆点颜色根据四象限优先级动态渲染（红/黄/蓝/绿）。
* **全景时间流**：以时间先后顺序呈现日程计划与历史记录，已完成事项以划线保留在时间轴中，便于复盘当日执行轨迹。
* **规划时间设置**：添加任务时支持通过原生选择器设置具体的日期与执行时刻（默认当前时间）。

### 3. 清单列表（全局管理与恢复）
* **多维度筛选**：提供【全部】、【未完成】、【已完成】快速切换。
* **撤销与重入四象限**：在【已完成】列表中若不小心勾选错，只需取消勾选，该任务便会立刻恢复到未完成状态，并自动重新回到四象限的对应格子中。

### 4. 独立备忘录便签（随手速记）
* **与待办彻底解耦**：拥有独立的 SQLite 数据库表，界面与数据流完全隔离，互不干扰。
* **便签卡片流**：仿便签纸浅黄色卡片排布，记录随手灵感、草稿与信息，支持一键「复制到剪贴板」。

---

## 🛠️ 技术栈与架构设计

| 维度 | 技术选型 | 说明 |
| :--- | :--- | :--- |
| **开发语言** | Java 8 (纯原生) | 兼容性广，字节码精简，无 Kotlin 运行时开销 |
| **目标平台** | Android SDK 21 ~ 31+ | 最低兼容 Android 5.0，全面支持 Android 11/12/13/14+ 及澎湃OS/鸿蒙 |
| **数据存储** | 本地 SQLite (原生 `SQLiteOpenHelper`) | 零三方 ORM 框架，轻量直接，支持平滑数据库升级迁移 |
| **网络能力** | **无 (零网络权限)** | `AndroidManifest.xml` 中剔除 `INTERNET` 与任何网络权限，物理断网级安全 |
| **SDK 依赖** | **0 第三方库** | 无推送、无广告、无统计、无任何第三方服务，杜绝隐私泄漏与后台耗电 |
| **构建体系** | `aapt2` + `javac` + `d8` + `uber-apk-signer` | 工业级纯脚本自动化打包流水线，生成 v1/v2/v3 强签名 APK |

---

## 📂 源码目录结构

```text
clean_todo_app/
├── AndroidManifest.xml        # 清单文件 (声明 Activity、主题、图标，无敏感权限)
├── build.ps1                  # 自动化一键编译、DEX、打包、强签名脚本
├── .gitignore                 # Git 忽略配置
├── README.md                  # 项目架构与技术文档
├── HANDOVER.md                # 团队工程交接文档
├── res/                       # 资源目录
│   ├── drawable/              # 象限圆角卡片、时间轴连接线与节点形状
│   │   ├── bg_card_white.xml
│   │   ├── bg_dot_1.xml ~ 4.xml
│   │   ├── bg_note_card.xml
│   │   └── bg_quad_1.xml ~ 4.xml
│   ├── layout/                # UI 布局文件
│   │   ├── activity_main.xml  # 主界面 (四象限宫格 + 时间轴 + 清单 + 备忘录)
│   │   ├── dialog_add_task.xml# 新增待办对话框 (含象限选择与时间拾取器)
│   │   ├── dialog_add_note.xml# 新增便签备忘录对话框
│   │   ├── item_task.xml      # 清单待办条目
│   │   ├── item_timeline.xml  # 时间轴条目 (轴线 + 节点 + 卡片)
│   │   ├── item_quadrant_task.xml # 2x2 宫格内紧凑待办条目
│   │   └── item_note.xml      # 便签卡片条目 (带复制按钮)
│   ├── mipmap-*/              # 高清位图图标 (兼容高版本系统安装器扫描)
│   └── values/                # 颜色规范、字符串常量与样式
│       ├── colors.xml
│       ├── strings.xml
│       └── styles.xml
└── src/                       # Java 源代码
    └── net/icycloud/fdtodolist/local/
        ├── MainActivity.java  # 核心业务控制器 (多Tab切换、事件调度、交互逻辑)
        ├── DatabaseHelper.java# 本地 SQLite 数据引擎 (Task 与 Note 的 CRUD)
        ├── Task.java          # 待办任务实体数据模型
        └── Note.java          # 独立便签备忘实体数据模型
```

---

## 💾 数据库设计

### 1. 待办任务表 (`local_task`)
```sql
CREATE TABLE local_task (
    _id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,           -- 待办标题
    description TEXT,              -- 详细描述
    quadrant INTEGER DEFAULT 1,    -- 四象限分类 (1:重急, 2:重不急, 3:急不重, 4:不急不重)
    is_completed INTEGER DEFAULT 0,-- 完成状态 (0:未完成, 1:已完成)
    created_at INTEGER DEFAULT 0,  -- 创建时间戳
    due_time INTEGER DEFAULT 0     -- 时间轴执行时间戳
);
```

### 2. 便签备忘表 (`local_note`)
```sql
CREATE TABLE local_note (
    _id INTEGER PRIMARY KEY AUTOINCREMENT,
    content TEXT NOT NULL,         -- 便签内容
    created_at INTEGER DEFAULT 0   -- 记录时间戳
);
```

---

## 🚀 编译与构建

在包含 `android_toolchain`（内置 `aapt2`、`android.jar`、`r8.jar`、`uber-apk-signer.jar`）与 Java 8 环境的终端中直接执行：

```powershell
.\build.ps1
```

构建脚本将自动执行资源编译、R 类生成、Java 源码编译、D8 DEX 处理、ZIP 对齐并注入 **v1、v2、v3** 强签名，最终输出可直接安装的离线 APK。
