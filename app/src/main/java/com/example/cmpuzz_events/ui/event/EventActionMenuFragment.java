package com.example.cmpuzz_events.ui.event;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.cmpuzz_events.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;

public class EventActionMenuFragment extends Fragment {

    private static final String ARG_EVENT_ID = "eventId";
    private String eventId;

    public static EventActionMenuFragment newInstance(String eventId) {
        EventActionMenuFragment fragment = new EventActionMenuFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            eventId = getArguments().getString(ARG_EVENT_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_event_action_menu, container, false);

        // Setup toolbar
        MaterialToolbar toolbar = root.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            if (getActivity() != null) {
                Navigation.findNavController(root).navigate(R.id.action_to_event_details);
            }
        });

        // Setup click listeners for all menu items
        setupClickListeners(root);

        return root;
    }

    private void setupClickListeners(View root) {
        // Entrants section
        root.findViewById(R.id.cardCancelEntrants).setOnClickListener(v ->
                showToast("Cancel Entrants - Coming soon"));

        root.findViewById(R.id.cardViewDeclinedEntrants).setOnClickListener(v ->
                showToast("View Declined Entrants - Coming soon"));

        root.findViewById(R.id.cardViewWaitlist).setOnClickListener(v ->
                showToast("View Waitlist - Coming soon"));

        root.findViewById(R.id.cardViewInvitedEntrants).setOnClickListener(v ->
                showToast("View Invited Entrants - Coming soon"));

        root.findViewById(R.id.cardViewAttendees).setOnClickListener(v ->
                showToast("View Attendees - Coming soon"));

        // Notifications section
        root.findViewById(R.id.cardNotifyDeclined).setOnClickListener(v ->
                showToast("Send Notification to Declined - Coming soon"));

        root.findViewById(R.id.cardNotifyWaitlist).setOnClickListener(v ->
                showToast("Send Notification to Waitlist - Coming soon"));

        root.findViewById(R.id.cardNotifyInvited).setOnClickListener(v ->
                showToast("Send Notification to Invited - Coming soon"));

        root.findViewById(R.id.cardNotifyAttendees).setOnClickListener(v ->
                showToast("Send Notification to Attendees - Coming soon"));
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}
