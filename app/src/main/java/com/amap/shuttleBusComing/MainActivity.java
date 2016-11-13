package com.amap.shuttleBusComing;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationClientOption.AMapLocationMode;
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
import com.amap.api.maps.model.PolylineOptions;
import com.amap.database.DbAdapter;
import com.amap.record.PathRecord;
import com.amap.util.ApiService;
import com.amap.util.Coordinate;
import com.amap.util.CoordinateGson;
import com.liupeng.shuttleBusComing.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class MainActivity extends Activity implements LocationSource,
		AMapLocationListener {
	private MapView mMapView;
	private AMap mAMap;
	private OnLocationChangedListener mListener;
	private AMapLocationClient mLocationClient;
	private AMapLocationClientOption mLocationOption;
	private PolylineOptions mPolyoptions;
	private PathRecord record;
	private long mStartTime;
	private long mEndTime;
	private ToggleButton btn;
	private DbAdapter DbHepler;
    private Handler handler;
    private Runnable runnable;
    private Marker mMarker;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.basicmap_activity);
		mMapView = (MapView) findViewById(R.id.map);
		mMapView.onCreate(savedInstanceState);// 此方法必须重写
		init();
		initpolyline();


        getDataTask();
	}

	/**
	 * 初始化AMap对象
	 */
	private void init() {
		if (mAMap == null) {
			mAMap = mMapView.getMap();
			setUpMap();
		}
//		btn = (ToggleButton) findViewById(R.id.locationbtn);
//		btn.setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				if (btn.isChecked()) {
//					Log.i("MY", "isChecked");
//
//					mAMap.clear(true);
//					if (record != null) {
//						record = null;
//					}
//					record = new PathRecord();
//					mStartTime = System.currentTimeMillis();
//					record.setDate(getcueDate(mStartTime));
//				} else {
//					mEndTime = System.currentTimeMillis();
//					saveRecord(record.getPathline(), record.getDate());
//				}
//			}
//		});

	}

    private void getDataTask() {
        handler=new Handler();
        runnable=new Runnable() {
            @Override
            public void run() {
                getData();
                handler.postDelayed(this, 5000);
            }
        };

        handler.postDelayed(runnable, 5000);
    }

    protected void saveRecord(List<AMapLocation> list, String time) {
		if (list != null && list.size() > 0) {
			DbHepler = new DbAdapter(this);
			DbHepler.open();
			String duration = getDuration();
			float distance = getDistance(list);
			String average = getAverage(distance);
			String pathlineSring = getPathLineString(list);
			AMapLocation firstLocaiton = list.get(0);
			AMapLocation lastLocaiton = list.get(list.size() - 1);
			String stratpoint = amapLocationToString(firstLocaiton);
			String endpoint = amapLocationToString(lastLocaiton);
			DbHepler.createrecord(String.valueOf(distance), duration, average,
					pathlineSring, stratpoint, endpoint, time);
			DbHepler.close();
		} else {
			Toast.makeText(MainActivity.this, "没有记录到路径", Toast.LENGTH_SHORT)
					.show();
		}
	}

	private String getDuration() {
		return String.valueOf((mEndTime - mStartTime) / 1000f);
	}

	private String getAverage(float distance) {
		return String.valueOf(distance / (float) (mEndTime - mStartTime));
	}

	private float getDistance(List<AMapLocation> list) {
		float distance = 0;
		if (list == null || list.size() == 0) {
			return distance;
		}
		for (int i = 0; i < list.size() - 1; i++) {
			AMapLocation firstpoint = list.get(i);
			AMapLocation secondpoint = list.get(i + 1);
			LatLng firstLatLng = new LatLng(firstpoint.getLatitude(),
					firstpoint.getLongitude());
			LatLng secondLatLng = new LatLng(secondpoint.getLatitude(),
					secondpoint.getLongitude());
			double betweenDis = AMapUtils.calculateLineDistance(firstLatLng,
					secondLatLng);
			distance = (float) (distance + betweenDis);
		}
		return distance;
	}

	private String getPathLineString(List<AMapLocation> list) {
		if (list == null || list.size() == 0) {
			return "";
		}
		StringBuffer pathline = new StringBuffer();
		for (int i = 0; i < list.size(); i++) {
			AMapLocation location = list.get(i);
			String locString = amapLocationToString(location);
			pathline.append(locString).append(";");
		}
		String pathLineString = pathline.toString();
		pathLineString = pathLineString.substring(0,
				pathLineString.length() - 1);
		return pathLineString;
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
		mMapView.onDestroy();
        handler.removeCallbacks(runnable);// 关闭定时器处理
	}

	@Override
	public void activate(OnLocationChangedListener listener) {
		mListener = listener;
		startlocation();
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
		if (mListener != null && amapLocation != null) {
			if (amapLocation != null && amapLocation.getErrorCode() == 0) {
				mListener.onLocationChanged(amapLocation);// 显示系统小蓝点
				LatLng mylocation = new LatLng(amapLocation.getLatitude(),
						amapLocation.getLongitude());
				mAMap.moveCamera(CameraUpdateFactory.changeLatLng(mylocation));
				// 上传定位信息
                if (amapLocation.getAccuracy() < 80.0) {
                    record.postLocation(amapLocation);

                    record.addpoint(amapLocation);
                    mPolyoptions.add(mylocation);
                    redrawline();
                }
			}
		}
	}

	private void startlocation() {
		if (mLocationClient == null) {
			mLocationClient = new AMapLocationClient(this);
			mLocationOption = new AMapLocationClientOption();
			// 设置定位监听
			mLocationClient.setLocationListener(this);
			// 设置为高精度定位模式
			mLocationOption.setLocationMode(AMapLocationMode.Hight_Accuracy);

			mLocationOption.setInterval(2000);
			// 设置定位参数
			mLocationClient.setLocationOption(mLocationOption);
			// 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
			// 注意设置合适的定位时间的间隔（最小间隔支持为2000ms），并且在合适时间调用stopLocation()方法来取消定位请求
			// 在定位结束后，在合适的生命周期调用onDestroy()方法
			// 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
			mLocationClient.startLocation();

		}
	}

	private void redrawline() {
		if (mPolyoptions.getPoints().size() > 0) {
			mAMap.clear(true);
			mAMap.addPolyline(mPolyoptions);
		}
	}

	@SuppressLint("SimpleDateFormat")
	private String getcueDate(long time) {
		SimpleDateFormat formatter = new SimpleDateFormat(
				"yyyy-MM-dd  HH:mm:ss ");
		Date curDate = new Date(time);
		String date = formatter.format(curDate);
		return date;
	}

	public void record(View view) {
		Intent intent = new Intent(MainActivity.this, RecordActivity.class);
		startActivity(intent);
	}

    public void getData() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://180.76.169.196:8000/")
                //增加返回值为String的支持
                .addConverterFactory(ScalarsConverterFactory.create())
                //增加返回值为Gson的支持(以实体类返回)
                .addConverterFactory(GsonConverterFactory.create())
                //增加返回值为Oservable<T>的支持
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build();
        ApiService apiManager = retrofit.create(ApiService.class);//这里采用的是Java的动态代理模式


        Call<CoordinateGson> call = apiManager.getCoordinateData("Bus6");
        call.enqueue(new Callback<CoordinateGson>() {
            @Override
            public void onResponse(Call<CoordinateGson> call, Response<CoordinateGson> response) {
                //处理请求成功
                List<Coordinate> coordinatesList = new ArrayList<Coordinate>();
                for (CoordinateGson.DataBean dataBean : response.body().getData()) {
                    if (mMarker != null) {
                        mMarker.remove();
                    }
                    // 设置当前地图显示为当前位置
                    LatLng latLng = new LatLng(Double.valueOf(dataBean.getLat()),Double.valueOf(dataBean.getLng()));
                    mAMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                    MarkerOptions mMarkerOption = new MarkerOptions();
                    mMarkerOption.position(latLng);
                    mMarkerOption.title("班车");
                    mMarkerOption.draggable(true);
                    mMarkerOption.icon(
                            BitmapDescriptorFactory.fromBitmap(BitmapFactory
                                    .decodeResource(getResources(),
                                            R.drawable.gps)));
                    // 将Marker设置为贴地显示，可以双指下拉看效果
                    mMarkerOption.setFlat(true);
                    mMarkerOption.visible(true);
                    mMarker = mAMap.addMarker(mMarkerOption);
                }
            }

            @Override
            public void onFailure(Call<CoordinateGson> call, Throwable t) {
                //处理请求失败
                Toast.makeText(getApplicationContext(), "位置获取失败", Toast.LENGTH_SHORT).show();
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

}
