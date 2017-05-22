package com.lucasbritz.walkhelper;

import java.util.ArrayList;

/**
 * Created by Lucas Britz on 22/05/2017.
 */

public class Beacon {
    private Long id;
    private double latitude;
    private double longitude;
    private String location;
    private ArrayList<Beacon> neighborhood;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public ArrayList<Beacon> getNeighborhood() {
        return neighborhood;
    }

    public void setNeighborhood(ArrayList<Beacon> neighborhood) {
        this.neighborhood = neighborhood;
    }
}
