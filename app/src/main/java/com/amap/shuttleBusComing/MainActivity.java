package com.amap.shuttleBusComing;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.PolylineOptions;
import com.amap.util.ApiService;
import com.amap.util.Coordinate;
import com.amap.util.CoordinateGson;
import com.liupeng.shuttleBusComing.R;

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
    private OnLocationChangedListener mListener;
    private AMapLocationClient mLocationClient;
    private AMapLocationClientOption mLocationOption;
    private PolylineOptions mPolyoptions;
    private Handler handler;
    private Runnable runnable;
    private Marker mMarker;
    private String mSelectedBusLine;
    final String URL = "http://180.76.169.196:8000/";
    final String mFileName = "BusLine";
    final String mLineKey = "LINE_KEY";
    final String mUUIDKey = "UUID_KEY";


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
    }

    /**
     * 初始化AMap对象
     */
    private void init() {
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
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, mItems);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //绑定 Adapter到控件
        spinner.setAdapter(adapter);

        // 读取存储数据
        SharedPreferences settings = getSharedPreferences(mFileName, MODE_PRIVATE);
        mSelectedBusLine = "Bus" + settings.getInt(mLineKey, 0);

        if (!mSelectedBusLine.equals("Bus0")) {
            spinner.setSelection(settings.getInt(mLineKey, 0) - 1);
        }

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int pos, long id) {
                String[] lines = getResources().getStringArray(R.array.lines);

                // 存储选择数据
                //步骤1：获取输入值
                //步骤2-1：创建一个SharedPreferences.Editor接口对象，lock表示要写入的XML文件名
                SharedPreferences.Editor editor = getSharedPreferences(mFileName, MODE_PRIVATE).edit();
                //步骤2-2：将获取过来的值放入文件
                editor.putInt(mLineKey, pos + 1);
                //步骤3：提交
                editor.apply();

                mSelectedBusLine = "Bus" + (pos + 1);

                Toast.makeText(getApplicationContext(), "你选择的是:" + lines[pos].trim(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Another interface callback
            }
        });
    }

    private void getDataTask() {

        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                getData();
                handler.postDelayed(this, 5000);
            }
        };

        handler.postDelayed(runnable, 5000);
    }

    private String amapLocationToString(AMapLocation location) {
        StringBuffer locString = new StringBuffer();
        locString.append(location.getLatitude()).append(",");
        locString.append(location.getLongitude()).append(",");
        locString.append(location.getProvider()).append(",");
        locString.append(location.getTime()).append(",");
        locString.append(location.getSpeed()).append(",");
        locString.append(location.getBearing());
        return locString.toString();
    }

    private void initpolyline() {
        mPolyoptions = new PolylineOptions();
        mPolyoptions.width(10f);
        mPolyoptions.color(Color.BLUE);
    }

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
        }
        catch(RuntimeException e) {
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
    }

    @Override
    public void onLocationChanged(AMapLocation amapLocation) {
        if (mListener != null && amapLocation != null && amapLocation.getErrorCode() == 0) {
            mListener.onLocationChanged(amapLocation);// 显示系统小蓝点
            LatLng mylocation = new LatLng(amapLocation.getLatitude(),
                    amapLocation.getLongitude());
            //mAMap.moveCamera(CameraUpdateFactory.changeLatLng(mylocation));
            // 上传定位信息
            if (amapLocation.getAccuracy() < 80.0) {
                postLocation(mylocation);
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
            //mLocationOption.setMockEnable(true);
            //设置定位间隔,单位毫秒,默认为2000ms
            mLocationOption.setInterval(3000);
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

        Call<CoordinateGson> call = apiManager.getCoordinateData(mSelectedBusLine);
        call.enqueue(new Callback<CoordinateGson>() {
            @Override
            public void onResponse(Call<CoordinateGson> call, Response<CoordinateGson> response) {
                //处理请求成功

                if (mMarker != null) {
                    mMarker.remove();
                }

                List<Coordinate> coordinatesList = new ArrayList<Coordinate>();
                for (CoordinateGson.DataBean dataBean : response.body().getData()) {

                    // 设置当前地图显示为当前位置
                    LatLng latLng = new LatLng(Double.valueOf(dataBean.getLat()), Double.valueOf(dataBean.getLng()));
                    mAMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                    MarkerOptions mMarkerOption = new MarkerOptions();
                    mMarkerOption.position(latLng);
                    mMarkerOption.title("班车");
                    mMarkerOption.draggable(true);
                    mMarkerOption.icon(
                            BitmapDescriptorFactory.fromBitmap(BitmapFactory
                                    .decodeResource(getResources(),
                                            R.drawable.marker)));
                    // 将Marker设置为贴地显示，可以双指下拉看效果
                    mMarkerOption.setFlat(true);
                    mMarkerOption.visible(true);

                    //mMarker.setPosition(latLng);
                    mMarker = mAMap.addMarker(mMarkerOption);
                }
            }

            @Override
            public void onFailure(Call<CoordinateGson> call, Throwable t) {
                //处理请求失败
                Toast.makeText(getApplicationContext(), mSelectedBusLine + "位置获取失败", Toast.LENGTH_SHORT).show();
            }
        });

//                .subscribeOn(Schedulers.io())
//                .map(new Func1<CoordinateGson, List<Coordinate>>() {
//                    @Override
//                    public List<Coordinate> call(CoordinateGson coordinateGson) { //
//                        List<Coordinate> coordinatesList = new ArrayList<Coordinate>();
//                        for (CoordinateGson.DataBean dataBean : coordinateGson.getData()) {
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

    public void postLocation(LatLng myLocation) {
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
        coordinate.setUser(getuuid(this));
        coordinate.setLat(String.valueOf(myLocation.latitude));
        coordinate.setLng(String.valueOf(myLocation.longitude));

        Call<Coordinate> call = apiManager.updateCoordinate(coordinate);
        call.enqueue(new Callback<Coordinate>() {
            @Override
            public void onResponse(Call<Coordinate> call, Response<Coordinate> response) {
                //处理请求成功
            }

            @Override
            public void onFailure(Call<Coordinate> call, Throwable t) {
                //处理请求失败
                Toast.makeText(getApplicationContext(), "位置获取失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public String getuuid(Context context) {
        String mUUID = "";
        // 读取存储数据
        SharedPreferences settings = getSharedPreferences(mFileName, MODE_PRIVATE);
        mUUID = settings.getString(mUUIDKey, "");

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
            SharedPreferences.Editor editor = getSharedPreferences(mFileName, MODE_PRIVATE).edit();
            //步骤2-2：将获取过来的值放入文件
            editor.putString(mUUIDKey, mUUID);
            //步骤3：提交
            editor.apply();
        }

        return mUUID;
    }

}
