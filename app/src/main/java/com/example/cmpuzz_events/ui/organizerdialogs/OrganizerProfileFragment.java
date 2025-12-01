package com.example.cmpuzz_events.ui.organizerdialogs;

import androidx.appcompat.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.example.cmpuzz_events.R;
import com.example.cmpuzz_events.auth.AuthManager;
import com.example.cmpuzz_events.models.user.User;
import com.example.cmpuzz_events.service.ProfileService;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class OrganizerProfileFragment extends Fragment {

    private static final String TAG = "OrganizerProfileFragment";
    private User currentUser;
    private ProfileService profileService;
    private Uri selectedImageUri;
    private ProgressDialog uploadProgressDialog;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private ImageView currentDialogProfileImageView;
    private ImageView profileAvatarImageView;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View root = inflater.inflate(R.layout.fragment_organizer_profile, container, false);
        
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
        
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        currentUser = AuthManager.getInstance().getCurrentUser();
        profileService = new ProfileService();
        profileAvatarImageView = view.findViewById(R.id.imgAvatar);

        // Load and display organizer data from Firebase
        loadOrganizerData(view);

        // EDIT button
        View edit = view.findViewById(R.id.btnEdit);
        if (edit != null) {
            edit.setOnClickListener(v -> {
                showEditDialog();
            });
        }

        // Setup logout button
        View logoutButton = view.findViewById(R.id.btnLogout);
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> logout());
        }
    }

    private void loadOrganizerData(View view) {
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

        // Set email
        TextView tvBio = view.findViewById(R.id.tvBio);
        if (tvBio != null && currentUser.getEmail() != null) {
            tvBio.setText(currentUser.getEmail());
        }

        // Load and display profile image using Glide
        if (profileAvatarImageView != null) {
            if (currentUser.getProfileImageUrl() != null && !currentUser.getProfileImageUrl().isEmpty()) {
                Glide.with(requireContext())
                        .load(currentUser.getProfileImageUrl())
                        .transform(new CircleCrop())
                        .placeholder(R.drawable.bg_image_placeholder)
                        .into(profileAvatarImageView);
            } else {
                profileAvatarImageView.setImageResource(R.drawable.ic_profile);
            }
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

    /**
     * Displays a dialog for editing the organizer's profile information such as name, username, and email.
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
        ImageView imgProfilePicture = dialogView.findViewById(R.id.imgProfilePicture);
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
            String fullName  = etFull.getText()  != null ? etFull.getText().toString().trim()  : "";
            String username  = etUser.getText()  != null ? etUser.getText().toString().trim()  : "";
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";

            if (fullName.isEmpty()) { etFull.setError("Full name required"); return; }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Invalid email"); return;
            }

            dialogView.findViewById(R.id.btnSave).setEnabled(false);

            // If an image was selected, upload it first
            if (selectedImageUri != null) {
                uploadProfileImage(currentUser.getUid(), selectedImageUri, dialogView, dialog, fullName, username, email);
            } else {
                // No image selected, just update profile info
                updateProfileInfo(dialogView, dialog, fullName, username, email, null);
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
                Snackbar.make(requireView(), "Error uploading image", Snackbar.LENGTH_LONG).show();
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
                                    if (profileAvatarImageView != null) {
                                        Glide.with(requireContext())
                                                .load(profileImageUrl)
                                                .transform(new CircleCrop())
                                                .placeholder(R.drawable.ic_profile)
                                                .into(profileAvatarImageView);
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
                    FirebaseUser fu = FirebaseAuth.getInstance().getCurrentUser();
                    boolean authEmailMatches = fu != null && fu.getEmail() != null
                            && fu.getEmail().equalsIgnoreCase(email);

                    if (authEmailMatches) {
                        // Retry Firestore email field quietly so UI/DB stay in sync
                        Map<String, Object> up = new HashMap<>();
                        up.put("email", email);
                        FirebaseFirestore.getInstance()
                                .collection("users").document(currentUser.getUid())
                                .set(up, SetOptions.merge());

                        // Update UI & local cache, then close dialog
                        finishProfileUpdate(dialogView, dialog, fullName, email, username);
                    } else {
                        dialogView.findViewById(R.id.btnSave).setEnabled(true);
                        Snackbar.make(requireView(),
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
        TextView tvName = requireView().findViewById(R.id.tvName);
        if (tvName != null) {
            tvName.setText(fullName);
        }
        
        TextView tvBio = requireView().findViewById(R.id.tvBio);
        if (tvBio != null) {
            tvBio.setText(email);
        }

        // Keep local cache consistent
        currentUser.setDisplayName(fullName);
        currentUser.setUsername(username);

        // Clear the ImageView reference
        currentDialogProfileImageView = null;

        Snackbar.make(requireView(), "Profile updated", Snackbar.LENGTH_LONG).show();
        dialog.dismiss();
    }

    /**
     * Shows a dialog requesting the user's password for re-authentication.
     * This is required for sensitive operations like changing an email address.
     * After successful re-authentication, it retries the profile update.
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
                Snackbar.make(requireView(), "Not signed in", Snackbar.LENGTH_LONG).show();
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
                                        TextView tvBio = requireView().findViewById(R.id.tvBio);
                                        if (tvBio != null) {
                                            tvBio.setText(newEmail);
                                        }

                                        // Ensure name/username persisted too (safe even if email already changed)
                                        profileService.updateProfile(user.getUid(), fullName, username, newEmail);

                                        Snackbar.make(requireView(), "Email updated", Snackbar.LENGTH_LONG).show();
                                        d.dismiss();
                                    })
                                    .addOnFailureListener(err -> {
                                        Snackbar.make(requireView(), err.getMessage(), Snackbar.LENGTH_LONG).show();
                                    });
                        })
                        .addOnFailureListener(err -> {
                            Snackbar.make(requireView(), err.getMessage(), Snackbar.LENGTH_LONG).show();
                        });
            }).addOnFailureListener(err -> {
                Snackbar.make(requireView(), err.getMessage(), Snackbar.LENGTH_LONG).show();
            });
        });
    }
}
