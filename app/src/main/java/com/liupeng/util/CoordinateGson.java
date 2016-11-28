package com.liupeng.util;

/**
 * Created by LiuPeng on 2016/11/12.
 */

public class CoordinateGson {


    /**
     * error : {"code":2000,"msg":"字段名称：[roleId] 是必须填写的"}
     */

    private ErrorBean error;

    /**
     * data : {"id":6,"uuid":"03002B56-8578-4B70-9582-E6BB5B6B7378","lat":"39.1499449327257","lng":"117.40011311849","createdTime":"2016-11-01 13:06:45","updateTime":"2016-11-25 13:05:13"}
     */

    private DataBean data;

    public ErrorBean getError() {
        return error;
    }

    public void setError(ErrorBean error) {
        this.error = error;
    }

    public DataBean getData() {
        return data;
    }

    public void setData(DataBean data) {
        this.data = data;
    }

    public static class ErrorBean {
        /**
         * code : 2000
         * msg : 字段名称：[roleId] 是必须填写的
         */

        private int code;
        private String msg;

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
    }


    public static class DataBean {
        /**
         * id : 6
         * uuid : 03002B56-8578-4B70-9582-E6BB5B6B7378
         * lat : 39.1499449327257
         * lng : 117.40011311849
         * createdTime : 2016-11-01 13:06:45
         * updateTime : 2016-11-25 13:05:13
         */

        private long id;
        private String uuid;
        private String lat;
        private String lng;
        private String createdTime;
        private String updateTime;

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
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
