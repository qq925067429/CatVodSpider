package com.github.catvod.utils;

import android.text.TextUtils;

import com.github.catvod.bean.webdav.BackupConfig;
import com.github.catvod.bean.webdav.BackupInfo;
import com.thegrizzlylabs.sardineandroid.DavResource;
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * WebDAV 云端备份管理器
 * 
 * 提供简洁的 API 用于：
 * 1. 上传配置/数据到 WebDAV 云端备份
 * 2. 从 WebDAV 云端拉取备份并恢复
 * 3. 管理云端备份文件（列表、删除）
 * 
 * 使用示例：
 * <pre>
 *     // 1. 创建配置
 *     BackupConfig config = new BackupConfig("我的WebDAV");
 *     config.setServer("http://192.168.1.1/dav");
 *     config.setUser("username");
 *     config.setPass("password");
 *     config.setBackupPath("/CatVodBackup/");
 *     
 *     // 2. 上传备份
 *     String jsonData = "{\"config\":\"...\",\"history\":\"...\"}";
 *     boolean success = WebDavBackupManager.upload(config, jsonData, null);
 *     
 *     // 3. 获取备份列表
 *     List<BackupInfo> list = WebDavBackupManager.list(config);
 *     
 *     // 4. 下载最新备份
 *     String data = WebDavBackupManager.downloadLatest(config);
 *     
 *     // 5. 下载指定备份
 *     String data = WebDavBackupManager.download(config, "/CatVodBackup/catvod_backup_20260814_120000.json");
 * </pre>
 */
public class WebDavBackupManager {

    private static final String BACKUP_FILE_PREFIX = "catvod_backup_";
    private static final String BACKUP_FILE_EXT = ".json";

    /**
     * 测试 WebDAV 连接
     * @param config WebDAV 配置
     * @return true 连接成功，false 连接失败
     */
    public static boolean testConnection(BackupConfig config) {
        try {
            OkHttpSardine sardine = createSardine(config);
            sardine.list(config.getHost());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 上传备份到 WebDAV 云端
     * @param config WebDAV 配置
     * @param data 要备份的 JSON 数据字符串
     * @param fileName 备份文件名，为 null 时自动生成带时间戳的文件名
     * @return 上传成功返回远程路径，失败返回 null
     */
    public static String upload(BackupConfig config, String data, String fileName) {
        try {
            if (TextUtils.isEmpty(data)) return null;
            
            OkHttpSardine sardine = createSardine(config);
            ensureDirExists(sardine, config);
            
            if (TextUtils.isEmpty(fileName)) {
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                fileName = BACKUP_FILE_PREFIX + timestamp + BACKUP_FILE_EXT;
            }
            
            String remotePath = config.getRemoteFilePath(fileName);
            byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
            sardine.put(remotePath, bytes, "application/json");
            return remotePath;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 从 WebDAV 云端下载指定备份
     * @param config WebDAV 配置
     * @param remotePath 远程文件的完整路径
     * @return 备份数据字符串，失败返回 null
     */
    public static String download(BackupConfig config, String remotePath) {
        try {
            if (TextUtils.isEmpty(remotePath)) return null;
            
            OkHttpSardine sardine = createSardine(config);
            InputStream inputStream = sardine.get(remotePath);
            byte[] bytes = readAllBytes(inputStream);
            inputStream.close();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 从 WebDAV 云端下载最新的备份
     * @param config WebDAV 配置
     * @return 最新备份数据字符串，没有备份或失败返回 null
     */
    public static String downloadLatest(BackupConfig config) {
        try {
            List<BackupInfo> backups = list(config);
            if (backups.isEmpty()) return null;
            return download(config, backups.get(0).getRemotePath());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 列出 WebDAV 云端的所有备份文件
     * @param config WebDAV 配置
     * @return 备份信息列表，按修改时间倒序排列
     */
    public static List<BackupInfo> list(BackupConfig config) {
        List<BackupInfo> backups = new ArrayList<>();
        try {
            OkHttpSardine sardine = createSardine(config);
            String path = config.getFullBackupPath();
            
            if (!sardine.exists(path)) {
                return backups;
            }
            
            List<DavResource> resources = sardine.list(path);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            
            for (DavResource resource : resources) {
                if (resource.isDirectory()) continue;
                
                String name = resource.getName();
                if (name.startsWith(BACKUP_FILE_PREFIX) && name.endsWith(BACKUP_FILE_EXT)) {
                    BackupInfo info = new BackupInfo();
                    info.setFileName(name);
                    info.setRemotePath(path + name);
                    info.setSize(resource.getContentLength());
                    if (resource.getModified() != null) {
                        info.setLastModified(dateFormat.format(resource.getModified()));
                    }
                    backups.add(info);
                }
            }
            
            // 按修改时间倒序排列
            backups.sort((a, b) -> {
                if (a.getLastModified() == null || b.getLastModified() == null) return 0;
                return b.getLastModified().compareTo(a.getLastModified());
            });
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return backups;
    }

    /**
     * 删除 WebDAV 云端的指定备份
     * @param config WebDAV 配置
     * @param remotePath 远程文件路径
     * @return true 删除成功，false 删除失败
     */
    public static boolean delete(BackupConfig config, String remotePath) {
        try {
            OkHttpSardine sardine = createSardine(config);
            sardine.delete(remotePath);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 按文件名删除备份
     * @param config WebDAV 配置
     * @param fileName 备份文件名
     * @return true 删除成功，false 删除失败
     */
    public static boolean deleteByName(BackupConfig config, String fileName) {
        return delete(config, config.getRemoteFilePath(fileName));
    }

    /**
     * 检查指定备份文件是否存在
     * @param config WebDAV 配置
     * @param fileName 备份文件名
     * @return true 存在，false 不存在
     */
    public static boolean exists(BackupConfig config, String fileName) {
        try {
            OkHttpSardine sardine = createSardine(config);
            return sardine.exists(config.getRemoteFilePath(fileName));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 清理旧备份，只保留最近的 N 个
     * @param config WebDAV 配置
     * @param keepCount 保留的备份数量
     * @return 删除的备份数量
     */
    public static int cleanOldBackups(BackupConfig config, int keepCount) {
        try {
            List<BackupInfo> backups = list(config);
            if (backups.size() <= keepCount) return 0;
            
            int deletedCount = 0;
            OkHttpSardine sardine = createSardine(config);
            for (int i = keepCount; i < backups.size(); i++) {
                try {
                    sardine.delete(backups.get(i).getRemotePath());
                    deletedCount++;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return deletedCount;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    // ==================== 内部工具方法 ====================

    private static OkHttpSardine createSardine(BackupConfig config) {
        OkHttpSardine sardine = new OkHttpSardine();
        sardine.setCredentials(config.getUser(), config.getPass());
        return sardine;
    }

    private static void ensureDirExists(OkHttpSardine sardine, BackupConfig config) {
        try {
            String path = config.getFullBackupPath();
            if (!sardine.exists(path)) {
                sardine.createDirectory(path);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static byte[] readAllBytes(InputStream inputStream) throws Exception {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[4096];
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }
}
