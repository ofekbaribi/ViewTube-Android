package com.example.viewtube.viewmodels;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.viewtube.entities.VideoItem;
import com.example.viewtube.repositories.VideosRepository;

import java.io.File;
import java.util.List;

public class VideosViewModel extends AndroidViewModel {

    private VideosRepository mRepository;
    private LiveData<List<VideoItem>> videoItemsLiveData;
    private LiveData<VideoItem> selectedVideoItemLiveData;

    public VideosViewModel(Application application) {
        super(application);
        mRepository = new VideosRepository(application);
        videoItemsLiveData = mRepository.getAllVideos();
        selectedVideoItemLiveData = mRepository.getSelectedVideoItem();
    }

    public LiveData<List<VideoItem>> getVideoItemsLiveData() {
        return videoItemsLiveData;
    }

    public LiveData<VideoItem> getSelectedVideoItemLiveData() {
        return selectedVideoItemLiveData;
    }

    public void fetchAllVideos() {
        mRepository.fetchAllVideos();
    }


    public VideoItem getVideoById(int videoId) {
        return mRepository.getVideoItem(videoId);
    }


    public void setSelectedVideoItem(VideoItem videoItem) {
        mRepository.setSelectedVideoItem(videoItem);
    }

    public void postSelectedVideoItem(VideoItem videoItem) {
        mRepository.postSelectedVideoItem(videoItem);
    }

    public void fetchSelectedVideoItem(int videoId) {
        new Thread(() -> {
            VideoItem videoItem = getVideoById(videoId);
            postSelectedVideoItem(videoItem);
        }).start();
    }

    public void addVideoItem(final VideoItem videoItem, File videoFile, File thumbnailFile) {
        mRepository.add(videoItem, videoFile, thumbnailFile);
    }
    public void updateVideoItemTitle(int videoId, String username, String title) {
        VideoItem videoItem = selectedVideoItemLiveData.getValue();
        if (videoItem != null) {
            videoItem.setTitle(title);
            mRepository.update(videoId, username, title, videoItem.getDescription());
        }
    }

    public void updateVideoItemDescription(int videoId, String username, String description) {
        VideoItem videoItem = selectedVideoItemLiveData.getValue();
        if (videoItem != null) {
            videoItem.setDescription(description);
            mRepository.update(videoId, username, videoItem.getTitle(), description);
        }
    }

    public void deleteVideoItem(int videoId, String username) {
        mRepository.delete(videoId, username);
    }

    public void reload() {
        mRepository.reload();
    }

    public void updateVideo(VideoItem videoItem) {
        mRepository.updateVideo(videoItem);
    }


    public void userLiked(int videoId, String username) {
        mRepository.userLiked(videoId, username);
    }

    public void incrementViewCount(int videoId) {
        mRepository.incrementViewCount(videoId);
    }

}
