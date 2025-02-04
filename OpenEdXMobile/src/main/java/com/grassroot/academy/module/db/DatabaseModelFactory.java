package com.grassroot.academy.module.db;

import android.database.Cursor;
import android.webkit.URLUtil;

import androidx.annotation.Nullable;

import com.grassroot.academy.model.VideoModel;
import com.grassroot.academy.model.api.VideoResponseModel;
import com.grassroot.academy.model.course.BlockPath;
import com.grassroot.academy.model.course.IBlock;
import com.grassroot.academy.model.course.VideoBlockModel;
import com.grassroot.academy.model.course.VideoData;
import com.grassroot.academy.model.course.VideoInfo;
import com.grassroot.academy.model.db.DownloadEntry;

/**
 * Model Factory class for the database models.
 *
 * @author rohan
 */
public class DatabaseModelFactory {

    /**
     * Returns new instance of {@link com.grassroot.academy.model.VideoModel} initialized with given cursor.
     *
     * @param c
     * @return
     */
    public static VideoModel getModel(Cursor c) {
        DownloadEntry de = new DownloadEntry();

        de.dmId = c.getLong(c.getColumnIndex(DbStructure.Column.DM_ID));
        de.downloaded = DownloadEntry.DownloadedState.values()[c.getInt(c.getColumnIndex(DbStructure.Column.DOWNLOADED))];
        de.duration = c.getLong(c.getColumnIndex(DbStructure.Column.DURATION));
        de.filepath = c.getString(c.getColumnIndex(DbStructure.Column.FILEPATH));
        de.id = c.getInt(c.getColumnIndex(DbStructure.Column.ID));
        de.size = c.getLong(c.getColumnIndex(DbStructure.Column.SIZE));
        de.username = c.getString(c.getColumnIndex(DbStructure.Column.USERNAME));
        de.title = c.getString(c.getColumnIndex(DbStructure.Column.TITLE));
        de.url = c.getString(c.getColumnIndex(DbStructure.Column.URL));
        de.url_hls = c.getString(c.getColumnIndex(DbStructure.Column.URL_HLS));
        de.url_high_quality = c.getString(c.getColumnIndex(DbStructure.Column.URL_HIGH_QUALITY));
        de.url_low_quality = c.getString(c.getColumnIndex(DbStructure.Column.URL_LOW_QUALITY));
        de.url_youtube = c.getString(c.getColumnIndex(DbStructure.Column.URL_YOUTUBE));
        de.videoId = c.getString(c.getColumnIndex(DbStructure.Column.VIDEO_ID));
        de.watched = DownloadEntry.WatchedState.values()[c.getInt(c.getColumnIndex(DbStructure.Column.WATCHED))];
        de.eid = c.getString(c.getColumnIndex(DbStructure.Column.EID));
        de.chapter = c.getString(c.getColumnIndex(DbStructure.Column.CHAPTER));
        de.section = c.getString(c.getColumnIndex(DbStructure.Column.SECTION));
        de.downloadedOn = c.getLong(c.getColumnIndex(DbStructure.Column.DOWNLOADED_ON));
        de.lastPlayedOffset = c.getLong(c.getColumnIndex(DbStructure.Column.LAST_PLAYED_OFFSET));
        de.isCourseActive = c.getInt(c.getColumnIndex(DbStructure.Column.IS_COURSE_ACTIVE));
        de.isVideoForWebOnly = c.getInt(c.getColumnIndex(DbStructure.Column.VIDEO_FOR_WEB_ONLY)) == 1;
        de.lmsUrl = c.getString(c.getColumnIndex(DbStructure.Column.UNIT_URL));

        return de;
    }

    /**
     * Returns an object of IVideoModel which has all the fields copied from given VideoResponseModel.
     *
     * @param vrm
     * @return
     */
    public static VideoModel getModel(VideoResponseModel vrm) {
        DownloadEntry e = new DownloadEntry();
        e.chapter = vrm.getChapterName();
        e.section = vrm.getSequentialName();
        e.eid = vrm.getCourseId();
        e.duration = vrm.getSummary().getDuration();
        e.size = vrm.getSummary().getSize();
        e.title = vrm.getSummary().getDisplayName();
        e.url = vrm.getSummary().getVideoUrl();
        e.url_high_quality = vrm.getSummary().getHighEncoding();
        e.url_low_quality = vrm.getSummary().getLowEncoding();
        e.url_youtube = vrm.getSummary().getYoutubeLink();
        e.videoId = vrm.getSummary().getId();
        e.transcript = vrm.getSummary().getTranscripts();
        e.lmsUrl = vrm.getUnitUrl();

        e.isVideoForWebOnly = vrm.getSummary().isOnlyOnWeb();
        return e;
    }

    /**
     * Returns an object of IVideoModel which has all the fields copied from given VideoData.
     *
     * @param vrm
     * @return
     */
    public static VideoModel getModel(VideoData vrm, VideoBlockModel block) {
        DownloadEntry e = new DownloadEntry();
        //FIXME - current database schema is not suitable for arbitary level of course structure tree
        //solution - store the navigation path info in into one column field in the database,
        //rather than individual column fields.
        BlockPath path = block.getPath();
        e.chapter = path.get(1) == null ? "" : path.get(1).getDisplayName();
        e.section = path.get(2) == null ? "" : path.get(2).getDisplayName();
        IBlock root = block.getRoot();
        e.eid = root.getCourseId();
        e.duration = vrm.duration;
        final VideoInfo preferredVideoInfo = vrm.encodedVideos.getPreferredNativeVideoInfo();
        e.size = preferredVideoInfo.fileSize;
        e.title = block.getDisplayName();
        e.url = preferredVideoInfo.url;
        e.url_hls = getVideoNetworkUrlOrNull(vrm.encodedVideos.getHls());
        e.url_high_quality = getVideoNetworkUrlOrNull(vrm.encodedVideos.getMobileHigh());
        e.url_low_quality = getVideoNetworkUrlOrNull(vrm.encodedVideos.getMobileLow());
        e.url_youtube = getVideoNetworkUrlOrNull(vrm.encodedVideos.getYoutube());
        e.videoId = block.getId();
        e.transcript = vrm.transcripts;
        e.lmsUrl = block.getBlockUrl();
        e.isVideoForWebOnly = vrm.onlyOnWeb;
        return e;
    }

    @Nullable
    private static String getVideoNetworkUrlOrNull(@Nullable VideoInfo videoInfo) {
        return videoInfo != null && URLUtil.isNetworkUrl(videoInfo.url) ? videoInfo.url : null;
    }
}
