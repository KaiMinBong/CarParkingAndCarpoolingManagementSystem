package com.example.carparkingpool;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ManagePost extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference carpoolRef, usersRef;
    private LinearLayout managePostItemList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_post);

        // Initialize Firebase Auth and Database reference
        mAuth = FirebaseAuth.getInstance();
        carpoolRef = FirebaseDatabase.getInstance().getReference("carpool");
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        // Initialize the addPostButton
        Button addPostButton = findViewById(R.id.addPostButton);

        // Set OnClickListener for the Add Post button
        addPostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to AddPostActivity when the button is clicked
                Intent intent = new Intent(ManagePost.this, AddPostActivity.class);
                startActivity(intent);
            }
        });

        managePostItemList = findViewById(R.id.managePostItemList);

        // Fetch and display the posts of the logged-in user
        fetchUserPosts();
    }

    private void fetchUserPosts() {
        String currentUserId = mAuth.getCurrentUser().getUid();

        carpoolRef.child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                managePostItemList.removeAllViews(); // Clear the list before adding new items

                // Loop through all posts of the current user
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    // Get the unique post ID
                    final String postKey = postSnapshot.getKey();
                    String postType = postSnapshot.child("postType").getValue(String.class);
                    String date = postSnapshot.child("date").getValue(String.class);
                    String pickupTime = postSnapshot.child("pickupTime").getValue(String.class);
                    String pickupLocation = postSnapshot.child("pickupLocation").getValue(String.class);
                    String dropoffLocation = postSnapshot.child("dropoffLocation").getValue(String.class);

                    // Inflate each post item dynamically
                    View postItemView = LayoutInflater.from(ManagePost.this).inflate(R.layout.carpool_item1, managePostItemList, false);

                    // Set values to TextViews
                    ((TextView) postItemView.findViewById(R.id.postType)).setText("My " + postType);
                    ((TextView) postItemView.findViewById(R.id.date)).setText("Date: " + date);
                    ((TextView) postItemView.findViewById(R.id.time)).setText("Time: " + pickupTime);
                    ((TextView) postItemView.findViewById(R.id.pickup)).setText("Pick Up: " + pickupLocation);
                    ((TextView) postItemView.findViewById(R.id.dropoff)).setText("Drop Off: " + dropoffLocation);

                    // Add edit and delete button functionality
                    ImageView editButton = postItemView.findViewById(R.id.editPost);
                    ImageView deleteButton = postItemView.findViewById(R.id.deletePost);

                    // Edit button functionality
                    editButton.setOnClickListener(v -> {
                        // Navigate to AddPostActivity with data for editing
                        Intent intent = new Intent(ManagePost.this, AddPostActivity.class);
                        intent.putExtra("postKey", postKey);  // Pass the post key (ID)
                        intent.putExtra("postType", postType);
                        intent.putExtra("date", date);
                        intent.putExtra("pickupTime", pickupTime);
                        intent.putExtra("pickupLocation", pickupLocation);
                        intent.putExtra("dropoffLocation", dropoffLocation);
                        startActivity(intent);
                    });

                    // Delete button functionality
                    deleteButton.setOnClickListener(v -> {
                        // Show confirmation dialog before deleting
                        new AlertDialog.Builder(ManagePost.this)
                                .setTitle("Delete Post")
                                .setMessage("Are you sure you want to delete this post?")
                                .setPositiveButton("Yes", (dialog, which) -> {
                                    // Remove post from Firebase
                                    carpoolRef.child(currentUserId).child(postKey).removeValue()
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(ManagePost.this, "Post deleted", Toast.LENGTH_SHORT).show();
                                                fetchUserPosts(); // Refresh the list after deletion
                                            })
                                            .addOnFailureListener(e -> Toast.makeText(ManagePost.this, "Failed to delete post", Toast.LENGTH_SHORT).show());
                                })
                                .setNegativeButton("No", null)
                                .show();
                    });

                    // Check if there are any interested users
                    carpoolRef.child(currentUserId).child(postKey).child("interestedUsers").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot interestedSnapshot) {
                            if (interestedSnapshot.exists()) {
                                // Loop through interested users
                                for (DataSnapshot interestedUser : interestedSnapshot.getChildren()) {
                                    String interestedUserId = interestedUser.getKey();
                                    if (interestedUserId != null) {
                                        fetchUserDetails(interestedUserId, postItemView);
                                    }
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Toast.makeText(ManagePost.this, "Failed to load interested users", Toast.LENGTH_SHORT).show();
                        }
                    });

                    // Add the post item to the LinearLayout
                    managePostItemList.addView(postItemView);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle potential errors
            }
        });
    }

    // Method to fetch user details such as phone number and email
    private void fetchUserDetails(String interestedUserId, View postItemView) {
        usersRef.child(interestedUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Get email and phone number
                    String studentId = dataSnapshot.child("studentId").getValue(String.class);
                    String email = dataSnapshot.child("email").getValue(String.class);
                    String phone = dataSnapshot.child("phone").getValue(String.class);

                    // Add email and phone to the post item view
                    TextView studentIdTextView = new TextView(ManagePost.this);
                    studentIdTextView.setText("\nInterested Student ID: " + studentId);
                    TextView emailTextView = new TextView(ManagePost.this);
                    emailTextView.setText("Interested Email: " + email);
                    TextView phoneTextView = new TextView(ManagePost.this);
                    phoneTextView.setText("Interested Phone: " + phone);

                    // Add these details to the post item view dynamically
                    LinearLayout layout = postItemView.findViewById(R.id.interestedUserInfoContainer);
                    layout.addView(studentIdTextView);
                    layout.addView(emailTextView);
                    layout.addView(phoneTextView);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ManagePost.this, "Failed to load user details", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
