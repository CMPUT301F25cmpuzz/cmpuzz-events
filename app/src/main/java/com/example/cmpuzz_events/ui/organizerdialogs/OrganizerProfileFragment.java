package com.example.cmpuzz_events.ui.organizerdialogs;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cmpuzz_events.R;
import com.example.cmpuzz_events.auth.AuthManager;
import com.example.cmpuzz_events.models.user.User;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class OrganizerProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_organizer_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Load and display organizer data from Firebase
        loadOrganizerData(view);

        // EDIT button (optional for later)
        View edit = view.findViewById(R.id.btnEdit);
        if (edit != null) {
            edit.setOnClickListener(v -> {
                // TODO: navigate to edit profile screen
            });
        }

        // Setup logout button
        View logoutButton = view.findViewById(R.id.btnLogout);
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> logout());
        }
    }

    private void loadOrganizerData(View view) {
        User currentUser = AuthManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            return;
        }

        // Set organizer name
        TextView tvName = view.findViewById(R.id.tvName);
        if (tvName != null && currentUser.getDisplayName() != null) {
            tvName.setText(currentUser.getDisplayName());
        }

        // Set account creation date
        TextView tvCreated = view.findViewById(R.id.tvCreated);
        if (tvCreated != null && currentUser.getCreatedAt() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
            String dateString = sdf.format(new Date(currentUser.getCreatedAt()));
            tvCreated.setText("Account Created: " + dateString);
        }

        // Set bio/description (using email as placeholder since User model doesn't have bio field)
        TextView tvBio = view.findViewById(R.id.tvBio);
        if (tvBio != null && currentUser.getEmail() != null) {
            tvBio.setText(currentUser.getEmail());
        }
    }

    private void logout() {
        // Clear user session
        AuthManager.getInstance().signOut();
        
        // Show toast
        Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
        
        // Redirect to LoginActivity
        Intent intent = new Intent(getActivity(), com.example.cmpuzz_events.auth.LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        
        if (getActivity() != null) {
            getActivity().finish();
        }
    }
}
