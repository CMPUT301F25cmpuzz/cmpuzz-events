package com.example.cmpuzz_events.ui.organizerdialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cmpuzz_events.R;
import com.example.cmpuzz_events.auth.AuthManager;
import com.example.cmpuzz_events.models.user.User;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class organizerProfileFragment extends Fragment {

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

        // Listen for result from confirm delete dialog
        getParentFragmentManager().setFragmentResultListener(
                confirmDeleteDialogFragment.REQUEST_KEY,
                getViewLifecycleOwner(),
                (key, bundle) -> {
                    boolean confirmed =
                            bundle.getBoolean(confirmDeleteDialogFragment.RESULT_CONFIRMED, false);
                    if (confirmed) {
                        // TODO: Hook to ViewModel or Repo to delete profile
                        // viewModel.deleteProfile();
                    }
                }
        );

        // DELETE button opens the dialog
        view.findViewById(R.id.btnDelete).setOnClickListener(v ->
                new confirmDeleteDialogFragment().show(
                        getParentFragmentManager(),
                        "confirm_delete"
                )
        );

        // EDIT button (optional for later)
        View edit = view.findViewById(R.id.btnEdit);
        if (edit != null) {
            edit.setOnClickListener(v -> {
                // TODO: navigate to edit profile screen
            });
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
}