package com.example.carparkingpool;

public class ParkingData {
    private String vehicle;
    private String location;
    private String startTime;
    private String endTime;
    private String blockedVehicles;
    private String status;
    private String createdTime;  // New field to store the parking creation time

    // Default constructor required for calls to DataSnapshot.getValue(ParkingData.class)
    public ParkingData() {
    }

    // Constructor with all parameters
    public ParkingData(String vehicle, String location, String startTime, String endTime, String blockedVehicles, String status, String createdTime) {
        this.vehicle = vehicle;
        this.location = location;
        this.startTime = startTime;
        this.endTime = endTime;
        this.blockedVehicles = blockedVehicles;
        this.status = status;
        this.createdTime = createdTime;
    }

    // Getter and Setter methods for each field
    public String getVehicle() {
        return vehicle;
    }

    public void setVehicle(String vehicle) {
        this.vehicle = vehicle;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getBlockedVehicles() {
        return blockedVehicles;
    }

    public void setBlockedVehicles(String blockedVehicles) {
        this.blockedVehicles = blockedVehicles;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(String createdTime) {
        this.createdTime = createdTime;
    }
}
