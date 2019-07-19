package com.example.gpsforinvo;

import com.amap.api.maps.AMap;

public class MapMode {


    static void mapMode (AMap myMap,int x){
        //注意break是否会造成影响
        switch (x){

            case 1:
                myMap.setMapType(AMap.MAP_TYPE_NORMAL);//标准地图
                break;
            case 2:
                myMap.setMapType(AMap.MAP_TYPE_SATELLITE);//卫星图
                break;
            case 3:
                myMap.setMapType(AMap.MAP_TYPE_NIGHT);//夜景模式
                break;
            case 4:
                myMap.setMapType(AMap.MAP_TYPE_NAVI);//导航地图
                break;
        }


    }

}
