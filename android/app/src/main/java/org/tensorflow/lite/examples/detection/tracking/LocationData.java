package org.tensorflow.lite.examples.detection.tracking;

public class LocationData {
    int id;
    private String city;
    private String street;
    private double latitude;
    private double longitude;
    private String time;
    private float distanceBetween2Points;
    private int parking_status;
    private String time_detector;
    private float accuracy_in_meters;

    public LocationData() {
        this.id = id;
        this.city=city;
        this.street=street;
        this.latitude = latitude;
        this.longitude = longitude;
        this.time = time;
        this.distanceBetween2Points = distanceBetween2Points;
        this.parking_status = parking_status;
        this.time_detector = time_detector;
        this.accuracy_in_meters = accuracy_in_meters;
    }

    public LocationData(int id,String city, String street, double latitude, double longitude, String time, float distanceBetween2Points,  int parking_status , String time_detector, float accuracy_in_meters) {
        this.id = id;
        this.city=city;
        this.street=street;
        this.latitude = latitude;
        this.longitude = longitude;
        this.time = time;
        this.distanceBetween2Points = distanceBetween2Points;
        this.parking_status = parking_status;
        this.time_detector = time_detector;
        this.accuracy_in_meters = accuracy_in_meters;


    }


    @Override
    public String toString() {
        return id + ". (" + latitude + ", " + longitude + ")   " + time.toString() + "  ||" + distanceBetween2Points + "||"; ///change time to date format
    }
    public String Coordinates() {
        return "(" + latitude + ", " + longitude + ")";
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
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

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public float getDistanceBetween2Points() {
        return distanceBetween2Points;
    }

    public void setDistanceBetween2Points(float distanceBetween2Points) {
        this.distanceBetween2Points = distanceBetween2Points;

    }

    public String getTime_detector() {
        return time_detector;
    }

    public void setTime_detector(String time_detector) {
        this.time_detector = time_detector;
    }

    public int getParking_status() {
        return parking_status;
    }

    public float getAccuracy_in_meters() {
        return accuracy_in_meters;
    }

    public void setAccuracy_in_meters(float accuracy_in_meters) {
        this.accuracy_in_meters = accuracy_in_meters;
    }

    public void setParking_status(int parking_status) {
        this.parking_status = parking_status;
    }
}
