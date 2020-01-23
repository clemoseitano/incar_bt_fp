package com.fgtit.utils;

public class FingerprintResponse {
    private int id;
    private String fpRespondentId;
    private int fpFingerId;
    private String fpJPGImage;
    private byte[] fpData;
    private String fpWSQImage;
    private String fpRawImage;
    private int fpVerificationIndex;
    private String createdAt;
    private String fpFingerRecordId;

    public FingerprintResponse(int id, String fpRespondentId, int fpFingerId, String fpJPGImage, byte[] fpData, String fpWSQImage, String fpRawImage, int fpVerificationIndex, String createdAt, String fpFingerRecordId) {
        this.id = id;
        this.fpRespondentId = fpRespondentId;
        this.fpFingerId = fpFingerId;
        this.fpJPGImage = fpJPGImage;
        this.fpData = fpData;
        this.fpWSQImage = fpWSQImage;
        this.fpRawImage = fpRawImage;
        this.fpVerificationIndex = fpVerificationIndex;
        this.createdAt = createdAt;
        this.fpFingerRecordId = fpFingerRecordId;
    }

    public FingerprintResponse(){

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFpRespondentId() {
        return fpRespondentId;
    }

    public void setFpRespondentId(String fpRespondentId) {
        this.fpRespondentId = fpRespondentId;
    }

    public int getFpFingerId() {
        return fpFingerId;
    }

    public void setFpFingerId(int fpFingerId) {
        this.fpFingerId = fpFingerId;
    }

    public String getFpJPGImage() {
        return fpJPGImage;
    }

    public void setFpJPGImage(String fpJPGImage) {
        this.fpJPGImage = fpJPGImage;
    }

    public byte[] getFpData() {
        return fpData;
    }

    public void setFpData(byte[] fpData) {
        this.fpData = fpData;
    }

    public String getFpWSQImage() {
        return fpWSQImage;
    }

    public void setFpWSQImage(String fpWSQImage) {
        this.fpWSQImage = fpWSQImage;
    }

    public String getFpRawImage() {
        return fpRawImage;
    }

    public void setFpRawImage(String fpRawImage) {
        this.fpRawImage = fpRawImage;
    }

    public int getFpVerificationIndex() {
        return fpVerificationIndex;
    }

    public void setFpVerificationIndex(int fpVerificationIndex) {
        this.fpVerificationIndex = fpVerificationIndex;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getFpFingerRecordId() {
        return fpFingerRecordId;
    }

    public void setFpFingerRecordId(String fpFingerRecordId) {
        this.fpFingerRecordId = fpFingerRecordId;
    }
}
