package com.example.cmpuzz_events.ui.organizerdialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cmpuzz_events.R;

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

        // TODO later:
        // - Bind tvName, tvCreated, tvBio, imgAvatar with real user data
        // - Setup RecyclerView rvCreatedEvents with adapter + layoutManager
    }
}