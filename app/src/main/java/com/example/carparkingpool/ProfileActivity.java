package com.example.carparkingpool;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileActivity extends AppCompatActivity {

    private EditText studentIdEditText, phoneNumberEditText;
    private TextView emailTextView;
    private Button editButton, logoutButton;
    private DatabaseReference userRef;
    private FirebaseUser currentUser;
    private boolean isEditing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize Firebase
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            // Reference to the current user's data in the "users" table
            String userId = currentUser.getUid();
            userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

            // Initialize views
            studentIdEditText = findViewById(R.id.studentId);
            phoneNumberEditText = findViewById(R.id.phoneNumber);
            emailTextView = findViewById(R.id.email);
            editButton = findViewById(R.id.buttonEditProfile);
            logoutButton = findViewById(R.id.buttonLogout);

            // Load and display user profile data
            loadUserProfileData();

            // Handle Edit button functionality
            editButton.setOnClickListener(v -> {
                if (isEditing) {
                    // Save changes to Firebase
                    saveUserProfileData();
                } else {
                    // Enable editing
                    studentIdEditText.setEnabled(true);
                    phoneNumberEditText.setEnabled(true);
                    editButton.setText("Save");
                }
                isEditing = !isEditing;
            });

            // Handle Logout button functionality
            logoutButton.setOnClickListener(v -> {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(ProfileActivity.this, MainActivity.class)); // Navigate to login
                finish();
            });
        } else {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadUserProfileData() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Retrieve the user data from the snapshot
                    String studentId = dataSnapshot.child("studentId").getValue(String.class);
                    String email = dataSnapshot.child("email").getValue(String.class);
                    String phone = dataSnapshot.child("phone").getValue(String.class);

                    // Set the retrieved data to the corresponding TextViews
                    studentIdEditText.setText(studentId);
                    emailTextView.setText(email);
                    phoneNumberEditText.setText(phone);
                } else {
                    Toast.makeText(ProfileActivity.this, "User data not found!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ProfileActivity.this, "Failed to load user data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveUserProfileData() {
        String newStudentId = studentIdEditText.getText().toString().trim();
        String newPhoneNumber = phoneNumberEditText.getText().toString().trim();

        if (newStudentId.isEmpty() || newPhoneNumber.isEmpty()) {
            Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save updated data to Firebase
        userRef.child("studentId").setValue(newStudentId);
        userRef.child("phone").setValue(newPhoneNumber);

        // Disable editing
        studentIdEditText.setEnabled(false);
        phoneNumberEditText.setEnabled(false);
        editButton.setText("Edit");

        Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
    }
}
