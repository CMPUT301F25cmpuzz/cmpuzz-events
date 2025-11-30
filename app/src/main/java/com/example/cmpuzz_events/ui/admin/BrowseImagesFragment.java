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
import com.example.cmpuzz_events.service.IImageService;
import com.example.cmpuzz_events.service.ImageService;

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
    private IImageService imageService;
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
        
        imageService = ImageService.getInstance();
        
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
        recyclerView.setVisibility(View.VISIBLE);
        emptyStateText.setVisibility(View.GONE);
        
        imageService.loadAllImages(new IImageService.ImageListCallback() {
            @Override
            public void onSuccess(List<ImageItem> images) {
                if (images.isEmpty()) {
                    showEmptyState("No images found");
                } else {
                    adapter.updateImages(images);
                    recyclerView.setVisibility(View.VISIBLE);
                    emptyStateText.setVisibility(View.GONE);
                }
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading images: " + error);
                showEmptyState("Error loading images");
            }
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
        imageService.deleteImage(imageItem, new IImageService.VoidCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(getContext(), "Image deleted successfully", Toast.LENGTH_SHORT).show();
                // Reload images
                hasLoadedImages = false;
                loadImages();
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Error deleting image: " + error);
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void showEmptyState(String message) {
        adapter.updateImages(new ArrayList<>());
        recyclerView.setVisibility(View.GONE);
        emptyStateText.setVisibility(View.VISIBLE);
        emptyStateText.setText(message);
    }
}
