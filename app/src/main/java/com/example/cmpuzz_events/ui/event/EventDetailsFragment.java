package com.example.cmpuzz_events.ui.event;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.cmpuzz_events.Entrant;
import com.example.cmpuzz_events.R;
import com.example.cmpuzz_events.auth.AuthManager;
import com.example.cmpuzz_events.models.user.User;
import com.example.cmpuzz_events.models.event.EventEntity;
import com.example.cmpuzz_events.models.event.Invitation;
import com.example.cmpuzz_events.service.EventService;
import com.example.cmpuzz_events.service.IEventService;
import com.google.android.gms.location.FusedLocationProviderClient;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import android.net.Uri;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.example.cmpuzz_events.service.IImageService;
import com.example.cmpuzz_events.service.ImageService;
import com.example.cmpuzz_events.ui.admin.ImageItem;
import com.google.android.material.imageview.ShapeableImageView;
import com.bumptech.glide.Glide;


import java.util.ArrayList;
import java.util.List;

/**
 * Displays the detailed information for a single event.
 * It handles different UI states based on user roles (organizer vs. regular user) and event status.
 */
public class EventDetailsFragment extends Fragment {

    private static final String TAG = "EventDetailsFragment";
    private static final String ARG_EVENT_ID = "eventId";

    private String eventId;

    private IEventService eventService;

    // Store the UI Event
    private Event currentEvent;
    
    // Views
    private TextView eventTitle;
    private TextView eventHost;
    private TextView eventAvailability;
    private TextView eventPrice;
    private TextView datePosted;
    private TextView descriptionText;
    private MaterialButton editButton;
    private MaterialButton shareButton;
    private MaterialButton viewMapButton;
    private MaterialButton joinButton;
    private MaterialButton deleteButton;
    private MaterialButton viewAllEntrantsButton;
    private TextView additionalActionsButton;
    private View dividerTop;
    private View dividerBottom;
    private TextView usersEnrolledTitle;
    private RecyclerView usersRecyclerView;
    private EnrolledUsersAdapter usersAdapter;
    private TextView tvWaitlistCount;

    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private Uri selectedImageUri;
    private ProgressDialog uploadProgressDialog;
    private ShapeableImageView eventImage;


