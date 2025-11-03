package com.example.cmpuzz_events.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cmpuzz_events.MainActivity;
import com.example.cmpuzz_events.databinding.ActivitySignupBinding;
import com.example.cmpuzz_events.models.User;

public class SignupActivity extends AppCompatActivity {
    private ActivitySignupBinding binding;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authManager = AuthManager.getInstance();
        setupUI();
    }

    private void setupUI() {
        binding.btnSignup.setOnClickListener(v -> handleSignup());
        
        binding.tvLogin.setOnClickListener(v -> {
            finish(); // Go back to login
        });
    }

    private void handleSignup() {
        String name = binding.etName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String confirmPassword = binding.etConfirmPassword.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(name)) {
            binding.etName.setError("Name is required");
            binding.etName.requestFocus();
            return;
        }

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

        if (!password.equals(confirmPassword)) {
            binding.etConfirmPassword.setError("Passwords do not match");
            binding.etConfirmPassword.requestFocus();
            return;
        }

        // Show loading
        setLoading(true);

        authManager.signUp(email, password, name, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                setLoading(false);
                Toast.makeText(SignupActivity.this,
                        "Welcome, " + user.getDisplayName() + "!",
                        Toast.LENGTH_SHORT).show();
                navigateToMain();
            }

            @Override
            public void onError(String error) {
                setLoading(false);
                Toast.makeText(SignupActivity.this,
                        "Signup failed: " + error,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.btnSignup.setEnabled(!isLoading);
        binding.etName.setEnabled(!isLoading);
        binding.etEmail.setEnabled(!isLoading);
        binding.etPassword.setEnabled(!isLoading);
        binding.etConfirmPassword.setEnabled(!isLoading);
    }

    private void navigateToMain() {
        Intent intent = new Intent(SignupActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
