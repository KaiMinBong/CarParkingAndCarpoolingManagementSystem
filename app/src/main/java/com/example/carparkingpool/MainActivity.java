package com.example.carparkingpool;
import android.util.Log;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Bundle;
import android.util.Log;
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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    private EditText emailInput, passwordInput;
    private Button loginButton;
    private TextView signUpText;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private DatabaseReference parkingRef;
    private DatabaseReference blockedMessagesRef; // Reference to send messages
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView signUpText = findViewById(R.id.signUpText);
        signUpText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SignUp.class);
                startActivity(intent);
            }
        });

        // Initialize UI components
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        progressBar = findViewById(R.id.progressBar); // Optional if you want to show progress
        TextView forgotPasswordText = findViewById(R.id.forgotPassword);

        forgotPasswordText.setOnClickListener(v -> {
            ForgotPasswordDialog forgotPasswordDialog = new ForgotPasswordDialog();
            forgotPasswordDialog.show(getSupportFragmentManager(), "ForgotPasswordDialog");
        });

        // Set up login button click listener
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });


        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize Firebase Database reference to the parking table
        parkingRef = FirebaseDatabase.getInstance().getReference("parking");

        // Check if the user is already logged in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
            blockedMessagesRef = FirebaseDatabase.getInstance().getReference("blockedMessages"); // For storing blocked vehicle messages
            // User is already logged in, navigate to HomeActivity
            navigateToHome();
        }

        // Call method to check parking records
        checkParkingStatus();
    }

    private void loginUser() {
        // Get values from input fields
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        // Input validation
        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Email is required.");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Password is required.");
            return;
        }

        // Show progress bar (optional)
        progressBar.setVisibility(View.VISIBLE);

        // Log in the user with Firebase Authentication
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    // Hide progress bar (optional)
                    progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful()) {
                        // Sign in success
                        Toast.makeText(MainActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                        navigateToHome();
                    } else {
                        // If sign in fails, display a message to the user
                        Toast.makeText(MainActivity.this, "Login failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }

                });
    }

    private void sendEndOfParkingTimeMessage() {
        if (userId != null) {
            DatabaseReference messageRef = blockedMessagesRef.child(userId).push(); // Store under this user's ID
            String currentDateTime = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(new Date()); // Current date and time

            // Create a message object with the necessary details
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("message", "Your parking session has ended.");
            messageData.put("createdDateTime", currentDateTime);
            messageData.put("userId", userId); // ID of the user whose parking session ended
            messageData.put("messageType", 4); // Message type for end-of-parking notification

            // Set the value in the database
            messageRef.setValue(messageData);
        }
    }

    private void checkParkingStatus() {
        // Attach a listener to read data from the parking table
        parkingRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot data : snapshot.getChildren()) {
                    for (DataSnapshot parkingRecord : data.getChildren()) {
                        // Retrieve the endTime and status from the record
                        String endTime = parkingRecord.child("endTime").getValue(String.class);
                        String status = parkingRecord.child("status").getValue(String.class);

                        if (endTime != null && status != null && status.equals("parked")) {
                            // Check if endTime has already passed
                            if (isEndTimePassed(endTime)) {
                                // If endTime has passed, update the status to "exit"
                                parkingRecord.getRef().child("status").setValue("exit");
                                Log.d("MainActivity", "Updated status to exit for vehicle: " + parkingRecord.child("vehicle").getValue());
                                sendEndOfParkingTimeMessage();
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle possible errors
                Toast.makeText(MainActivity.this, "Error reading from Firebase: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean isEndTimePassed(String endTime) {
        try {
            // Get current time in 24-hour format regardless of device time format
            Calendar currentCalendar = Calendar.getInstance();
            SimpleDateFormat sdf24 = new SimpleDateFormat("HH:mm", Locale.getDefault());

            // Parse the endTime from the parking data (which is assumed to be in 24-hour format)
            Date endTimeDate = sdf24.parse(endTime);

            // Get the current time in 24-hour format
            String currentTimeString = sdf24.format(currentCalendar.getTime());
            Date currentTimeDate = sdf24.parse(currentTimeString);

            // Compare the end time to the current time
            if (endTimeDate != null && currentTimeDate != null) {
                return endTimeDate.before(currentTimeDate);
            }
        } catch (Exception e) {
            Log.e("ParkingManagementActivity", "Error parsing endTime: " + e.getMessage());
            Toast.makeText(this, "Error parsing time: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return false;
    }



    // Method to navigate to HomeActivity
    private void navigateToHome() {
        Intent intent = new Intent(MainActivity.this, HomeActivty.class);
        startActivity(intent);
        finish(); // Close the current activity
    }
}
