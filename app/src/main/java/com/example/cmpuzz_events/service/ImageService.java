package com.example.cmpuzz_events.service;

import android.util.Log;

import com.example.cmpuzz_events.ui.admin.ImageItem;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing event poster images in Firebase Storage.
 */
public class ImageService implements IImageService {
    
    private static final String TAG = "ImageService";
    private static ImageService instance;
    private final FirebaseStorage storage;
    private final FirebaseFirestore db;
    private final StorageReference eventPostersRef;
    
    private ImageService() {
        storage = FirebaseStorage.getInstance();
        db = FirebaseFirestore.getInstance();
        eventPostersRef = storage.getReference().child("event_posters");
    }
    
    public static synchronized ImageService getInstance() {
        if (instance == null) {
            instance = new ImageService();
        }
        return instance;
    }
    
    @Override
    public void loadAllImages(ImageListCallback callback) {
        eventPostersRef.listAll()
            .addOnSuccessListener(listResult -> {
                if (listResult.getItems().isEmpty()) {
                    callback.onSuccess(new ArrayList<>());
                    return;
                }
                
                List<ImageItem> imageItems = new ArrayList<>();
                
                // Load URLs asynchronously
                for (StorageReference item : listResult.getItems()) {
                    item.getDownloadUrl().addOnSuccessListener(uri -> {
                        ImageItem imageItem = new ImageItem();
                        imageItem.setName(item.getName());
                        imageItem.setUrl(uri.toString());
                        imageItem.setReference(item);
                        imageItems.add(imageItem);
                        
                        // Call callback with current list (progressive loading)
                        callback.onSuccess(new ArrayList<>(imageItems));
                    }).addOnFailureListener(e -> {
                        Log.e(TAG, "Error getting download URL for: " + item.getName(), e);
                    });
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error listing images", e);
                callback.onError("Error loading images: " + e.getMessage());
            });
    }
    
    @Override
    public void deleteImage(ImageItem imageItem, VoidCallback callback) {
        String imageUrl = imageItem.getUrl();
        
        // First, delete the image from Storage
        imageItem.getReference().delete()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Deleted image from storage: " + imageItem.getName());
                
                // Then, remove posterUrl from any events using this image
                updateEventsUsingImage(imageUrl, callback);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error deleting image: " + imageItem.getName(), e);
                callback.onError("Error deleting image: " + e.getMessage());
            });
    }
    
    private void updateEventsUsingImage(String imageUrl, VoidCallback callback) {
        // Find all events with this posterUrl
        db.collection("events")
            .whereEqualTo("posterUrl", imageUrl)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                if (querySnapshot.isEmpty()) {
                    Log.d(TAG, "No events found using this image");
                    callback.onSuccess();
                    return;
                }
                
                // Update each event to remove the posterUrl
                int eventCount = querySnapshot.size();
                Log.d(TAG, "Removing posterUrl from " + eventCount + " event(s)");
                
                int[] updatedCount = {0};
                int[] failedCount = {0};
                
                querySnapshot.forEach(document -> {
                    document.getReference().update("posterUrl", null)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Removed posterUrl from event: " + document.getId());
                            updatedCount[0]++;
                            
                            // Call callback when all updates are done
                            if (updatedCount[0] + failedCount[0] == eventCount) {
                                if (failedCount[0] > 0) {
                                    callback.onError("Updated " + updatedCount[0] + " events, but " + failedCount[0] + " failed");
                                } else {
                                    callback.onSuccess();
                                }
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error updating event: " + document.getId(), e);
                            failedCount[0]++;
                            
                            // Call callback when all updates are done
                            if (updatedCount[0] + failedCount[0] == eventCount) {
                                if (failedCount[0] > 0) {
                                    callback.onError("Updated " + updatedCount[0] + " events, but " + failedCount[0] + " failed");
                                } else {
                                    callback.onSuccess();
                                }
                            }
                        });
                });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error querying events by posterUrl", e);
                callback.onError("Error updating events: " + e.getMessage());
            });
    }
}
