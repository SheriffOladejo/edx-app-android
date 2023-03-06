package com.grassroot.academy.task

import android.content.Context
import dagger.hilt.android.EntryPointAccessors
import com.grassroot.academy.core.EdxDefaultModule.ProviderEntryPoint
import com.grassroot.academy.model.db.DownloadEntry

abstract class EnqueueDownloadTask(
    context: Context,
    private var downloadList: List<DownloadEntry>
) : Task<Long?>(context) {
    private var transcriptManager = EntryPointAccessors
        .fromApplication(context, ProviderEntryPoint::class.java).getTranscriptManager()

    override fun doInBackground(vararg params: Void?): Long {
        var count = 0
        for (downloadEntry in downloadList) {
            if (environment.storage.addDownload(downloadEntry) != -1L) {
                count++
                downloadEntry.transcript?.run {
                    for (value in this.values) {
                        transcriptManager.downloadTranscriptsForVideo(value, null)
                    }
                }
            }
        }
        return count.toLong()
    }
}
