package com.example.cmpuzz_events.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cmpuzz_events.R;

import java.util.List;

public class ImageListAdapter extends RecyclerView.Adapter<ImageListAdapter.ImageViewHolder> {
    
    private List<ImageItem> imageItems;
    private OnImageActionListener listener;
    
    public interface OnImageActionListener {
        void onDeleteClick(ImageItem imageItem);
    }
    
    public ImageListAdapter(List<ImageItem> imageItems) {
        this.imageItems = imageItems;
    }
    
    public void setOnImageActionListener(OnImageActionListener listener) {
        this.listener = listener;
    }
    
    public void updateImages(List<ImageItem> newImages) {
        this.imageItems = newImages;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_image, parent, false);
        return new ImageViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        ImageItem imageItem = imageItems.get(position);
        
        // Load image using Glide with optimizations
        Glide.with(holder.itemView.getContext())
            .load(imageItem.getUrl())
            .placeholder(R.drawable.ic_image_placeholder)
            .thumbnail(0.1f)  // Load a 10% quality thumbnail first for faster display
            .override(400, 400)  // Resize to reasonable dimensions for grid
            .centerCrop()
            .into(holder.imageView);
        
        holder.tvImageName.setText(imageItem.getName());
        
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(imageItem);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return imageItems.size();
    }
    
    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView tvImageName;
        ImageButton btnDelete;
        
        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            tvImageName = itemView.findViewById(R.id.tvImageName);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
