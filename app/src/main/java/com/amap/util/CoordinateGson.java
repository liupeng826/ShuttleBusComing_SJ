package com.amap.util;

import java.util.List;

/**
 * Created by LiuPeng on 2016/11/12.
 */

public class CoordinateGson {

    private String msg;
    /**
     * id : 15
     * user : 98ABC45B-4968-4AA9-ABB0-9183169D32CC
     * role : Bus6
     * lat : 39.1202872721354
     * lng : 117.225327690972
     * createdTime : 2016-11-04 16:54:48
     * updateTime : 2016-11-08 07:55:14
     */

    private List<CoordinatelistBean> coordinatelist;

    public static CoordinateGson objectFromData(String str) {

        return new com.google.gson.Gson().fromJson(str, CoordinateGson.class);
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public List<CoordinatelistBean> getCoordinatelist() {
        return coordinatelist;
    }

    public void setCoordinatelist(List<CoordinatelistBean> coordinatelist) {
        this.coordinatelist = coordinatelist;
    }

    public static class CoordinatelistBean {
        private String id;
        private String user;
        private String role;
        private String lat;
        private String lng;
        private String createdTime;
        private String updateTime;

        public static CoordinatelistBean objectFromData(String str) {

            return new com.google.gson.Gson().fromJson(str, CoordinatelistBean.class);
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
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

        public String getCreatedTime() {
            return createdTime;
        }

        public void setCreatedTime(String createdTime) {
            this.createdTime = createdTime;
        }

        public String getUpdateTime() {
            return updateTime;
        }

        public void setUpdateTime(String updateTime) {
            this.updateTime = updateTime;
        }
    }
}
