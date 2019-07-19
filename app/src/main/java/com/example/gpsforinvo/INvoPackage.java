package com.example.gpsforinvo;

import com.amap.api.location.AMapLocation;
import com.amap.api.maps.AMap;

public class INvoPackage {
    static AMapLocation aMapLocation;
    static AMap myMap;

    public INvoPackage(AMapLocation amapLocation,AMap myMap){

        this.aMapLocation=amapLocation;
        this.myMap=myMap;
    }
    public void setLocate(AMapLocation amapLocation){
        this.aMapLocation=amapLocation;
    }
    public AMapLocation getLocate(){
        return aMapLocation;
    }
}
