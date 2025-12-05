package com.onedream.version_updater.downloader;

import android.content.Context;


import com.onedream.version_updater.R;
import com.onedream.version_updater.listener.ApkFileDownloadProgressListener;
import com.onedream.version_updater.utils.log.AppVersionUpdateLogUtils;
import com.onedream.version_updater.utils.FileService;
import com.onedream.version_updater.utils.LocalApkFileUtils;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class ApkFileDownloader {
    private static final String TAG = "ApkFileDownloader";
    private final FileService mFileService;

    private int downloadSize = 0;

    private int fileSize = 0;

    private final ApkFileDownloadThread[] threads;

    private final File saveFile;

    private final Map<Integer, Integer> data = new ConcurrentHashMap<Integer, Integer>();

    private final int block;

    private final String mDownloadUrl;




    public ApkFileDownloader(Context context, String downloadUrl, int threadNum) {
        try {
            this.mDownloadUrl = downloadUrl;
            mFileService = new FileService(context);
            //开始一个下载器前，先删除之前该路径存在的数据（暂时先这样解决，避免）
            mFileService.delete(downloadUrl);
            //
            URL url = new URL(this.mDownloadUrl);
            print("下载地址:" + this.mDownloadUrl);
            //
            LocalApkFileUtils.ensureDirectoryExists(context);
            //
            this.threads = new ApkFileDownloadThread[threadNum];
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5 * 60 * 1000);
            conn.setReadTimeout(5 * 60 * 1000);
            conn.setRequestMethod("GET");
            //conn.setRequestProperty("Accept", "image/gif, image/jpeg, image/pjpeg, image/pjpeg, application/x-shockwave-flash, application/xaml+xml, application/vnd.ms-xpsdocument, application/x-ms-xbap, application/x-ms-application, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, */*");
            //conn.setRequestProperty("Accept-Language", "zh-CN");
            //conn.setRequestProperty("Referer", downloadUrl);
            //conn.setRequestProperty("Charset", "UTF-8");
            //conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)");
            //conn.setRequestProperty("Connection", "Keep-Alive");
            conn.connect();
            printResponseHeader(conn);
            if (conn.getResponseCode() == 200) {
                this.fileSize = conn.getContentLength();
                if (this.fileSize <= 0) throw new RuntimeException("Unkown file size ");
                print("file size:" + this.fileSize);
                String filename = LocalApkFileUtils.getApkFileNameByUpdateUrl(this.mDownloadUrl);
                this.saveFile = new File(LocalApkFileUtils.getUpdateServerApkFileLocalSaveFilePath(context, filename));
                print("save to:" + this.saveFile.getAbsolutePath());
                Map<Integer, Integer> logdata = mFileService.getData(downloadUrl);
                if (logdata.size() > 0) {
                    for (Map.Entry<Integer, Integer> entry : logdata.entrySet())
                        data.put(entry.getKey(), entry.getValue());
                }
                this.block = (this.fileSize % this.threads.length) == 0 ? this.fileSize / this.threads.length : this.fileSize / this.threads.length + 1;
                if (this.data.size() == this.threads.length) {
                    for (int i = 0; i < this.threads.length; i++) {
                        this.downloadSize += this.data.get(i + 1);
                    }
                    print("下载进度" + this.downloadSize);
                }
            } else {
                throw new RuntimeException(String.format(context.getString(R.string.version_updater_http_exception_), conn.getResponseCode() + ""));
            }
        } catch (Exception e) {
            print("获取apk文件长度发生异常：" + e.toString());
            if (e instanceof SocketTimeoutException) {
                throw new RuntimeException(context.getString(R.string.version_updater_network_timeout));
            } else {
                throw new RuntimeException(context.getString(R.string.version_updater_network_error));
            }
        }
    }

    public int download(ApkFileDownloadProgressListener listener) throws Exception {
        try {
            RandomAccessFile randOut = new RandomAccessFile(this.saveFile, "rw");
            if (this.fileSize > 0) randOut.setLength(this.fileSize);
            randOut.close();
            URL url = new URL(this.mDownloadUrl);
            if (this.data.size() != this.threads.length) {
                this.data.clear();
                for (int i = 0; i < this.threads.length; i++) {
                    this.data.put(i + 1, 0);
                }
            }
            for (int i = 0; i < this.threads.length; i++) {
                int downLength = this.data.get(i + 1);
                if (downLength < this.block && this.downloadSize < this.fileSize) {
                    this.threads[i] = new ApkFileDownloadThread(this, url, this.saveFile, this.block, this.data.get(i + 1), i + 1);
                    this.threads[i].setPriority(7);
                    this.threads[i].start();
                } else {
                    this.threads[i] = null;
                }
            }
            this.mFileService.save(this.mDownloadUrl, this.data);
            boolean notFinish = true;
            while (notFinish) {
                Thread.sleep(900);
                notFinish = false;
                for (int i = 0; i < this.threads.length; i++) {
                    if (this.threads[i] != null && !this.threads[i].isFinish()) {
                        notFinish = true;
                        if (this.threads[i].getDownLength() == -1) {
                            this.threads[i] = new ApkFileDownloadThread(this, url, this.saveFile, this.block, this.data.get(i + 1), i + 1);
                            this.threads[i].setPriority(7);
                            this.threads[i].start();
                        }
                    }
                }
                if (listener != null) listener.onDownloadSize(this.downloadSize, this.fileSize);
            }
            mFileService.delete(this.mDownloadUrl);
            listener.onDownloadFinish();
        } catch (Exception e) {
            print("apk文件下载发生异常：" + e.toString());
            throw new Exception("apk file download fail:" + e.getMessage());
        }
        return this.downloadSize;
    }

    public static Map<String, String> getHttpResponseHeader(HttpURLConnection http) {
        Map<String, String> header = new LinkedHashMap<String, String>();
        for (int i = 0; ; i++) {
            String mine = http.getHeaderField(i);
            if (mine == null) break;
            header.put(http.getHeaderFieldKey(i), mine);
        }
        return header;
    }

    public static void printResponseHeader(HttpURLConnection http) {
        Map<String, String> header = getHttpResponseHeader(http);
        for (Map.Entry<String, String> entry : header.entrySet()) {
            String key = entry.getKey() != null ? entry.getKey() + ":" : "";
            print(key + entry.getValue());
        }
    }

    protected void update(int threadId, int pos) {
        this.data.put(threadId, pos);
    }

    protected synchronized void saveLogFile() {
        this.mFileService.update(this.mDownloadUrl, this.data);
    }

    protected synchronized void append(int size) {
        downloadSize += size;
    }

    private static void print(String msg) {
        AppVersionUpdateLogUtils.printLog(TAG, msg);
    }
}
