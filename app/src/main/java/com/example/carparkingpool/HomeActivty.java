package com.example.carparkingpool;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import android.widget.ProgressBar; // Import ProgressBar

public class HomeActivty extends AppCompatActivity {
    private ProgressBar progressBarP2, progressBarP1, progressBarPG;
    private TextView p2Status, p1Status, pgStatus;

    private DatabaseReference parkingRef,vehiclesRef;

    // Set the max capacities for each location
    private final int MAX_P1 = 205;
    private final int MAX_P2 = 100;
    private final int MAX_PG = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_activty);

        // Initialize progress bars and text views
        progressBarP2 = findViewById(R.id.progressBarP2);
        progressBarP1 = findViewById(R.id.progressBarP1);
        progressBarPG = findViewById(R.id.progressBarPG);

        p2Status = findViewById(R.id.p2Status);
        p1Status = findViewById(R.id.p1Status);
        pgStatus = findViewById(R.id.pgStatus);

        // Set maximum capacities for progress bars
        progressBarP1.setMax(MAX_P1);
        progressBarP2.setMax(MAX_P2);
        progressBarPG.setMax(MAX_PG);

        // Initialize Firebase Database reference for parking
        parkingRef = FirebaseDatabase.getInstance().getReference("parking");
        vehiclesRef = FirebaseDatabase.getInstance().getReference("vehicles");
        // Count and update parking vacancy based on Firebase data
        countParkingForLocations();
    }

    // This method is triggered when any card is clicked
    public void onCardClick(View view) {
        Intent intent = null;

        if (view.getId() == R.id.carPark) {
            intent = new Intent(HomeActivty.this, ParkActivity.class);
        } else if (view.getId() == R.id.carCarpool) {
            intent = new Intent(HomeActivty.this, CarpoolActivity.class);
        } else if (view.getId() == R.id.carParkingManagement) {
            intent = new Intent(HomeActivty.this, ParkingManagementActivity.class);
        } else if (view.getId() == R.id.carVehicleManagement) {
            intent = new Intent(HomeActivty.this, VehicleManagementActivity.class);
        } else if (view.getId() == R.id.cardSearchVehicle) {
            showSearchVehicleDialog();
            return;
        } else if (view.getId() == R.id.cardPeakTime) {
            intent = new Intent(HomeActivty.this, PeakTimeActivity.class);
        } else if (view.getId() == R.id.cardNotification) {
            intent = new Intent(HomeActivty.this, NotificationActivity.class);
        } else if (view.getId() == R.id.cardProfile) {
            intent = new Intent(HomeActivty.this, ProfileActivity.class);
        }

        if (intent != null) {
            startActivity(intent);
        }
    }

    // Method to count the parked vehicles per location (P1, P2, PG)
    private void countParkingForLocations() {
        parkingRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int p1Count = 0;
                int p2Count = 0;
                int pgCount = 0;

                // Loop through all users' parking data
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    for (DataSnapshot parkingSnapshot : userSnapshot.getChildren()) {
                        String location = parkingSnapshot.child("location").getValue(String.class);
                        String status = parkingSnapshot.child("status").getValue(String.class);

                        if (status != null && status.equals("parked")) {
                            if (location != null) {
                                switch (location) {
                                    case "P1":
                                        p1Count++;
                                        break;
                                    case "P2":
                                        p2Count++;
                                        break;
                                    case "PG":
                                        pgCount++;
                                        break;
                                }
                            }
                        }
                    }
                }

                // Update progress bars with the count
                updateParkingVacancy(p2Count, p1Count, pgCount);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(HomeActivty.this, "Failed to load parking data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // This method updates the progress bars with the number of parked vehicles per location
    private void updateParkingVacancy(int p2, int p1, int pg) {

        // Ensure p2 does not exceed the maximum allowed value
        p2 = Math.min(p2, MAX_P2);
        p1 = Math.min(p1, MAX_P1);
        pg = Math.min(pg, MAX_PG);

        // Update P2 progress bar and status
        progressBarP2.setProgress(p2);
        p2Status.setText(String.format("%d/%d", p2, MAX_P2));

        // Update P1 progress bar and status
        progressBarP1.setProgress(p1);
        p1Status.setText(String.format("%d/%d", p1, MAX_P1));

        // Update PG progress bar and status
        progressBarPG.setProgress(pg);
        pgStatus.setText(String.format("%d/%d", pg, MAX_PG));
    }

    // Show the search vehicle dialog
    private void showSearchVehicleDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View searchVehicleView = inflater.inflate(R.layout.dialog_search_vehicle, null);

        EditText editTextVehiclePlate = searchVehicleView.findViewById(R.id.editTextVehiclePlate);
        Button buttonSearchVehicle = searchVehicleView.findViewById(R.id.buttonSearchVehicle);
        Button buttonCancel = searchVehicleView.findViewById(R.id.cancelButton);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(searchVehicleView);
        AlertDialog searchDialog = builder.create();
        searchDialog.show();

        buttonSearchVehicle.setOnClickListener(v -> {
            String plateNumber = editTextVehiclePlate.getText().toString().trim();
            if (plateNumber.isEmpty()) {
                Toast.makeText(HomeActivty.this, "Please enter a plate number", Toast.LENGTH_SHORT).show();
            } else {
                searchVehicleByPlate(plateNumber);
                searchDialog.dismiss();
            }
        });

        buttonCancel.setOnClickListener(v -> searchDialog.dismiss());
    }

    // Modify vehiclesRef to refer to parkingRef for correct vehicle searching
    private void searchVehicleByPlate(String plateNumber) {
        vehiclesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean vehicleFound = false;
                String userId = null;

                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    for (DataSnapshot vehicleSnapshot : userSnapshot.getChildren()) {
                        String vehiclePlate = vehicleSnapshot.child("plateNumber").getValue(String.class);

                        if (vehiclePlate != null && vehiclePlate.equals(plateNumber)) {
                            userId = userSnapshot.getKey();
                            vehicleFound = true;
                            break;
                        }
                    }

                    if (vehicleFound) break;
                }

                if (vehicleFound && userId != null) {
                    getUserPhoneNumber(userId, plateNumber);
                } else {
                    Toast.makeText(HomeActivty.this, "Vehicle not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(HomeActivty.this, "Failed to search for vehicle", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getUserPhoneNumber(String userId, String vehiclePlate) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String phoneNumber = dataSnapshot.child("phone").getValue(String.class);
                    showVehicleInfoDialog(vehiclePlate, phoneNumber);
                } else {
                    Toast.makeText(HomeActivty.this, "User not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(HomeActivty.this, "Failed to get user phone number", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showVehicleInfoDialog(String vehiclePlate, String phoneNumber) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View vehicleInfoView = inflater.inflate(R.layout.dialog_vehicle_info, null);

        TextView textViewCarPlate = vehicleInfoView.findViewById(R.id.textViewCarPlate);
        TextView textViewPhoneNumber = vehicleInfoView.findViewById(R.id.textViewPhoneNumber);
        Button buttonCloseInfo = vehicleInfoView.findViewById(R.id.buttonCloseInfo);

        textViewCarPlate.setText("Car Plate: " + vehiclePlate);
        textViewPhoneNumber.setText("Phone Number: " + phoneNumber);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(vehicleInfoView);
        AlertDialog infoDialog = builder.create();
        infoDialog.show();

        buttonCloseInfo.setOnClickListener(v -> infoDialog.dismiss());
    }
}
