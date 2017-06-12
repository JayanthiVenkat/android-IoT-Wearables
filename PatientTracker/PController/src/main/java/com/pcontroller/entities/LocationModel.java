package com.pcontroller.entities;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Dayana Kanagaraj on 23-Apr-17.
 */

public class LocationModel  implements Parcelable{
    private String latitude;
    private String longitude;
    private String deviceId;
    private String timeObserved;
    private String deviceName;

    public LocationModel(){}
    public LocationModel(Parcel in) {
        latitude = in.readString();
        longitude = in.readString();
        deviceId = in.readString();
        timeObserved = in.readString();
        deviceName = in.readString();
    }

    public static final Creator<LocationModel> CREATOR = new Creator<LocationModel>() {
        @Override
        public LocationModel createFromParcel(Parcel in) {
            return new LocationModel(in);
        }

        @Override
        public LocationModel[] newArray(int size) {
            return new LocationModel[size];
        }
    };

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

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(latitude);
        dest.writeString(longitude);
        dest.writeString(deviceId);
        dest.writeString(timeObserved);
        dest.writeString(deviceName);
    }
}
