package com.liupeng.shuttleBusComingsj;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.liupeng.offlinemap.OfflineMapActivity;
import com.liupeng.util.ApiService;
import com.liupeng.util.Coordinate;
import com.liupeng.util.CoordinateGson;
import com.liupeng.util.Station;
import com.liupeng.util.StationGson;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import static android.content.ContentValues.TAG;

public class MainActivity extends Activity implements LocationSource,
        AMapLocationListener {
    private MapView mMapView;
    private AMap mAMap;
    private Marker mMarker;
    private OnLocationChangedListener mListener;
    private AMapLocationClient mLocationClient;
    private AMapLocationClientOption mLocationOption = null;
    private Handler handler;
    private Runnable runnable;
    private List<LatLng> mLocations;
	private List<Station> mStationList;
    private String mSelectedBusLine;
    private int mSelectedBusLineNumber;
    private int mRoleId;
    private boolean mSelectedBusLineChanged = false;
    private CheckBox mDriver;
    private boolean mDriverChecked = false;
    private String mUUID;
	private LatLng currentlocation;

    // 通过设置间隔时间和距离可以控制速度和图标移动的距离
    private static final double DISTANCE = 0.0001;

    private static final int FETCH_TIME_INTERVAL = 7000;
    final String URL = "http://60.205.182.57/";
    final String FILENAME = "ShuttleBusComingsj";
    final String LINE_KEY = "LINE_KEY";
    final String DRIVERLINE_KEY = "DRIVERLINE_KEY";
    final String UUIDKEY = "UUID_KEY";
    final String DRIVERKEY = "DRIVER_KEY";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.basicmap_activity);
        mMapView = (MapView) findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);// 此方法必须重写

        init();
        // new task to get coordinate data
        getDataTask();
        //initRoadData();
    }

    /**
     * 初始化AMap对象
     */
    private void init() {
	    currentlocation = new LatLng(0,0);
	    mStationList = new ArrayList<>();
        mUUID = getUUID(this);
        mLocations = new ArrayList<>();
        // 地图
        if (mAMap == null) {
            mAMap = mMapView.getMap();
            setUpMap();
        }

        // 初始化控件
        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        // 建立数据源
        String[] mItems = getResources().getStringArray(R.array.lines);
        // 建立Adapter并且绑定数据源
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, mItems);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //绑定 Adapter到控件
        spinner.setAdapter(adapter);

        // 读取存储数据
        SharedPreferences settings = getSharedPreferences(FILENAME, MODE_PRIVATE);
        mSelectedBusLineNumber = settings.getInt(LINE_KEY, 0);
        mSelectedBusLine = "Bus" + mSelectedBusLineNumber;

        if (!mSelectedBusLine.equals("Bus0")) {
            spinner.setSelection(settings.getInt(LINE_KEY, 0) - 1);
        }

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int pos, long id) {

                mSelectedBusLineChanged = true;
                mLocations = new ArrayList<>();
                String[] lines = getResources().getStringArray(R.array.lines);

                // 存储选择数据
                //步骤1：获取输入值
                //步骤2-1：创建一个SharedPreferences.Editor接口对象，lock表示要写入的XML文件名
                SharedPreferences.Editor editor = getSharedPreferences(FILENAME, MODE_PRIVATE).edit();
                //步骤2-2：将获取过来的值放入文件
                editor.putInt(LINE_KEY, pos + 1);
                //步骤3：提交
                editor.apply();

                mSelectedBusLineNumber = pos + 1;
                mSelectedBusLine = "Bus" + mSelectedBusLineNumber;

	            // 读取司机key
	            SharedPreferences settings = getSharedPreferences(FILENAME, MODE_PRIVATE);
	            boolean isDriver = settings.getBoolean(DRIVERKEY, false);
	            mRoleId = settings.getInt(DRIVERLINE_KEY, 0);
	            if (isDriver && mSelectedBusLineNumber == mRoleId){
		            mDriverChecked = true;
		            mDriver.setChecked(true);
		            mDriver.setButtonDrawable(getResources().getDrawable(R.drawable.driver_on));
	            }
	            else
	            {
		            mDriver.setButtonDrawable(getResources().getDrawable(R.drawable.driver_off));
		            mDriverChecked = false;
		            mDriver.setChecked(false);
	            }

                Toast.makeText(getApplicationContext(), "你选择的是:" + lines[pos].trim(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Another interface callback
            }
        });


        // 初始化路况控件
        final CheckBox mTraffic = (CheckBox) findViewById(R.id.traffic_btn);
        //路况图层触发事件
        mTraffic.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                //判断路况图层是否显示
                if (mTraffic.isChecked()) {
                    // 显示实时交通状况
                    mAMap.setTrafficEnabled(true);
                    mTraffic.setButtonDrawable(getResources().getDrawable(R.drawable.traffic_on));
                } else {
                    mAMap.setTrafficEnabled(false);
                    mTraffic.setButtonDrawable(getResources().getDrawable(R.drawable.traffic_off));
                }
            }
        });

        // 初始化司机控件
        mDriver = (CheckBox) findViewById(R.id.driver_btn);
        mDriverChecked = mDriver.isChecked();
        //司机图层触发事件
        mDriver.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                //判断司机图层是否显示
                if (mDriver.isChecked()) {
                    new AlertDialog.Builder(MainActivity.this).setTitle("老司机")//设置对话框标题
                            .setMessage("确定要为 " + mSelectedBusLine.replace("Bus", "") + "号线" + " 的同学指条明路吗？")//设置显示的内容
                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {//添加确定按钮
                                @Override
                                public void onClick(DialogInterface dialog, int which) {//确定按钮的响应事件
                                    mDriver.setButtonDrawable(getResources().getDrawable(R.drawable.driver_on));
                                    mDriverChecked = true;

                                    SharedPreferences.Editor editor = getSharedPreferences(FILENAME, MODE_PRIVATE).edit();
                                    //步骤2-2：将获取过来的值放入文件
                                    editor.putBoolean(DRIVERKEY, true);
                                    editor.putInt(DRIVERLINE_KEY, mSelectedBusLineNumber);
                                    mRoleId = mSelectedBusLineNumber;
                                    //步骤3：提交
                                    editor.apply();

	                                getStationList();
	                                postLocation(currentlocation, 0);

                                    dialog.dismiss();
                                }
                            }).setNegativeButton("返回", new DialogInterface.OnClickListener() {//添加返回按钮
                        @Override
                        public void onClick(DialogInterface dialog, int which) {//响应事件
                            mDriver.setChecked(false);
                            mDriverChecked = false;
                            dialog.dismiss();
                        }
                    }).show();//在按键响应事件中显示此对话框
                } else {
                    SharedPreferences.Editor editor = getSharedPreferences(FILENAME, MODE_PRIVATE).edit();
                    //步骤2-2：将获取过来的值放入文件
                    editor.putBoolean(DRIVERKEY, false);
                    editor.putInt(DRIVERLINE_KEY, 0);
                    //步骤3：提交
                    editor.apply();
                    Toast.makeText(getApplicationContext(), "已关闭老司机功能", Toast.LENGTH_SHORT).show();
                    mDriver.setButtonDrawable(getResources().getDrawable(R.drawable.driver_off));
                    mDriverChecked = false;
//	                mRoleId = 0;
	                postLocation(currentlocation, 0);
                }
            }
        });

        // 读取司机key
        boolean isDriver = settings.getBoolean(DRIVERKEY, false);
        mRoleId = settings.getInt(DRIVERLINE_KEY, 0);
        if (isDriver){
            mDriverChecked = true;
            mDriver.setChecked(true);
            mDriver.setButtonDrawable(getResources().getDrawable(R.drawable.driver_on));
            getStationList();
        }

        // 初始化离线地图
        final Button mDownload = (Button) findViewById(R.id.meBtn);
        //路况图层触发事件
        mDownload.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, OfflineMapActivity.class));
            }
        });
    }

    private void getDataTask() {

        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                getData();
                handler.postDelayed(this, FETCH_TIME_INTERVAL);
            }
        };

        handler.postDelayed(runnable, FETCH_TIME_INTERVAL);
    }

