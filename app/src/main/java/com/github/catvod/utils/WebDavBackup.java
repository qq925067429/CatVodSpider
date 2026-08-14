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
 * WebDAV 云端备份工具类
 * 提供上传备份、下载备份、列出备份列表等功能
 */
public class WebDavBackup {

    private static final String TAG = "WebDavBackup";
    private static final String BACKUP_FILE_PREFIX = "catvod_backup_";
    private static final String BACKUP_FILE_EXT = ".json";

    private OkHttpSardine sardine;
    private BackupConfig config;

    public WebDavBackup(BackupConfig config) {
        this.config = config;
        initSardine();
    }

    private void initSardine() {
        sardine = new OkHttpSardine();
        sardine.setCredentials(config.getUser(), config.getPass());
    }

    /**
     * 测试 WebDAV 连接是否正常
     * @return true 表示连接成功，false 表示失败
     */
    public boolean testConnection() {
        try {
            sardine.list(config.getHost());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 确保备份目录存在
     */
    public void ensureBackupDirExists() {
        try {
            String path = config.getFullBackupPath();
            if (!sardine.exists(path)) {
                sardine.createDirectory(path);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 上传备份数据到 WebDAV 云端
     * @param data 要备份的 JSON 数据
     * @param fileName 备份文件名（可选，为 null 时自动生成）
     * @return 上传成功后的远程文件路径，失败返回 null
     */
    public String uploadBackup(String data, String fileName) {
        try {
            ensureBackupDirExists();
            
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
     * 从 WebDAV 云端下载备份数据
     * @param remotePath 远程文件路径
     * @return 备份数据内容，失败返回 null
     */
    public String downloadBackup(String remotePath) {
        try {
            InputStream inputStream = sardine.get(remotePath);
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[4096];
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            inputStream.close();
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 从 WebDAV 云端下载最新的备份
     * @return 最新备份数据内容，失败返回 null
     */
    public String downloadLatestBackup() {
        try {
            List<BackupInfo> backups = listBackups();
            if (backups.isEmpty()) {
                return null;
            }
            // 假设列表已按时间倒序排列，第一个就是最新的
            BackupInfo latest = backups.get(0);
            return downloadBackup(latest.getRemotePath());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 列出 WebDAV 云端的所有备份文件
     * @return 备份文件信息列表
     */
    public List<BackupInfo> listBackups() {
        List<BackupInfo> backups = new ArrayList<>();
        try {
            String path = config.getFullBackupPath();
            if (!sardine.exists(path)) {
                return backups;
            }
            
            List<DavResource> resources = sardine.list(path);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            
            for (DavResource resource : resources) {
                // 跳过目录本身
                if (resource.isDirectory()) {
                    continue;
                }
                
                String name = resource.getName();
                // 只列出备份文件
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
            
            // 按修改时间倒序排列（最新的在前）
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
     * 删除 WebDAV 云端的备份文件
     * @param remotePath 远程文件路径
     * @return true 表示删除成功，false 表示失败
     */
    public boolean deleteBackup(String remotePath) {
        try {
            sardine.delete(remotePath);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 删除指定名称的备份文件
     * @param fileName 备份文件名
     * @return true 表示删除成功，false 表示失败
     */
    public boolean deleteBackupByName(String fileName) {
        String remotePath = config.getRemoteFilePath(fileName);
        return deleteBackup(remotePath);
    }

    /**
     * 检查指定的备份文件是否存在
     * @param fileName 备份文件名
     * @return true 表示存在，false 表示不存在
     */
    public boolean backupExists(String fileName) {
        try {
            String remotePath = config.getRemoteFilePath(fileName);
            return sardine.exists(remotePath);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 获取备份配置信息
     */
    public BackupConfig getConfig() {
        return config;
    }

    /**
     * 更新备份配置
     */
    public void setConfig(BackupConfig config) {
        this.config = config;
        initSardine();
    }
}
