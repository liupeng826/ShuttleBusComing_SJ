// Generated code from Butter Knife. Do not modify!
package com.amap.fragment;

import android.support.annotation.CallSuper;
import android.support.annotation.UiThread;
import android.view.View;
import butterknife.Unbinder;
import butterknife.internal.Utils;
import com.amap.api.maps.MapView;
import com.liupeng.shuttleBusComing.R;
import java.lang.IllegalStateException;
import java.lang.Override;

public class CoordinateFragment_ViewBinding<T extends CoordinateFragment> implements Unbinder {
  protected T target;

  @UiThread
  public CoordinateFragment_ViewBinding(T target, View source) {
    this.target = target;

    target.mMapView = Utils.findRequiredViewAsType(source, R.id.map, "field 'mMapView'", MapView.class);
  }

  @Override
  @CallSuper
  public void unbind() {
    T target = this.target;
    if (target == null) throw new IllegalStateException("Bindings already cleared.");

    target.mMapView = null;

    this.target = null;
  }
}
