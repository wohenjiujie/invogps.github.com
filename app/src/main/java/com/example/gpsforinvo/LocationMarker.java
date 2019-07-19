package com.example.gpsforinvo;

import android.app.VoiceInteractor;
import android.graphics.Color;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.animation.CycleInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;

import com.amap.api.maps.AMap;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.Circle;
import com.amap.api.maps.model.CircleOptions;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;

import java.util.Timer;
import java.util.TimerTask;


public class LocationMarker {
    private static Marker mLocMarker;
    private static final int STROKE_COLOR = Color.argb(180, 3, 145, 255);
    private static final int FILL_COLOR = Color.argb(10, 0, 0, 180);
    private static Circle mCircle;
    public static final String LOCATION_MARKER_FLAG = "mylocation";
    private TimerTask mTimerTask;
    private Timer mTimer = new Timer();
    private long start;
    private final Interpolator interpolator = new CycleInterpolator(1);
    private final Interpolator interpolator1 = new LinearInterpolator();


    public static void addCircle(LatLng latlng, double radius, AMap myMap) {
            CircleOptions options = new CircleOptions();
            options.strokeWidth(1f);
            options.fillColor(FILL_COLOR);
            options.strokeColor(STROKE_COLOR);
            options.center(latlng);
            options.radius(radius);
            mCircle = myMap.addCircle(options);
    }

    public void circleProcess(LatLng latlng, double radius){
        mCircle.setRadius(radius);
        mCircle.setCenter(latlng);
    }

    public void markerProcess(LatLng latlng){
        mLocMarker.setPosition(latlng);
    }

    public static void addMarker(LatLng latlng, AMap myMap) {
            if (mLocMarker != null) {
                return;
            }
            MarkerOptions options = new MarkerOptions();
            options.position(latlng).icon(BitmapDescriptorFactory.fromResource(R.drawable.location_marker));
            options.anchor(0.5f, 0.5f);
            options.position(latlng);
            mLocMarker = myMap.addMarker(options);
            mLocMarker.setTitle(LOCATION_MARKER_FLAG);
    }

    public void Scalecircle(final Circle circle) {
        start = SystemClock.uptimeMillis();
        mTimerTask = new circleTask(circle, 1000);
        mTimer.schedule(mTimerTask, 0, 30);
    }

    private  class circleTask extends TimerTask {
        private double r;
        private Circle circle;
        private long duration = 1000;

        public circleTask(Circle circle, long rate){
            this.circle = circle;
            this.r = circle.getRadius();
            if (rate > 0 ) {
                this.duration = rate;
            }
        }
        @Override
        public void run() {
            try {
                long elapsed = SystemClock.uptimeMillis() - start;
                float input = (float)elapsed / duration;
//                外圈循环缩放
//                float t = interpolator.getInterpolation((float)(input-0.25));//return (float)(Math.sin(2 * mCycles * Math.PI * input))
//                外圈放大后消失
                float t = interpolator1.getInterpolation(input);
                double r1 = (t + 1) * r;
                circle.setRadius(r1);
                if (input > 2){
                    start = SystemClock.uptimeMillis();
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public static Marker getMarker(){
        return mLocMarker;
    }

    public static Circle getCircle(){
        return mCircle;
    }
}
