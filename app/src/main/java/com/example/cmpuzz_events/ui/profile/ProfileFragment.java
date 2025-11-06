package com.example.cmpuzz_events.ui.profile;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.cmpuzz_events.R;
import com.example.cmpuzz_events.auth.AuthManager;
import com.example.cmpuzz_events.databinding.FragmentProfileBinding;
import com.example.cmpuzz_events.models.user.User;
import com.example.cmpuzz_events.service.EventService;
import com.example.cmpuzz_events.service.IEventService;
import com.example.cmpuzz_events.ui.event.Event;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private EventService eventService;
    private EnrolledEventsAdapter adapter;
    private static final String TAG = "ProfileFragment";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentProfileBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        eventService = EventService.getInstance();

        // Display user info
        User currentUser = AuthManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            binding.tvUserName.setText(currentUser.getDisplayName());
            binding.tvUserEmail.setText(currentUser.getEmail());
            binding.tvUserRole.setText("Role: " + currentUser.getRole().getRoleName());
            
            // Setup enrolled events RecyclerView
            setupEnrolledEvents(root, currentUser);
        }

        return root;
    }

    private void setupEnrolledEvents(View root, User currentUser) {
        binding.recyclerViewEnrolledEvents.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new EnrolledEventsAdapter(new ArrayList<>());
        
        adapter.setOnEventActionListener(new EnrolledEventsAdapter.OnEventActionListener() {
            @Override
            public void onLeaveEvent(Event event) {
                leaveEvent(event, currentUser);
            }

            @Override
            public void onViewEvent(Event event) {
                // Navigate to event details
                Bundle bundle = new Bundle();
                bundle.putString("eventId", event.getEventId());
                Navigation.findNavController(root).navigate(
                    R.id.action_profile_to_event_details,
                    bundle
                );
            }
        });
        
        binding.recyclerViewEnrolledEvents.setAdapter(adapter);
        loadEnrolledEvents(currentUser.getUid());
    }

    private void loadEnrolledEvents(String userId) {
        eventService.getEventsUserEnrolledIn(userId, new IEventService.UIEventListCallback() {
            @Override
            public void onSuccess(List<Event> events) {
                Log.d(TAG, "Loaded " + events.size() + " enrolled events");
                adapter.updateEvents(events);
                
                if (events.isEmpty()) {
                    binding.recyclerViewEnrolledEvents.setVisibility(View.GONE);
                    binding.tvEmptyEnrolled.setVisibility(View.VISIBLE);
                } else {
                    binding.recyclerViewEnrolledEvents.setVisibility(View.VISIBLE);
                    binding.tvEmptyEnrolled.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading enrolled events: " + error);
                Toast.makeText(getContext(), "Error loading enrolled events", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void leaveEvent(Event event, User currentUser) {
        eventService.removeFromWaitlist(event.getEventId(), currentUser.getUid(), new IEventService.VoidCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(getContext(), "Left " + event.getTitle(), Toast.LENGTH_SHORT).show();
                // Reload enrolled events
                loadEnrolledEvents(currentUser.getUid());
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Failed to leave: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
