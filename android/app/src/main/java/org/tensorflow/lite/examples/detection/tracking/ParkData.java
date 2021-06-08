package org.tensorflow.lite.examples.detection.tracking;

import com.google.firebase.Timestamp;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ParkData { //Names of values must be exactly the same as they appear in Firebase
    private int Available;
    private double Latitude;
    private double Longitude;
    private int PotentialParkingSpots;
    private Timestamp Time;

    public ParkData(int Available,double Latitude, double Longitude,int PotentialParkingSpots, Timestamp Time) {
        this.Available = Available;
        this.Latitude = Latitude;
        this.Longitude = Longitude;
        this.PotentialParkingSpots=PotentialParkingSpots;
        this.Time = Time;
    }

    public String DataToString(){
        Date date = Time.toDate();
        DateFormat dateFormat= new SimpleDateFormat("MM-dd, hh:mm a");
        String strDate = dateFormat.format(date);
        String CompleteString="Available: "+ Available +"; Time: "+strDate;
        return CompleteString;
    }

    public ParkData(){
        //public no-arg constructor needed
    }

    public int getPotentialParkingSpots() {
        return PotentialParkingSpots;
    }

    public void setPotentialParkingSpots(int potentialParkingSpots) {
        PotentialParkingSpots = potentialParkingSpots;
    }

    public double getLatitude() {
        return Latitude;
    }

    public void setLatitude(double Latitude) {
        this.Latitude = Latitude;
    }

    public double getLongitude() {
        return Longitude;
    }

    public void setLongitude(double Longitude) {
        this.Longitude = Longitude;
    }

    public int getAvailable() {
        return Available;
    }

    public void setAvailable(int Available) {
        this.Available = Available;
    }

    public Timestamp getTime() {
        return Time;
    }

    public void setTime(Timestamp Time) {
        this.Time = Time;
    }
}
