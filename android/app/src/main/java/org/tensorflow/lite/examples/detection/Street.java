package org.tensorflow.lite.examples.detection;

import com.google.firebase.Timestamp;

public class Street {
    private int AvailableParking;
    private double Capacity;
    private Timestamp LastUpdate;
    private int TotalParking;

    public Street(int AvailableParking, double Capacity,Timestamp LastUpdate,int TotalParking) {
        this.AvailableParking = AvailableParking;
        this.Capacity = Capacity;
        this.LastUpdate = LastUpdate;
        this.TotalParking = TotalParking;

    }

    public Street(){
        //public no-arg constructor needed
    }

    public int getAvailableParking() {
        return AvailableParking;
    }

    public void setAvailableParking(int AvailableParking) {
        this.AvailableParking = AvailableParking;
    }

    public double getCapacity() {
        return Capacity;
    }

    public void setCapacity(double Capacity) {
        this.Capacity = Capacity;
    }

    public Timestamp getLastUpdate() {
        return LastUpdate;
    }

    public void setLastUpdate(Timestamp LastUpdate) {
        this.LastUpdate = LastUpdate;
    }

    public int getTotalParking() {
        return TotalParking;
    }

    public void setTotalParking(int totalParking) {
        this.TotalParking = TotalParking;
    }
}
