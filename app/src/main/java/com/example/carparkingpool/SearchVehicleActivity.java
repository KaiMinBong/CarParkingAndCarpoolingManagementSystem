package com.example.carparkingpool;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SearchVehicleActivity extends AppCompatActivity {

    private DatabaseReference vehiclesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_vehicle);

        // Initialize Firebase Database reference for vehicles
        vehiclesRef = FirebaseDatabase.getInstance().getReference("parking");

        // Show the search vehicle dialog when the activity starts
        showSearchVehicleDialog();
    }

    private void showSearchVehicleDialog() {
        // Inflate the search vehicle dialog layout
        LayoutInflater inflater = LayoutInflater.from(this);
        View searchVehicleView = inflater.inflate(R.layout.dialog_search_vehicle, null);

        // Initialize dialog components
        EditText editTextVehiclePlate = searchVehicleView.findViewById(R.id.editTextVehiclePlate);
        Button buttonSearchVehicle = searchVehicleView.findViewById(R.id.buttonSearchVehicle);

        // Create and show the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(searchVehicleView);
        AlertDialog searchDialog = builder.create();
        searchDialog.show();

        // Set the search button click listener
        buttonSearchVehicle.setOnClickListener(v -> {
            String plateNumber = editTextVehiclePlate.getText().toString().trim();
            if (plateNumber.isEmpty()) {
                Toast.makeText(SearchVehicleActivity.this, "Please enter a plate number", Toast.LENGTH_SHORT).show();
            } else {
                searchVehicleByPlate(plateNumber);
                searchDialog.dismiss(); // Close the search dialog after the search
            }
        });
    }

    private void searchVehicleByPlate(String plateNumber) {
        // Search the entire "parking" table for a vehicle with the specified plate number
        vehiclesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean vehicleFound = false; // Flag to check if vehicle is found
                String userId = null; // To store the userId associated with the vehicle

                // Loop through all users and their parked vehicles
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    for (DataSnapshot vehicleSnapshot : userSnapshot.getChildren()) {
                        String vehiclePlate = vehicleSnapshot.child("vehicle").getValue(String.class);

                        if (vehiclePlate != null && vehiclePlate.equals(plateNumber)) {
                            // Vehicle found, get the associated user's UID
                            userId = userSnapshot.getKey();
                            vehicleFound = true;
                            break;  // Exit loop once vehicle is found
                        }
                    }

                    if (vehicleFound) break;  // Stop searching if vehicle is found
                }

                if (vehicleFound && userId != null) {
                    // If vehicle is found, retrieve the phone number from the "users" table
                    getUserPhoneNumber(userId, plateNumber);
                } else {
                    Toast.makeText(SearchVehicleActivity.this, "Vehicle not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(SearchVehicleActivity.this, "Failed to search for vehicle", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getUserPhoneNumber(String userId, String vehiclePlate) {
        // Reference to the users table
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

        // Get the phone number from the user's profile
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String phoneNumber = dataSnapshot.child("phone").getValue(String.class);
                    showVehicleInfoDialog(vehiclePlate, phoneNumber);
                } else {
                    Toast.makeText(SearchVehicleActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(SearchVehicleActivity.this, "Failed to get user phone number", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showVehicleInfoDialog(String vehiclePlate, String phoneNumber) {
        // Inflate the vehicle info dialog layout
        LayoutInflater inflater = LayoutInflater.from(this);
        View vehicleInfoView = inflater.inflate(R.layout.dialog_vehicle_info, null);

        // Initialize dialog components
        TextView textViewCarPlate = vehicleInfoView.findViewById(R.id.textViewCarPlate);
        TextView textViewPhoneNumber = vehicleInfoView.findViewById(R.id.textViewPhoneNumber);
        Button buttonCloseInfo = vehicleInfoView.findViewById(R.id.buttonCloseInfo);

        // Set the vehicle info
        textViewCarPlate.setText("Car Plate: " + vehiclePlate);
        textViewPhoneNumber.setText("Phone Number: " + phoneNumber);

        // Create and show the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(vehicleInfoView);
        AlertDialog infoDialog = builder.create();
        infoDialog.show();

        // Set the close button click listener
        buttonCloseInfo.setOnClickListener(v -> infoDialog.dismiss());
    }
}
