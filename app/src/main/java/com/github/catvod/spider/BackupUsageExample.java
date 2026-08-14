package com.github.catvod.spider;

import com.github.catvod.bean.webdav.BackupConfig;
import com.github.catvod.bean.webdav.BackupInfo;
import com.github.catvod.utils.WebDavBackupManager;

import java.util.List;

/**
 * WebDAV 云端备份使用示例
 * 
 * 本文件演示如何在 CatVod 项目中集成和使用 WebDAV 备份功能。
 * 
 * ==================== 功能概述 ====================
 * 
 * 1. 上传备份：将本地配置/数据上传到 WebDAV 云端保存
 * 2. 拉取备份：从 WebDAV 云端下载备份数据并恢复
 * 3. 管理备份：列出、删除、清理云端备份文件
 * 4. 连接测试：验证 WebDAV 服务器连接是否正常
 * 
 * ==================== 支持的 WebDAV 服务 ====================
 * 
 * - 群晖 NAS (Synology WebDAV Server)
 * - 坚果云 (https://dav.jianguoyun.com/dav/)
 * - Nextcloud / ownCloud
 * - 路由器挂载硬盘 (OpenWrt + WebDAV)
 * - 任何标准 WebDAV 协议服务器
 * 
 * ==================== 使用方式 ====================
 */
public class BackupUsageExample {

    /**
     * 示例 1：创建 WebDAV 备份配置
     */
    public static BackupConfig createConfig() {
        BackupConfig config = new BackupConfig("我的NAS");
        config.setServer("http://192.168.1.100:5005/webdav");
        config.setUser("admin");
        config.setPass("your_password");
        config.setBackupPath("/CatVodBackup/");
        return config;
    }

    /**
     * 示例 2：从 JSON 字符串创建配置
     * JSON 格式：
     * {
     *   "name": "我的NAS",
     *   "server": "http://192.168.1.100:5005/webdav",
     *   "user": "admin",
     *   "pass": "your_password",
     *   "backupPath": "/CatVodBackup/"
     * }
     */
    public static BackupConfig createConfigFromJson() {
        String json = "{\"name\":\"我的NAS\",\"server\":\"http://192.168.1.100:5005/webdav\"," +
                "\"user\":\"admin\",\"pass\":\"your_password\",\"backupPath\":\"/CatVodBackup/\"}";
        return BackupConfig.objectFrom(json);
    }

    /**
     * 示例 3：测试 WebDAV 连接
     */
    public static void testConnection() {
        BackupConfig config = createConfig();
        boolean success = WebDavBackupManager.testConnection(config);
        if (success) {
            // 连接成功，可以进行备份操作
        } else {
            // 连接失败，检查服务器地址、用户名、密码
        }
    }

    /**
     * 示例 4：上传备份到云端
     * 
     * 场景：将当前应用的配置信息备份到 WebDAV 云端
     */
    public static void uploadBackup() {
        BackupConfig config = createConfig();

        // 构造要备份的数据（通常是 JSON 格式的配置信息）
        String backupData = "{" +
                "\"config\": \"应用配置JSON字符串\"," +
                "\"history\": \"观看历史记录\"," +
                "\"favorites\": \"收藏列表\"," +
                "\"custom\": \"自定义数据\"" +
                "}";

        // 方式 1：自动生成文件名（带时间戳）
        // 文件名格式：catvod_backup_20260814_120000.json
        String remotePath = WebDavBackupManager.upload(config, backupData, null);

        // 方式 2：指定文件名
        String remotePath2 = WebDavBackupManager.upload(config, backupData, "my_backup.json");

        if (remotePath != null) {
            // 上传成功，remotePath 是远程文件路径
        } else {
            // 上传失败
        }
    }

    /**
     * 示例 5：列出云端所有备份
     */
    public static void listBackups() {
        BackupConfig config = createConfig();
        List<BackupInfo> backups = WebDavBackupManager.list(config);

        for (BackupInfo info : backups) {
            String fileName = info.getFileName();        // 文件名
            String remotePath = info.getRemotePath();    // 远程路径
            String sizeDesc = info.getSizeDesc();        // 可读大小 (如 "12.5 KB")
            String lastModified = info.getLastModified(); // 修改时间
        }
    }

    /**
     * 示例 6：下载最新备份并恢复
     * 
     * 场景：用户更换设备后，从云端拉取最近的备份恢复配置
     */
    public static void restoreLatestBackup() {
        BackupConfig config = createConfig();

        // 下载最新的备份
        String backupData = WebDavBackupManager.downloadLatest(config);

        if (backupData != null) {
            // 解析备份数据并恢复
            // 例如：恢复配置、历史记录、收藏等
            // restoreFromJson(backupData);
        } else {
            // 没有找到备份或下载失败
        }
    }

