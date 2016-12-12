package com.liupeng.util;

/**
 * Created by liupeng on 2016/12/7.
 * E-mail: liupeng826@hotmail.com
 */

public class Station {
        /**
         * id : 50
         * stationId : 1
         * line : 6
         * stationName : 体院北公交站
         * lat : 39.109597
         * lng : 117.254775
         * reachTime : 07:15:00
         * busNo : 津AW3021
         * driverName : 刘师傅
         * driverTel : 13821112075
         * updateTime : 2016-12-02 11:13:49
         * comments :
         */

        private int id;
        private int stationId;
        private int line;
        private String stationName;
        private String lat;
        private String lng;
        private String reachTime;
        private String busNo;
        private String driverName;
        private String driverTel;
        private String updateTime;
        private String comments;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getStationId() {
            return stationId;
        }

        public void setStationId(int stationId) {
            this.stationId = stationId;
        }

        public int getLine() {
            return line;
        }

        public void setLine(int line) {
            this.line = line;
        }

        public String getStationName() {
            return stationName;
        }

        public void setStationName(String stationName) {
            this.stationName = stationName;
        }

        public String getLat() {
            return lat;
        }

        public void setLat(String lat) {
            this.lat = lat;
        }

        public String getLng() {
            return lng;
        }

        public void setLng(String lng) {
            this.lng = lng;
        }

        public String getReachTime() {
            return reachTime;
        }

        public void setReachTime(String reachTime) {
            this.reachTime = reachTime;
        }

        public String getBusNo() {
            return busNo;
        }

        public void setBusNo(String busNo) {
            this.busNo = busNo;
        }

        public String getDriverName() {
            return driverName;
        }

        public void setDriverName(String driverName) {
            this.driverName = driverName;
        }

        public String getDriverTel() {
            return driverTel;
        }

        public void setDriverTel(String driverTel) {
            this.driverTel = driverTel;
        }

        public String getUpdateTime() {
            return updateTime;
        }

        public void setUpdateTime(String updateTime) {
            this.updateTime = updateTime;
        }

        public String getComments() {
            return comments;
        }

        public void setComments(String comments) {
            this.comments = comments;
        }
    }
