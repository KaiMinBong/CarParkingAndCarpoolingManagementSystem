package com.example.carparkingpool;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.view.LayoutInflater;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import android.text.Editable;
import android.text.TextWatcher;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import android.text.TextUtils;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.text.TextWatcher;
import android.text.Editable;
import androidx.appcompat.app.AlertDialog;


public class ParkActivity extends AppCompatActivity {

    private Spinner vehicleSpinner, locationSpinner;
    private EditText startTimeInput, endTimeInput;
    private Button registerButton, cancelButton;
    private TextView selectBlockedVehicles, selectBlockedText,alreadyRegisterVehical;
    private TabLayout tabLayout;

    private FirebaseAuth mAuth;
    private DatabaseReference vehicleRef, locationRef, parkingRef, usersRef, blockedMessagesRef;

    private ArrayList<String> vehicleList, locationList, selectedBlockedVehicles;
    private ArrayList<String> otherUserVehicles; // For vehicles of other users
    private boolean[] selectedBlockedVehicleItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_park);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            vehicleRef = FirebaseDatabase.getInstance().getReference("vehicles").child(userId);
            locationRef = FirebaseDatabase.getInstance().getReference("locations");
            parkingRef = FirebaseDatabase.getInstance().getReference("parking").child(userId);
            usersRef = FirebaseDatabase.getInstance().getReference("vehicles"); // Fetch all vehicles for all users
            blockedMessagesRef = FirebaseDatabase.getInstance().getReference("blockedMessages"); // For storing blocked vehicle messages
        }

        // Initialize UI elements
        vehicleSpinner = findViewById(R.id.vehicleSpinner);
        locationSpinner = findViewById(R.id.locationSpinner);
        selectBlockedVehicles = findViewById(R.id.selectBlockedVehicles);
        selectBlockedText = findViewById(R.id.doubleVehicleLabel);
        startTimeInput = findViewById(R.id.startTime);
        endTimeInput = findViewById(R.id.endTime);
        registerButton = findViewById(R.id.registerButton);
        cancelButton = findViewById(R.id.cancelButton);
        tabLayout = findViewById(R.id.tabLayout);
        alreadyRegisterVehical = findViewById(R.id.alreadyRegisterVehical);
        vehicleList = new ArrayList<>();
        locationList = new ArrayList<>();
        selectedBlockedVehicles = new ArrayList<>();
        otherUserVehicles = new ArrayList<>(); // To store vehicles of other users

        // Load data from Firebase
        loadUserVehicles();
        loadLocations();
        loadOtherUserVehicles(); // Load vehicles from other users

        // Check if user already has a parking record and disable the register button if so
        checkUserParkingRecord();

        // Time pickers for Start and End times
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        // Format the current time as HH:mm
        String currentTime = String.format("%02d:%02d", hour, minute);

        // Set the current time to the EditText
        startTimeInput.setText(currentTime);
        // Disable editing of the EditText (make it read-only)
        startTimeInput.setFocusable(false);
        startTimeInput.setClickable(false);
        endTimeInput.setOnClickListener(v -> showTimePickerDialog(endTimeInput));

        // Handle tab selection to toggle blocked vehicle selector
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 1) {  // Double tab selected
                    selectBlockedVehicles.setVisibility(View.VISIBLE);
                    selectBlockedText.setVisibility(View.VISIBLE);

                } else {  // Normal tab selected
                    selectBlockedVehicles.setVisibility(View.GONE);
                    selectBlockedText.setVisibility(View.GONE);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Show multi-select dialog when blocked vehicles TextView is clicked
        selectBlockedVehicles.setOnClickListener(v -> showBlockedVehicleDialog());

        // Register parking button
        registerButton.setOnClickListener(v -> registerParking());
        cancelButton.setOnClickListener(v -> clearFieldsAndCancel());
    }

    private void loadUserVehicles() {
        vehicleRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                vehicleList.clear();
                for (DataSnapshot vehicleSnapshot : dataSnapshot.getChildren()) {
                    String plateNumber = vehicleSnapshot.child("plateNumber").getValue(String.class);
                    vehicleList.add(plateNumber);
                }
                ArrayAdapter<String> vehicleAdapter = new ArrayAdapter<>(ParkActivity.this, android.R.layout.simple_spinner_item, vehicleList);
                vehicleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                vehicleSpinner.setAdapter(vehicleAdapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ParkActivity.this, "Failed to load vehicles", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadLocations() {
        locationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                locationList.clear();
                for (DataSnapshot locationSnapshot : dataSnapshot.getChildren()) {
                    String location = locationSnapshot.getValue(String.class);
                    locationList.add(location);
                }
                ArrayAdapter<String> locationAdapter = new ArrayAdapter<>(ParkActivity.this, android.R.layout.simple_spinner_item, locationList);
                locationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                locationSpinner.setAdapter(locationAdapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ParkActivity.this, "Failed to load locations", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadOtherUserVehicles() {
        String currentUserId = mAuth.getCurrentUser().getUid();
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                otherUserVehicles.clear();
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    String userId = userSnapshot.getKey();
                    // Only add vehicles from other users, not the current user
                    if (!userId.equals(currentUserId)) {
                        for (DataSnapshot vehicleSnapshot : userSnapshot.getChildren()) {
                            String plateNumber = vehicleSnapshot.child("plateNumber").getValue(String.class);
                            otherUserVehicles.add(plateNumber);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ParkActivity.this, "Failed to load other users' vehicles", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkUserParkingRecord() {
        parkingRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean showRegisterButton = true;
                boolean showalreadyRegisterVehical = false;

                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        String status = snapshot.child("status").getValue(String.class);

                        if (status != null && status.equals("parked")) {

                            showRegisterButton = false;
                            showalreadyRegisterVehical = true;
                            break;
                        }
                    }
                }

                if (showRegisterButton) {
                    registerButton.setVisibility(View.VISIBLE);
                    alreadyRegisterVehical.setVisibility(View.GONE);
                } else {
                    registerButton.setVisibility(View.GONE); // Hide register button if already parked
                    alreadyRegisterVehical.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ParkActivity.this, "Failed to check parking records.", Toast.LENGTH_SHORT).show();
            }
        });
    }


    // Modify showTimePickerDialog to ensure at least a 30-minute difference
    private void showTimePickerDialog(final EditText timeInput) {
        // Get the current time
        Calendar calendar = Calendar.getInstance();
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        int currentMinute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minuteOfHour) -> {
                    if (timeInput.getId() == R.id.endTime) {
                        // Get the current time in milliseconds
                        Calendar currentTime = Calendar.getInstance();
                        long currentTimeInMillis = currentTime.getTimeInMillis();

                        // Create the selected end time
                        Calendar selectedEndTime = Calendar.getInstance();
                        selectedEndTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        selectedEndTime.set(Calendar.MINUTE, minuteOfHour);

                        // Calculate the difference in time
                        long selectedEndTimeInMillis = selectedEndTime.getTimeInMillis();
                        long timeDifference = selectedEndTimeInMillis - currentTimeInMillis;

                        // Ensure the selected end time is at least 30 minutes after the current time
                        if (timeDifference < 30 * 60 * 1000) {
                            Toast.makeText(ParkActivity.this, "End time must be at least 30 minutes after the current time", Toast.LENGTH_SHORT).show();

                            // Do not update the timeInput EditText if the time is invalid
                            timeInput.setText("");  // Optionally clear the EditText if needed
                        } else {
                            // Set the valid selected end time
                            timeInput.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minuteOfHour));
                        }
                    }
                }, currentHour, currentMinute, true);
        timePickerDialog.show();
    }
    private void clearFieldsAndCancel() {
        // Clear all input fields
        vehicleSpinner.setSelection(0);
        locationSpinner.setSelection(0);
        startTimeInput.setText("");
        endTimeInput.setText("");
        selectBlockedVehicles.setText("");
        selectedBlockedVehicles.clear();

        // Optional: Finish the activity and go back to the previous screen
        finish();
    }

    private void showBlockedVehicleDialog() {
        // Inflate the custom layout with the search bar and list view
        LayoutInflater inflater = LayoutInflater.from(ParkActivity.this);
        View dialogView = inflater.inflate(R.layout.dialog_blocked_vehicle_search, null);

        // Initialize the search bar and list view from the dialog's layout
        EditText searchBar = dialogView.findViewById(R.id.searchBar);
        ListView listViewBlockedVehicles = dialogView.findViewById(R.id.listViewBlockedVehicles);

        // Initialize the filtered list and create a copy of the original list
        ArrayList<String> filteredBlockedVehicles = new ArrayList<>(otherUserVehicles); // Initially all vehicles are shown

        // Initialize the adapter for the ListView
        ArrayAdapter<String> blockedVehiclesAdapter = new ArrayAdapter<>(ParkActivity.this,
                android.R.layout.simple_list_item_multiple_choice, filteredBlockedVehicles);

        // Set the adapter to the ListView
        listViewBlockedVehicles.setAdapter(blockedVehiclesAdapter);
        listViewBlockedVehicles.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE); // Enable multiple selection

        // Method to update the checked state based on the selected vehicles list
        Runnable updateCheckedStates = () -> {
            for (int i = 0; i < filteredBlockedVehicles.size(); i++) {
                String vehicle = filteredBlockedVehicles.get(i);
                if (selectedBlockedVehicles.contains(vehicle)) {
                    listViewBlockedVehicles.setItemChecked(i, true); // Keep previously selected items checked
                } else {
                    listViewBlockedVehicles.setItemChecked(i, false); // Uncheck others
                }
            }
        };

        // Add TextWatcher to search bar to filter the list of blocked vehicles
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action needed here
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Filter the list based on the query in the search bar
                String query = s.toString().toLowerCase(Locale.getDefault());
                filteredBlockedVehicles.clear();

                if (query.isEmpty()) {
                    filteredBlockedVehicles.addAll(otherUserVehicles); // Show all vehicles when query is empty
                } else {
                    for (String vehicle : otherUserVehicles) {
                        if (vehicle.toLowerCase(Locale.getDefault()).contains(query)) {
                            filteredBlockedVehicles.add(vehicle); // Add matching vehicles to the filtered list
                        }
                    }
                }

                blockedVehiclesAdapter.notifyDataSetChanged(); // Update the list view with filtered results
                updateCheckedStates.run(); // Update the checked states of the list view after filtering
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No action needed here
            }
        });

        // Set item click listener to handle vehicle selection
        listViewBlockedVehicles.setOnItemClickListener((parent, view, position, id) -> {
            String selectedVehicle = filteredBlockedVehicles.get(position);

            // Toggle selection
            if (selectedBlockedVehicles.contains(selectedVehicle)) {
                selectedBlockedVehicles.remove(selectedVehicle);
            } else {
                selectedBlockedVehicles.add(selectedVehicle);
            }

            // Keep the checkboxes in sync with the selections
            updateCheckedStates.run();
        });

        // Create the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(ParkActivity.this);
        builder.setTitle("Select Blocked Vehicles");
        builder.setView(dialogView);

        // OK button to confirm the selection
        builder.setPositiveButton("OK", (dialog, which) -> {
            // Update the selected blocked vehicles TextView
            String selectedVehicles = TextUtils.join(", ", selectedBlockedVehicles);
            selectBlockedVehicles.setText(selectedVehicles); // Set the TextView with the selected vehicles
        });

        // Cancel button to dismiss the dialog without saving changes
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        // Clear button to reset the selection
        builder.setNeutralButton("Clear", (dialog, which) -> {
            selectedBlockedVehicles.clear(); // Clear the list of selected vehicles
            selectBlockedVehicles.setText(""); // Clear the TextView
            updateCheckedStates.run(); // Uncheck all items in the ListView
        });

        // Show the dialog
        builder.show();
    }


    private void registerParking() {
        String selectedVehicle = vehicleSpinner.getSelectedItem().toString();
        String selectedLocation = locationSpinner.getSelectedItem().toString();
        String startTime = startTimeInput.getText().toString();
        String endTime = endTimeInput.getText().toString();
        String blockedVehicles = TextUtils.join(", ", selectedBlockedVehicles);
        String currentUserId = mAuth.getCurrentUser().getUid(); // Current user's UID
        String currentDateTime = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(new Date()); // Current date and time

        if (selectedVehicle.isEmpty() || selectedLocation.isEmpty() || startTime.isEmpty() || endTime.isEmpty()) {
            Toast.makeText(ParkActivity.this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save parking information in Firebase with "parked" status and createdTime
        String parkingId = parkingRef.push().getKey();
        if (parkingId != null) {
            // Get the current timestamp in a specific format (useful for sorting/filtering later)
            String createdTime = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(new Date());

            // Create a new ParkingData object with the createdTime field
            ParkingData parkingData = new ParkingData(selectedVehicle, selectedLocation, startTime, endTime, blockedVehicles, "parked", createdTime);

            // Save the parking data into Firebase
            parkingRef.child(parkingId).setValue(parkingData).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(ParkActivity.this, "Parking Registered Successfully", Toast.LENGTH_SHORT).show();
                    // Send "double parked" messages to blocked vehicles' owners
                    sendBlockedVehicleMessages(currentUserId, currentDateTime, selectedBlockedVehicles, selectedVehicle);
                    // After registering, hide the register button
                    registerButton.setVisibility(View.GONE);
                    alreadyRegisterVehical.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(ParkActivity.this, "Failed to register parking", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }



    private void sendBlockedVehicleMessages(String currentUserId, String currentDateTime, ArrayList<String> blockedVehicles, String selectedVehicle) {
        // For each blocked vehicle, find the owner and send the message
        for (String blockedVehicle : blockedVehicles) {
            usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    boolean vehicleFound = false;

                    // Iterate over each user
                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        String userId = userSnapshot.getKey();

                        // Check all vehicles of this user
                        for (DataSnapshot vehicleSnapshot : userSnapshot.getChildren()) {
                            String plateNumber = vehicleSnapshot.child("plateNumber").getValue(String.class);

                            if (plateNumber != null && plateNumber.equals(blockedVehicle)) {
                                // We found the user that owns this vehicle
                                vehicleFound = true;

                                // Send the blocked message to this user
                                DatabaseReference messageRef = blockedMessagesRef.child(userId).push(); // Store under this user's ID
                                Map<String, Object> messageData = new HashMap<>();
                                messageData.put("message", "You had been double parked.");
                                messageData.put("blockedVehicle", blockedVehicle);
                                messageData.put("selectedVehicle", selectedVehicle); // Include the selected vehicle in the message
                                messageData.put("createdDateTime", currentDateTime);
                                messageData.put("userId", currentUserId); // User who caused the block
                                messageData.put("messageType", 1); // Default message type is 1

                                messageRef.setValue(messageData);
                                break;
                            }
                        }

                        if (vehicleFound) break; // Exit outer loop once the vehicle is found
                    }

                    if (!vehicleFound) {
                        Toast.makeText(ParkActivity.this, "Vehicle owner not found for " + blockedVehicle, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(ParkActivity.this, "Failed to send blocked vehicle message", Toast.LENGTH_SHORT).show();
                }
            });
        }

        Toast.makeText(ParkActivity.this, "", Toast.LENGTH_SHORT).show();
    }


}
