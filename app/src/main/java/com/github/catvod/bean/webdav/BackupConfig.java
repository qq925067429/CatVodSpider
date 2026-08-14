package com.github.catvod.bean.webdav;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

/**
 * WebDAV 备份配置
 * 用于存储 WebDAV 服务器连接信息和备份路径配置
 */
public class BackupConfig {

    @SerializedName("name")
    private String name;
    @SerializedName("server")
    private String server;
    @SerializedName("user")
    private String user;
    @SerializedName("pass")
    private String pass;
    @SerializedName("backupPath")
    private String backupPath;

    public static BackupConfig objectFrom(String str) {
        return new Gson().fromJson(str, BackupConfig.class);
    }

    public static List<BackupConfig> arrayFrom(String str) {
        Type listType = TypeToken.getParameterized(List.class, BackupConfig.class).getType();
        return new Gson().fromJson(str, listType);
    }

    public BackupConfig(String name) {
        this.name = name;
    }

    public String getName() {
        return TextUtils.isEmpty(name) ? "" : name;
    }

    public String getServer() {
        return TextUtils.isEmpty(server) ? "" : server;
    }

    public void setServer(String server) {
        this.server = TextUtils.isEmpty(server) ? "" : server;
    }

    public String getUser() {
        return TextUtils.isEmpty(user) ? "" : user;
    }

    public void setUser(String user) {
        this.user = TextUtils.isEmpty(user) ? "" : user;
    }

    public String getPass() {
        return TextUtils.isEmpty(pass) ? "" : pass;
    }

    public void setPass(String pass) {
        this.pass = TextUtils.isEmpty(pass) ? "" : pass;
    }

    public String getBackupPath() {
        return TextUtils.isEmpty(backupPath) ? "/CatVodBackup/" : backupPath;
    }

    public void setBackupPath(String backupPath) {
        this.backupPath = TextUtils.isEmpty(backupPath) ? "/CatVodBackup/" : backupPath;
    }

    /**
     * 获取 WebDAV 服务器主机地址（不含路径）
     */
    public String getHost() {
        return getServer();
    }

    /**
     * 获取完整的备份目录路径
     */
    public String getFullBackupPath() {
        String path = getBackupPath();
        if (!path.startsWith("/")) path = "/" + path;
        if (!path.endsWith("/")) path = path + "/";
        return path;
    }

    /**
     * 获取指定文件的完整远程路径
     */
    public String getRemoteFilePath(String fileName) {
        return getFullBackupPath() + fileName;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof BackupConfig)) return false;
        BackupConfig it = (BackupConfig) obj;
        return getName().equals(it.getName());
    }
}
