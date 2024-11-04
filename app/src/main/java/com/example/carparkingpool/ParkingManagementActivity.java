package com.example.carparkingpool;
import android.util.Log;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import android.widget.LinearLayout;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import android.app.TimePickerDialog;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class ParkingManagementActivity extends AppCompatActivity {

    private TextView plateNumber, parkingLocation, parkedStartTime, parkedEndTime, parkingType;
    private Button updateButton, exitButton;
    private DatabaseReference blockedMessagesRef; // Reference to send messages
    private FirebaseAuth mAuth;
    private DatabaseReference parkingRef;

    private String userId;
    private String parkingId; // This will store the parking entry ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parking_management);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // Initialize Firebase Database Reference for the parking data
        if (currentUser != null) {
            userId = currentUser.getUid();
            parkingRef = FirebaseDatabase.getInstance().getReference("parking").child(userId);
            blockedMessagesRef = FirebaseDatabase.getInstance().getReference("blockedMessages"); // For storing blocked vehicle messages

        }

        // Initialize views
        plateNumber = findViewById(R.id.parkedCarPlate);
        parkingLocation = findViewById(R.id.parkedLocation);
        parkedStartTime = findViewById(R.id.parkedStartTime);
        parkedEndTime = findViewById(R.id.parkedEndTime);
        parkingType = findViewById(R.id.parkedType);
        updateButton = findViewById(R.id.updateButton);
        exitButton = findViewById(R.id.exitButton);

        // Load the user's parked car details from Firebase
        loadUserParkingData();

        // Set OnClickListeners for the buttons
        updateButton.setOnClickListener(v -> showUpdatePopup());
        exitButton.setOnClickListener(v -> {
            exitParking();
            sendExitParkingMessage();
        });
    }

    private void loadUserParkingData() {
        parkingRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean hasParkedVehicle = false;  // Flag to track if there's any parked vehicle

                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        String status = snapshot.child("status").getValue(String.class);  // Get the status value

                        // Check if the status is "parked"
                        if (status != null && status.equals("parked")) {
                            hasParkedVehicle = true;  // Set flag to true if a parked vehicle is found

                            // Retrieve the parking data
                            parkingId = snapshot.getKey();
                            String plate = snapshot.child("vehicle").getValue(String.class);
                            String location = snapshot.child("location").getValue(String.class);
                            String startTime = snapshot.child("startTime").getValue(String.class);
                            String endTime = snapshot.child("endTime").getValue(String.class);
                            String blockedVehicles = snapshot.child("blockedVehicles").getValue(String.class);

                            // Set the data to the views
                            plateNumber.setText(plate);
                            parkingLocation.setText(location);
                            parkedStartTime.setText(startTime);
                            parkedEndTime.setText(endTime);

                            // Determine parking type based on blockedVehicles field
                            if (blockedVehicles == null || blockedVehicles.isEmpty()) {
                                parkingType.setText("Normal");
                            } else {
                                parkingType.setText("Double");
                            }

                            // Check if the parking end time has passed, update status if necessary
                            if (isEndTimePassed(endTime)) {
                                // If endTime has passed, update the status to "exit"
                                exitParking();
                            }
                        }
                    }

                    // If no "parked" vehicle was found, display a message
                    if (!hasParkedVehicle) {
                        displayNoParkedVehiclesMessage();
                    }
                } else {
                    // If the snapshot does not exist, show the message
                    displayNoParkedVehiclesMessage();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ParkingManagementActivity.this, "Failed to load parking data", Toast.LENGTH_SHORT).show();
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



    // Method to display the "No parked vehicles" message
    private void displayNoParkedVehiclesMessage() {
        LinearLayout parkingInfoLayout = findViewById(R.id.parkingInfoLayout);
        parkingInfoLayout.removeAllViews();
        TextView noParkingMessage = new TextView(this);
        noParkingMessage.setText("No parked vehicles");
        noParkingMessage.setTextSize(18);
        noParkingMessage.setTextColor(getResources().getColor(android.R.color.black));
        updateButton.setVisibility(View.GONE);
        exitButton.setVisibility(View.GONE);
        parkingInfoLayout.addView(noParkingMessage);
    }
    private void showUpdatePopup() {
        // Inflate the custom layout for the popup
        LayoutInflater inflater = getLayoutInflater();
        View popupView = inflater.inflate(R.layout.popup_update_end_time, null);

        // Initialize popup views
        TextView currentStartTimeTextView = popupView.findViewById(R.id.currentStartTime);
        TextView currentEndTimeTextView = popupView.findViewById(R.id.currentEndTime);
        EditText newEndTimeEditText = popupView.findViewById(R.id.newEndTime);
        Button updateEndTimeButton = popupView.findViewById(R.id.updateTimeButton);
        Button cancelUpdateButton = popupView.findViewById(R.id.cancelButton);

        newEndTimeEditText.setVisibility(View.GONE);

        // Set the current times (assuming you have methods to get the current start/end times)
        currentStartTimeTextView.setText(parkedStartTime.getText().toString());
        currentEndTimeTextView.setText(parkedEndTime.getText().toString());

        // Create an AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(popupView);
        AlertDialog dialog = builder.create();
        dialog.show();

        // Handle update button click
        updateEndTimeButton.setOnClickListener(v -> {
            // Open TimePickerDialog to select a new time in 24-hour format
            Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            TimePickerDialog timePickerDialog = new TimePickerDialog(ParkingManagementActivity.this,
                    (view, selectedHour, selectedMinute) -> {
                        // Format the selected time into "HH:mm" format
                        String newEndTime = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute);
                        newEndTimeEditText.setText(newEndTime);

                        // Parse and compare the new time with the current end time
                        try {
                            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                            Date currentEnd = sdf.parse(currentEndTimeTextView.getText().toString());
                            Date newEnd = sdf.parse(newEndTime);

                            if (newEnd != null && newEnd.after(currentEnd)) {
                                // If the new time is valid (later than current end time), update the end time
                                updateEndTime(newEndTime);  // You should have this method to handle the time update
                                dialog.dismiss();
                            } else {
                                // Show error if the new time is not later than the current end time
                                Toast.makeText(ParkingManagementActivity.this, "Please select a time later than the current end time", Toast.LENGTH_SHORT).show();
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }, hour, minute, true); // 24-hour format (the last argument is true)
            timePickerDialog.show();
        });

        cancelUpdateButton.setOnClickListener(v -> dialog.dismiss());
    }
    private void updateEndTime(String newEndTime) {
        if (parkingId != null) {
            parkingRef.child(parkingId).child("endTime").setValue(newEndTime).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(ParkingManagementActivity.this, "End Time Updated Successfully", Toast.LENGTH_SHORT).show();
                    parkedEndTime.setText(newEndTime);
                } else {
                    Toast.makeText(ParkingManagementActivity.this, "Failed to update end time", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void sendExitParkingMessage() {
        if (userId != null) {
            DatabaseReference messageRef = blockedMessagesRef.child(userId).push(); // Store under this user's ID
            String currentDateTime = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(new Date()); // Current date and time

            // Create a message object with the necessary details
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("message", "You have exited the parking area.");
            messageData.put("createdDateTime", currentDateTime);
            messageData.put("userId", userId); // ID of the user whose parking session ended
            messageData.put("messageType", 5); // Message type for end-of-parking notification

            // Set the value in the database
            messageRef.setValue(messageData).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(ParkingManagementActivity.this, "End of parking message sent successfully.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ParkingManagementActivity.this, "Failed to send end of parking message.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }


    private void exitParking() {
        if (parkingId != null) {
            parkingRef.child(parkingId).child("status").setValue("exit").addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(ParkingManagementActivity.this, "Parking status updated to 'Exited'", Toast.LENGTH_SHORT).show();
                    exitButton.setEnabled(false);

                } else {
                    Toast.makeText(ParkingManagementActivity.this, "Failed to update parking status", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
