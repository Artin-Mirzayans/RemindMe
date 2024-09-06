package com.remindme.models;

import java.util.Map;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class User {

    private String userId; // Primary key (Email)
    private String phoneNumber;
    private String otpCode;
    private boolean isVerified;
    private int smsRequestCount;
    private long lastOtpSentTimestamp;

    public User() {
        this.smsRequestCount = 0;
        this.isVerified = false;
    }

    public User(Map<String, AttributeValue> item) {
        this.userId = item.get("Email").s();
        this.phoneNumber = item.getOrDefault("PhoneNumber", AttributeValue.builder().s("").build()).s();
        this.otpCode = item.getOrDefault("OtpCode", AttributeValue.builder().s("").build()).s();
        this.isVerified = item.getOrDefault("IsVerified", AttributeValue.builder().bool(false).build()).bool();
        this.smsRequestCount = Integer
                .parseInt(item.getOrDefault("SmsRequestCount", AttributeValue.builder().n("0").build()).n());
        this.lastOtpSentTimestamp = Long
                .parseLong(item.getOrDefault("LastOtpSentTimestamp", AttributeValue.builder().n("0").build()).n());
    }

    public Map<String, Object> getSimplifiedView() {
        return Map.of(
                "phoneNumber", phoneNumber,
                "isVerified", isVerified,
                "lastOtpSentTimestamp", lastOtpSentTimestamp);
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getOtpCode() {
        return otpCode;
    }

    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean isVerified) {
        this.isVerified = isVerified;
    }

    public int getSmsRequestCount() {
        return smsRequestCount;
    }

    public void setSmsRequestCount(int smsRequestCount) {
        this.smsRequestCount = smsRequestCount;
    }

    public long getLastOtpSentTimestamp() {
        return lastOtpSentTimestamp;
    }

    public void setLastOtpSentTimestamp(long lastOtpSentTimestamp) {
        this.lastOtpSentTimestamp = lastOtpSentTimestamp;
    }
}