    /**
     * Factory method to create a new instance of this fragment using the provided event ID.
     */
    public static EventDetailsFragment newInstance(String eventId) {
        EventDetailsFragment fragment = new EventDetailsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        fragment.setArguments(args);
        return fragment;
    }
    /**
     * Called when the fragment is first created. Initializes services and retrieves the event ID from arguments.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            eventId = getArguments().getString(ARG_EVENT_ID);
        }

        eventService = EventService.getInstance();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        
        // Setup image picker for editing event poster
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    uploadNewEventImage();
                }
            }
        );
    }
    /**
     * Inflates the fragment's layout, initializes all views, and triggers the loading of event details.
     * @return The root view for the fragment's UI.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_event_details, container, false);
        
        // Initialize views
        eventTitle = root.findViewById(R.id.event_title);
        eventHost = root.findViewById(R.id.event_host);
        eventImage = root.findViewById(R.id.event_image);
        eventAvailability = root.findViewById(R.id.event_availability);
        eventPrice = root.findViewById(R.id.event_price);
        datePosted = root.findViewById(R.id.date_posted);
        descriptionText = root.findViewById(R.id.description_text);
        editButton = root.findViewById(R.id.edit_button);
        shareButton = root.findViewById(R.id.share_button);
        viewMapButton = root.findViewById(R.id.view_map_button);
        joinButton = root.findViewById(R.id.join_button);
        deleteButton = root.findViewById(R.id.delete_button);
        viewAllEntrantsButton = root.findViewById(R.id.view_all_entrants_button);
        additionalActionsButton = root.findViewById(R.id.additional_actions_button);
        dividerTop = root.findViewById(R.id.divider_top);
        dividerBottom = root.findViewById(R.id.divider_bottom);
        usersEnrolledTitle = root.findViewById(R.id.users_enrolled_title);
        usersRecyclerView = root.findViewById(R.id.users_recycler_view);
        tvWaitlistCount = root.findViewById(R.id.tvWaitlistCount);
        
        // Setup RecyclerView
        usersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        usersAdapter = new EnrolledUsersAdapter(new ArrayList<>());
        usersRecyclerView.setAdapter(usersAdapter);
        
        // Setup UI based on user role
        setupRoleBasedUI(root);
        
        loadEventDetails();
        
        return root;
    }
    /**
     * Configures the visibility and functionality of UI elements based on the current user's role.
     */
    private void setupRoleBasedUI(View root) {
        User currentUser = AuthManager.getInstance().getCurrentUser();
        assert currentUser != null;
        
        if (currentUser.canManageEvents()) {
            // Organizer view - show all management controls
            editButton.setVisibility(View.VISIBLE);
            deleteButton.setVisibility(View.VISIBLE);
            additionalActionsButton.setVisibility(View.VISIBLE);
            dividerTop.setVisibility(View.VISIBLE);
            dividerBottom.setVisibility(View.VISIBLE);
            joinButton.setVisibility(View.GONE);
            viewAllEntrantsButton.setVisibility(View.GONE);
            shareButton.setVisibility(View.VISIBLE);
            viewMapButton.setVisibility(View.VISIBLE);
            usersEnrolledTitle.setVisibility(View.VISIBLE);
            usersRecyclerView.setVisibility(View.VISIBLE);
            
            // Update constraint for organizers: users_enrolled_title below additional_actions_button
            ConstraintLayout constraintLayout = (ConstraintLayout) usersEnrolledTitle.getParent();
            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(constraintLayout);
            constraintSet.connect(R.id.users_enrolled_title, ConstraintSet.TOP, 
                                R.id.additional_actions_button, ConstraintSet.BOTTOM, 
                                (int) (24 * getResources().getDisplayMetrics().density));
            constraintSet.applyTo(constraintLayout);
            
            // Navigate to Action Menu, and pass the event object
            additionalActionsButton.setOnClickListener(v -> {
                if (currentEvent != null) {
                    Bundle bundle = new Bundle();
                    bundle.putSerializable("event", currentEvent);
                    Navigation.findNavController(root).navigate(
                        R.id.action_to_event_action_menu,
                        bundle
                    );
                }
            });
            
        } else if (currentUser.isUser()) {
            // User view - show Join button, essential info, and entrants list
            editButton.setVisibility(View.GONE);
            deleteButton.setVisibility(View.GONE);
            shareButton.setVisibility(View.GONE);
            viewMapButton.setVisibility(View.GONE);
            additionalActionsButton.setVisibility(View.GONE);
            dividerTop.setVisibility(View.GONE);
            dividerBottom.setVisibility(View.GONE);
            usersEnrolledTitle.setVisibility(View.VISIBLE);
            usersRecyclerView.setVisibility(View.VISIBLE);
            joinButton.setVisibility(View.VISIBLE);
            viewAllEntrantsButton.setVisibility(View.GONE);
            
            // Join event functionality
            joinButton.setOnClickListener(v -> joinEvent());
        }
    }
    /**
     * Activity result launcher for handling the location permission request.
     */
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    fetchLocationAndJoin();
                } else {
                    Toast.makeText(getContext(), "Location required to join this event", Toast.LENGTH_SHORT).show();
                }
            });
    /**
     * Initiates the process for a user to join an event, checking for location requirements.
     */
    private void joinEvent() {
        User currentUser = AuthManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Please log in to join events", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentEvent.isGeolocationRequired()) {
            checkPermissionAndJoin();
        } else {

            // Use user ID to join waitlist
            String userId = currentUser.getUid();

            eventService.joinEvent(eventId, userId, new IEventService.VoidCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(getContext(), "Successfully joined event!", Toast.LENGTH_SHORT).show();
                    // Reload event details to update button state
                    loadEventDetails();
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(getContext(), "Failed to join: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    /**
     * Checks if location permission has been granted; if not, it requests the permission.
     */
    private void checkPermissionAndJoin() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fetchLocationAndJoin();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    /**
     * Fetches the user's last known location and then attempts to join the event with that location.
     */
    private void fetchLocationAndJoin() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        String userId = AuthManager.getInstance().getCurrentUser().getUid();

                        // Call the NEW service method
                        eventService.joinEventWithLocation(eventId, userId,
                                location.getLatitude(), location.getLongitude(),
                                new IEventService.VoidCallback() {
                                    @Override
                                    public void onSuccess() {
                                        Toast.makeText(getContext(), "Joined with location!", Toast.LENGTH_SHORT).show();
                                        loadEventDetails();
                                    }
                                    @Override
                                    public void onError(String error) {
                                        Toast.makeText(getContext(), "Failed: " + error, Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(getContext(), "Could not determine location. Try opening Maps first.", Toast.LENGTH_LONG).show();
                    }
                });
    }
    /**
     * Allows the current user to leave the event's waitlist.
     */
    private void leaveEvent() {
        User currentUser = AuthManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Please log in", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String userId = currentUser.getUid();
        
        eventService.removeFromWaitlist(eventId, userId, new IEventService.VoidCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(getContext(), "Left the waitlist", Toast.LENGTH_SHORT).show();
                // Reload event details to update button state
                loadEventDetails();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Failed to leave: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
    /**
     * Fetches all details for the specified event from the service and updates the UI.
     */
    private void loadEventDetails() {
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(getContext(), "Invalid event", Toast.LENGTH_SHORT).show();
            return;
        }

        eventService.getEvent(eventId, new IEventService.EventCallback() {
            @Override
            public void onSuccess(EventEntity eventEntity) {
                List<String> waitlistIds = eventEntity.getWaitlist();
                // Convert to UI Event and store
                currentEvent = new Event(
                    eventEntity.getEventId(),
                    eventEntity.getTitle(),
                    eventEntity.getDescription(),
                    eventEntity.getCapacity(),
                    eventEntity.getRegistrationStart(),
                    eventEntity.getRegistrationEnd(),
                    eventEntity.getOrganizerId(),
                    eventEntity.getOrganizerName(),
                    eventEntity.isGeolocationRequired(),
                        waitlistIds
                );
                currentEvent.setMaxEntrants(eventEntity.getMaxEntrants());
                currentEvent.setPosterUrl(eventEntity.getPosterUrl());
                currentEvent.setPrice(eventEntity.getPrice());
                
                displayEventDetails(eventEntity);
                
                // Load enrolled users for both organizers and regular users
                User currentUser = AuthManager.getInstance().getCurrentUser();
                if (currentUser != null) {
                    loadEnrolledUsers(eventEntity.getWaitlist());
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading event: " + error);
                Toast.makeText(getContext(), "Failed to load event details", Toast.LENGTH_SHORT).show();
            }
        });
    }
    /**
     * Fetches user profiles for all IDs associated with an event (waitlist, invited, attending) and displays them.
     */
    private void loadEnrolledUsers(List<String> waitlist) {
        // Combine all user IDs: waitlist + invited + attendees
        List<String> allUserIds = new ArrayList<>();
        
        // Add waitlist users
        if (waitlist != null) {
            allUserIds.addAll(waitlist);
        }
        
        // Add invited users and attendees
        EventService.getInstance().getEvent(eventId, new IEventService.EventCallback() {
            @Override
            public void onSuccess(EventEntity event) {
                // Add users with pending invitations
                if (event.getInvitations() != null) {
                    for (Invitation invitation : event.getInvitations()) {
                        String userId = invitation.getUserId();
                        if (userId != null && !userId.isEmpty() && !allUserIds.contains(userId)) {
                            allUserIds.add(userId);
                        }
                    }
                }
                
                // Add attendees (users who accepted invitations)
                if (event.getAttendees() != null) {
                    for (String userId : event.getAttendees()) {
                        if (userId != null && !userId.isEmpty() && !allUserIds.contains(userId)) {
                            allUserIds.add(userId);
                        }
                    }
                }
                
                // Now load all users
                if (allUserIds.isEmpty()) {
                    usersAdapter.updateUsers(new ArrayList<>());
                    usersRecyclerView.setVisibility(View.GONE);
                    return;
                }
                
                AuthManager.getInstance().getUsersByIds(allUserIds, new AuthManager.UsersCallback() {
                    @Override
                    public void onSuccess(List<User> users) {
                        Log.d(TAG, "Loaded " + users.size() + " total entrants");
                        usersAdapter.updateUsers(users);
                        usersRecyclerView.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Error loading users: " + error);
                        usersAdapter.updateUsers(new ArrayList<>());
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading event for entrants: " + error);
            }
        });
    }
    /**
     * Populates the UI fields with data from the event object. This method must be called on the UI thread.
     */
    private void displayEventDetails(EventEntity event) {
        if (getActivity() == null) return;
        
        getActivity().runOnUiThread(() -> {
            eventTitle.setText(event.getTitle());
            // Display organizer name if available, otherwise fall back to ID
            String hostText = event.getOrganizerName() != null && !event.getOrganizerName().isEmpty()
                    ? "Hosted by: " + event.getOrganizerName()
                    : "Hosted by: " + event.getOrganizerId();
            eventHost.setText(hostText);
            descriptionText.setText(event.getDescription());

            // load poster image
            String posterUrl = event.getPosterUrl();
            if (posterUrl != null && !posterUrl.isEmpty()) {
                Glide.with(requireContext())
                        .load(posterUrl)
                        .skipMemoryCache(true)  // Don't use cached version
                        .diskCacheStrategy(DiskCacheStrategy.NONE)  // Don't use disk cache
                        .placeholder(R.drawable.bg_image_placeholder)
                        .error(R.drawable.bg_image_placeholder)
                        .into(eventImage);
            } else {
                // Clear any previous image and show placeholder
                Glide.with(requireContext()).clear(eventImage);
                eventImage.setImageResource(R.drawable.bg_image_placeholder);
            }

            // same as in eventviewholder
            List<String> waitlist = event.getWaitlist();
            int waitlistCount = (waitlist != null) ? waitlist.size() : 0;

            if (waitlistCount >= 0) {
                // If the waitlist has people, show the count
                tvWaitlistCount.setVisibility(View.VISIBLE);
                tvWaitlistCount.setText(getString(R.string.waitlist_count_format, waitlistCount));
            } else {
                // Otherwise, make sure the view is hidden
                tvWaitlistCount.setVisibility(View.GONE);
            }

            // Format dates
            if (event.getRegistrationStart() != null) {
                datePosted.setText("Registration Start: " + event.getRegistrationStart());
            }
            
            // Display availability
            int capacity = event.getCapacity();
            eventAvailability.setText("Capacity: " + capacity);
            
            // Display price separately
            Double price = event.getPrice();
            if (price != null) {
                eventPrice.setText("Price: $" + String.format("%.2f", price));
            } else {
                eventPrice.setText("Price: Free");
            }
            eventPrice.setVisibility(View.VISIBLE);
            
            // Check user's status and update Join button accordingly
            User currentUser = AuthManager.getInstance().getCurrentUser();
            if (currentUser != null) {
                String userId = currentUser.getUid();
                boolean isInWaitlist = event.getWaitlist() != null && event.getWaitlist().contains(userId);
                
                // Check if user has declined
                boolean hasDeclined = event.getDeclined() != null && event.getDeclined().contains(userId);
                
                // Check if user is an attendee
                boolean isAttending = event.getAttendees() != null && event.getAttendees().contains(userId);
                
                // Check if user has a pending invitation
                boolean hasInvitation = false;
                if (event.getInvitations() != null) {
                    for (Invitation inv : event.getInvitations()) {
                        if (inv.getUserId() != null && inv.getUserId().equals(userId)) {
                            hasInvitation = true;
                            break;
                        }
                    }
                }
                
                // Check if registration has started
                boolean registrationStarted = true;
                if (event.getRegistrationStart() != null) {
                    registrationStarted = new java.util.Date().getTime() >= event.getRegistrationStart().getTime();
                }
                
                // Check if registration has ended
                boolean registrationEnded = false;
                if (event.getRegistrationEnd() != null) {
                    registrationEnded = new java.util.Date().getTime() > event.getRegistrationEnd().getTime();
                }
                
                // Configure button based on status
                if (!registrationStarted) {
                    joinButton.setText("Registration Not Started");
                    joinButton.setEnabled(false);
                    joinButton.setAlpha(0.6f);
                } else if (registrationEnded) {
                    joinButton.setText("Registration Ended");
                    joinButton.setEnabled(false);
                    joinButton.setAlpha(0.6f);
                } else if (hasDeclined) {
                    joinButton.setText("Declined");
                    joinButton.setEnabled(false);
                    joinButton.setAlpha(0.6f);
                } else if (isAttending) {
                    joinButton.setText("Attending");
                    joinButton.setEnabled(false);
                    joinButton.setAlpha(0.6f);
                } else if (hasInvitation) {
                    joinButton.setText("Already Invited");
                    joinButton.setEnabled(false);
                    joinButton.setAlpha(0.6f);
                } else if (isInWaitlist) {
                    joinButton.setText("Leave Waitlist");
                    joinButton.setEnabled(true);
                    joinButton.setAlpha(1.0f);
                    joinButton.setOnClickListener(v -> leaveEvent());
                } else {
                    joinButton.setText("Join Waitlist");
                    joinButton.setEnabled(true);
                    joinButton.setAlpha(1.0f);
                    joinButton.setOnClickListener(v -> joinEvent());
                }
            }
            
            // Setup edit button to change event poster
            editButton.setOnClickListener(v -> showEditOptionsDialog());

            deleteButton.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Confirm")
                    .setMessage("Are you sure you want to delete this Event? This action can not be undone.")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String eventId = event.getEventId();
                            eventService.deleteEvent(eventId, new IEventService.VoidCallback() {

                                @Override
                                public void onSuccess() {
                                    Log.d("Event Deletion", "Event Deleted Successful");
                                    NavHostFragment.findNavController(EventDetailsFragment.this)
                                            .popBackStack();
                                }

                                @Override
                                public void onError(String error) {
                                    Log.d("Event Deletion Error", error);

                                }
                            });
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
            });

            
            shareButton.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Share functionality coming soon", Toast.LENGTH_SHORT).show();
            });

            viewMapButton.setOnClickListener(v -> {
                if (currentEvent == null) return;

                // Check if there are any locations to show
                // (Optional: prevents opening empty map, though Fragment handles empty state too)

                Bundle bundle = new Bundle();
                bundle.putString("eventId", currentEvent.getEventId());

                // Ensure you have added this action/destination to your nav_graph.xml
                // OR use ID navigation if you added the fragment to the graph
                Navigation.findNavController(v).navigate(R.id.action_event_details_to_event_map, bundle);
            });
        });
    }
    
    private void showEditOptionsDialog() {
        User currentUser = AuthManager.getInstance().getCurrentUser();
        
        // Debug logging.. FML
        if (currentUser != null) {
            Log.d(TAG, "Current user role: " + currentUser.getRole());
            Log.d(TAG, "Current user isAdmin(): " + currentUser.isAdmin());
        } else {
            Log.d(TAG, "Current user is NULL");
        }
        
        if (currentEvent != null) {
            Log.d(TAG, "Current event posterUrl: " + currentEvent.getPosterUrl());
        } else {
            Log.d(TAG, "Current event is NULL");
        }
        
        // Check if user is admin
        boolean isAdmin = currentUser != null && currentUser.isAdmin();
        
        Log.d(TAG, "Edit dialog - isAdmin: " + isAdmin);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
            .setTitle("Edit Event")
            .setMessage("What would you like to do?");
        
        // Add delete option for admins (they can delete any event's poster)
        if (isAdmin) {
            builder.setNeutralButton("Delete Poster", (dialog, which) -> {
                confirmDeletePosterImage();
            });
        }
        
        builder.setPositiveButton("Change Poster", (dialog, which) -> {
                imagePickerLauncher.launch("image/*");
            })
            .setNegativeButton("Cancel", null);
        
        builder.show();
    }
    
    private void confirmDeletePosterImage() {
        new AlertDialog.Builder(requireContext())
            .setTitle("Delete Poster Image")
            .setMessage("Are you sure you want to delete this event's poster image?")
            .setPositiveButton("Delete", (dialog, which) -> {
                deletePosterImage();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void deletePosterImage() {
        if (currentEvent == null || currentEvent.getPosterUrl() == null) {
            Toast.makeText(getContext(), "No poster image to delete", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show non-cancelable progress dialog
        uploadProgressDialog = new ProgressDialog(requireContext());
        uploadProgressDialog.setTitle("Deleting Image");
        uploadProgressDialog.setMessage("Please wait...");
        uploadProgressDialog.setCancelable(false);
        uploadProgressDialog.show();
        
        // Create ImageItem from the current event's poster
        String posterUrl = currentEvent.getPosterUrl();
        String eventId = currentEvent.getEventId();
        
        // Get storage reference for the image
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference posterRef = storage.getReferenceFromUrl(posterUrl);
        
        ImageItem imageItem = new ImageItem();
        imageItem.setUrl(posterUrl);
        imageItem.setName(eventId + ".jpg");
        imageItem.setReference(posterRef);
        
        // Use ImageService to delete the image and update events
        ImageService imageService = ImageService.getInstance();
        imageService.deleteImage(imageItem, new IImageService.VoidCallback() {
            @Override
            public void onSuccess() {
                if (uploadProgressDialog != null && uploadProgressDialog.isShowing()) {
                    uploadProgressDialog.dismiss();
                }
                Toast.makeText(getContext(), "Poster image deleted successfully", Toast.LENGTH_SHORT).show();
                // Clear Glide cache for this image to prevent showing cached version
                if (getContext() != null) {
                    com.bumptech.glide.Glide.get(getContext()).clearMemory();
                    // Also clear disk cache in background
                    new Thread(() -> {
                        com.bumptech.glide.Glide.get(getContext()).clearDiskCache();
                    }).start();
                }
                // Reload event details to show updated state
                loadEventDetails();
            }
            
            @Override
            public void onError(String error) {
                if (uploadProgressDialog != null && uploadProgressDialog.isShowing()) {
                    uploadProgressDialog.dismiss();
                }
                Log.e(TAG, "Error deleting poster: " + error);
                Toast.makeText(getContext(), "Error deleting poster: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void uploadNewEventImage() {
        if (selectedImageUri == null || currentEvent == null) {
            return;
        }
        
        // Show non-cancelable progress dialog
        uploadProgressDialog = new ProgressDialog(requireContext());
        uploadProgressDialog.setTitle("Uploading Image");
        uploadProgressDialog.setMessage("Please wait...");
        uploadProgressDialog.setCancelable(false);
        uploadProgressDialog.show();
        
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        String filename = currentEvent.getEventId() + ".jpg";
        StorageReference posterRef = storageRef.child("event_posters/" + filename);
        
        posterRef.putFile(selectedImageUri)
            .addOnSuccessListener(taskSnapshot -> {
                // Get the download URL
                posterRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    // Update the event with the new poster URL
                    eventService.updateEventPoster(currentEvent.getEventId(), uri.toString(), new IEventService.VoidCallback() {
                        @Override
                        public void onSuccess() {
                            if (uploadProgressDialog != null && uploadProgressDialog.isShowing()) {
                                uploadProgressDialog.dismiss();
                            }
                            Toast.makeText(getContext(), "Poster updated successfully", Toast.LENGTH_SHORT).show();
                            // Reload event details to show new image
                            loadEventDetails();
                        }
                        
                        @Override
                        public void onError(String error) {
                            if (uploadProgressDialog != null && uploadProgressDialog.isShowing()) {
                                uploadProgressDialog.dismiss();
                            }
                            Log.e(TAG, "Error updating poster URL: " + error);
                            Toast.makeText(getContext(), "Error updating poster", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
            })
            .addOnFailureListener(e -> {
                if (uploadProgressDialog != null && uploadProgressDialog.isShowing()) {
                    uploadProgressDialog.dismiss();
                }
                Log.e(TAG, "Error uploading image: " + e.getMessage());
                Toast.makeText(getContext(), "Error uploading image", Toast.LENGTH_SHORT).show();
            });
    }
}
