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

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class UpdateVehicleDialog extends DialogFragment {

    private EditText carPlateNumberInput, carModelInput;
    private Button confirmButton, cancelButton;

    // Firebase Database reference
    private DatabaseReference databaseReference;
    private String vehicleId;
    private String userId;
    private String initialPlateNumber;
    private String initialCarModel;

    // Constructor to pass vehicle data to the dialog
    public UpdateVehicleDialog(String vehicleId, String userId, String initialPlateNumber, String initialCarModel) {
        this.vehicleId = vehicleId;
        this.userId = userId;
        this.initialPlateNumber = initialPlateNumber;
        this.initialCarModel = initialCarModel;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_update_vehicle_dialog, container, false);

        carPlateNumberInput = view.findViewById(R.id.carPlateNumberInput);
        carModelInput = view.findViewById(R.id.carModelInput);
        confirmButton = view.findViewById(R.id.confirmButton);
        cancelButton = view.findViewById(R.id.cancelButton);

        // Pre-fill the dialog with the current vehicle data
        carPlateNumberInput.setText(initialPlateNumber);
        carModelInput.setText(initialCarModel);

        databaseReference = FirebaseDatabase.getInstance().getReference("vehicles").child(userId).child(vehicleId);

        confirmButton.setOnClickListener(v -> {
            String updatedPlateNumber = carPlateNumberInput.getText().toString().trim();
            String updatedCarModel = carModelInput.getText().toString().trim();

            if (!updatedPlateNumber.isEmpty() && !updatedCarModel.isEmpty()) {
                // Update vehicle data in the database
                updateVehicleInDatabase(updatedPlateNumber, updatedCarModel);
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

    // Method to update vehicle information in Firebase Realtime Database
    private void updateVehicleInDatabase(String updatedPlateNumber, String updatedCarModel) {
        databaseReference.child("plateNumber").setValue(updatedPlateNumber);
        databaseReference.child("carModel").setValue(updatedCarModel).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getActivity(), "Vehicle updated successfully", Toast.LENGTH_SHORT).show();
                dismiss();
            } else {
                Toast.makeText(getActivity(), "Failed to update vehicle", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
