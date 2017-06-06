package com.lucasbritz.walkhelper;

import java.util.ArrayList;

/**
 * Created by Lucas Britz on 22/05/2017.
 */

public class Beacon {
    private String address;
    private double latitude;
    private double longitude;
    private String description;
    private int floorLevel;
    private ArrayList<String> neighborhood;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getFloorLevel() {
        return floorLevel;
    }

    public void setFloorLevel(int floorLevel) {
        this.floorLevel = floorLevel;
    }

    public ArrayList<String> getNeighborhood() {
        return neighborhood;
    }

    public void setNeighborhood(ArrayList<String> neighborhood) {
        this.neighborhood = neighborhood;
    }
}
