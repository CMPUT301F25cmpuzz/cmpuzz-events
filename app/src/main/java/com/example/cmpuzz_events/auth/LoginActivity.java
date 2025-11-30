package com.example.cmpuzz_events.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cmpuzz_events.MainActivity;
import com.example.cmpuzz_events.databinding.ActivityLoginBinding;
import com.example.cmpuzz_events.models.user.User;

/**
 * LoginActivity allows users to log in using their email and password, navigate to the sign-up screen,
 * or reset their password if forgotten. If a user is already signed in, they are automatically
 * redirected to the main application screen.
 *
 * This activity uses {@link AuthManager} for authentication logic and
 * {@link ActivityLoginBinding} for view binding.
 *
 */
public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private AuthManager authManager;
    private AuthManager.AuthStateListener authStateListener;
    private boolean isManualLoginInProgress = false;

    /**
     * This method initializes the UI using view binding, sets up authentication,
     * and redirects signed-in users to the main screen.
     *
     * @param savedInstanceState the previously saved instance state, if any
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authManager = AuthManager.getInstance();

        // Check if already signed in with user data loaded
        if (authManager.isSignedIn() && authManager.getCurrentUser() != null) {
            // Check if auto-login is allowed (only for entrants)
            if (authManager.isAutoLoginAllowed()) {
                // Auto-login for entrant - navigate without manual login flag
                navigateToMain(false);
                return;
            } else {
                // Organizer or admin - sign them out and show login screen
                // Remove any existing auth state listener first to prevent interference
                if (authStateListener != null) {
                    authManager.removeAuthStateListener(authStateListener);
                    authStateListener = null;
                }
                authManager.signOut();
                setupUI();
                return;
            }
        }

        // If Firebase has a persisted session but user data isn't loaded yet,
        // wait for it to load before deciding whether to navigate
        if (authManager.hasFirebaseSessionButNoUserData()) {
            // Set up listener to wait for user data to load
            // Use a one-time listener that removes itself after handling
            authStateListener = user -> {
                // Don't process if manual login is in progress
                if (isManualLoginInProgress) {
                    return;
                }
                
                if (user != null) {
                    // User data loaded - remove listener before processing to prevent duplicate calls
                    if (authStateListener != null) {
                        authManager.removeAuthStateListener(authStateListener);
                        authStateListener = null;
                    }
                    
                    // Check if auto-login is allowed
                    if (authManager.isAutoLoginAllowed()) {
                        // Entrant - auto-login, navigate without manual login flag
                        navigateToMain(false);
                    } else {
                        // Organizer or admin - sign them out and show login UI
                        authManager.signOut();
                        setupUI();
                    }
                } else {
                    // User is null - this might be the initial call before user data loads
                    // Don't remove listener yet, wait for user data to load
                    // Only show UI if Firebase session is also gone (user was signed out)
                    if (!authManager.isSignedIn()) {
                        // Firebase session is gone, remove listener and show login UI
                        if (authStateListener != null) {
                            authManager.removeAuthStateListener(authStateListener);
                            authStateListener = null;
                        }
                        setupUI();
                    }
                    // Otherwise, keep listener active to wait for user data to load
                }
            };
            authManager.addAuthStateListener(authStateListener);
            return;
        }

        // No persisted session, show login UI
        setupUI();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove auth state listener when activity is destroyed
        if (authStateListener != null) {
            authManager.removeAuthStateListener(authStateListener);
            authStateListener = null;
        }
        // Reset manual login flag
        isManualLoginInProgress = false;
    }

    /**
     * Sets up the click listeners for UI components:
     *
     */
    private void setupUI() {
        binding.btnLogin.setOnClickListener(v -> handleLogin());
        
        binding.tvSignup.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignupActivity.class));
        });

        binding.tvForgotPassword.setOnClickListener(v -> handleForgotPassword());
    }

    /**
     * This method handles and validates the email and password of the user
     */
    private void handleLogin() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(email)) {
            binding.etEmail.setError("Email is required");
            binding.etEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError("Please enter a valid email");
            binding.etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            binding.etPassword.setError("Password is required");
            binding.etPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            binding.etPassword.setError("Password must be at least 6 characters");
            binding.etPassword.requestFocus();
            return;
        }

        // Show loading
        setLoading(true);

        // Remove any existing auth state listener before manual login to prevent interference
        if (authStateListener != null) {
            authManager.removeAuthStateListener(authStateListener);
            authStateListener = null;
        }

        // Set flag to indicate manual login is in progress
        isManualLoginInProgress = true;

        authManager.signIn(email, password, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                setLoading(false);
                isManualLoginInProgress = false; // Reset flag
                Toast.makeText(LoginActivity.this, 
                        "Welcome back, " + user.getDisplayName() + "!", 
                        Toast.LENGTH_SHORT).show();
                // Navigate immediately after successful manual login
                navigateToMain();
            }

            @Override
            public void onError(String error) {
                setLoading(false);
                isManualLoginInProgress = false; // Reset flag on error
                Toast.makeText(LoginActivity.this, 
                        "Login failed: " + error, 
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Sends a password reset email using Firebase Authentication if the user provides a valid email.
     * Displays a confirmation or error message based on the result.
     */
    private void handleForgotPassword() {
        String email = binding.etEmail.getText().toString().trim();
        
        if (TextUtils.isEmpty(email)) {
            binding.etEmail.setError("Enter your email to reset password");
            binding.etEmail.requestFocus();
            return;
        }

        authManager.getAuth().sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this,
                                "Password reset email sent!",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(LoginActivity.this,
                                "Error: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
    /**
     * Toggles the loading state of the login screen.
     *
     * @param isLoading {@code true} to show the loading indicator; {@code false} to hide it
     */
    private void setLoading(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!isLoading);
        binding.etEmail.setEnabled(!isLoading);
        binding.etPassword.setEnabled(!isLoading);
    }

    /**
     * Navigates the user to the main activity after successful login.
     * 
     * @param isManualLogin {@code true} if this is a manual login, {@code false} if it's an auto-login
     */
    private void navigateToMain(boolean isManualLogin) {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        // Flag to indicate whether this is a manual login or auto-login
        intent.putExtra("is_manual_login", isManualLogin);
        startActivity(intent);
        finish();
    }
    
    /**
     * Navigates the user to the main activity after successful manual login.
     * Convenience method that calls navigateToMain(true).
     */
    private void navigateToMain() {
        navigateToMain(true);
    }
}
