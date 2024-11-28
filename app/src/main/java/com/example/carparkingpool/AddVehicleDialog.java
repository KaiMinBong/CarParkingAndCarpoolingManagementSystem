package com.example.carparkingpool;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AddVehicleDialog extends DialogFragment {

    private EditText carPlateNumberInput, carModelInput;
    private Button confirmButton, cancelButton;

    // Firebase Database reference
    private DatabaseReference databaseReference;

    // Firebase Authentication instance
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dialog_add_vehicle, container, false);

        // Initialize Firebase Authentication and Database reference
        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("vehicles");

        carPlateNumberInput = view.findViewById(R.id.carPlateNumberInput);
        carModelInput = view.findViewById(R.id.carModelInput);
        confirmButton = view.findViewById(R.id.confirmButton);
        cancelButton = view.findViewById(R.id.cancelButton);

        confirmButton.setOnClickListener(v -> {
            String plateNumber = carPlateNumberInput.getText().toString().trim();
            String carModel = carModelInput.getText().toString().trim();

            if (!plateNumber.isEmpty() && !carModel.isEmpty()) {
                // Check for duplicate plate numbers across all users
                checkDuplicateAndSave(plateNumber, carModel);
            } else {
                Toast.makeText(getActivity(), "Please enter all fields", Toast.LENGTH_SHORT).show();
            }
        });

        cancelButton.setOnClickListener(v -> dismiss());

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            // Set the width and height of the dialog
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            // Set window animations or soft input mode
            getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
    }

    // Method to check for duplicate plate numbers and save if none are found
    private void checkDuplicateAndSave(String plateNumber, String carModel) {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                boolean duplicateFound = false;

                // Iterate through all users' vehicles
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    for (DataSnapshot vehicleSnapshot : userSnapshot.getChildren()) {
                        String existingPlateNumber = vehicleSnapshot.child("plateNumber").getValue(String.class);

                        // Check if the plate number matches
                        if (existingPlateNumber != null && existingPlateNumber.equalsIgnoreCase(plateNumber)) {
                            duplicateFound = true;
                            break;
                        }
                    }
                    if (duplicateFound) break;
                }

                if (duplicateFound) {
                    Toast.makeText(getActivity(), "Vehicle with this plate number already exists", Toast.LENGTH_SHORT).show();
                } else {
                    // No duplicate found, save the vehicle
                    saveVehicleToDatabase(plateNumber, carModel);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getActivity(), "Error checking for duplicates: " + databaseError.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }


    // Method to save vehicle information to Firebase Realtime Database
    private void saveVehicleToDatabase(String plateNumber, String carModel) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Create a unique key for each vehicle
            String vehicleId = databaseReference.child(currentUser.getUid()).push().getKey();

            // Create a Vehicle object
            Vehicle vehicle = new Vehicle(plateNumber, carModel);

            // Save the vehicle to Firebase under the user's UID and the generated unique vehicle ID
            if (vehicleId != null) {
                databaseReference.child(currentUser.getUid()).child(vehicleId).setValue(vehicle).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getActivity(), "Vehicle Added Successfully", Toast.LENGTH_SHORT).show();
                        dismiss(); // Close the dialog
                    } else {
                        Toast.makeText(getActivity(), "Failed to add vehicle", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } else {
            Toast.makeText(getActivity(), "User not logged in", Toast.LENGTH_SHORT).show();
        }
    }
}

