package com.fgtit.utils;

public class FingerprintRespondent {
    private int id;
    private String enrolmentId;
    private int verificationCount;
    private String createdAt;
    private int hasImages;

    public FingerprintRespondent(int id, String enrolmentId, int verificationCount, String createdAt, int hasImages) {
        this.id = id;
        this.enrolmentId = enrolmentId;
        this.verificationCount = verificationCount;
        this.createdAt = createdAt;
        this.hasImages = hasImages;
    }

    public FingerprintRespondent(){

    }

    public String getEnrolmentId() {
        return enrolmentId;
    }

    public void setEnrolmentId(String enrolmentId) {
        this.enrolmentId = enrolmentId;
    }

    public int getVerificationCount() {
        return verificationCount;
    }

    public void setVerificationCount(int verificationCount) {
        this.verificationCount = verificationCount;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public int getHasImages() {
        return hasImages;
    }

    public void setHasImages(int hasImages) {
        this.hasImages = hasImages;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
