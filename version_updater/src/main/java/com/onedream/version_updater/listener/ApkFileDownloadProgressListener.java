package com.onedream.version_updater.listener;

public interface ApkFileDownloadProgressListener {
	void onDownloadSize(int size, int total_size);
	void onDownloadFinish();
}
