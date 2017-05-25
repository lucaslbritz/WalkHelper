package com.lucasbritz.walkhelper;

import java.util.ArrayList;

/**
 * Created by Lucas Britz on 22/05/2017.
 */

public class Beacon {
    private String id;
    private double latitude;
    private double longitude;
    private String description;
    private int floorLevel;
    private boolean active;
    private ArrayList<Beacon> neighborhood;

    public Beacon() {
        active = true;
    }

    public Beacon(String id, double latitude, double longitude,
                  String description, int floorLevel, boolean active) {

        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.description = description;
        this.floorLevel = floorLevel;
        this.active = active;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public ArrayList<Beacon> getNeighborhood() {
        return neighborhood;
    }

    public void setNeighborhood(ArrayList<Beacon> neighborhood) {
        this.neighborhood = neighborhood;
    }
}
