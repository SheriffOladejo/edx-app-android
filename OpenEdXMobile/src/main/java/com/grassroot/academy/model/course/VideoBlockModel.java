package com.grassroot.academy.model.course;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.grassroot.academy.model.db.DownloadEntry;
import com.grassroot.academy.model.video.VideoQuality;
import com.grassroot.academy.module.storage.IStorage;
import com.grassroot.academy.util.UrlUtil;

/**
 * common base class for all type of units
 */
public class VideoBlockModel extends CourseComponent implements HasDownloadEntry {

    private DownloadEntry downloadEntry;
    private VideoData data;
    private String downloadUrl;
    private String videoThumbnail;

    public VideoBlockModel(@NonNull VideoBlockModel other) {
        super(other);
        this.downloadEntry = other.downloadEntry;
        this.data = other.data;
        this.downloadUrl = other.downloadUrl;
    }

    public VideoBlockModel(BlockModel blockModel, CourseComponent parent) {
        super(blockModel, parent);
        this.data = (VideoData) blockModel.data;
    }

    @Nullable
    public DownloadEntry getDownloadEntry(IStorage storage) {
        if (data.encodedVideos.getPreferredNativeVideoInfo() == null) {
            return null;
        }
        if (storage != null) {
            downloadEntry = (DownloadEntry) storage
                    .getDownloadEntryFromVideoModel(this);
        }
        return downloadEntry;
    }

    public void setDownloadUrl(@Nullable String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    @Nullable
    public String getVideoThumbnail(@Nullable String baseURL) {
        return UrlUtil.makeAbsolute(videoThumbnail, baseURL);
    }

    public void setVideoThumbnail(String videoThumbnail) {
        this.videoThumbnail = videoThumbnail;
    }

    @Nullable
    @Override
    public String getDownloadUrl() {
        return downloadUrl;
    }

    public VideoData getData() {
        return data;
    }

    public void setData(VideoData data) {
        this.data = data;
    }

    /**
     * Returns the size of the video file of whichever encoding is currently preferred within the
     * app for playing or downloading.
     *
     * @param preferredVideoQuality [VideoQuality] selected by the user.
     * @return The size of the video if available, <code>-1</code> otherwise.
     */
    public long getPreferredVideoEncodingSize(VideoQuality preferredVideoQuality) {
        if (data != null && data.encodedVideos != null
                && data.encodedVideos.getPreferredVideoInfoForDownloading(preferredVideoQuality) != null) {
            return data.encodedVideos.getPreferredVideoInfoForDownloading(preferredVideoQuality).fileSize;
        }
        return -1;
    }
}
