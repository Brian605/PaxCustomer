package com.paxboda.customer.models;

import com.google.firebase.firestore.ServerTimestamp;

import java.io.Serializable;
import java.util.Date;

public class Journey implements Serializable {
    private LocationObj fromLatLng = null;
    private LocationObj toLatLng = null;
    private String fromName = null;
    private String toName = null;
    private String userName = null;
    private long startTime = 0;
    private long endTime = 0;
    private int riderId = 0;
    private int icons_id = 0;
    private String county="";
    private String riderImage = "";
    private String userPhone = null;
    private String riderPhone = null;
    @ServerTimestamp
    private Date date = new Date();
    private int journeyId = 0;
    private int cost = 0;
    private String status = "Pending";

    public Journey(LocationObj fromLatLng, LocationObj toLatLng, String fromName, String toName, String userName, long startTime, long endTime, int riderId, int icons_id, String userPhone, Date date, int journeyId, int cost, String status) {
        this.fromLatLng = fromLatLng;
        this.toLatLng = toLatLng;
        this.fromName = fromName;
        this.toName = toName;
        this.userName = userName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.riderId = riderId;
        this.icons_id = icons_id;
        this.userPhone = userPhone;
        this.date = date;
        this.journeyId = journeyId;
        this.cost = cost;
        this.status = status;
    }

    public Journey() {
    }

    public void setCounty(String county) {
        this.county = county;
    }

    public String getCounty() {
        return county;
    }

    public String getRiderPhone() {
        return riderPhone;
    }

    public void setRiderPhone(String riderPhone) {
        this.riderPhone = riderPhone;
    }

    public String getRiderImage() {
        return riderImage;
    }

    public void setRiderImage(String riderImage) {
        this.riderImage = riderImage;
    }

    public LocationObj getFromLatLng() {
        return fromLatLng;
    }

    public void setFromLatLng(LocationObj fromLatLng) {
        this.fromLatLng = fromLatLng;
    }

    public LocationObj getToLatLng() {
        return toLatLng;
    }

    public void setToLatLng(LocationObj toLatLng) {
        this.toLatLng = toLatLng;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public String getToName() {
        return toName;
    }

    public void setToName(String toName) {
        this.toName = toName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public int getRiderId() {
        return riderId;
    }

    public void setRiderId(int riderId) {
        this.riderId = riderId;
    }

    public int getIcons_id() {
        return icons_id;
    }

    public void setIcons_id(int icons_id) {
        this.icons_id = icons_id;
    }

    public String getUserPhone() {
        return userPhone;
    }

    public void setUserPhone(String userPhone) {
        this.userPhone = userPhone;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public int getJourneyId() {
        return journeyId;
    }

    public void setJourneyId(int journeyId) {
        this.journeyId = journeyId;
    }

    public int getCost() {
        return cost;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
