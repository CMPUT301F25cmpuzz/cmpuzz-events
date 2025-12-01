package com.example.cmpuzz_events.ui.profile;

import android.content.Context;
import android.content.DialogInterface;
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
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.cmpuzz_events.R;
import com.example.cmpuzz_events.auth.AuthManager;
import com.example.cmpuzz_events.databinding.FragmentProfileBinding;
import com.example.cmpuzz_events.models.event.EventEntity;
import com.example.cmpuzz_events.models.event.Invitation;
import com.example.cmpuzz_events.models.user.User;
import com.example.cmpuzz_events.service.AdminService;
import com.example.cmpuzz_events.service.EventService;
import com.example.cmpuzz_events.service.IEventService;
import com.example.cmpuzz_events.service.INotificationService;
import com.example.cmpuzz_events.service.NotificationService;
import com.example.cmpuzz_events.ui.event.Event;
import com.example.cmpuzz_events.ui.event.EventDetailsFragment;
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

import android.net.Uri;
import android.app.ProgressDialog;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

/**
 * A fragment that displays the current user's profile information.
 * This includes their name, email, role, and a list of events they are enrolled in.
 * It also provides functionality for editing the profile, logging out, and navigating to settings.
 */
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private EventService eventService;
    private AdminService adminService;
    private NotificationService notificationService;
    private EnrolledEventsAdapter adapter;
    private static final String TAG = "ProfileFragment";

    private ProfileService profileService;
    private User currentUser;
    private User passedUser;
    private SharedPreferences preferences;
    private Uri selectedImageUri;
    private ProgressDialog uploadProgressDialog;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private android.widget.ImageView currentDialogProfileImageView;

    /**
     * Called to have the fragment instantiate its user interface view.
     * This is where the layout is inflated, views are initialized, and listeners are set up.
     *
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentProfileBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        eventService = EventService.getInstance();
        adminService = AdminService.getInstance();
        notificationService = NotificationService.getInstance();
        notificationService.setContext(requireContext().getApplicationContext());
        preferences = requireContext().getSharedPreferences("user_preferences", Context.MODE_PRIVATE);

        // Setup image picker launcher (must be registered during fragment initialization)
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    // Update the image view immediately if we have a reference
                    if (currentDialogProfileImageView != null && getContext() != null) {
                        Glide.with(getContext())
                                .load(uri)
                                .transform(new CircleCrop())
                                .placeholder(R.drawable.ic_profile)
                                .into(currentDialogProfileImageView);
                    }
                }
            }
        );

        if(getArguments() != null)
        {
            passedUser = (User) getArguments().getSerializable("user");
            Log.d(TAG, "Args Passed, user: " + passedUser.getDisplayName());
        }
        currentUser = AuthManager.getInstance().getCurrentUser();
        binding.tvEventsEnrolledTitle.setVisibility(View.GONE);

        if(passedUser != null) // If profile was triggered by passing a user (i.e. from an adapter click)
        {
            binding.tvUserName.setText(passedUser.getDisplayName());
            binding.tvUserEmail.setText(passedUser.getEmail());
            binding.tvUserRole.setText("Role: " + passedUser.getRole().getRoleName());
            
            // Load profile image
            if (passedUser.getProfileImageUrl() != null && !passedUser.getProfileImageUrl().isEmpty()) {
                Glide.with(requireContext())
                        .load(passedUser.getProfileImageUrl())
                        .transform(new CircleCrop())
                        .placeholder(R.drawable.bg_image_placeholder)
                        .into(binding.imgProfileAvatar);
            } else {
                binding.imgProfileAvatar.setImageResource(R.drawable.ic_profile);
            }
            
            binding.btnSettings.setVisibility(View.GONE);
            binding.btnLotteryGuidelines.setVisibility(View.GONE);
            binding.btnLogout.setVisibility(View.GONE);
            // Bro
            if(currentUser.isAdmin())
            {
                binding.btnEditProfile.setVisibility(View.VISIBLE);
                binding.btnDeleteAccount.setVisibility(View.VISIBLE);
            } else {
                binding.btnEditProfile.setVisibility(View.GONE);
                binding.btnDeleteAccount.setVisibility(View.GONE);
            }

            // Setup enrolled events RecyclerView
            // setupEnrolledEvents(root, passedUser);
        } else if (currentUser != null) { // If profile was triggered by the user themselves
            binding.tvUserName.setText(currentUser.getDisplayName());
            binding.tvUserEmail.setText(currentUser.getEmail());
            binding.tvUserRole.setText("Role: " + currentUser.getRole().getRoleName());
            
            // Load profile image
            if (currentUser.getProfileImageUrl() != null && !currentUser.getProfileImageUrl().isEmpty()) {
                Glide.with(requireContext())
                        .load(currentUser.getProfileImageUrl())
                        .transform(new CircleCrop())
                        .placeholder(R.drawable.bg_image_placeholder)
                        .into(binding.imgProfileAvatar);
            } else {
                binding.imgProfileAvatar.setImageResource(R.drawable.ic_profile);
            }
            
            binding.btnEditProfile.setVisibility(View.VISIBLE);

            binding.btnLogout.setVisibility(View.VISIBLE);
            binding.btnDeleteAccount.setVisibility(View.GONE);

            if(currentUser.isUser())
            {
                binding.btnSettings.setVisibility(View.VISIBLE);
                binding.btnLotteryGuidelines.setVisibility(View.VISIBLE);
                setupEnrolledEvents(root, currentUser);
            } else if(currentUser.canManageEvents())
            {
                binding.btnSettings.setVisibility(View.GONE);
                binding.btnLotteryGuidelines.setVisibility(View.GONE);
            }
        }

        binding.btnEditProfile.setOnClickListener(v -> showEditDialog());

        // Setup settings button
        binding.btnSettings.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(v);
            navController.navigate(R.id.action_profile_to_settings);
        });

        // Guidelines button
        binding.btnLotteryGuidelines.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(v);
            navController.navigate(R.id.action_profile_to_guidelines);
        });

        profileService = new ProfileService();
        if (currentUser == null) {
            currentUser = AuthManager.getInstance().getCurrentUser();
        }

        // Setup logout button
        binding.btnLogout.setOnClickListener(v -> logout());

        binding.btnDeleteAccount.setOnClickListener(v->deleteAccount());

        return root;
    }

    /**
     * Initializes the RecyclerView for displaying the events the user is enrolled in.
     * Sets up the adapter and defines actions for event interactions.
     *
     * @param root The root view of the fragment, used for navigation.
     * @param currentUser The currently logged-in user.
     */
    private void setupEnrolledEvents(View root, User currentUser) {
        binding.recyclerViewEnrolledEvents.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.tvEventsEnrolledTitle.setVisibility(View.VISIBLE);
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

    /**
     * Fetches and displays the list of events associated with the user from the event service.
     * This includes events they are attending, invited to, or on the waitlist for.
     *
     * @param userId The unique ID of the user whose events are to be loaded.
     */
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

    /**
     * Converts an {@link EventEntity} data model to an {@link Event} UI model.
     *
     * @param entity The {@link EventEntity} to convert.
     * @return The resulting {@link Event} object for UI display.
     */
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

    /**
     * Determines the user's status for a given event (e.g., attending, invited, waitlist).
     *
     * @param entity The event entity containing attendee, invitation, and waitlist data.
     * @param userId The ID of the user to check the status for.
     * @return A string representing the user's status.
     */
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

    /**
     * Handles the action of a user leaving the waitlist for an event.
     * It calls the event service to update the waitlist and reloads the enrolled events on success.
     *
     * @param event The event from which the user is leaving the waitlist.
     * @param currentUser The current user performing the action.
     */
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

    /**
     * Handles the action of a user accepting an invitation to an event.
     * Notifies the organizer of the response and reloads the event list.
     *
     * @param event The event for which the invitation is being accepted.
     * @param currentUser The current user accepting the invitation.
     */
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

    /**
     * Handles the action of a user declining an invitation to an event.
     * Notifies the organizer of the response and reloads the event list.
     *
     * @param event The event for which the invitation is being declined.
     * @param currentUser The current user declining the invitation.
     */
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

    /**
     * Signs the current user out, clears their session, and navigates to the login screen.
     */
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


    /**
     * Displays a dialog for editing the user's profile information such as name, username, and email.
     * It handles input validation and calls the profile service to apply the changes.
     */
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
        android.widget.ImageView imgProfilePicture = dialogView.findViewById(R.id.imgProfilePicture);
        com.google.android.material.button.MaterialButton btnUploadProfileImage = dialogView.findViewById(R.id.btnUploadProfileImage);

        // Pre-fill from current user
        etFull.setText(currentUser.getDisplayName());
        etUser.setText(currentUser.getUsername());
        etEmail.setText(currentUser.getEmail());

        // Load and display profile image using Glide
        if (currentUser.getProfileImageUrl() != null && !currentUser.getProfileImageUrl().isEmpty()) {
            Glide.with(requireContext())
                    .load(currentUser.getProfileImageUrl())
                    .transform(new CircleCrop())
                    .placeholder(R.drawable.bg_image_placeholder)
                    .into(imgProfilePicture);
        } else {
            imgProfilePicture.setImageResource(R.drawable.ic_profile);
        }

        // Reset selected image URI when dialog opens
        selectedImageUri = null;
        
        // Store reference to this dialog's ImageView so the launcher callback can update it
        currentDialogProfileImageView = imgProfilePicture;
        
        // Setup upload button click listener
        btnUploadProfileImage.setOnClickListener(v -> {
            imagePickerLauncher.launch("image/*");
        });

        // Build dialog and make the window background transparent
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.show();

        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> {
            currentDialogProfileImageView = null; // Clear reference when dialog is dismissed
            dialog.dismiss();
        });

        dialogView.findViewById(R.id.btnSave).setOnClickListener(v -> {
            String full  = etFull.getText()  != null ? etFull.getText().toString().trim()  : "";
            String user  = etUser.getText()  != null ? etUser.getText().toString().trim()  : "";
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";

            if (full.isEmpty()) { etFull.setError("Full name required"); return; }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Invalid email"); return;
            }

            dialogView.findViewById(R.id.btnSave).setEnabled(false);

            // If an image was selected, upload it first
            if (selectedImageUri != null) {
                uploadProfileImage(currentUser.getUid(), selectedImageUri, dialogView, dialog, full, user, email);
            } else {
                // No image selected, just update profile info
                updateProfileInfo(dialogView, dialog, full, user, email, null);
            }
        });
    }

    /**
     * Uploads the selected profile image to Firebase Storage and then updates the profile.
     */
    private void uploadProfileImage(String uid, Uri imageUri, View dialogView, AlertDialog dialog, 
                                    String fullName, String username, String email) {
        // Show progress dialog
        uploadProgressDialog = new ProgressDialog(requireContext());
        uploadProgressDialog.setTitle("Uploading Image");
        uploadProgressDialog.setMessage("Please wait...");
        uploadProgressDialog.setCancelable(false);
        uploadProgressDialog.show();

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        String filename = uid + ".jpg";
        StorageReference profileImageRef = storageRef.child("profile_images/" + filename);

        profileImageRef.putFile(imageUri)
            .addOnSuccessListener(taskSnapshot -> {
                // Get the download URL
                profileImageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    if (uploadProgressDialog != null && uploadProgressDialog.isShowing()) {
                        uploadProgressDialog.dismiss();
                    }
                    // Update profile with image URL
                    updateProfileInfo(dialogView, dialog, fullName, username, email, uri.toString());
                });
            })
            .addOnFailureListener(e -> {
                if (uploadProgressDialog != null && uploadProgressDialog.isShowing()) {
                    uploadProgressDialog.dismiss();
                }
                Log.e(TAG, "Error uploading image: " + e.getMessage());
                Snackbar.make(binding.getRoot(), "Error uploading image", Snackbar.LENGTH_LONG).show();
                dialogView.findViewById(R.id.btnSave).setEnabled(true);
            });
    }

    /**
     * Updates the profile information in Firestore.
     */
    private void updateProfileInfo(View dialogView, AlertDialog dialog, String fullName, 
                                   String username, String email, String profileImageUrl) {

        // Firestore merge + Auth displayName + Auth email
        profileService.updateProfile(currentUser.getUid(), fullName, username, email)
                .addOnSuccessListener(vv -> {
                    // If profile image URL was provided, update it
                    if (profileImageUrl != null) {
                        profileService.updateProfileImageUrl(currentUser.getUid(), profileImageUrl)
                                .addOnSuccessListener(v -> {
                                    currentUser.setProfileImageUrl(profileImageUrl);
                                    // Update profile image in UI if visible
                                    if (binding.imgProfileAvatar != null) {
                                        Glide.with(requireContext())
                                                .load(profileImageUrl)
                                                .transform(new CircleCrop())
                                                .placeholder(R.drawable.ic_profile)
                                                .into(binding.imgProfileAvatar);
                                    }
                                    finishProfileUpdate(dialogView, dialog, fullName, email, username);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error updating profile image URL: " + e.getMessage());
                                    // Still update other fields even if image URL update fails
                                    finishProfileUpdate(dialogView, dialog, fullName, email, username);
                                });
                    } else {
                        finishProfileUpdate(dialogView, dialog, fullName, email, username);
                    }
                })
                    .addOnFailureListener(e -> {
                        // Case 1: needs re-auth → show password dialog
                        if (ProfileService.isRecentLoginRequired(e)) {
                            dialogView.findViewById(R.id.btnSave).setEnabled(true);
                            dialog.dismiss();
                            showReauthDialogAndRetry(email, fullName, username);
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
                            binding.tvUserName.setText(fullName);
                            binding.tvUserEmail.setText(email);
                            currentUser.setDisplayName(fullName);
                            currentUser.setUsername(username);

                            Snackbar.make(binding.getRoot(), "Profile updated", Snackbar.LENGTH_LONG).show();
                            dialog.dismiss();
                        } else {
                            dialogView.findViewById(R.id.btnSave).setEnabled(true);
                            Snackbar.make(binding.getRoot(),
                                    (e.getMessage() != null ? e.getMessage() : "Update failed"),
                                    Snackbar.LENGTH_LONG).show();
                        }
                    });
    }

    /**
     * Completes the profile update by updating UI and closing the dialog.
     */
    private void finishProfileUpdate(View dialogView, AlertDialog dialog, String fullName, 
                                    String email, String username) {
        // Update header UI
        binding.tvUserName.setText(fullName);
        binding.tvUserEmail.setText(email);

        // Keep local cache consistent
        currentUser.setDisplayName(fullName);
        currentUser.setUsername(username);

        // Clear the ImageView reference
        currentDialogProfileImageView = null;

        Snackbar.make(binding.getRoot(), "Profile updated", Snackbar.LENGTH_LONG).show();
        dialog.dismiss();
    }

    /**
     * Shows a dialog requesting the user's password for re-authentication.
     * This is required for sensitive operations like changing an email address.
     * After successful re-authentication, it retries the profile update.
     *
     * @param newEmail The new email address to be set.
     * @param fullName The full name to be set.
     * @param username The username to be set.
     */
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

    private void deleteAccount()
    {
        if(passedUser != null)
        {
            new android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Confirm")
                    .setMessage("Are you sure you want to delete this account? This action can not be undone.")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            adminService.deleteAccountByUid(passedUser.getUid())
                                    .addOnSuccessListener(v -> {
                                        Log.d(TAG, "Account " + passedUser.getUsername() + " successfully deleted");
                                        Navigation.findNavController(requireView()).popBackStack();
                                    })
                                    .addOnFailureListener(v -> {
                                        Log.e(TAG, "Error deleting account: " + v.getMessage());
                                    });
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
    }

    /**
     * Called when the view previously created by {@link #onCreateView} has been detached from the fragment.
     * This is where we clean up references to the binding class instance.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
