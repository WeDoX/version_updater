package com.onedream.version_updater.downloader;

import com.onedream.version_updater.utils.log.AppVersionUpdateLogUtils;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

public class ApkFileDownloadThread extends Thread {
    private static final String TAG = "ApkFileDownloadThread";
    private final ApkFileDownloader mDownloader;
    private final URL mDownUrl;
    private final File mSaveFile;

    private final int mBlock;

    private int mDownLength;
    private final int mThreadId;
    private boolean mFinish = false;


    public ApkFileDownloadThread(ApkFileDownloader downloader, URL downUrl, File saveFile, int block, int downLength, int threadId) {
        this.mDownloader = downloader;
        this.mDownUrl = downUrl;
        this.mSaveFile = saveFile;
        this.mBlock = block;
        this.mDownLength = downLength;
        this.mThreadId = threadId;
    }

    @Override
    public void run() {
        if (mDownLength < mBlock) {
            try {
                HttpURLConnection http = (HttpURLConnection) mDownUrl.openConnection();
                http.setConnectTimeout(5 * 60 * 1000);
                http.setReadTimeout(5 * 60 * 1000);
                http.setRequestMethod("GET");
                http.setRequestProperty("Accept", "image/gif, image/jpeg, image/pjpeg, image/pjpeg, application/x-shockwave-flash, application/xaml+xml, application/vnd.ms-xpsdocument, application/x-ms-xbap, application/x-ms-application, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, */*");
                http.setRequestProperty("Accept-Language", "zh-CN");
                http.setRequestProperty("Referer", mDownUrl.toString());
                http.setRequestProperty("Charset", "UTF-8");
                int startPos = mBlock * (mThreadId - 1) + mDownLength;
                int endPos = mBlock * mThreadId - 1;
                http.setRequestProperty("Range", "bytes=" + startPos + "-" + endPos);
                http.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)");
                http.setRequestProperty("Connection", "Keep-Alive");
                //阿里云OSS,有的文件会出现范围读取会返回全部的byte回来，设置请求头x-oss-range-behavior:standard, 使用标准范围下载行为
                http.setRequestProperty("x-oss-range-behavior", "standard");

                InputStream inStream = http.getInputStream();
                byte[] buffer = new byte[1024 * 8];
                int offset = 0;
                print("Thread " + this.mThreadId + " start download from position " + startPos);
                RandomAccessFile threadFile = new RandomAccessFile(this.mSaveFile, "rwd");
                threadFile.seek(startPos);
                while ((offset = inStream.read(buffer)) != -1) {
                    threadFile.write(buffer, 0, offset);
                    mDownLength += offset;
                    mDownloader.update(this.mThreadId, mDownLength);
                    mDownloader.saveLogFile();
                    mDownloader.append(offset);
                }
                threadFile.close();
                inStream.close();
                print("Thread " + this.mThreadId + " download finish");
                this.mFinish = true;
            } catch (Exception e) {
                this.mDownLength = -1;
                print("Thread " + this.mThreadId + ":" + e);
            }
        }
    }


    public boolean isFinish() {
        return mFinish;
    }

    public long getDownLength() {
        return mDownLength;
    }


    private static void print(String msg) {
        AppVersionUpdateLogUtils.printLog(TAG, msg);
    }
}
