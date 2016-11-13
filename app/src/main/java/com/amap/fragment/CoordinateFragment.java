package com.amap.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.amap.api.maps.MapView;
import com.amap.util.ApiService;
import com.amap.util.Coordinate;
import com.amap.util.CoordinateGson;
import com.liupeng.shuttleBusComing.R;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by LiuPeng on 2016/11/12.
 */

public class CoordinateFragment extends BaseFragment {

    @BindView(R.id.map)
    MapView mMapView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.basicmap_activity, container, false);
        ButterKnife.bind(this, view);
        //mMapView.setAnimation();
        return view;

    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getData();
    }


    private void getData() {
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
        apiManager.getCoordinateData("Bus6")
                .subscribeOn(Schedulers.io())
                .map(new Func1<CoordinateGson, List<Coordinate>>() {
                    @Override
                    public List<Coordinate> call(CoordinateGson coordinateGson) { //
                        List<Coordinate> coordinatesList = new ArrayList<Coordinate>();
                        for (CoordinateGson.CoordinatelistBean coordinatelistBean : coordinateGson.getCoordinatelist()) {
                            Coordinate Coordinate1 = new Coordinate();
                            Coordinate1.setLat(coordinatelistBean.getLat());
                            Coordinate1.setLng(coordinatelistBean.getLng());
                            Coordinate1.setUpdateTime(coordinatelistBean.getUpdateTime());
                            Coordinate1.setCreatedTime(coordinatelistBean.getCreatedTime());
                            Coordinate1.setUser(coordinatelistBean.getUser());
                            Coordinate1.setRole(coordinatelistBean.getRole());
                            coordinatesList.add(Coordinate1);
                        }
                        return coordinatesList; // 返回类型
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<List<Coordinate>>() {
                    @Override
                    public void onNext(List<Coordinate> coordinatesList) {

                    }

                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(getContext(),
                                "网络连接失败", Toast.LENGTH_LONG).show();
                    }
                });
    }

}
