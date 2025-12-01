package com.example.cmpuzz_events.ui.event;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.example.cmpuzz_events.R;
import com.example.cmpuzz_events.models.user.User;

import java.util.List;

/**
 * Adapter to display enrolled users in an event's waitlist
 */
public class EnrolledUsersAdapter extends RecyclerView.Adapter<EnrolledUsersAdapter.UserViewHolder> {

    private List<User> users;

    public EnrolledUsersAdapter(List<User> users) {
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
        holder.bind(users.get(position));

    }

    @Override
    public int getItemCount() {
        return users != null ? users.size() : 0;
    }
    static class UserViewHolder extends RecyclerView.ViewHolder {
        private final TextView userName;
        private final ImageView userAvatar;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.user_name);
            userAvatar = itemView.findViewById(R.id.user_avatar);
        }

        public void bind(User user) {
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

                // Load and display profile image using Glide
                if (userAvatar != null) {
                    if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                        Glide.with(itemView.getContext())
                                .load(user.getProfileImageUrl())
                                .transform(new CircleCrop())
                                .placeholder(R.drawable.ic_profile)
                                .into(userAvatar);
                    } else {
                        userAvatar.setImageResource(R.drawable.ic_profile);
                    }
                }
            }
        }
    }
}