    /**
     * 示例 7：下载指定的备份文件
     */
    public static void restoreSpecificBackup() {
        BackupConfig config = createConfig();

        // 先列出所有备份
        List<BackupInfo> backups = WebDavBackupManager.list(config);

        // 选择要恢复的备份（例如第一个）
        if (!backups.isEmpty()) {
            BackupInfo target = backups.get(0);
            String backupData = WebDavBackupManager.download(config, target.getRemotePath());

            if (backupData != null) {
                // 恢复数据
                // restoreFromJson(backupData);
            }
        }
    }

    /**
     * 示例 8：删除旧备份
     */
    public static void deleteBackup() {
        BackupConfig config = createConfig();

        // 按文件名删除
        boolean success = WebDavBackupManager.deleteByName(config, "catvod_backup_20260801_120000.json");

        // 或按远程路径删除
        boolean success2 = WebDavBackupManager.delete(config, "/CatVodBackup/catvod_backup_20260801_120000.json");
    }

    /**
     * 示例 9：自动清理旧备份，只保留最近 5 个
     */
    public static void autoCleanOldBackups() {
        BackupConfig config = createConfig();

        // 先上传新备份
        WebDavBackupManager.upload(config, "{\"data\":\"new backup\"}", null);

        // 清理旧备份，只保留最近 5 个
        int deletedCount = WebDavBackupManager.cleanOldBackups(config, 5);
        // deletedCount 是实际删除的备份数量
    }

    /**
     * 示例 10：通过 Spider action 接口使用备份功能
     * 
     * 在 config.json 中配置 Spider：
     * {
     *   "key": "webdav_backup",
     *   "name": "WebDAV 备份",
     *   "type": 3,
     *   "api": "com.github.catvod.spider.WebDavBackupSpider",
     *   "ext": "{\"name\":\"我的NAS\",\"server\":\"http://192.168.1.100:5005/webdav\",\"user\":\"admin\",\"pass\":\"password\",\"backupPath\":\"/CatVodBackup/\"}"
     * }
     * 
     * 然后通过 action 调用（action 参数为 JSON 字符串）：
     * - {"action":"testConnection"}                              测试连接
     * - {"action":"upload","data":"JSON数据","fileName":"可选"}  上传备份
     * - {"action":"list"}                                         列出备份
     * - {"action":"download","remotePath":"远程路径"}              下载指定备份
     * - {"action":"downloadLatest"}                               下载最新备份
     * - {"action":"delete","fileName":"文件名"}                    删除备份
     * - {"action":"exists","fileName":"文件名"}                    检查备份是否存在
     * - {"action":"clean","keepCount":5}                          清理旧备份
     */

    /**
     * 示例 11：坚果云 WebDAV 配置
     */
    public static BackupConfig createJianGuoYunConfig() {
        BackupConfig config = new BackupConfig("坚果云");
        config.setServer("https://dav.jianguoyun.com/dav/");
        config.setUser("your_email@example.com");
        config.setPass("your_app_password"); // 使用坚果云的应用密码，不是登录密码
        config.setBackupPath("/CatVodBackup/");
        return config;
    }

    /**
     * 示例 12：完整的备份恢复流程
     */
    public static void fullBackupRestoreFlow() {
        // 步骤 1：创建配置
        BackupConfig config = createConfig();

        // 步骤 2：测试连接
        if (!WebDavBackupManager.testConnection(config)) {
            // 连接失败，提示用户检查配置
            return;
        }

        // 步骤 3：执行备份
        String backupData = collectBackupData(); // 收集需要备份的数据
        String remotePath = WebDavBackupManager.upload(config, backupData, null);
        if (remotePath == null) {
            // 备份失败
            return;
        }

        // 步骤 4：清理旧备份（可选）
        WebDavBackupManager.cleanOldBackups(config, 10);

        // ===== 恢复流程 =====

        // 步骤 5：列出可用备份
        List<BackupInfo> backups = WebDavBackupManager.list(config);
        if (backups.isEmpty()) {
            // 没有可用备份
            return;
        }

        // 步骤 6：下载并恢复
        String data = WebDavBackupManager.download(config, backups.get(0).getRemotePath());
        if (data != null) {
            restoreFromData(data); // 恢复数据
        }
    }

    // 模拟收集备份数据
    private static String collectBackupData() {
        return "{\"config\":\"...\",\"history\":\"...\",\"favorites\":\"...\"}";
    }

    // 模拟恢复数据
    private static void restoreFromData(String data) {
        // 解析 JSON 并恢复配置、历史记录、收藏等
    }
}
