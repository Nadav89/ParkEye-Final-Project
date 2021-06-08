package org.tensorflow.lite.examples.detection.tracking;

public class DetectorResult {
    int id;
    private int parking_status;
    private String time_detector;
    private long lastProcessingTimeMs;

    public DetectorResult() {
        this.parking_status = 0;
        this.time_detector = "0";
        this.lastProcessingTimeMs=0;
    }

    public DetectorResult(DetectorResult detectorResult) {
        this.id = detectorResult.id;
        this.parking_status = detectorResult.parking_status;
        this.time_detector = detectorResult.time_detector;
        this.lastProcessingTimeMs=detectorResult.lastProcessingTimeMs;
    }

    public DetectorResult(int id, int parking_status , String time_detector, long lastProcessingTimeMs) {
        this.id = id;
        this.parking_status = parking_status;
        this.time_detector = time_detector;
        this.lastProcessingTimeMs=lastProcessingTimeMs;
    }

    public long getLastProcessingTimeMs() {
        return lastProcessingTimeMs;
    }

    public void setLastProcessingTimeMs(long lastProcessingTimeMs) {
        this.lastProcessingTimeMs = lastProcessingTimeMs;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getParking_status() {
        return parking_status;
    }

    public void setParking_status(int parking_status) {
        this.parking_status = parking_status;
    }

    public String getTime_detector() {
        return time_detector;
    }

    public void setTime_detector(String time_detector) {
        this.time_detector = time_detector;
    }
}