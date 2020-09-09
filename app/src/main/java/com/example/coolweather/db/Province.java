package com.example.coolweather.db;

import org.litepal.crud.DataSupport;

public class Province extends DataSupport {

    private int id;
    private String provinceName;
    private int getId(){
        return  id;
    }

    public String getProvinceName() {
        return provinceName;
    }

    public void setProvinceName(String provinceName) {
        this.provinceName = provinceName;
    }

    public void setId(int id) {
        this.id = id;
    }
}
