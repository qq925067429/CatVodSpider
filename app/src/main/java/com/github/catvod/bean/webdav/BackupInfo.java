package com.github.catvod.bean.webdav;

import com.google.gson.annotations.SerializedName;

/**
 * 备份文件信息
 * 用于描述 WebDAV 服务器上的备份文件元数据
 */
public class BackupInfo {

    @SerializedName("fileName")
    private String fileName;
    @SerializedName("remotePath")
    private String remotePath;
    @SerializedName("size")
    private long size;
    @SerializedName("lastModified")
    private String lastModified;
    @SerializedName("configName")
    private String configName;

    public BackupInfo() {
    }

    public BackupInfo(String fileName, String remotePath, long size, String lastModified) {
        this.fileName = fileName;
        this.remotePath = remotePath;
        this.size = size;
        this.lastModified = lastModified;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getRemotePath() {
        return remotePath;
    }

    public void setRemotePath(String remotePath) {
        this.remotePath = remotePath;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    /**
     * 获取可读的文件大小描述
     */
    public String getSizeDesc() {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
    }

    @Override
    public String toString() {
        return "BackupInfo{" +
                "fileName='" + fileName + '\'' +
                ", remotePath='" + remotePath + '\'' +
                ", size=" + getSizeDesc() +
                ", lastModified='" + lastModified + '\'' +
                '}';
    }
}
