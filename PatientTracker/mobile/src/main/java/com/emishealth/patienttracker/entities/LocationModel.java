package com.emishealth.patienttracker.entities;

/**
 * Created by Dayana Kanagaraj on 23-Apr-17.
 */

public class LocationModel {
    private String latitude;
    private String longitude;
    private String deviceId;
    private String timeObserved;

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getTimeObserved() {
        return timeObserved;
    }

    public void setTimeObserved(String timeObserved) {
        this.timeObserved = timeObserved;
    }
}
