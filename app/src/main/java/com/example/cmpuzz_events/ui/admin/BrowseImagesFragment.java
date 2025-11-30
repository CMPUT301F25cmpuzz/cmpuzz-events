package com.example.cmpuzz_events.ui.admin;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cmpuzz_events.R;
import com.example.cmpuzz_events.auth.AuthManager;
import com.example.cmpuzz_events.models.user.User;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Admin-only fragment to browse and delete event poster images from Firebase Storage.
 */
public class BrowseImagesFragment extends Fragment {
    
    private static final String TAG = "BrowseImagesFragment";
    private RecyclerView recyclerView;
    private ImageListAdapter adapter;
    private TextView emptyStateText;
    private FirebaseStorage storage;
    private StorageReference eventPostersRef;
    private User currentUser;
    private boolean hasLoadedImages = false;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_browse_images, container, false);
        
        currentUser = AuthManager.getInstance().getCurrentUser();
        
        // Check if user is admin
        if (currentUser == null || !currentUser.isAdmin()) {
            Log.w(TAG, "Non-admin user attempted to access browse images");
            return root;
        }
        
        storage = FirebaseStorage.getInstance();
        eventPostersRef = storage.getReference().child("event_posters");
        
        recyclerView = root.findViewById(R.id.recyclerViewImages);
        emptyStateText = root.findViewById(R.id.tvEmptyState);
        
        // Use GridLayoutManager for 2 columns
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        adapter = new ImageListAdapter(new ArrayList<>());
        
        adapter.setOnImageActionListener(new ImageListAdapter.OnImageActionListener() {
            @Override
            public void onDeleteClick(ImageItem imageItem) {
                showDeleteConfirmation(imageItem);
            }
        });
        
        recyclerView.setAdapter(adapter);
        
        // Don't load images immediately, wait until fragment is actually visible or app wil shit itself LOL

        return root;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Only load images when the fragment is actually visible to the user
        if (!hasLoadedImages) {
            loadImages();
            hasLoadedImages = true;
        }
    }
    
    private void loadImages() {
        eventPostersRef.listAll()
            .addOnSuccessListener(listResult -> {
                if (listResult.getItems().isEmpty()) {
                    showEmptyState("No images found");
                    return;
                }
                
                List<ImageItem> imageItems = new ArrayList<>();
                
                // Show empty adapter first for immediate feedback
                recyclerView.setVisibility(View.VISIBLE);
                emptyStateText.setVisibility(View.GONE);
                
                // Load URLs asynchronously but update UI immediately as each loads
                for (StorageReference item : listResult.getItems()) {
                    // Get download URL for each image
                    item.getDownloadUrl().addOnSuccessListener(uri -> {
                        ImageItem imageItem = new ImageItem();
                        imageItem.setName(item.getName());
                        imageItem.setUrl(uri.toString());
                        imageItem.setReference(item);
                        imageItems.add(imageItem);
                        
                        // Update adapter immediately as each image URL is loaded
                        adapter.updateImages(new ArrayList<>(imageItems));
                    }).addOnFailureListener(e -> {
                        Log.e(TAG, "Error getting download URL for: " + item.getName(), e);
                    });
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error listing images", e);
                showEmptyState("Error loading images");
            });
    }
    
    private void showDeleteConfirmation(ImageItem imageItem) {
        new AlertDialog.Builder(requireContext())
            .setTitle("Delete Image")
            .setMessage("Are you sure you want to delete this image?\n\n" + imageItem.getName())
            .setPositiveButton("Delete", (dialog, which) -> deleteImage(imageItem))
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void deleteImage(ImageItem imageItem) {
        String imageUrl = imageItem.getUrl();
        
        // First, delete the image from Storage
        imageItem.getReference().delete()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Deleted image from storage: " + imageItem.getName());
                
                // Then, remove posterUrl from any events using this image
                updateEventsUsingImage(imageUrl);
                
                Toast.makeText(getContext(), "Image deleted successfully", Toast.LENGTH_SHORT).show();
                // Reload images
                loadImages();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error deleting image: " + imageItem.getName(), e);
                Toast.makeText(getContext(), "Error deleting image", Toast.LENGTH_SHORT).show();
            });
    }
    
    private void updateEventsUsingImage(String imageUrl) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Find all events with this posterUrl
        db.collection("events")
            .whereEqualTo("posterUrl", imageUrl)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                if (querySnapshot.isEmpty()) {
                    Log.d(TAG, "No events found using this image");
                    return;
                }
                
                // Update each event to remove the posterUrl
                int eventCount = querySnapshot.size();
                Log.d(TAG, "Removing posterUrl from " + eventCount + " event(s)");
                
                querySnapshot.forEach(document -> {
                    document.getReference().update("posterUrl", null)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Removed posterUrl from event: " + document.getId());
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error updating event: " + document.getId(), e);
                        });
                });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error querying events by posterUrl", e);
            });
    }
    
    private void showEmptyState(String message) {
        adapter.updateImages(new ArrayList<>());
        recyclerView.setVisibility(View.GONE);
        emptyStateText.setVisibility(View.VISIBLE);
        emptyStateText.setText(message);
    }
}
