# WebDAV 云端备份功能

基于 [CatVodSpider](https://github.com/FongMi/CatVodSpider) 项目扩展的 WebDAV 云端备份/恢复功能。

## 功能概述

本模块为 CatVodSpider 添加了 WebDAV 云端备份能力，支持将应用配置、观看历史、收藏列表等数据备份到 WebDAV 服务器，并支持从云端拉取备份进行恢复。

### 核心功能

| 功能 | 说明 |
|------|------|
| **上传备份** | 将本地数据上传到 WebDAV 云端保存 |
| **下载备份** | 从云端下载指定备份或最新备份 |
| **备份列表** | 列出云端所有备份文件及详细信息 |
| **删除备份** | 删除指定的云端备份文件 |
| **自动清理** | 保留最近 N 个备份，自动清理旧备份 |
| **连接测试** | 验证 WebDAV 服务器连接是否正常 |

### 支持的 WebDAV 服务

- 群晖 NAS (Synology WebDAV Server)
- 坚果云 (`https://dav.jianguoyun.com/dav/`)
- Nextcloud / ownCloud
- 路由器挂载硬盘 (OpenWrt + WebDAV)
- 任何标准 WebDAV 协议服务器

## 文件结构

```
app/src/main/java/com/github/catvod/
├── bean/webdav/
│   ├── BackupConfig.java          # WebDAV 备份配置 Bean
│   ├── BackupInfo.java            # 备份文件信息 Bean
│   ├── Drive.java                 # 原有 WebDAV Drive（已有）
│   └── Sorter.java                # 原有排序器（已有）
├── utils/
│   ├── WebDavBackup.java          # WebDAV 备份工具类（实例方式）
│   └── WebDavBackupManager.java   # WebDAV 备份管理器（静态方式，推荐）
├── spider/
│   ├── WebDavBackupSpider.java    # Spider 接口封装（action 模式）
│   └── BackupUsageExample.java    # 使用示例代码
└── ...

json/
└── webdav_backup.json             # 备份配置示例
```

## 快速开始

### 1. 创建配置

```java
BackupConfig config = new BackupConfig("我的NAS");
config.setServer("http://192.168.1.100:5005/webdav");
config.setUser("admin");
config.setPass("your_password");
config.setBackupPath("/CatVodBackup/");
```

或者从 JSON 创建：

```java
String json = "{\"name\":\"我的NAS\",\"server\":\"http://192.168.1.100:5005/webdav\"," +
              "\"user\":\"admin\",\"pass\":\"password\",\"backupPath\":\"/CatVodBackup/\"}";
BackupConfig config = BackupConfig.objectFrom(json);
```

### 2. 测试连接

```java
boolean success = WebDavBackupManager.testConnection(config);
if (success) {
    // 连接正常
}
```

### 3. 上传备份

```java
String jsonData = "{\"config\":\"...\",\"history\":\"...\",\"favorites\":\"...\"}";

// 自动生成文件名（带时间戳）
String remotePath = WebDavBackupManager.upload(config, jsonData, null);
// 文件名格式：catvod_backup_20260814_120000.json

// 或指定文件名
String remotePath = WebDavBackupManager.upload(config, jsonData, "my_backup.json");
```

### 4. 列出备份

```java
List<BackupInfo> backups = WebDavBackupManager.list(config);
for (BackupInfo info : backups) {
    Log.d("Backup", info.getFileName() + " - " + info.getSizeDesc() + " - " + info.getLastModified());
}
```

### 5. 下载备份

```java
// 下载最新备份
String data = WebDavBackupManager.downloadLatest(config);

// 下载指定备份
String data = WebDavBackupManager.download(config, "/CatVodBackup/catvod_backup_20260814_120000.json");
```

### 6. 删除备份

```java
// 按文件名删除
WebDavBackupManager.deleteByName(config, "catvod_backup_20260814_120000.json");

// 按远程路径删除
WebDavBackupManager.delete(config, "/CatVodBackup/catvod_backup_20260814_120000.json");
```

### 7. 自动清理

```java
// 只保留最近 5 个备份
int deletedCount = WebDavBackupManager.cleanOldBackups(config, 5);
```

## 通过 Spider Action 使用

### 配置 Spider

在 `config.json` 中添加：

```json
{
  "key": "webdav_backup",
  "name": "WebDAV 备份",
  "type": 3,
  "api": "com.github.catvod.spider.WebDavBackupSpider",
  "ext": "{\"name\":\"我的NAS\",\"server\":\"http://192.168.1.100:5005/webdav\",\"user\":\"admin\",\"pass\":\"password\",\"backupPath\":\"/CatVodBackup/\"}"
}
```

### Action 接口

| Action | 说明 | 返回 |
|--------|------|------|
| `testConnection` | 测试连接 | `{"code": 0/1, "msg": "..."}` |
| `upload` | 上传备份 | `{"code": 0/1, "msg": "...", "path": "..."}` |
| `list` | 列出备份 | `{"code": 0, "data": [...]}` |
| `download` | 下载指定备份 | `{"code": 0/1, "data": "..."}` |
| `downloadLatest` | 下载最新备份 | `{"code": 0/1, "data": "..."}` |
| `delete` | 删除备份 | `{"code": 0/1, "msg": "..."}` |
| `exists` | 检查备份是否存在 | `{"code": 0, "exists": true/false}` |

### 便捷静态方法

```java
// 直接上传
String result = WebDavBackupSpider.upload(config, jsonData, null);

// 直接下载最新
String data = WebDavBackupSpider.downloadLatest(config);

// 直接列出
String listJson = WebDavBackupSpider.list(config);
```

## 配置参数说明

### BackupConfig 参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `name` | String | 是 | WebDAV 配置名称（自定义标识） |
| `server` | String | 是 | WebDAV 服务器地址（含协议和路径） |
| `user` | String | 是 | 用户名 |
| `pass` | String | 是 | 密码 |
| `backupPath` | String | 否 | 备份目录路径，默认 `/CatVodBackup/` |

### 常见 WebDAV 服务器配置示例

**群晖 NAS：**
```json
{
  "name": "群晖NAS",
  "server": "http://192.168.1.100:5005/webdav",
  "user": "admin",
  "pass": "password",
  "backupPath": "/CatVodBackup/"
}
```

**坚果云：**
```json
{
  "name": "坚果云",
  "server": "https://dav.jianguoyun.com/dav/",
  "user": "your_email@example.com",
  "pass": "your_app_password",
  "backupPath": "/CatVodBackup/"
}
```
> 注意：坚果云需要使用**应用密码**，不是登录密码。在坚果云设置 → 安全选项 → 第三方应用管理中创建。

**Nextcloud：**
```json
{
  "name": "Nextcloud",
  "server": "https://your-nextcloud.com/remote.php/dav/files/username/",
  "user": "username",
  "pass": "password",
  "backupPath": "/CatVodBackup/"
}
```

## 备份文件格式

备份文件以 `catvod_backup_` 为前缀，`.json` 为后缀，中间部分为时间戳：

```
catvod_backup_20260814_120000.json
catvod_backup_20260815_083000.json
catvod_backup_20260816_210000.json
```

备份数据内容为 JSON 字符串，格式由调用方自定义。

## 依赖

本功能依赖项目已有的 `sardine-android` 库（WebDAV 客户端），无需额外添加依赖：

```gradle
implementation libs.sardine.android
```

## 注意事项

1. **网络权限**：确保应用有网络访问权限
2. **HTTPS**：生产环境建议使用 HTTPS 协议的 WebDAV 服务器
3. **密码安全**：建议使用应用专用密码，不要使用主账号密码
4. **备份大小**：建议定期清理旧备份，避免占用过多云端空间
5. **异常处理**：所有方法在失败时返回 null/false/-1，调用方需做好异常处理

## License

与 CatVodSpider 项目保持一致。
