package com.example.carparkingpool;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class SignUp extends AppCompatActivity {

    private EditText studentIdInput, phoneInput, emailInput, passwordInput, confirmPasswordInput;
    private Button signUpButton;
    private TextView loginButton;
    private ProgressBar progressBar;

    // Firebase instances
    private FirebaseAuth mAuth;
    private DatabaseReference dbReference; // Reference for Realtime Database

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Initialize Firebase Auth and Realtime Database
        mAuth = FirebaseAuth.getInstance();
        dbReference = FirebaseDatabase.getInstance().getReference("users"); // Reference to "users" node in Realtime Database

        // Initialize UI components
        studentIdInput = findViewById(R.id.studentIdInput);
        phoneInput = findViewById(R.id.phoneInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        signUpButton = findViewById(R.id.loginButton);
        loginButton = findViewById(R.id.signUpText);
        progressBar = findViewById(R.id.progressBar);

        // Set up the SignUp button click listener
        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });

        // Set up the Log in button click listener
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SignUp.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }

    private void registerUser() {
        // Get values from input fields
        String studentId = studentIdInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        // Define the password pattern
        String passwordPattern = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{6,}$";

        // Input validation
        if (TextUtils.isEmpty(studentId)) {
            studentIdInput.setError("Student ID is required.");
            return;
        }

        // Validate studentId (must start with "P" followed by 8 digits)
        if (!studentId.matches("^P\\d{8}$")) {
            studentIdInput.setError("Student ID must start with 'P' followed by 8 digits.");
            return;
        }

        if (TextUtils.isEmpty(phone)) {
            phoneInput.setError("Phone is required.");
            return;
        }

        // Validate phone (must contain exactly 10 digits)
        if (!phone.matches("^\\d{10}$")) {
            phoneInput.setError("Phone must contain exactly 10 digits.");
            return;
        }

        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Email is required.");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Password is required.");
            return;
        }

        if (!password.matches(passwordPattern)) {
            passwordInput.setError("Password must be at least 6 characters, with uppercase, lowercase, number, and symbol.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordInput.setError("Passwords do not match.");
            return;
        }

        // Show progress bar
        progressBar.setVisibility(View.VISIBLE);

        // Check if the phone or studentId already exists
        dbReference.orderByChild("phone").equalTo(phone).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    progressBar.setVisibility(View.GONE);
                    phoneInput.setError("This phone number is already registered.");
                    return;
                }

                dbReference.orderByChild("studentId").equalTo(studentId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            progressBar.setVisibility(View.GONE);
                            studentIdInput.setError("This student ID is already registered.");
                            return;
                        }

                        // Create a new user with Firebase Authentication
                        mAuth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener(SignUp.this, task -> {
                                    // Hide progress bar
                                    progressBar.setVisibility(View.GONE);

                                    if (task.isSuccessful()) {
                                        // Registration success
                                        FirebaseUser user = mAuth.getCurrentUser();
                                        if (user != null) {
                                            saveUserProfile(user.getUid(), studentId, phone, email);
                                        }
                                    } else {
                                        // If sign up fails, display a message to the user
                                        Toast.makeText(SignUp.this, "Authentication failed: " + task.getException().getMessage(),
                                                Toast.LENGTH_LONG).show();
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        progressBar.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void saveUserProfile(@NonNull String uid, @NonNull String studentId, @NonNull String phone, @NonNull String email) {
        // Create a user profile map
        Map<String, Object> userProfile = new HashMap<>();
        userProfile.put("studentId", studentId);
        userProfile.put("phone", phone);
        userProfile.put("email", email);

        // Save the user's profile to Realtime Database
        dbReference.child(uid).setValue(userProfile)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(SignUp.this, "Profile saved successfully!", Toast.LENGTH_SHORT).show();
                        // Optionally, navigate to the main activity or home screen
                        Intent intent = new Intent(SignUp.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(SignUp.this, "Profile saving failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}
