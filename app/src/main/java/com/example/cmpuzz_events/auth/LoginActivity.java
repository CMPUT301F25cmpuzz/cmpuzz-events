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

        // Check if already signed in
        if (authManager.isSignedIn()) {
            navigateToMain();
            return;
        }

        setupUI();
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

        authManager.signIn(email, password, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, 
                        "Welcome back, " + user.getDisplayName() + "!", 
                        Toast.LENGTH_SHORT).show();
                navigateToMain();
            }

            @Override
            public void onError(String error) {
                setLoading(false);
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
     */
    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
