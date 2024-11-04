package com.example.carparkingpool;

public class ParkingData {
    private String vehicle;
    private String location;
    private String startTime;
    private String endTime;

    // Default constructor required for Firebase
    public ParkingData() {
    }

    // Parameterized constructor
    public ParkingData(String vehicle, String location, String startTime, String endTime) {
        this.vehicle = vehicle;
        this.location = location;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    // Getter and Setter methods
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
}
