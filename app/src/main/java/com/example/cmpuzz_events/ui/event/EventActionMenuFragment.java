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

    private static final String ARG_EVENT = "event";
    private Event event;

    public static EventActionMenuFragment newInstance(Event event) {
        EventActionMenuFragment fragment = new EventActionMenuFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_EVENT, event);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            event = (Event) getArguments().getSerializable(ARG_EVENT);
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
                getActivity().onBackPressed();
            }
        });

        // Setup click listeners for all menu items
        setupClickListeners(root);

        return root;
    }

    private void setupClickListeners(View root) {
        // Now you have access to the event object!
        String eventTitle = event != null ? event.getTitle() : "Unknown";
        
        // Entrants section
        root.findViewById(R.id.cardCancelEntrants).setOnClickListener(v ->
                showToast("Cancel Entrants for: " + eventTitle));

        root.findViewById(R.id.cardViewDeclinedEntrants).setOnClickListener(v ->
                showToast("View Declined Entrants for: " + eventTitle));

        root.findViewById(R.id.cardViewWaitlist).setOnClickListener(v -> {
            if (event != null) {
                Bundle bundle = new Bundle();
                bundle.putString("eventId", event.getEventId());
                Navigation.findNavController(root).navigate(
                    R.id.action_to_view_entrants,
                    bundle
                );
            }
        });

        root.findViewById(R.id.cardViewInvitedEntrants).setOnClickListener(v -> {
            if (event != null) {
                Bundle bundle = new Bundle();
                bundle.putString("eventId", event.getEventId());
                Navigation.findNavController(root).navigate(
                    R.id.action_to_view_entrants,
                    bundle
                );
            }
        });

        root.findViewById(R.id.cardViewAttendees).setOnClickListener(v -> {
            if (event != null) {
                Bundle bundle = new Bundle();
                bundle.putString("eventId", event.getEventId());
                Navigation.findNavController(root).navigate(
                    R.id.action_to_view_entrants,
                    bundle
                );
            }
        });

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
