package com.example.carparkingpool;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class ConfirmDeleteDialog extends DialogFragment {

    private String vehiclePlateNumber;
    private ConfirmDeleteListener listener;

    // Interface to handle confirm and cancel actions
    public interface ConfirmDeleteListener {
        void onConfirmDelete();
        void onCancelDelete();
    }

    public ConfirmDeleteDialog(String vehiclePlateNumber, ConfirmDeleteListener listener) {
        this.vehiclePlateNumber = vehiclePlateNumber;
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dialog_confirm_delete, container, false);

        TextView deleteMessage = view.findViewById(R.id.deleteMessage);
        deleteMessage.setText("Remove Vehicle " + vehiclePlateNumber + "?");

        Button confirmButton = view.findViewById(R.id.confirmButton);
        Button cancelButton = view.findViewById(R.id.cancelButton);

        confirmButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onConfirmDelete();
            }
            dismiss(); // Close the dialog
        });

        cancelButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCancelDelete();
            }
            dismiss(); // Close the dialog
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            // Set the width and height of the dialog
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            // Optionally, set window animations or soft input mode
            getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
    }
}
