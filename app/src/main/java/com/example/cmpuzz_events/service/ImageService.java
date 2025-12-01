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
    private final StorageReference profilePostersRef;
    private ImageService() {
        storage = FirebaseStorage.getInstance();
        db = FirebaseFirestore.getInstance();
        eventPostersRef = storage.getReference().child("event_posters");
        profilePostersRef = storage.getReference().child("profile_images");
    }
    
    public static synchronized ImageService getInstance() {
        if (instance == null) {
            instance = new ImageService();
        }
        return instance;
    }
    
    @Override
    public void loadAllImages(ImageListCallback callback) {
        List<ImageItem> allImageItems = new ArrayList<>();
        
        // Load event posters
        eventPostersRef.listAll()
            .addOnSuccessListener(listResult -> {
                if (listResult.getItems().isEmpty()) {
                    callback.onSuccess(new ArrayList<>());
                    return;
                }
                
                // Load URLs asynchronously
                for (StorageReference item : listResult.getItems()) {
                    item.getDownloadUrl().addOnSuccessListener(uri -> {
                        ImageItem imageItem = new ImageItem();
                        imageItem.setName("event_posters/" + item.getName());
                        imageItem.setUrl(uri.toString());
                        imageItem.setReference(item);
                        allImageItems.add(imageItem);
                        
                        // Call callback with current list (progressive loading)
                        callback.onSuccess(new ArrayList<>(allImageItems));
                    }).addOnFailureListener(e -> {
                        Log.e(TAG, "Error getting download URL for: " + item.getName(), e);
                    });
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error listing images", e);
                callback.onError("Error loading images: " + e.getMessage());
            });
        
        // Load profile images
        profilePostersRef.listAll()
            .addOnSuccessListener(listResult -> {
                if (listResult.getItems().isEmpty()) {
                    callback.onSuccess(new ArrayList<>());
                    return;
                }
                
                // Load URLs asynchronously
                for (StorageReference item : listResult.getItems()) {
                    item.getDownloadUrl().addOnSuccessListener(uri -> {
                        ImageItem imageItem = new ImageItem();
                        imageItem.setName("profile_images/" + item.getName());
                        imageItem.setUrl(uri.toString());
                        imageItem.setReference(item);
                        allImageItems.add(imageItem);
                        
                        // Call callback with current list (progressive loading)
                        callback.onSuccess(new ArrayList<>(allImageItems));
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
        String imageName = imageItem.getName();
        
        // First, delete the image from Storage
        imageItem.getReference().delete()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Deleted image from storage: " + imageName);
                
                // Check if it's an event poster or profile image
                if (imageName != null && imageName.startsWith("event_posters/")) {
                    // Remove posterUrl from any events using this image
                    updateEventsUsingImage(imageUrl, callback);
                } else if (imageName != null && imageName.startsWith("profile_images/")) {
                    // Remove profileImageUrl from any users using this image
                    updateUsersUsingImage(imageUrl, callback);
                } else {
                    // Image doesn't have a recognized prefix - just complete successfully
                    // (Storage deletion already succeeded, and we can't determine which type it was)
                    Log.w(TAG, "Deleted image without recognized prefix: " + imageName);
                    callback.onSuccess();
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error deleting image: " + imageName, e);
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
    
    /**
     * Removes profileImageUrl from all users using this image.
     * Called when a profile image is deleted.
     */
    private void updateUsersUsingImage(String imageUrl, VoidCallback callback) {
        // Find all users with this profileImageUrl
        db.collection("users")
            .whereEqualTo("profileImageUrl", imageUrl)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                if (querySnapshot.isEmpty()) {
                    Log.d(TAG, "No users found using this profile image");
                    callback.onSuccess();
                    return;
                }
                
                // Update each user to remove the profileImageUrl
                int userCount = querySnapshot.size();
                Log.d(TAG, "Removing profileImageUrl from " + userCount + " user(s)");
                
                int[] updatedCount = {0};
                int[] failedCount = {0};
                
                querySnapshot.forEach(document -> {
                    document.getReference().update("profileImageUrl", null)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Removed profileImageUrl from user: " + document.getId());
                            updatedCount[0]++;
                            
                            // Call callback when all updates are done
                            if (updatedCount[0] + failedCount[0] == userCount) {
                                if (failedCount[0] > 0) {
                                    callback.onError("Updated " + updatedCount[0] + " users, but " + failedCount[0] + " failed");
                                } else {
                                    callback.onSuccess();
                                }
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error updating user: " + document.getId(), e);
                            failedCount[0]++;
                            
                            // Call callback when all updates are done
                            if (updatedCount[0] + failedCount[0] == userCount) {
                                if (failedCount[0] > 0) {
                                    callback.onError("Updated " + updatedCount[0] + " users, but " + failedCount[0] + " failed");
                                } else {
                                    callback.onSuccess();
                                }
                            }
                        });
                });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error querying users by profileImageUrl", e);
                callback.onError("Error updating users: " + e.getMessage());
            });
    }
}
