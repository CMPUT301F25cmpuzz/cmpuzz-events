package com.example.cmpuzz_events.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cmpuzz_events.R;
import com.example.cmpuzz_events.models.user.User;
import com.example.cmpuzz_events.ui.event.OnItemClickListener;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;


import java.util.List;

/**
 * Adapter to display users with a clickable interface
 */
public class UserListAdapter extends RecyclerView.Adapter<UserListAdapter.UserViewHolder> {

    private List<User> users;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(User user, int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener)
    {
        this.listener = listener;
    }

    public UserListAdapter(List<User> users) {
        this.users = users;
    }

    public void updateUsers(List<User> newUsers) {
        this.users = newUsers;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_attendee, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User currUser = users.get(position);
        holder.bind(currUser);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(currUser, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return users != null ? users.size() : 0;
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        private final TextView userName;
        private final ImageView avatar;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.user_name);
            avatar = itemView.findViewById(R.id.user_avatar);
        }

        public void bind(User user) {
            // 1. Always clear any previous Glide request on this ImageView
            Glide.with(avatar.getContext()).clear(avatar);
            // 2. Set a default placeholder immediately so recycled garbage doesn't show
            avatar.setImageResource(R.drawable.ic_profile);

            if (user != null) {
                // Display user name or username
                String displayName = user.getDisplayName();
                if (displayName == null || displayName.isEmpty()) {
                    displayName = user.getUsername();
                }
                if (displayName == null || displayName.isEmpty()) {
                    displayName = user.getEmail();
                }
                userName.setText(displayName);

                // --- Profile image logic ---
                String profileUrl = user.getProfileImageUrl();  // adjust name if different

                if (profileUrl != null && !profileUrl.isEmpty()) {
                    Glide.with(avatar.getContext())
                            .load(profileUrl)
                            // cache strategy optional; you *don't* need to disable cache here
                            .placeholder(R.drawable.ic_profile)
                            .error(R.drawable.ic_profile)
                            .circleCrop()
                            .into(avatar);
                    // If profileUrl is null/empty, we just keep the default icon we set above
                }
            }
        }
    }
}
