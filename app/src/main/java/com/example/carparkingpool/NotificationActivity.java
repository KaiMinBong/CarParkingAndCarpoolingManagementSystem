package com.example.carparkingpool;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import java.util.ArrayList;

public class NotificationActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private ArrayList<BlockedMessage> blockedMessageList;
    private DatabaseReference blockedMessagesRef;
    private FirebaseAuth mAuth;
    private Button clearButton;  // Add clear button reference

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Get reference to the blockedMessages node in Firebase
        blockedMessagesRef = FirebaseDatabase.getInstance().getReference("blockedMessages");

        // Initialize the RecyclerView
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize the blocked message list and adapter
        blockedMessageList = new ArrayList<>();
        adapter = new NotificationAdapter(this, blockedMessageList);
        recyclerView.setAdapter(adapter);

        // Fetch and display blocked messages for the logged-in user
        fetchBlockedMessagesForUser();

        // Initialize Clear Button and set the click listener
        clearButton = findViewById(R.id.clearButton);
        clearButton.setOnClickListener(v -> clearBlockedMessagesForUser());
    }

    private void fetchBlockedMessagesForUser() {
        String currentUserId = mAuth.getCurrentUser().getUid();

        // Query Firebase to get the blocked messages for the logged-in user
        blockedMessagesRef.child(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                blockedMessageList.clear(); // Clear the list before adding new items

                // Loop through all the blocked messages for this user
                for (DataSnapshot messageSnapshot : dataSnapshot.getChildren()) {
                    BlockedMessage message = messageSnapshot.getValue(BlockedMessage.class);
                    if (message != null) {
                        blockedMessageList.add(0,message); // Add the message to the list
                    }
                }

                // Notify the adapter of data changes
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(NotificationActivity.this, "Failed to load messages.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void clearBlockedMessagesForUser() {
        String currentUserId = mAuth.getCurrentUser().getUid();

        // Delete all blocked messages for the logged-in user
        blockedMessagesRef.child(currentUserId).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                blockedMessageList.clear(); // Clear the local list of messages
                adapter.notifyDataSetChanged(); // Notify adapter of data changes
                Toast.makeText(NotificationActivity.this, "All notifications cleared.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(NotificationActivity.this, "Failed to clear notifications.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
