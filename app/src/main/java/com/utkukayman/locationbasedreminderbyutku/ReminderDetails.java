package com.utkukayman.locationbasedreminderbyutku;

import com.google.android.gms.maps.model.LatLng;

import java.util.UUID;

public class ReminderDetails {
    public String id;
    public LatLng latLng;
    public Double radius;
    public String message;

    public ReminderDetails(LatLng latLng, Double radius, String message) {

        this.id = UUID.randomUUID().toString();
        this.latLng = latLng;
        this.radius = radius;
        this.message = message;

    }
}
