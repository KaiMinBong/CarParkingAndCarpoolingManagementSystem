package com.example.carparkingpool;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class AddPostActivity extends AppCompatActivity {

    private Spinner spinnerPostType;
    private EditText editTextDate, editTextPickupTime, editTextPickupLocation, editTextDropoffLocation;
    private Button buttonPost, buttonCancel;

    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    private String postKey = null;  // Store the post ID if in edit mode

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_post);

        // Initialize Firebase Auth and Database Reference
        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("carpool");

        // Initialize Views
        TextView pageTitleText = findViewById(R.id.title); // Ensure this is a TextView
        spinnerPostType = findViewById(R.id.spinnerPostType);
        editTextDate = findViewById(R.id.editTextDate);
        editTextPickupTime = findViewById(R.id.editTextPickupTime);
        editTextPickupLocation = findViewById(R.id.editTextPickupLocation);
        editTextDropoffLocation = findViewById(R.id.editTextDropoffLocation);
        buttonPost = findViewById(R.id.buttonPost);
        buttonCancel = findViewById(R.id.buttonCancel);

        // Populate the spinner with "Offer" and "Request" options
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.post_type_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPostType.setAdapter(adapter);

        // Check if data is passed from ManagePost for editing
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("postKey")) {
            // We're in edit mode
            postKey = intent.getStringExtra("postKey");

            // Populate the fields with existing data
            String postType = intent.getStringExtra("postType");
            String date = intent.getStringExtra("date");
            String pickupTime = intent.getStringExtra("pickupTime");
            String pickupLocation = intent.getStringExtra("pickupLocation");
            String dropoffLocation = intent.getStringExtra("dropoffLocation");

            // Set the data to the input fields
            spinnerPostType.setSelection(adapter.getPosition(postType));
            editTextDate.setText(date);
            editTextPickupTime.setText(pickupTime);
            editTextPickupLocation.setText(pickupLocation);
            editTextDropoffLocation.setText(dropoffLocation);

            // Change title to "Edit Post"
            pageTitleText.setText("Edit Post");

            // Change button text to "Update"
            buttonPost.setText("Update Post");
        }

        // Set Click Listener for the Post/Update Button
        buttonPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (postKey == null) {
                    addPostToDatabase();  // Add new post
                } else {
                    updatePostInDatabase();  // Update existing post
                }
            }
        });

        // Set Click Listener for the Cancel Button
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Close the activity
            }
        });
    }

    // Method to add post to Firebase Realtime Database
    private void addPostToDatabase() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            // Get user UID
            String uid = currentUser.getUid();

            // Get form data
            String postType = spinnerPostType.getSelectedItem().toString();
            String date = editTextDate.getText().toString().trim();
            String pickupTime = editTextPickupTime.getText().toString().trim();
            String pickupLocation = editTextPickupLocation.getText().toString().trim();
            String dropoffLocation = editTextDropoffLocation.getText().toString().trim();

            // Validate form data
            if (date.isEmpty() || pickupTime.isEmpty() || pickupLocation.isEmpty() || dropoffLocation.isEmpty()) {
                Toast.makeText(AddPostActivity.this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create a unique key for the post
            String postId = databaseReference.child(uid).push().getKey();

            // Create a post object
            Map<String, Object> post = new HashMap<>();
            post.put("postType", postType);
            post.put("date", date);
            post.put("pickupTime", pickupTime);
            post.put("pickupLocation", pickupLocation);
            post.put("dropoffLocation", dropoffLocation);

            // Save post to Firebase Database under the user's UID
            if (postId != null) {
                databaseReference.child(uid).child(postId).setValue(post).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(AddPostActivity.this, "Post added successfully", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(AddPostActivity.this, ManagePost.class);
                        startActivity(intent);
                    } else {
                        Toast.makeText(AddPostActivity.this, "Failed to add post", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } else {
            Toast.makeText(AddPostActivity.this, "User not logged in", Toast.LENGTH_SHORT).show();
        }
    }

    // Method to update an existing post in Firebase Realtime Database
    private void updatePostInDatabase() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            // Get user UID
            String uid = currentUser.getUid();

            // Get updated form data
            String postType = spinnerPostType.getSelectedItem().toString();
            String date = editTextDate.getText().toString().trim();
            String pickupTime = editTextPickupTime.getText().toString().trim();
            String pickupLocation = editTextPickupLocation.getText().toString().trim();
            String dropoffLocation = editTextDropoffLocation.getText().toString().trim();

            // Validate form data
            if (date.isEmpty() || pickupTime.isEmpty() || pickupLocation.isEmpty() || dropoffLocation.isEmpty()) {
                Toast.makeText(AddPostActivity.this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create a post object to update
            Map<String, Object> postUpdates = new HashMap<>();
            postUpdates.put("postType", postType);
            postUpdates.put("date", date);
            postUpdates.put("pickupTime", pickupTime);
            postUpdates.put("pickupLocation", pickupLocation);
            postUpdates.put("dropoffLocation", dropoffLocation);

            // Update the post in Firebase Database
            databaseReference.child(uid).child(postKey).updateChildren(postUpdates).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(AddPostActivity.this, "Post updated successfully", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(AddPostActivity.this, ManagePost.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(AddPostActivity.this, "Failed to update post", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(AddPostActivity.this, "User not logged in", Toast.LENGTH_SHORT).show();
        }
    }
}
