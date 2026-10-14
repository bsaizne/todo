# 高效Todo (纯净离线版) - 工程交接文档

## 一、 项目基础信息

| 属性 | 内容 |
| :--- | :--- |
| **项目名称** | 高效Todo (纯净离线版) / Todo Offline |
| **包名** | `net.icycloud.fdtodolist.local` |
| **版本号** | `versionCode 1`, `versionName 1.0.0` |
| **代码仓库** | `git@github.com:bsaizne/todo.git` |
| **运行平台** | Android 5.0 (API 21) 至 Android 14+ (API 34+) |
| **目标设备** | 主流 Android 手机（支持澎湃OS、鸿蒙OS、MIUI、ColorOS、OriginOS等） |
| **安装包大小**| **~51 KB** |
| **权限要求** | 仅声明屏幕适配与基本硬件特性，**零敏感权限、零网络权限** |

---

## 二、 交接背景与设计初衷

1. **原版 APK 的问题与淘汰**：
   * 原版商业 APK（20MB+）内置大量第三方广告、百度推送与定位、微信/支付宝/微博支付与分享等冗余 SDK。
   * 原版在现代系统（Android 13/14）上因前台服务启动限制（ForegroundService）以及复杂的账号初始化，频繁触发全局未捕获异常并弹窗崩溃（“很抱歉,程序出现异常,请重新打开”）。
   * 逆向修补成本极高且稳定性差，故全面转向纯原生极简重构路线。

2. **纯原生重构核心原则**：
   * **零依赖、零 SDK**：不引入任何第三方复杂类库，全部基于 Android 原生 SDK 与原生控件编写。
   * **100% 离线与隐私安全**：完全本地 SQLite 存储，不发起任何 HTTP/Socket 网络连接。
   * **四大核心场景明确分离**：彻底拆分【四象限】、【时间轴】、【清单】、【备忘录】，解决旧版本待办与备忘功能混杂冲突的痛点。

---

## 三、 核心业务逻辑与交互规则

### 1. 四象限管理（当前聚焦待办）
* **定位**：专注于“当前待办”。仅展示尚未完成的事项（`is_completed = 0`）。
* **自动隐去交互**：
  * 用户在四象限内点击 CheckBox 标记完成时，程序首先应用中划线并保持选中，提供 **250ms 的视觉反馈**；
  * 延迟结束后，触发局部重绘，已完成任务**自动从四象限卡片中移出**，右上角数字减少；
  * 保持四象限始终干净，只呈现需要处理的待办，免除手动点击“✕”逐个删除的困扰。

### 2. 时间轴模块（时间维度轨迹）
* **定位**：按时间先后顺序（`due_time`）编排一天与近期的任务流。
* **数据保留**：四象限中已完成的任务**在时间轴中依然完好保留**，并显示为已划线完成状态。
* **节点颜色指示**：垂直轴线上的圆点根据象限优先级（红/黄/蓝/绿）着色，快速识别轻重缓急。

### 3. 清单列表（全局管理与误操作恢复）
* **定位**：待办事项的全局视图，支持【全部】、【未完成】、【已完成】Tab筛选。
* **撤销与重入四象限**：如果用户误操作在四象限中完成了任务，只需进入【清单】->【已完成】，取消勾选该任务，任务状态即重置为未完成，并**自动重新回到四象限矩阵的对应象限中**。

### 4. 独立备忘录便签（随手速记）
* **定位**：独立的灵感速记与草稿本。
* **物理隔离**：使用独立的 SQLite 表 `local_note`，不与 `local_task` 混用，新增、查看、删除均在独立页面进行，支持一键复制到剪贴板。

---

## 四、 核心源码与模块清单

```text
src/net/icycloud/fdtodolist/local/
│
├── MainActivity.java
│   ├── switchTab(int tab)               # 底部 4 个主模块切换调度器
│   ├── loadQuadrants() / populateQuadrant # 四象限 2x2 宫格渲染与自动隐去监听
│   ├── loadTimeline()                   # 时间轴列表加载与适配器绑定
│   ├── loadAllTasks()                   # 清单列表加载与筛选（全部/未完成/已完成）
│   ├── loadNotes()                      # 独立便签加载与复制监听
│   ├── showAddTaskDialog(int quadrant)  # 新增待办弹窗（含日期时间原生选择器）
│   └── showAddNoteDialog()              # 新增便签弹窗
│
├── DatabaseHelper.java
│   ├── TABLE_TASK (local_task)          # 待办任务表
│   ├── TABLE_NOTE (local_note)          # 备忘便签表
│   ├── getTasksByQuadrant(int q)        # 查询指定象限未完成待办 (is_completed=0)
│   ├── getTimelineTasks()               # 查询按执行时间升序排列的全量待办
│   ├── getAllTasks(Integer filter)      # 查询全量/未完成/已完成待办
│   └── insertNote / getAllNotes         # 便签独立 CRUD
│
├── Task.java                            # 待办数据实体 (含 quadrant, dueTime 等字段)
└── Note.java                            # 便签数据实体 (含 content, createdAt 等字段)
```

---

## 五、 构建与签名环境配置

### 1. 环境依赖
* **JDK**：Java 8 (OpenJDK 1.8 / JetBrains Runtime 1.8+)
* **构建工具集**：
  * `aapt2.exe`：资源编译与链接
  * `android.jar`：Android SDK 平台 API 类库
  * `r8.jar`：包含 D8 工具，将 class 编译为 classes.dex
  * `uber-apk-signer.jar`：APK 对齐（zipalign）与注入 v1/v2/v3 强签名

### 2. 一键构建指令
在项目根目录下通过 PowerShell 运行：
```powershell
.\build.ps1
```
脚本将全自动清理、编译资源、编译 Java 代码、生成 DEX、注入 classes.dex、执行 4 字节对齐、生成 v1/v2/v3 签名，并输出至目标路径 `D:\clean_todo_offline.apk`。

---

## 六、 维护与后续扩展建议

1. **数据备份与导出**：
   * 当前 SQLite 数据库文件位于 `/data/data/net.icycloud.fdtodolist.local/databases/clean_todo.db`；
   * 后续如需增加本地备份功能，可在设置中增加“导出为 JSON / 导入 JSON”的本地文件读写操作。
2. **桌面微件（App Widget）**：
   * 可利用 Android 原生 `AppWidgetProvider` 实现 4×2 或 2×2 的四象限桌面小组件。
3. **本地定时提醒（AlarmManager）**：
   * 当前 `Task` 模型已具备 `dueTime` 规划执行时间戳，可扩展通过系统 `AlarmManager` 设置本地准时振动/通知提醒。

---

## 七、 交接确认

* **交付产物**：完整源代码、资源文件、构建脚本、README.md、HANDOVER.md、已签名的离线安装包 `clean_todo_offline.apk`。
* **测试验证**：已在主流系统（Android 11+ / 小米澎湃OS）真机实机验证安装与四象限、时间轴、清单、备忘录全流程功能闭环。
