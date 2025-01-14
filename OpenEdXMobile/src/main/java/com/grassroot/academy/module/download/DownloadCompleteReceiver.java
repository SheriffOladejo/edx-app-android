package com.grassroot.academy.module.download;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import com.grassroot.academy.core.IEdxEnvironment;
import com.grassroot.academy.logger.Logger;
import com.grassroot.academy.model.VideoModel;
import com.grassroot.academy.model.db.DownloadEntry;
import com.grassroot.academy.model.download.NativeDownloadModel;
import com.grassroot.academy.module.analytics.AnalyticsRegistry;
import com.grassroot.academy.module.db.DataCallback;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class DownloadCompleteReceiver extends BroadcastReceiver {
    private final Logger logger = new Logger(getClass().getName());

    @Inject
    IEdxEnvironment environment;

    @Override
    public void onReceive(Context context, Intent data) {
        if (data != null && data.getAction() != null) {
            switch (data.getAction()) {
                case DownloadManager.ACTION_DOWNLOAD_COMPLETE:
                    handleDownloadCompleteIntent(data);
                    break;
                case DownloadManager.ACTION_NOTIFICATION_CLICKED:
                    // Open downloads activity
                    environment.getRouter().showDownloads(context);
                    break;
            }
        }
    }

    private void handleDownloadCompleteIntent(final Intent data) {
        if (data.hasExtra(DownloadManager.EXTRA_DOWNLOAD_ID)) {
            final long id = data.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (id != -1) {
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        logger.debug("Received download notification for id: " + id);

                        // check if download was SUCCESSFUL
                        NativeDownloadModel nm = environment.getDownloadManager().getDownload(id);

                        if (nm == null || nm.status != DownloadManager.STATUS_SUCCESSFUL) {
                            logger.debug("Download seems failed or cancelled for id : " + id);
                            final VideoModel downloadEntry = environment.getDatabase().getDownloadEntryByDmId(id, null);
                            if (downloadEntry != null) {
                                // This means that the download was either cancelled from the native
                                // download manager app or the cancel button on download notification
                                environment.getStorage().removeDownload(downloadEntry);
                            } else {
                                environment.getDownloadManager().removeDownloads(id);
                            }
                            return;
                        } else {
                            logger.debug("Download successful for id : " + id);
                        }

                        // mark download as completed
                        environment.getStorage().markDownloadAsComplete(id, new DataCallback<VideoModel>() {
                            @Override
                            public void onResult(VideoModel result) {
                                if (result != null) {
                                    DownloadEntry download = (DownloadEntry) result;

                                    AnalyticsRegistry analyticsRegistry = environment.getAnalyticsRegistry();
                                    analyticsRegistry.trackDownloadComplete(download.videoId, download.eid,
                                            download.lmsUrl);
                                }
                            }

                            @Override
                            public void onFail(Exception ex) {
                                logger.error(ex);
                            }
                        });
                    }
                });
            }
        }
    }
}
