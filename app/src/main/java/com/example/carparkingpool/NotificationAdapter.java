package com.example.carparkingpool;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    FirebaseUser currentUser = mAuth.getCurrentUser();
    String loggedInUserId = currentUser != null ? currentUser.getUid() : null;
    private Context context;
    private ArrayList<BlockedMessage> blockedMessageList;
    private DatabaseReference usersRef;

    // Constructor
    public NotificationAdapter(Context context, ArrayList<BlockedMessage> blockedMessageList) {
        this.context = context;
        this.blockedMessageList = blockedMessageList;
        this.usersRef = FirebaseDatabase.getInstance().getReference("users"); // Assuming users node in Firebase
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BlockedMessage message = blockedMessageList.get(position);

        // Bind the message data to the views
        holder.messageText.setText(message.getMessage());
        holder.blockedVehicleText.setText("Created Time: " + message.getCreatedDateTime());

        // Set an onClickListener for item click
        holder.itemView.setOnClickListener(v -> {
            // Show dialog based on the messageType
            if (message.getMessageType() == 1) {
                showCustomDialogType1(message);
            } else if (message.getMessageType() == 2) {
                showCustomDialogType2(message);
            }
        });
    }

    @Override
    public int getItemCount() {
        return blockedMessageList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView messageText, blockedVehicleText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            messageText = itemView.findViewById(R.id.messageText);
            blockedVehicleText = itemView.findViewById(R.id.blockedVehicleText);
        }
    }

    // Show custom Dialog Type 1
// Show custom Dialog Type 1
    private void showCustomDialogType1(BlockedMessage message) {
        // Create a custom dialog
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); // Removes the default dialog title
        dialog.setContentView(R.layout.custom_dialog_type_1); // Inflate custom layout

        // Set the dialog views
        TextView vehicleText = dialog.findViewById(R.id.vehicleText);
        TextView contactText = dialog.findViewById(R.id.contactText);
        Button notifyButton = dialog.findViewById(R.id.notifyButton);

        // Set dialog data - Display the blocked vehicle
        vehicleText.setText("BLOCKED by (" + message.getSelectedVehicle() + ")");

        // Fetch the contact information based on userId
        fetchContactInfo(message.getUserId(), contactText);  // This will fetch and display the phone number or contact info

        // Handle the Notify button click
        notifyButton.setOnClickListener(v -> {
            BlockedMessage newMessage = new BlockedMessage(
                    message.getBlockedVehicle(),            // The blocked vehicle
                    message.getSelectedVehicle(),           // The selected vehicle
                    message.getCreatedDateTime(),           // Created date-time
                    "Please move your vehicle.",     // Message for Notify button
                    2,                                      // Use the same message type
                    loggedInUserId                          // Set the current logged-in user as userId
            );

            // Save the BlockedMessage under message.getUserId()
            saveBlockedMessageToFirebase(newMessage, message.getUserId());

            // Show a confirmation message and dismiss the dialog
            Toast.makeText(context, "Notify button clicked and message saved.", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });


        dialog.show(); // Display the dialog
    }


    // Show custom Dialog Type 2
// Show custom Dialog Type 2
    private void showCustomDialogType2(BlockedMessage message) {
        // Create a custom dialog
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); // Removes the default dialog title
        dialog.setContentView(R.layout.custom_dialog_type_2); // Inflate custom layout

        // Set the dialog views
        TextView vehicleText = dialog.findViewById(R.id.vehicleText);
        TextView contactText = dialog.findViewById(R.id.contactText);
        Button notifyButton = dialog.findViewById(R.id.notifyButton);

        // Set dialog data - Display the selected vehicle
        vehicleText.setText("BLOCKING   (" + message.getBlockedVehicle() + ")");

        // Fetch the contact information based on userId
        fetchContactInfo(message.getUserId(), contactText);  // This will fetch and display the phone number or contact info

        // Handle the Notify button click
        notifyButton.setOnClickListener(v -> {
            BlockedMessage newMessage = new BlockedMessage(
                    message.getBlockedVehicle(),            // The blocked vehicle
                    message.getSelectedVehicle(),           // The selected vehicle
                    message.getCreatedDateTime(),           // Created date-time
                    "On my way to move my vehicle.", // Message for Notify button
                    3,                                      // Use the same message type
                    message.getUserId()                          // Set the current logged-in user as userId
            );

            // Save the BlockedMessage under message.getUserId()
            saveBlockedMessageToFirebase(newMessage, message.getUserId());

            // Show a confirmation message and dismiss the dialog
            Toast.makeText(context, "Notify button clicked and message saved.", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });


        dialog.show(); // Display the dialog
    }


    // Fetch contact information from Firebase based on userId
    private void fetchContactInfo(String userId, TextView contactText) {
        usersRef.child(userId).child("phone").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String contact = dataSnapshot.getValue(String.class);
                    contactText.setText(contact); // Set the fetched contact information
                } else {
                    contactText.setText("Contact info not available");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                contactText.setText("Error loading contact info");
            }
        });
    }
    private void saveBlockedMessageToFirebase(BlockedMessage blockedMessage, String targetUserId) {
        // Get the reference to the "blockedMessages" node in Firebase
        DatabaseReference blockedMessagesRef = FirebaseDatabase.getInstance().getReference("blockedMessages");

        // Generate a unique key for the new message
        String messageId = blockedMessagesRef.push().getKey();

        if (messageId != null) {
            // Save the new BlockedMessage under the target user's ID (message.getUserId())
            blockedMessagesRef.child(targetUserId).child(messageId)
                    .setValue(blockedMessage)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(context, "Message saved successfully.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, "Failed to save message.", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

}
