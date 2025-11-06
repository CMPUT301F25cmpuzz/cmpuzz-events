package com.example.cmpuzz_events.ui.event;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.cmpuzz_events.R;
import com.example.cmpuzz_events.databinding.FragmentEventDetailsBinding;

public class EventDetailsFragment extends Fragment {

    private FragmentEventDetailsBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        EventDetailsViewModel eventDetailsViewModel =
                new ViewModelProvider(this).get(EventDetailsViewModel.class);

        binding = FragmentEventDetailsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textEventDetails;
        eventDetailsViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        // TODO: Fix/Implement? these statements are just placeholders
        binding.eventHost.setText(getString(R.string.hosted_by, "Cmpuzz"));
        binding.eventAvailability.setText(getString(R.string.event_status, "Open"));

        // Additional Actions Button
        binding.additionalActionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Navigation.findNavController(view).navigate(R.id.action_navigation_event_details_to_navigation_event_action_menu);
            }
        });

        // Edit Button
        binding.editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Navigation.findNavController(view).navigate(R.id.action_navigation_event_details_to_navigation_event_edit_menu);
            }
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
