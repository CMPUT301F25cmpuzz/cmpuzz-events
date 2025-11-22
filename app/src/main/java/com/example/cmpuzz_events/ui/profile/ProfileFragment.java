package com.example.cmpuzz_events.ui.profile;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.cmpuzz_events.R;
import com.example.cmpuzz_events.auth.AuthManager;
import com.example.cmpuzz_events.databinding.FragmentProfileBinding;
import com.example.cmpuzz_events.models.event.EventEntity;
import com.example.cmpuzz_events.models.event.Invitation;
import com.example.cmpuzz_events.models.user.User;
import com.example.cmpuzz_events.service.EventService;
import com.example.cmpuzz_events.service.IEventService;
import com.example.cmpuzz_events.service.INotificationService;
import com.example.cmpuzz_events.service.NotificationService;
import com.example.cmpuzz_events.ui.event.Event;
import com.example.cmpuzz_events.ui.profile.EnrolledEventsAdapter.EventWithStatus;

import java.util.ArrayList;
import java.util.List;



import androidx.appcompat.app.AlertDialog;
import android.widget.EditText;

import com.google.android.material.snackbar.Snackbar;

import com.example.cmpuzz_events.auth.AuthManager;
import com.example.cmpuzz_events.models.user.User;
import com.example.cmpuzz_events.service.ProfileService;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Map;
import java.util.HashMap;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;