//    private void initRoadData() {
//
//        MarkerOptions markerOptions = new MarkerOptions();
//        markerOptions.setFlat(true);
//        markerOptions.anchor(0.5f, 0.5f);
//        markerOptions.icon(BitmapDescriptorFactory
//                .fromResource(R.drawable.map_icon_driver_car));
////        markerOptions.position(new LatLng(39.1489337022569, 117.301793619792));
//        mMarker = mAMap.addMarker(markerOptions);
////        mMarker.setRotateAngle((float) getAngle(0));
//
////        LatLngBounds.Builder builder=new LatLngBounds.Builder();
////        builder.include(new LatLng(39.896049, 116.379792));
////        builder.include(new LatLng(39.936049, 116.419792));
////        mAMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(),10));
//    }

    /**
     * 设置一些amap的属性
     */
    private void setUpMap() {
        mAMap.setLocationSource(this);// 设置定位监听
        mAMap.getUiSettings().setMyLocationButtonEnabled(true);// 设置默认定位按钮是否显示
        mAMap.setMyLocationEnabled(true);// 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
        // 设置定位的类型为定位模式 ，可以由定位、跟随或地图根据面向方向旋转几种
        mAMap.setMyLocationType(AMap.LOCATION_TYPE_LOCATE);

        // 指南针
        mAMap.getUiSettings().setCompassEnabled(true);
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            mMapView.onDestroy();
            handler.removeCallbacks(runnable);// 关闭定时器处理
        } catch (RuntimeException e) {
            Log.e(TAG, "onDestroy: ", e);
        }
    }

    @Override
    public void activate(OnLocationChangedListener listener) {
        mListener = listener;
        startLocation();
    }

    @Override
    public void deactivate() {
        mListener = null;
        if (mLocationClient != null) {
            mLocationClient.stopLocation();
            mLocationClient.onDestroy();

        }
        mLocationClient = null;
        mLocationOption = null;
    }

    @Override
    public void onLocationChanged(AMapLocation amapLocation) {
        if (mListener != null && amapLocation != null && amapLocation.getErrorCode() == 0) {
            mListener.onLocationChanged(amapLocation);// 显示系统小蓝点
	        currentlocation = new LatLng(amapLocation.getLatitude(),
                    amapLocation.getLongitude());
//            //添加Marker显示定位位置
//            if (mMarker == null) {
//                //如果是空的添加一个新的,icon方法就是设置定位图标，可以自定义
//                mMarker = mAMap.addMarker(new MarkerOptions()
//                        .position(mylocation)
//                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker)));
//            } else {
//                //已经添加过了，修改位置即可m
//                mMarker.setPosition(mylocation);
//            }
            //mAMap.moveCamera(CameraUpdateFactory.changeLatLng(mylocation));

            // 上传定位信息
            if (amapLocation.getAccuracy() < 80.0) {
	            // 判断班车是否在圆内，类似于地理围栏功能，返回YES，表示进入围栏，返回NO，表示离开围栏。
	            int stationId = 0;
	            for (int i = 0; i<mStationList.size(); i++) {
		            Station station = mStationList.get(i);
		            LatLng point2 = new LatLng(Double.valueOf(station.getLat()), Double.valueOf(station.getLng()));
		            float distance = AMapUtils.calculateLineDistance(currentlocation,point2);
		            if (distance < 100 ) {
			            stationId = station.getStationId();
			            break;
		            }
	            }

                postLocation(currentlocation, stationId);
            }
        }
    }

    private void startLocation() {
        if (mLocationClient == null) {
            //初始化定位
            mLocationClient = new AMapLocationClient(this);
            //设置定位回调监听
            mLocationClient.setLocationListener(this);

            //初始化定位参数
            mLocationOption = new AMapLocationClientOption();
            //设置定位模式为高精度模式，Battery_Saving为低功耗模式，Device_Sensors是仅设备模式
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            //设置是否返回地址信息（默认返回地址信息）
            mLocationOption.setNeedAddress(true);
            //设置是否只定位一次,默认为false
            mLocationOption.setOnceLocation(false);
            //设置是否强制刷新WIFI，默认为强制刷新
            mLocationOption.setWifiActiveScan(true);
            //设置是否允许模拟位置,默认为false，不允许模拟位置
            mLocationOption.setMockEnable(true);
            //设置定位间隔,单位毫秒,默认为2000ms
            mLocationOption.setInterval(3 * 1000);
            //给定位客户端对象设置定位参数
            mLocationClient.setLocationOption(mLocationOption);
            // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
            // 注意设置合适的定位时间的间隔（最小间隔支持为2000ms），并且在合适时间调用stopLocation()方法来取消定位请求
            // 在定位结束后，在合适的生命周期调用onDestroy()方法
            // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
            //启动定位
            mLocationClient.startLocation();
        }
    }

    public void getData() {
	    Retrofit retrofit = new Retrofit.Builder()
			    .baseUrl(URL)
			    //增加返回值为String的支持
			    .addConverterFactory(ScalarsConverterFactory.create())
			    //增加返回值为Gson的支持(以实体类返回)
			    .addConverterFactory(GsonConverterFactory.create())
			    //增加返回值为Oservable<T>的支持
			    .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
			    .build();
	    ApiService apiManager = retrofit.create(ApiService.class);//这里采用的是Java的动态代理模式

	    Call<CoordinateGson> call = apiManager.getCoordinateData(mSelectedBusLineNumber);
	    call.enqueue(new Callback<CoordinateGson>() {
		    @Override
		    public void onResponse(Call<CoordinateGson> call, Response<CoordinateGson> response) {
			    //处理请求成功

			    if (response.body().getData() == null) {
				    if (mMarker != null) {
					    mMarker.setVisible(false);
				    }
				    Toast.makeText(getApplicationContext(), mSelectedBusLine.replace("Bus", "") + "号线 没有开启定位", Toast.LENGTH_SHORT).show();
				    mLocations = new ArrayList<LatLng>();
			    } else {

				    Coordinate dataBean;
				    dataBean = response.body().getData();

				    // 设置当前地图显示为当前位置
				    LatLng latLng = new LatLng(Double.valueOf(dataBean.getLat()), Double.valueOf(dataBean.getLng()));

				    if (mLocations.size() > 1) {
					    mLocations.remove(0);
				    }

				    mLocations.add(latLng);

				    if (mMarker == null) {
					    MarkerOptions mMarkerOption = new MarkerOptions();
					    mMarkerOption.position(latLng);
					    mMarkerOption.title("班车");
					    mMarkerOption.draggable(true);
					    mMarkerOption.anchor(0.5f, 0.5f);
					    mMarkerOption.icon(
							    BitmapDescriptorFactory.fromBitmap(BitmapFactory
									    .decodeResource(getResources(),
											    R.drawable.map_icon_driver_car)));
					    // 将Marker设置为贴地显示，可以双指下拉看效果
					    mMarkerOption.setFlat(true);
					    mMarkerOption.visible(true);

					    //mMarker.setPosition(latLng);
					    mMarker = mAMap.addMarker(mMarkerOption);
					    //mMarker.setRotateAngle(-90);
				    } else {
					    //mMarker.setPosition(latLng);
					    mMarker.setVisible(true);
				    }

				    if (mSelectedBusLineChanged) {
					    mMarker.setRotateAngle(0);
					    mMarker.setPosition(latLng);
					    mAMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14));
					    mSelectedBusLineChanged = false;
				    }
			    }

			    if (mLocations.size() > 1) {
				    if (mLocations.get(0).latitude != mLocations.get(1).latitude
						    || mLocations.get(0).longitude != mLocations.get(1).longitude) {
					    // 距离
					    //float distance = AMapUtils.calculateLineDistance(mLocations.get(1),mLocations.get(0));
					    moveLooper();
				    }
			    }
		    }

		    @Override
		    public void onFailure(Call<CoordinateGson> call, Throwable t) {
			    //处理请求失败
			    if (mMarker != null) {
				    mMarker.remove();
			    }
			    Toast.makeText(getApplicationContext(), mSelectedBusLine + "获取班车位置出现错误", Toast.LENGTH_SHORT).show();
		    }
	    });
    }

    public void getStationList() {
		mStationList = new ArrayList<>();
	    if (mRoleId == 0) { return; }

	    Retrofit retrofit = new Retrofit.Builder()
			    .baseUrl(URL)
			    //增加返回值为String的支持
			    .addConverterFactory(ScalarsConverterFactory.create())
			    //增加返回值为Gson的支持(以实体类返回)
			    .addConverterFactory(GsonConverterFactory.create())
			    //增加返回值为Oservable<T>的支持
			    .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
			    .build();
	    ApiService apiManager = retrofit.create(ApiService.class);//这里采用的是Java的动态代理模式

	    Call<StationGson> call = apiManager.getStations(mRoleId);
	    call.enqueue(new Callback<StationGson>() {
		    @Override
		    public void onResponse(Call<StationGson> call, Response<StationGson> response) {
			    //处理请求成功
			    if (response.body().getData() != null) {

				    mStationList = response.body().getData();

			    }
		    }

		    @Override
		    public void onFailure(Call<StationGson> call, Throwable t) {
			    //处理请求失败
			    Toast.makeText(getApplicationContext(), mSelectedBusLine + "获取班车位置出现错误", Toast.LENGTH_SHORT).show();
		    }
	    });

//                .subscribeOn(Schedulers.io())
//                .map(new Func1<CoordinateGson, List<Coordinate>>() {
//                    @Override
//                    public List<Coordinate> call(CoordinateGson coordinateGson) { //
//                        List<Coordinate> coordinatesList = new ArrayList<Coordinate>();
//                        for (Coordinate dataBean : coordinateGson.getData()) {
//                            Coordinate Coordinate = new Coordinate();
//                            Coordinate.setId(String.valueOf(dataBean.getId()));
//                            Coordinate.setLat(dataBean.getLat());
//                            Coordinate.setLng(dataBean.getLng());
//                            Coordinate.setUpdateTime(dataBean.getUpdateTime());
//                            Coordinate.setCreatedTime(dataBean.getCreatedTime());
//                            Coordinate.setUser(dataBean.getUser());
//                            Coordinate.setRole(dataBean.getRole());
//                            coordinatesList.add(Coordinate);
//                        }
//                        return coordinatesList; // 返回类型
//                    }
//                })
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(new Subscriber<List<Coordinate>>() {
//                    @Override
//                    public void onNext(List<Coordinate> coordinatesList) {
//                        Log.i("coordinatesList:", String.valueOf(coordinatesList.size()));
//                    }
//
//                    @Override
//                    public void onCompleted() {
//                        Log.i("onCompleted","onCompleted");
//                    }
//
//                    @Override
//                    public void onError(Throwable e) {
//                        Toast.makeText(getApplicationContext(), "网络连接失败", Toast.LENGTH_LONG).show();
//                    }
//                });
    }

    public void postLocation(LatLng myLocation, int stationId) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(URL)
                //增加返回值为String的支持
                .addConverterFactory(ScalarsConverterFactory.create())
                //增加返回值为Gson的支持(以实体类返回)
                .addConverterFactory(GsonConverterFactory.create())
                //增加返回值为Oservable<T>的支持
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build();
        ApiService apiManager = retrofit.create(ApiService.class);//这里采用的是Java的动态代理模式

        Coordinate coordinate = new Coordinate();
        coordinate.setUuid(getUUID(this));
        coordinate.setLat(String.valueOf(myLocation.latitude));
        coordinate.setLng(String.valueOf(myLocation.longitude));

        if(mDriverChecked){
            coordinate.setRoleId(mRoleId);
        }else{
            coordinate.setRoleId(0);
        }

	    if(stationId > 0){
		    coordinate.setStationId(stationId);
	    }

        Call<Coordinate> call = apiManager.updateCoordinate(coordinate);
        call.enqueue(new Callback<Coordinate>() {
            @Override
            public void onResponse(Call<Coordinate> call, Response<Coordinate> response) {
                //处理请求成功
            }

            @Override
            public void onFailure(Call<Coordinate> call, Throwable t) {
                //处理请求失败
                //Toast.makeText(getApplicationContext(), "位置上传失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public String getUUID(Context context) {
        try {
            // 读取存储数据
            SharedPreferences settings = getSharedPreferences(FILENAME, MODE_PRIVATE);
            mUUID = settings.getString(UUIDKEY, "");

            if (mUUID.equals("")) {

                // IMEI
                TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                String m_szImei = tm.getDeviceId();

                // The WLAN MAC Address string
                WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                String m_szWLANMAC = wm.getConnectionInfo().getMacAddress();

                String m_szLongID = m_szImei + m_szWLANMAC;
                // compute md5
                MessageDigest m = null;
                try {
                    m = MessageDigest.getInstance("MD5");
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
                m.update(m_szLongID.getBytes(), 0, m_szLongID.length());
                // get md5 bytes
                byte p_md5Data[] = m.digest();
                // create a hex string
                String m_szUniqueID = new String();
                for (int i = 0; i < p_md5Data.length; i++) {
                    int b = (0xFF & p_md5Data[i]);
                    // if it is a single digit, make sure it have 0 in front (proper padding)
                    if (b <= 0xF)
                        m_szUniqueID += "0";
                    // add number to string
                    m_szUniqueID += Integer.toHexString(b);
                }   // hex string to uppercase
                mUUID = m_szUniqueID.toUpperCase();


                // 存储UUID
                //步骤1：获取输入值
                //步骤2-1：创建一个SharedPreferences.Editor接口对象，lock表示要写入的XML文件名
                SharedPreferences.Editor editor = getSharedPreferences(FILENAME, MODE_PRIVATE).edit();
                //步骤2-2：将获取过来的值放入文件
                editor.putString(UUIDKEY, mUUID);
                //步骤3：提交
                editor.apply();
            }
        }catch(Exception ignored){

        }

        return mUUID;
    }


    /**
     * 根据点获取图标转的角度
     */
    private double getAngle(int startIndex) {

        if ((startIndex + 1) > mLocations.size()) {
            return 0;
        }
        LatLng startPoint = mLocations.get(startIndex);
        LatLng endPoint = mLocations.get(startIndex + 1);
        return getAngle(startPoint, endPoint);
    }

    /**
     * 根据两点算取图标转的角度
     */
    private double getAngle(LatLng fromPoint, LatLng toPoint) {
        double slope = getSlope(fromPoint, toPoint);
        if (slope == Double.MAX_VALUE) {
            if (toPoint.latitude > fromPoint.latitude) {
                return 0;
            } else {
                return 180;
            }
        }
        float deltAngle = 0;
        if ((toPoint.latitude - fromPoint.latitude) * slope < 0) {
            deltAngle = 180;
        }
        double radio = Math.atan(slope);
        return 180 * (radio / Math.PI) + deltAngle - 90;
    }

    /**
     * 根据点和斜率算取截距
     */
    private double getInterception(double slope, LatLng point) {

        return point.latitude - slope * point.longitude;
    }

    /**
     * 算斜率
     */
    private double getSlope(LatLng fromPoint, LatLng toPoint) {
        if (toPoint.longitude == fromPoint.longitude) {
            return Double.MAX_VALUE;
        }
        return ((toPoint.latitude - fromPoint.latitude) / (toPoint.longitude - fromPoint.longitude));
    }

    /**
     * 计算每次移动的距离
     */
    private double getMoveDistance(double slope) {
        if (slope == Double.MAX_VALUE || slope == 0) {
            return DISTANCE;
        }
        return Math.abs((DISTANCE * slope) / Math.sqrt(1 + slope * slope));
    }

    /**
     * 判断是否为反序
     */
    private boolean isReverse(LatLng startPoint, LatLng endPoint, double slope) {
        if (slope == 0) {
            return startPoint.longitude > endPoint.longitude;
        }
        return (startPoint.latitude > endPoint.latitude);

    }

    /**
     * 获取循环初始值大小
     */
    private double getStart(LatLng startPoint, double slope) {
        if (slope == 0) {
            return startPoint.longitude;
        }
        return startPoint.latitude;
    }

    /**
     * 获取循环结束大小
     */
    private double getEnd(LatLng endPoint, double slope) {
        if (slope == 0) {
            return endPoint.longitude;
        }
        return endPoint.latitude;
    }

    /**
     * 循环进行移动逻辑
     */
    public void moveLooper() {
        new Thread() {

            public void run() {
                LatLng startPoint = mLocations.get(0);
                LatLng endPoint = mLocations.get(1);
                mMarker.setPosition(startPoint);
                mMarker.setRotateAngle((float) getAngle(startPoint, endPoint));

                double slope = getSlope(startPoint, endPoint);
                boolean isReverse = isReverse(startPoint, endPoint, slope);
                double moveDistance = isReverse ? getMoveDistance(slope) : -1 * getMoveDistance(slope);
                double intercept = getInterception(slope, startPoint);

                int m = 1;
                for (double j = getStart(startPoint, slope); (j > getEnd(endPoint, slope)) == isReverse; j = j - moveDistance) {
                    m++;
                }
                long duration = FETCH_TIME_INTERVAL / m;

                for (double j = getStart(startPoint, slope); (j > getEnd(endPoint, slope)) == isReverse; j = j - moveDistance) {
                    LatLng latLng;
                    if (slope == 0) {
                        latLng = new LatLng(startPoint.latitude, j);
                    } else if (slope == Double.MAX_VALUE) {
                        latLng = new LatLng(j, startPoint.longitude);
                    } else {

                        latLng = new LatLng(j, (j - intercept) / slope);
                    }
                    mMarker.setPosition(latLng);
                    mAMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14));
                    try {
                        Thread.sleep(duration);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

        }.start();
    }

}
