package com.example.carparkingpool;

public class Vehicle {

    private String plateNumber;
    private String carModel;

    // Default constructor required for calls to DataSnapshot.getValue(Vehicle.class)
    public Vehicle() {
    }

    public Vehicle(String plateNumber, String carModel) {
        this.plateNumber = plateNumber;
        this.carModel = carModel;
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public void setPlateNumber(String plateNumber) {
        this.plateNumber = plateNumber;
    }

    public String getCarModel() {
        return carModel;
    }

    public void setCarModel(String carModel) {
        this.carModel = carModel;
    }
}
