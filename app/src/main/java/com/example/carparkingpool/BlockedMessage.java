package com.example.carparkingpool;

public class BlockedMessage {

    private String blockedVehicle;
    private String selectedVehicle; // Add this field
    private String createdDateTime;
    private String message;
    private int messageType;
    private String userId;

    // Default constructor (required for Firebase)
    public BlockedMessage() {
    }

    // Constructor with parameters
    public BlockedMessage(String blockedVehicle, String selectedVehicle, String createdDateTime, String message, int messageType, String userId) {
        this.blockedVehicle = blockedVehicle;
        this.selectedVehicle = selectedVehicle;  // Initialize selectedVehicle
        this.createdDateTime = createdDateTime;
        this.message = message;
        this.messageType = messageType;
        this.userId = userId;
    }

    // Getter and Setter methods
    public String getBlockedVehicle() {
        return blockedVehicle;
    }

    public void setBlockedVehicle(String blockedVehicle) {
        this.blockedVehicle = blockedVehicle;
    }

    public String getSelectedVehicle() {
        return selectedVehicle;
    }

    public void setSelectedVehicle(String selectedVehicle) {
        this.selectedVehicle = selectedVehicle;
    }

    public String getCreatedDateTime() {
        return createdDateTime;
    }

    public void setCreatedDateTime(String createdDateTime) {
        this.createdDateTime = createdDateTime;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getMessageType() {
        return messageType;
    }

    public void setMessageType(int messageType) {
        this.messageType = messageType;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
