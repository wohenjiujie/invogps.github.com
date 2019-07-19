package com.example.gpsforinvo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.MenuItem;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.animation.CycleInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.Projection;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.Circle;
import com.amap.api.maps.model.CircleOptions;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static com.example.gpsforinvo.LocationMarker.LOCATION_MARKER_FLAG;

public class MapViewActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        LocationSource, AMapLocationListener ,AMap.OnMapTouchListener{
//    ,AMap.OnMapTouchListener
    static int mapMode=1;//地图模式
    static int mapKind=1;//地图（室内室外）
    //创建map视图
    MapView basicMapView;
    //创建AMap
    AMap myMap;
    //定位蓝点样式
    private OnLocationChangedListener mListener;
    private AMapLocationClient mlocationClient;
    private AMapLocationClientOption mLocationOption;
    private boolean followMove = true;

    //坐标和经纬度转换工具
    Projection projection;


//    marker随sensor变换角度
    private TextView mLocationErrText;
    private boolean mFirstFix = false;
    private SensorEventHelper mSensorHelper;


    //other
    private final Interpolator interpolator = new CycleInterpolator(1);
    private final Interpolator interpolator1 = new LinearInterpolator();
    private static final int STROKE_COLOR = Color.argb(180, 3, 145, 255);
    private static final int FILL_COLOR = Color.argb(10, 0, 0, 180);
    private Marker mLocMarker;
    private Circle mCircle;
    //自定义定位小蓝点的Marker
    Marker locationMarker;
    private TimerTask mTimerTask;
    private Timer mTimer = new Timer();
    private long start;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_view);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);
        basicMapView = findViewById(R.id.basic_map);
        //在activity执行onCreate时执行mMapView.onCreate(savedInstanceState)，创建地图
        basicMapView.onCreate(savedInstanceState);
        String a = sHA1(this);
        Log.d("xxx", "onCreate: "+a);
        init();
    }

    public static String sHA1(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), PackageManager.GET_SIGNATURES);
            byte[] cert = info.signatures[0].toByteArray();
            MessageDigest md = MessageDigest.getInstance("SHA1");
            byte[] publicKey = md.digest(cert);
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < publicKey.length; i++) {
                String appendString = Integer.toHexString(0xFF & publicKey[i])
                        .toUpperCase(Locale.US);
                if (appendString.length() == 1)
                    hexString.append("0");
                hexString.append(appendString);
                hexString.append(":");
            }
            String result = hexString.toString();
            return result.substring(0, result.length() - 1);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
    /*
     * 初始化AMap、marker、圆环
     * */
    private void init() {
        if (myMap == null) {
            myMap = basicMapView.getMap();
            setUpMap();
        }
        mSensorHelper = new SensorEventHelper(this);
        if (mSensorHelper != null) {
            mSensorHelper.registerSensorListener();
        }
        mLocationErrText = findViewById(R.id.location_errInfo_text);
        mLocationErrText.setVisibility(View.GONE);
    }
    /*
     * 设置AMap属性
     * */
    private void setUpMap(){
        myMap.setLocationSource(this);//设置定位监听
//        myLocationStyle = new MyLocationStyle();
//        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER);
//        myMap.setMyLocationStyle(myLocationStyle);
        myMap.setOnMapTouchListener(this);//设置手势控制监听
        myMap.getUiSettings().setMyLocationButtonEnabled(false);//设置默认定位按钮是否显示
        myMap.setMyLocationEnabled(true);//设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
        myMap.getUiSettings().setCompassEnabled(true);
//        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER);
//        myMap.setMyLocationStyle(myLocationStyle);
        mLocationOption.setOnceLocation(false);
        mLocationOption.setOnceLocationLatest(false);//取消单次定位
        mLocationOption.setInterval(1500);//定位间隔
        mLocationOption.setNeedAddress(true);//是否返回地址
        mlocationClient.setLocationOption(mLocationOption);//开始定位
        mlocationClient.startLocation();
    }

    public void jump(final View view){
        try
        {
            Intent intent=new Intent(Intent.ACTION_MAIN);
//            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            ComponentName componentName=new ComponentName("com.itgowo.sport.trace.tracedemo",
                    "com.itgowo.sport.trace.tracedemo.Trace.TraceMainActivity");
            intent.setComponent(componentName);
            startActivity(intent);
        }catch (Exception e)

        {
        }
    }

    public void inToOut(View view){
        findViewById(R.id.indoor).setVisibility(View.GONE);
        findViewById(R.id.outdoor).setVisibility(View.VISIBLE);
        Toast.makeText(this, "切换为标准地图", Toast.LENGTH_SHORT).show();
//        myMap.getUiSettings().setScaleControlsEnabled(true);
        myMap.showIndoorMap(false);
        // 关闭SDK自带的室内地图控件
//        myMap.getUiSettings().setIndoorSwitchEnabled(false);
        mapKind=1;
    }

    public void outToIn(View view){
        if(mapMode==1){
            findViewById(R.id.outdoor).setVisibility(View.GONE);
            findViewById(R.id.indoor).setVisibility(View.VISIBLE);
            Toast.makeText(this, "切换为室内地图", Toast.LENGTH_SHORT).show();
//            myMap.getUiSettings().setScaleControlsEnabled(true);
            myMap.showIndoorMap(true);
            // 关闭SDK自带的室内地图控件
//            myMap.getUiSettings().setIndoorSwitchEnabled(false);
            mapKind=2;
        }else {
            Toast.makeText(this, "该模式下无法切换室内地图", Toast.LENGTH_SHORT).show();
        }
    }

    public void location(final View view){
////        followMove=true;
        AMapLocation amapLocation= INvoPackage.aMapLocation;
//        if(amapLocation!=null){
//            LatLng latLng = new LatLng(amapLocation.getLatitude(), amapLocation.getLongitude());
//            myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,17));
//        }else{
//            Toast.makeText(this, "notSuccess", Toast.LENGTH_SHORT).show();
//        }

        mLocationOption.setOnceLocation(true); //设置为单次定位模式
        mLocationOption.setNeedAddress(true); //返回地址描述
        mLocationOption.setHttpTimeOut(10000); //设置请求超时时间
        mlocationClient.setLocationOption(mLocationOption);
//设置定位回调监听器
        mlocationClient.setLocationListener(new AMapLocationListener() {
            @Override
            public void onLocationChanged(AMapLocation aMapLocation) {
                if(aMapLocation != null){
                    LatLng latLng = new LatLng(aMapLocation.getLatitude(), aMapLocation.getLongitude());
//                    locationMarker.setPosition(latLng);
                    //将标记移动到定位点，使用animateCamera就有动画效果
                    myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17));
                    myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,17));
                }else{
                    Toast.makeText(MapViewActivity.this, "定位失败", Toast.LENGTH_SHORT).show();
                }
            }
        });
        LatLng latLng = new LatLng(amapLocation.getLatitude(), amapLocation.getLongitude());
        myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,17));
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.standard) {
            mapMode=1;
            MapMode.mapMode(myMap,mapMode);
        } else if (id == R.id.satellite) {
            mapMode=2;
            if(mapKind==2){
                findViewById(R.id.indoor).setVisibility(View.GONE);
                findViewById(R.id.outdoor).setVisibility(View.VISIBLE);
                myMap.showIndoorMap(false);
                mapKind=1;
                Toast.makeText(this, "该模式下自动切换为标准地图", Toast.LENGTH_SHORT).show();
            }
            MapMode.mapMode(myMap,mapMode);
        } else if (id == R.id.darkness) {
            mapMode=3;
            if(mapKind==2){
                findViewById(R.id.indoor).setVisibility(View.GONE);
                findViewById(R.id.outdoor).setVisibility(View.VISIBLE);
                myMap.showIndoorMap(false);
                mapKind=1;
                Toast.makeText(this, "该模式下自动切换为标准地图", Toast.LENGTH_SHORT).show();
            }
            MapMode.mapMode(myMap,mapMode);
        } else if (id == R.id.navi) {
            mapMode=4;
            if(mapKind==2){
                findViewById(R.id.indoor).setVisibility(View.GONE);
                findViewById(R.id.outdoor).setVisibility(View.VISIBLE);
                myMap.showIndoorMap(false);
                mapKind=1;
                Toast.makeText(this, "该模式下自动切换为标准地图", Toast.LENGTH_SHORT).show();
            }
            MapMode.mapMode(myMap,mapMode);
        } else if (id == R.id.indoor) {
            mapMode=5;
            if(mapKind==2){
                findViewById(R.id.indoor).setVisibility(View.GONE);
                findViewById(R.id.outdoor).setVisibility(View.VISIBLE);
                myMap.showIndoorMap(false);
                mapKind=1;
                Toast.makeText(this, "该模式下自动切换为标准地图", Toast.LENGTH_SHORT).show();
            }
            MapMode.mapMode(myMap,mapMode);
        }
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
    /*
     * 定位成功后回调函数
     * */
    @Override
    public void onLocationChanged(AMapLocation amapLocation) {
        if (mListener != null && amapLocation != null) {
            if (amapLocation != null
                    && amapLocation.getErrorCode() == 0) {
                LatLng latLng = new LatLng(amapLocation.getLatitude(), amapLocation.getLongitude());
                //展示自定义定位小蓝点
                if(!mFirstFix){
                    mFirstFix = true;
                    LocationMarker.addCircle(latLng, amapLocation.getAccuracy(),myMap);
                    LocationMarker.addMarker(latLng,myMap);
                    mSensorHelper.setCurrentMarker(LocationMarker.getMarker());//定位图标旋转

//                    addCircle(latLng, amapLocation.getAccuracy());//添加定位精度圆
//                    addMarker(latLng);//添加定位图标
//                    mSensorHelper.setCurrentMarker(mLocMarker);//定位图标旋转

                    myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,17));
                    INvoPackage alk=new INvoPackage(amapLocation,myMap);
                    alk.setLocate(amapLocation);
                } else {
//                    LocationMarker.getMarker().setPosition(latLng);
//                    LocationMarker.getCircle().setCenter(latLng);
//                    LocationMarker.getCircle().setRadius(amapLocation.getAccuracy());

                    locationMarker.setPosition(latLng);//不出bug最关键的代码

//                    mCircle.setCenter(latLng);
//                    mCircle.setRadius(amapLocation.getAccuracy());
//                    mLocMarker.setPosition(latLng);

                    myMap.moveCamera(CameraUpdateFactory.changeLatLng(latLng));
                    if(followMove) {
                        //二次以后定位，使用sdk中没有的模式，让地图和小蓝点一起移动到中心点（类似导航锁车时的效果）
                        startMoveLocationAndMap(latLng);
                    } else {
                        startChangeLocation(latLng);
                    }
                }
            } else {
                String errText = "定位失败," + amapLocation.getErrorCode()+ ": " + amapLocation.getErrorInfo();
                Log.e("AmapErr",errText);
                mLocationErrText.setVisibility(View.VISIBLE);
                mLocationErrText.setText(errText);
            }
        }
        LocationMarker lm=new LocationMarker();
        lm.Scalecircle(LocationMarker.getCircle());
