package com.amap.util;

import com.google.gson.Gson;

import java.util.List;

/**
 * Created by LiuPeng on 2016/11/12.
 */

public class CoordinateGson {


    private List<DataBean> data;

    public List<DataBean> getData() {
        return data;
    }

    public void setData(List<DataBean> data) {
        this.data = data;
    }

    private int code;
    private String msg;

    public static CoordinateGson objectFromData(String str) {

        return new Gson().fromJson(str, CoordinateGson.class);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public static class DataBean {
        /**
         * id : 6
         * user : 03002B56-8578-4B70-9582-E6BB5B6B7378
         * role : User
         * lat : 39.149404296875
         * lng : 117.402110731337
         * createdTime : 2016-11-01 13:06:45
         * updateTime : 2016-11-10 11:56:47
         */

        private int id;
        private String user;
        private String role;
        private String lat;
        private String lng;
        private String createdTime;
        private String updateTime;

        public static DataBean objectFromData(String str) {

            return new Gson().fromJson(str, DataBean.class);
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
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
