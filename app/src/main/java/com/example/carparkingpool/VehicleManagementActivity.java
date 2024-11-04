package com.example.carparkingpool;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class VehicleManagementActivity extends AppCompatActivity {

    private Button addButton;
    private LinearLayout vehicleListContainer;

    // Firebase Database reference
    private DatabaseReference vehicleRef;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vehicle_management);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        vehicleListContainer = findViewById(R.id.vehicleListContainer);
        addButton = findViewById(R.id.addButton);

        // If the user is logged in, retrieve their vehicles
        if (currentUser != null) {
            String userId = currentUser.getUid();
            vehicleRef = FirebaseDatabase.getInstance().getReference("vehicles").child(userId);
            loadUserVehicles();
        }

        // Open the Add Vehicle Dialog when the Add button is clicked
        addButton.setOnClickListener(v -> {
            AddVehicleDialog dialog = new AddVehicleDialog();
            dialog.show(getSupportFragmentManager(), "AddVehicleDialog");
        });
    }

    // Method to load vehicles of the logged-in user from Firebase
    private void loadUserVehicles() {
        vehicleRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                vehicleListContainer.removeAllViews(); // Clear previous entries

                // Iterate over all vehicles and add them to the UI
                for (DataSnapshot vehicleSnapshot : dataSnapshot.getChildren()) {
                    String vehicleId = vehicleSnapshot.getKey(); // Get vehicle ID
                    String plateNumber = vehicleSnapshot.child("plateNumber").getValue(String.class);
                    String carModel = vehicleSnapshot.child("carModel").getValue(String.class);

                    // Dynamically add a card for each vehicle
                    addVehicleCard(plateNumber, carModel, vehicleId);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(VehicleManagementActivity.this, "Failed to load vehicles", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Method to dynamically add a vehicle card to the UI
    private void addVehicleCard(String plateNumber, String carModel, String vehicleId) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View vehicleCardView = inflater.inflate(R.layout.vehicle_card_item, vehicleListContainer, false);

        TextView plateNumberView = vehicleCardView.findViewById(R.id.plateNumber);
        TextView carModelView = vehicleCardView.findViewById(R.id.carModel);
        ImageView editButton = vehicleCardView.findViewById(R.id.editVehicle); // Change to ImageView
        ImageView deleteButton = vehicleCardView.findViewById(R.id.deleteVehicle); // Change to ImageView

        plateNumberView.setText(plateNumber);
        carModelView.setText(carModel);

        // Set the edit button functionality
        editButton.setOnClickListener(v -> UpdateVehicleCard(plateNumber, carModel, vehicleId));

        // Set the delete button functionality with confirmation
        deleteButton.setOnClickListener(v -> deleteVehicle(vehicleId, plateNumber));

        vehicleListContainer.addView(vehicleCardView);
    }

    private void UpdateVehicleCard(String plateNumber, String carModel, String vehicleId) {
        // Open UpdateVehicleDialog to update the vehicle
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            UpdateVehicleDialog updateDialog = new UpdateVehicleDialog(vehicleId, userId, plateNumber, carModel);
            updateDialog.show(getSupportFragmentManager(), "UpdateVehicleDialog");
        }
    }

    // Method to delete a vehicle from Firebase with confirmation dialog
    private void deleteVehicle(String vehicleId, String plateNumber) {
        // Show the custom confirmation dialog
        ConfirmDeleteDialog confirmDialog = new ConfirmDeleteDialog(plateNumber, new ConfirmDeleteDialog.ConfirmDeleteListener() {
            @Override
            public void onConfirmDelete() {
                // Proceed with the deletion if confirmed
                vehicleRef.child(vehicleId).removeValue().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(VehicleManagementActivity.this, "Vehicle deleted successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(VehicleManagementActivity.this, "Failed to delete vehicle", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelDelete() {
                // Handle cancel action if needed (optional)
                Toast.makeText(VehicleManagementActivity.this, "Deletion canceled", Toast.LENGTH_SHORT).show();
            }
        });

        confirmDialog.show(getSupportFragmentManager(), "ConfirmDeleteDialog");
    }

}
