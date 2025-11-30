package com.example.cmpuzz_events.service;

import com.example.cmpuzz_events.ui.admin.ImageItem;

import java.util.List;

/**
 * Interface for Image service operations.
 * Handles Firebase Storage operations for event poster images.
 */
public interface IImageService {
    
    /**
     * Callback interface for image list operations
     */
    interface ImageListCallback {
        void onSuccess(List<ImageItem> images);
        void onError(String error);
    }
    
    /**
     * Callback interface for void operations
     */
    interface VoidCallback {
        void onSuccess();
        void onError(String error);
    }
    
    /**
     * Load all images from the event_posters folder in Firebase Storage
     *
     * @param callback Callback with list of ImageItem or error
     */
    void loadAllImages(ImageListCallback callback);
    
    /**
     * Delete an image from Firebase Storage and update all events using it
     *
     * @param imageItem The image to delete
     * @param callback Callback on success or error
     */
    void deleteImage(ImageItem imageItem, VoidCallback callback);
}
