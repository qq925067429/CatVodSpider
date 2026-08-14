package com.github.catvod.spider;

import android.content.Context;
import android.text.TextUtils;

import com.github.catvod.bean.webdav.BackupConfig;
import com.github.catvod.bean.webdav.BackupInfo;
import com.github.catvod.crawler.Spider;
import com.github.catvod.utils.WebDavBackupManager;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.List;

/**
 * WebDAV 云端备份 Spider 模块
 *
 * 通过 action 接口提供以下操作，action 字符串为 JSON 格式：
 *
 * 1. 测试连接：
 *    {"action":"testConnection"}
 *    返回：{"code":0/1,"msg":"连接成功/失败原因"}
 *
 * 2. 上传备份：
 *    {"action":"upload","data":"要备份的JSON数据","fileName":"可选文件名"}
 *    返回：{"code":0/1,"msg":"上传成功/失败","path":"远程路径"}
 *
 * 3. 列出备份：
 *    {"action":"list"}
 *    返回：{"code":0,"data":[备份列表]}
 *
 * 4. 下载备份：
 *    {"action":"download","remotePath":"远程文件路径"}
 *    返回：{"code":0/1,"data":"备份数据内容"}
 *
 * 5. 下载最新备份：
 *    {"action":"downloadLatest"}
 *    返回：{"code":0/1,"data":"最新备份数据内容"}
 *
 * 6. 删除备份：
 *    {"action":"delete","remotePath":"远程文件路径"}
 *    返回：{"code":0/1,"msg":"删除成功/失败"}
 *
 * 7. 检查备份是否存在：
 *    {"action":"exists","fileName":"文件名"}
 *    返回：{"code":0,"exists":true/false}
 *
 * 8. 清理旧备份：
 *    {"action":"clean","keepCount":5}
 *    返回：{"code":0,"deletedCount":3}
 *
 * ext 参数为 WebDAV 配置 JSON，在 init 时传入：
 * {"name":"我的NAS","server":"http://192.168.1.100:5005/webdav","user":"admin","pass":"password","backupPath":"/CatVodBackup/"}
 */
public class WebDavBackupSpider extends Spider {

    private BackupConfig config;

