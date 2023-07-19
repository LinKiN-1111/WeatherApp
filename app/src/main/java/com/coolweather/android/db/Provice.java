package com.coolweather.android.db;

import org.litepal.crud.LitePalSupport;

public class Provice extends LitePalSupport {

    /**
     * id是每个实体类都应该有的字段,provinceName记录省名,provinceCode记录省的代号
     * 注意,LitePal每个实体类都需要继承LitePalSupport
     */

    private int id;
    private String provinceName;
    private int provinceCode;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getProvinceName() {
        return provinceName;
    }

    public void setProvinceName(String provinceName) {
        this.provinceName = provinceName;
    }

    public int getProvinceCode() {
        return provinceCode;
    }

    public void setProvinceCode(int provinceCode) {
        this.provinceCode = provinceCode;
    }
}