public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private EventService eventService;
    private NotificationService notificationService;
    private EnrolledEventsAdapter adapter;
    private static final String TAG = "ProfileFragment";

    private ProfileService profileService;
    private User currentUser;
    private SharedPreferences preferences;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentProfileBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        eventService = EventService.getInstance();
        notificationService = NotificationService.getInstance();
        notificationService.setContext(requireContext().getApplicationContext());
        preferences = requireContext().getSharedPreferences("user_preferences", Context.MODE_PRIVATE);

        // Display user info
        currentUser = AuthManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            binding.tvUserName.setText(currentUser.getDisplayName());
            binding.tvUserEmail.setText(currentUser.getEmail());
            binding.tvUserRole.setText("Role: " + currentUser.getRole().getRoleName());
            
            // Setup enrolled events RecyclerView
            setupEnrolledEvents(root, currentUser);
        }
        
        // Setup settings button
        binding.btnSettings.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(v);
            navController.navigate(R.id.action_profile_to_settings);
        });


        profileService = new ProfileService();
        if (currentUser == null) {
            currentUser = AuthManager.getInstance().getCurrentUser();
        }
        binding.btnEditProfile.setOnClickListener(v -> showEditDialog());

        // Setup logout button
        binding.btnLogout.setOnClickListener(v -> logout());

        return root;
    }

    private void setupEnrolledEvents(View root, User currentUser) {
        binding.recyclerViewEnrolledEvents.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new EnrolledEventsAdapter(new ArrayList<>());
        
        adapter.setOnEventActionListener(new EnrolledEventsAdapter.OnEventActionListener() {
            @Override
            public void onLeaveWaitlist(Event event) {
                leaveWaitlist(event, currentUser);
            }

            @Override
            public void onAcceptInvitation(Event event) {
                acceptInvitation(event, currentUser);
            }

            @Override
            public void onDeclineInvitation(Event event) {
                declineInvitation(event, currentUser);
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
        // Use getEventsForUserWithEntities to get full event data with invitations
        eventService.getEventsForUserWithEntities(userId, new IEventService.EventListCallback() {
            @Override
            public void onSuccess(List<EventEntity> entities) {
                Log.d(TAG, "Loaded " + entities.size() + " events for user");
                
                // Convert to EventWithStatus
                List<EventWithStatus> eventsWithStatus = new ArrayList<>();
                for (EventEntity entity : entities) {
                    Event uiEvent = convertToUIEvent(entity);
                    String status = determineUserStatus(entity, userId);
                    eventsWithStatus.add(new EventWithStatus(uiEvent, status));
                }
                
                adapter.updateEvents(eventsWithStatus);
                
                if (eventsWithStatus.isEmpty()) {
                    binding.recyclerViewEnrolledEvents.setVisibility(View.GONE);
                    binding.tvEmptyEnrolled.setVisibility(View.VISIBLE);
                } else {
                    binding.recyclerViewEnrolledEvents.setVisibility(View.VISIBLE);
                    binding.tvEmptyEnrolled.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading events: " + error);
                Toast.makeText(getContext(), "Error loading events", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Event convertToUIEvent(EventEntity entity) {
        List<String> waitlistIds = entity.getWaitlist();
        Event uiEvent = new Event(
            entity.getEventId(),
            entity.getTitle(),
            entity.getDescription(),
            entity.getCapacity(),
            entity.getRegistrationStart(),
            entity.getRegistrationEnd(),
            entity.getOrganizerId(),
            entity.getOrganizerName(),
            entity.isGeolocationRequired(),
                waitlistIds
        );
        uiEvent.setMaxEntrants(entity.getMaxEntrants());
        return uiEvent;
    }

    private String determineUserStatus(EventEntity entity, String userId) {
        // Check if user is an attendee (accepted invitation)
        if (entity.getAttendees() != null && entity.getAttendees().contains(userId)) {
            return "attending";
        }
        
        // Check if user has declined
        if (entity.getDeclined() != null && entity.getDeclined().contains(userId)) {
            return "declined";
        }
        
        // Check if user has an invitation
        if (entity.getInvitations() != null) {
            for (Invitation inv : entity.getInvitations()) {
                if (inv.getUserId() != null && inv.getUserId().equals(userId)) {
                    if (inv.isPending()) {
                        return "invited";
                    }
                }
            }
        }
        
        // If not attending, invited, or declined, must be in waitlist
        return "waitlist";
    }

    private void leaveWaitlist(Event event, User currentUser) {
        eventService.removeFromWaitlist(event.getEventId(), currentUser.getUid(), new IEventService.VoidCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(getContext(), "Left waitlist for " + event.getTitle(), Toast.LENGTH_SHORT).show();
                loadEnrolledEvents(currentUser.getUid());
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Failed to leave: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void acceptInvitation(Event event, User currentUser) {
        eventService.respondToInvitation(event.getEventId(), currentUser.getUid(), true, new IEventService.VoidCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(getContext(), "Accepted invitation to " + event.getTitle(), Toast.LENGTH_SHORT).show();
                
                // Notify organizer
                String userName = currentUser.getDisplayName() != null ? 
                                currentUser.getDisplayName() : "A user";
                notificationService.notifyOrganizerOfResponse(
                    event.getOrganizerId(),
                    userName,
                    event.getEventId(),
                    event.getTitle(),
                    true,
                    new INotificationService.VoidCallback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Organizer notified of acceptance");
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "Error notifying organizer: " + error);
                        }
                    }
                );
                
                loadEnrolledEvents(currentUser.getUid());
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Failed to accept: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void declineInvitation(Event event, User currentUser) {
        eventService.respondToInvitation(event.getEventId(), currentUser.getUid(), false, new IEventService.VoidCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(getContext(), "Declined invitation to " + event.getTitle(), Toast.LENGTH_SHORT).show();
                
                // Notify organizer
                String userName = currentUser.getDisplayName() != null ? 
                                currentUser.getDisplayName() : "A user";
                notificationService.notifyOrganizerOfResponse(
                    event.getOrganizerId(),
                    userName,
                    event.getEventId(),
                    event.getTitle(),
                    false,
                    new INotificationService.VoidCallback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Organizer notified of decline");
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "Error notifying organizer: " + error);
                        }
                    }
                );
                
                loadEnrolledEvents(currentUser.getUid());
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Failed to decline: " + error, Toast.LENGTH_SHORT).show();
            }
        });
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


    private void showEditDialog() {
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Not signed in", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_edit_profile, null, false);

        EditText etFull   = dialogView.findViewById(R.id.etFullName);
        EditText etUser   = dialogView.findViewById(R.id.etUsername);
        EditText etEmail  = dialogView.findViewById(R.id.etEmail);

        // Pre-fill from current user
        etFull.setText(currentUser.getDisplayName());
        etUser.setText(currentUser.getUsername());
        etEmail.setText(currentUser.getEmail());

        // Build dialog and make the window background transparent
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.show();

        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());

        dialogView.findViewById(R.id.btnSave).setOnClickListener(v -> {
            String full  = etFull.getText()  != null ? etFull.getText().toString().trim()  : "";
            String user  = etUser.getText()  != null ? etUser.getText().toString().trim()  : "";
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";

            if (full.isEmpty()) { etFull.setError("Full name required"); return; }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Invalid email"); return;
            }

            dialogView.findViewById(R.id.btnSave).setEnabled(false);

            // Firestore merge + Auth displayName + Auth email
            profileService.updateProfile(currentUser.getUid(), full, user, email)
                    .addOnSuccessListener(vv -> {
                        // Update header UI
                        binding.tvUserName.setText(full);
                        binding.tvUserEmail.setText(email);

                        // Keep local cache consistent if your model exposes setters
                        currentUser.setDisplayName(full);
                        currentUser.setUsername(user);
                        // if you have setEmail(): currentUser.setEmail(email);

                        Snackbar.make(binding.getRoot(), "Profile updated", Snackbar.LENGTH_LONG).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        // Case 1: needs re-auth → show password dialog
                        if (ProfileService.isRecentLoginRequired(e)) {
                            dialogView.findViewById(R.id.btnSave).setEnabled(true);
                            dialog.dismiss();
                            showReauthDialogAndRetry(email, full, user);
                            return;
                        }

                        // Case 2: best-effort — if Auth email already matches, treat as success
                        com.google.firebase.auth.FirebaseUser fu =
                                com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
                        boolean authEmailMatches = fu != null && fu.getEmail() != null
                                && fu.getEmail().equalsIgnoreCase(email);

                        if (authEmailMatches) {
                            // Retry Firestore email field quietly so UI/DB stay in sync
                            java.util.Map<String, Object> up = new java.util.HashMap<>();
                            up.put("email", email);
                            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                    .collection("users").document(currentUser.getUid())
                                    .set(up, com.google.firebase.firestore.SetOptions.merge());

                            // Update UI & local cache, then close dialog
                            binding.tvUserName.setText(full);
                            binding.tvUserEmail.setText(email);
                            currentUser.setDisplayName(full);
                            currentUser.setUsername(user);

                            Snackbar.make(binding.getRoot(), "Profile updated", Snackbar.LENGTH_LONG).show();
                            dialog.dismiss();
                        } else {
                            dialogView.findViewById(R.id.btnSave).setEnabled(true);
                            Snackbar.make(binding.getRoot(),
                                    (e.getMessage() != null ? e.getMessage() : "Update failed"),
                                    Snackbar.LENGTH_LONG).show();
                        }
                    });
        });
    }

    private void showReauthDialogAndRetry(String newEmail, String fullName, String username) {
        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_reauth_password, null, false);
        EditText etPassword = v.findViewById(R.id.etPassword);

        AlertDialog d = new AlertDialog.Builder(requireContext())
                .setView(v)
                .create();
        d.show();

        v.findViewById(R.id.btnCancel).setOnClickListener(x -> d.dismiss());
        v.findViewById(R.id.btnConfirm).setOnClickListener(x -> {
            String pass = etPassword.getText() != null ? etPassword.getText().toString() : "";
            if (pass.isEmpty()) { etPassword.setError("Required"); return; }

            FirebaseAuth auth = FirebaseAuth.getInstance();
            FirebaseUser user = auth.getCurrentUser();
            if (user == null || user.getEmail() == null) {
                Snackbar.make(binding.getRoot(), "Not signed in", Snackbar.LENGTH_LONG).show();
                d.dismiss();
                return;
            }

            AuthCredential cred = EmailAuthProvider.getCredential(user.getEmail(), pass);
            user.reauthenticate(cred).addOnSuccessListener(r -> {
                // Retry only the Auth email change
                profileService.updateAuthEmailOnly(newEmail)
                        .addOnSuccessListener(vv -> {
                            // Keep Firestore profile's email in sync
                            Map<String, Object> up = new HashMap<>();
                            up.put("email", newEmail);
                            FirebaseFirestore.getInstance()
                                    .collection("users").document(user.getUid())
                                    .set(up, SetOptions.merge())
                                    .addOnSuccessListener(v3 -> {
                                        binding.tvUserEmail.setText(newEmail);

                                        // Ensure name/username persisted too (safe even if email already changed)
                                        profileService.updateProfile(user.getUid(), fullName, username, newEmail);

                                        Snackbar.make(binding.getRoot(), "Email updated", Snackbar.LENGTH_LONG).show();
                                        d.dismiss();
                                    })
                                    .addOnFailureListener(err -> {
                                        Snackbar.make(binding.getRoot(), err.getMessage(), Snackbar.LENGTH_LONG).show();
                                    });
                        })
                        .addOnFailureListener(err -> {
                            Snackbar.make(binding.getRoot(), err.getMessage(), Snackbar.LENGTH_LONG).show();
                        });
            }).addOnFailureListener(err -> {
                Snackbar.make(binding.getRoot(), err.getMessage(), Snackbar.LENGTH_LONG).show();
            });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
