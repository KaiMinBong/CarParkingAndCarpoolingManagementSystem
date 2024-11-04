package com.example.carparkingpool;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.text.style.StyleSpan;
import android.graphics.Typeface;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableString;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class CarpoolActivity extends AppCompatActivity {

    private Button manageButton, requestTab, offerTab;
    private LinearLayout carpoolItemList;
    private DatabaseReference carpoolRef, usersRef;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private String selectedTab = "Request"; // Default to Request tab

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_carpool);

        // Initialize Firebase auth and database reference
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        carpoolRef = FirebaseDatabase.getInstance().getReference("carpool");
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        // Find views
        manageButton = findViewById(R.id.manageButton);
        requestTab = findViewById(R.id.requestTab);
        offerTab = findViewById(R.id.offerTab);
        carpoolItemList = findViewById(R.id.carpoolItemList);

        // Set OnClickListener for the manage button
        manageButton.setOnClickListener(v -> {
            Intent intent = new Intent(CarpoolActivity.this, ManagePost.class);
            startActivity(intent);
        });

        // Set initial tab selection for "Request" tab
        updateTabSelection(requestTab, offerTab, "Request");

        // Set OnClickListeners for Request and Offer tabs
        requestTab.setOnClickListener(v -> {
            updateTabSelection(requestTab, offerTab, "Request");
            loadCarpoolData(); // Load data based on "Request"
        });

        offerTab.setOnClickListener(v -> {
            updateTabSelection(offerTab, requestTab, "Offer");
            loadCarpoolData(); // Load data based on "Offer"
        });

        // Load the default tab (Request)
        loadCarpoolData();
    }

    // Update tab appearance based on selection
    private void updateTabSelection(Button selectedTab, Button unselectedTab, String tabName) {
        // Change background and text color of the selected tab
        selectedTab.setBackgroundTintList(getResources().getColorStateList(R.color.red));
        selectedTab.setTextColor(getResources().getColor(R.color.white));
        selectedTab.setSelected(true);

        // Reset background and text color of the unselected tab
        unselectedTab.setBackgroundTintList(getResources().getColorStateList(R.color.white));
        unselectedTab.setTextColor(getResources().getColor(R.color.black));
        unselectedTab.setSelected(false);

        // Update the selected tab name for data loading
        this.selectedTab = tabName;
    }

    // Method to load carpool data based on the selected tab (Request or Offer)
    private void loadCarpoolData() {
        carpoolItemList.removeAllViews(); // Clear the existing views

        carpoolRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Clear the current list
                carpoolItemList.removeAllViews();

                // Loop through all carpool posts
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    for (DataSnapshot carpoolPost : snapshot.getChildren()) {
                        String postType = carpoolPost.child("postType").getValue(String.class);
                        String userId = snapshot.getKey();
                        String postKey = carpoolPost.getKey();  // Get post key

                        // Only show the posts of the selected type and not by the current user
                        if (postType != null && postType.equals(selectedTab) && !userId.equals(currentUser.getUid())) {
                            // Get the carpool data
                            String date = carpoolPost.child("date").getValue(String.class);
                            String pickupLocation = carpoolPost.child("pickupLocation").getValue(String.class);
                            String dropoffLocation = carpoolPost.child("dropoffLocation").getValue(String.class);
                            String pickupTime = carpoolPost.child("pickupTime").getValue(String.class);

                            // Fetch the student's ID from the users table based on userId
                            getStudentId(userId, date, pickupLocation, dropoffLocation, pickupTime, postKey);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(CarpoolActivity.this, "Failed to load data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Method to fetch studentId from the users table
    private void getStudentId(String userId, String date, String pickupLocation, String dropoffLocation, String pickupTime, String postKey) {
        usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String studentId = dataSnapshot.child("studentId").getValue(String.class);
                    // Add the carpool item along with the student ID to the UI
                    addCarpoolItem(date, pickupLocation, dropoffLocation, pickupTime, studentId, userId, postKey);
                } else {
                    Toast.makeText(CarpoolActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(CarpoolActivity.this, "Failed to load studentId", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Method to add carpool item to the layout
    private void addCarpoolItem(String date, String pickupLocation, String dropoffLocation, String pickupTime, String studentId, String userId, String postKey) {
        View carpoolItemView = getLayoutInflater().inflate(R.layout.carpool_item, null);

        // Set data to the views
        ((TextView) carpoolItemView.findViewById(R.id.date)).setText(getBoldLabel("Date:", date));
        ((TextView) carpoolItemView.findViewById(R.id.time)).setText(getBoldLabel("Time:", pickupTime));
        ((TextView) carpoolItemView.findViewById(R.id.pickup)).setText(getBoldLabel("Pick Up:", pickupLocation));
        ((TextView) carpoolItemView.findViewById(R.id.dropoff)).setText(getBoldLabel("Drop Off:", dropoffLocation));
        ((TextView) carpoolItemView.findViewById(R.id.studentId)).setText(getBoldLabel("Student ID:", studentId));

        // Interested Button Logic
        Button interestedButton = carpoolItemView.findViewById(R.id.interestedButton);
        interestedButton.setOnClickListener(v -> {
            saveInterestedUser(userId, postKey);
        });

        // Add the view to the layout
        carpoolItemList.addView(carpoolItemView);
    }

    // Method to save interested user
    private void saveInterestedUser(String userId, String postKey) {
        DatabaseReference interestedUsersRef = carpoolRef.child(userId).child(postKey).child("interestedUsers");

        // Save the current user's UID under the interestedUsers node
        interestedUsersRef.child(currentUser.getUid()).setValue(true).addOnSuccessListener(aVoid -> {
            Toast.makeText(CarpoolActivity.this, "Marked as Interested", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            Toast.makeText(CarpoolActivity.this, "Failed to mark interest", Toast.LENGTH_SHORT).show();
        });
    }

    // Add this method to your class
    private SpannableString getBoldLabel(String label, String value) {
        SpannableString spannableString = new SpannableString(label + " " + value);
        spannableString.setSpan(new StyleSpan(Typeface.BOLD), 0, label.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannableString;
    }
}