    @Override
    public void init(Context context, String extend) {
        if (!TextUtils.isEmpty(extend)) {
            try {
                config = BackupConfig.objectFrom(extend);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String action(String action) {
        JsonObject result = new JsonObject();
        try {
            // 解析 action JSON 字符串
            JsonObject params;
            try {
                params = JsonParser.parseString(action).getAsJsonObject();
            } catch (Exception e) {
                // 如果不是 JSON，当作纯 action 名称处理
                params = new JsonObject();
                params.addProperty("action", action);
            }

            String actionName = params.has("action") ? params.get("action").getAsString() : "";

            // 检查是否允许覆盖 config（action 中可以传入临时 config）
            BackupConfig useConfig = config;
            if (params.has("config")) {
                try {
                    useConfig = BackupConfig.objectFrom(params.get("config").toString());
                } catch (Exception e) {
                    // 使用默认 config
                }
            }

            if (useConfig == null) {
                result.addProperty("code", 1);
                result.addProperty("msg", "WebDAV 配置未初始化，请在 ext 中传入 WebDAV 配置");
                return result.toString();
            }

            switch (actionName) {
                case "testConnection":
                    return handleTestConnection(useConfig);
                case "upload":
                    return handleUpload(useConfig, params);
                case "list":
                    return handleList(useConfig);
                case "download":
                    return handleDownload(useConfig, params);
                case "downloadLatest":
                    return handleDownloadLatest(useConfig);
                case "delete":
                    return handleDelete(useConfig, params);
                case "exists":
                    return handleExists(useConfig, params);
                case "clean":
                    return handleClean(useConfig, params);
                default:
                    result.addProperty("code", -1);
                    result.addProperty("msg", "未知操作: " + actionName);
                    return result.toString();
            }
        } catch (Exception e) {
            result.addProperty("code", -1);
            result.addProperty("msg", "操作失败: " + e.getMessage());
            return result.toString();
        }
    }

    /**
     * 测试 WebDAV 连接
     */
    private String handleTestConnection(BackupConfig cfg) {
        JsonObject result = new JsonObject();
        boolean connected = WebDavBackupManager.testConnection(cfg);
        result.addProperty("code", connected ? 0 : 1);
        result.addProperty("msg", connected ? "连接成功" : "连接失败，请检查服务器地址和账号密码");
        return result.toString();
    }

    /**
     * 上传备份
     * params: {"action":"upload","data":"JSON数据","fileName":"可选文件名"}
     */
    private String handleUpload(BackupConfig cfg, JsonObject params) {
        JsonObject result = new JsonObject();
        String data = params.has("data") ? params.get("data").getAsString() : "";
        String fileName = params.has("fileName") ? params.get("fileName").getAsString() : null;

        if (TextUtils.isEmpty(data)) {
            result.addProperty("code", 1);
            result.addProperty("msg", "备份数据为空");
            return result.toString();
        }

        String remotePath = WebDavBackupManager.upload(cfg, data, fileName);
        if (remotePath != null) {
            result.addProperty("code", 0);
            result.addProperty("msg", "备份上传成功");
            result.addProperty("path", remotePath);
        } else {
            result.addProperty("code", 1);
            result.addProperty("msg", "备份上传失败，请检查 WebDAV 配置和网络连接");
        }
        return result.toString();
    }

    /**
     * 列出所有备份
     */
    private String handleList(BackupConfig cfg) {
        JsonObject result = new JsonObject();
        List<BackupInfo> backups = WebDavBackupManager.list(cfg);
        JsonArray array = new JsonArray();
        for (BackupInfo info : backups) {
            JsonObject item = new JsonObject();
            item.addProperty("fileName", info.getFileName());
            item.addProperty("remotePath", info.getRemotePath());
            item.addProperty("size", info.getSize());
            item.addProperty("sizeDesc", info.getSizeDesc());
            item.addProperty("lastModified", info.getLastModified());
            array.add(item);
        }
        result.addProperty("code", 0);
        result.add("data", array);
        return result.toString();
    }

    /**
     * 下载指定备份
     * params: {"action":"download","remotePath":"远程文件路径"}
     */
    private String handleDownload(BackupConfig cfg, JsonObject params) {
        JsonObject result = new JsonObject();
        String remotePath = params.has("remotePath") ? params.get("remotePath").getAsString() : "";

        if (TextUtils.isEmpty(remotePath)) {
            result.addProperty("code", 1);
            result.addProperty("msg", "远程文件路径为空");
            return result.toString();
        }

        String data = WebDavBackupManager.download(cfg, remotePath);
        if (data != null) {
            result.addProperty("code", 0);
            result.addProperty("msg", "下载成功");
            result.addProperty("data", data);
        } else {
            result.addProperty("code", 1);
            result.addProperty("msg", "下载失败，请检查文件路径和网络连接");
        }
        return result.toString();
    }

    /**
     * 下载最新备份
     */
    private String handleDownloadLatest(BackupConfig cfg) {
        JsonObject result = new JsonObject();
        String data = WebDavBackupManager.downloadLatest(cfg);
        if (data != null) {
            result.addProperty("code", 0);
            result.addProperty("msg", "下载最新备份成功");
            result.addProperty("data", data);
        } else {
            result.addProperty("code", 1);
            result.addProperty("msg", "没有找到备份文件或下载失败");
        }
        return result.toString();
    }

    /**
     * 删除备份
     * params: {"action":"delete","remotePath":"远程文件路径"} 或 {"action":"delete","fileName":"文件名"}
     */
    private String handleDelete(BackupConfig cfg, JsonObject params) {
        JsonObject result = new JsonObject();
        boolean deleted;

        if (params.has("remotePath")) {
            String remotePath = params.get("remotePath").getAsString();
            deleted = WebDavBackupManager.delete(cfg, remotePath);
        } else if (params.has("fileName")) {
            String fileName = params.get("fileName").getAsString();
            deleted = WebDavBackupManager.deleteByName(cfg, fileName);
        } else {
            result.addProperty("code", 1);
            result.addProperty("msg", "请提供 remotePath 或 fileName");
            return result.toString();
        }

        result.addProperty("code", deleted ? 0 : 1);
        result.addProperty("msg", deleted ? "删除成功" : "删除失败");
        return result.toString();
    }

    /**
     * 检查备份是否存在
     * params: {"action":"exists","fileName":"文件名"}
     */
    private String handleExists(BackupConfig cfg, JsonObject params) {
        JsonObject result = new JsonObject();
        String fileName = params.has("fileName") ? params.get("fileName").getAsString() : "";

        if (TextUtils.isEmpty(fileName)) {
            result.addProperty("code", 1);
            result.addProperty("msg", "文件名为空");
            return result.toString();
        }

        boolean exists = WebDavBackupManager.exists(cfg, fileName);
        result.addProperty("code", 0);
        result.addProperty("exists", exists);
        return result.toString();
    }

    /**
     * 清理旧备份
     * params: {"action":"clean","keepCount":5}
     */
    private String handleClean(BackupConfig cfg, JsonObject params) {
        JsonObject result = new JsonObject();
        int keepCount = params.has("keepCount") ? params.get("keepCount").getAsInt() : 5;
        int deletedCount = WebDavBackupManager.cleanOldBackups(cfg, keepCount);
        result.addProperty("code", 0);
        result.addProperty("deletedCount", deletedCount);
        result.addProperty("msg", "清理完成，删除了 " + deletedCount + " 个旧备份");
        return result.toString();
    }

    // ==================== 便捷静态方法 ====================

    /**
     * 便捷方法：直接上传备份
     */
    public static String upload(BackupConfig config, String data, String fileName) {
        JsonObject result = new JsonObject();
        String remotePath = WebDavBackupManager.upload(config, data, fileName);
        if (remotePath != null) {
            result.addProperty("code", 0);
            result.addProperty("msg", "备份成功");
            result.addProperty("path", remotePath);
        } else {
            result.addProperty("code", 1);
            result.addProperty("msg", "备份失败");
        }
        return result.toString();
    }

    /**
     * 便捷方法：直接下载最新备份
     */
    public static String downloadLatest(BackupConfig config) {
        return WebDavBackupManager.downloadLatest(config);
    }

    /**
     * 便捷方法：列出所有备份
     */
    public static String list(BackupConfig config) {
        JsonObject result = new JsonObject();
        List<BackupInfo> backups = WebDavBackupManager.list(config);
        JsonArray array = new JsonArray();
        for (BackupInfo info : backups) {
            JsonObject item = new JsonObject();
            item.addProperty("fileName", info.getFileName());
            item.addProperty("remotePath", info.getRemotePath());
            item.addProperty("size", info.getSize());
            item.addProperty("sizeDesc", info.getSizeDesc());
            item.addProperty("lastModified", info.getLastModified());
            array.add(item);
        }
        result.addProperty("code", 0);
        result.add("data", array);
        return result.toString();
    }
}
