package com.amap.shuttleBusComing;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
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
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.PolylineOptions;
import com.amap.database.DbAdapter;
import com.amap.record.PathRecord;
import com.liupeng.shuttleBusComing.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.basicmap_activity);
		mMapView = (MapView) findViewById(R.id.map);
		mMapView.onCreate(savedInstanceState);// 此方法必须重写
		init();
		initpolyline();
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

}
