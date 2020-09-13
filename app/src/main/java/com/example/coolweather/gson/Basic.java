package com.example.coolweather.gson;

import android.text.style.UpdateAppearance;

import com.google.gson.annotations.SerializedName;
/**
 * 解析传回来的gson数据中的basic类
 *但是有时候json里命名和我们自己的变量命名有差别，这就要使用到属性重命名
 * @SerializedName("city")
 *  "cond":{"city":"北京"}
 * */
public class Basic {
    @SerializedName("city")
    public  String  cityName;

    @SerializedName("id")
    public String weatherId;

    public Update  update;

    //实时更新数据的时间
    public  class  Update{
        @SerializedName("loc")
        public  String updateTime;
    }

}