//        Scalecircle(mCircle);
    }

    /*public void Scalecircle(final Circle circle) {
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
    private void addCircle(LatLng latlng, double radius) {
        CircleOptions options = new CircleOptions();
        options.strokeWidth(1f);
        options.fillColor(FILL_COLOR);
        options.strokeColor(STROKE_COLOR);
        options.center(latlng);
        options.radius(radius);
        mCircle = myMap.addCircle(options);
    }

    private void addMarker(LatLng latlng) {
        if (mLocMarker != null) {
            return;
        }
        MarkerOptions options = new MarkerOptions();
        options.position(latlng).icon(BitmapDescriptorFactory.fromResource(R.drawable.location_marker));
        options.anchor(0.5f, 0.5f);
        options.position(latlng);
        mLocMarker = myMap.addMarker(options);
        mLocMarker.setTitle(LOCATION_MARKER_FLAG);
    }*/
    /**
     * 修改自定义定位小蓝点的位置
     * @param latLng
     * */
    private void startChangeLocation(LatLng latLng) {

        if(LocationMarker.getMarker() != null) {
            LatLng curLatlng = LocationMarker.getMarker().getPosition();
            if(curLatlng == null || !curLatlng.equals(latLng)) {
                LocationMarker.getMarker().setPosition(latLng);
            }
        }
    }

    /*
    * 同时修改自定义定位小蓝点和地图的位置
    * */
    private void startMoveLocationAndMap(LatLng latLng) {

        //将小蓝点提取到屏幕上
        if(projection == null) {
            projection = myMap.getProjection();
        }
        if(LocationMarker.getMarker()!= null && projection != null) {
            LatLng markerLocation =LocationMarker.getMarker().getPosition();
            Point screenPosition = myMap.getProjection().toScreenLocation(markerLocation);
            LocationMarker.getMarker().setPositionByPixels(screenPosition.x, screenPosition.y);

        }

        //移动地图，移动结束后，将小蓝点放到放到地图上
        myCancelCallback.setTargetLatlng(latLng);
        //动画移动的时间，最好不要比定位间隔长，如果定位间隔2000ms 动画移动时间最好小于2000ms，可以使用1000ms
        //如果超过了，需要在myCancelCallback中进行处理被打断的情况
        myMap.animateCamera(CameraUpdateFactory.changeLatLng(latLng),1000,myCancelCallback);

    }

    MapViewActivity.MyCancelCallback myCancelCallback = new MapViewActivity.MyCancelCallback();

    @Override
    public void onTouch(MotionEvent motionEvent) {
        Log.i("amap","onTouch 关闭地图和小蓝点一起移动的模式");
        if(followMove){
//            followMove = false;
        }
    }

    /*
    * 监控地图动画移动情况，如果结束或者被打断，都需要执行响应的操作
    * */
    class MyCancelCallback implements AMap.CancelableCallback {

        LatLng targetLatlng;
        public void setTargetLatlng(LatLng latlng) {
            this.targetLatlng = latlng;
        }

        @Override
        public void onFinish() {
            if(LocationMarker.getMarker()!= null && targetLatlng != null) {
                LocationMarker.getMarker().setPosition(targetLatlng);
            }
        }

        @Override
        public void onCancel() {
            if(LocationMarker.getMarker()!= null && targetLatlng != null) {
                LocationMarker.getMarker().setPosition(targetLatlng);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView.onResume ()，重新绘制加载地图
        basicMapView.onResume();
        if (mSensorHelper != null) {
            mSensorHelper.registerSensorListener();
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        if (mSensorHelper != null) {
            mSensorHelper.unRegisterSensorListener();
            mSensorHelper.setCurrentMarker(null);
            mSensorHelper = null;
        }
        //在activity执行onPause时执行mMapView.onPause ()，暂停地图的绘制
        basicMapView.onPause();
        deactivate();
        mFirstFix = false;
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //在activity执行onSaveInstanceState时执行mMapView.onSaveInstanceState (outState)，保存地图当前的状态
        basicMapView.onSaveInstanceState(outState);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
//        if (mLocMarker!= null) {
//            mLocMarker.destroy();
//        }
        if (LocationMarker.getMarker()!= null) {
            LocationMarker.getMarker().destroy();
        }
        //在activity执行onDestroy时执行mMapView.onDestroy()，销毁地图
        basicMapView.onDestroy();
        if(null != mlocationClient){
            mlocationClient.onDestroy();
        }
    }

    /**
     * 激活定位
     */
    @Override
    public void activate(LocationSource.OnLocationChangedListener listener) {
        mListener = listener;
        if (mlocationClient == null) {
            mlocationClient = new AMapLocationClient(this);
            mLocationOption = new AMapLocationClientOption();
            //设置定位监听
            mlocationClient.setLocationListener(this);
            //设置为高精度定位模式
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            //是指定位间隔
            mLocationOption.setInterval(2000);
            //设置定位参数
            mlocationClient.setLocationOption(mLocationOption);
            // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
            // 注意设置合适的定位时间的间隔（最小间隔支持为2000ms），并且在合适时间调用stopLocation()方法来取消定位请求
            // 在定位结束后，在合适的生命周期调用onDestroy()方法
            // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
            mlocationClient.startLocation();//启动定位
        }
    }
    /**
     * 停止定位
     */
    @Override
    public void deactivate() {
        mListener = null;
        if (mlocationClient != null) {
            mlocationClient.stopLocation();
            mlocationClient.onDestroy();
        }
        mlocationClient = null;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
