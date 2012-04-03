/**
 * This file was auto-generated by the Titanium Module SDK helper for Android
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 *
 */
package org.appcelerator.openstreetmap;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import org.andnav.osm.util.GeoPoint;
import org.andnav.osm.util.TypeConverter;
import org.andnav.osm.views.OpenStreetMapView;
import org.andnav.osm.views.controller.OpenStreetMapViewController;
import org.andnav.osm.views.util.OpenStreetMapRendererInfo;
import org.andnav.osm.views.util.OpenStreetMapTileFilesystemProvider;
import org.andnav.osm.views.util.StreamUtils;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.view.TiUIView;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.SearchRecentSuggestions;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.Toast;

import com.robert.maps.R;
import com.robert.maps.kml.PoiManager;
import com.robert.maps.overlays.CurrentTrackOverlay;
import com.robert.maps.overlays.MyLocationOverlay;
import com.robert.maps.overlays.PoiOverlay;
import com.robert.maps.overlays.SearchResultOverlay;
import com.robert.maps.overlays.TrackOverlay;
import com.robert.maps.overlays.YandexTrafficOverlay;
import com.robert.maps.utils.CompassView;
import com.robert.maps.utils.ScaleBarDrawable;
import com.robert.maps.utils.SearchSuggestionsProvider;
import com.robert.maps.utils.Ut;

public class TiMapView extends TiUIView
{
	// Standard Debugging variables
	private static final String LCAT = "OpenStreetMapView";
	private static final boolean DBG = true;//TiConfig.LOGD;
	
	private Activity mParentActivity;
	private OpenStreetMapView mOsmv; //, mOsmvMinimap;
	private PoiOverlay mPoiOverlay;
	private CurrentTrackOverlay mCurrentTrackOverlay;
	private TrackOverlay mTrackOverlay;
	private SearchResultOverlay mSearchResultOverlay;
	private MyLocationOverlay mMyLocationOverlay;
	private final SensorEventListener mListener = new SensorEventListener() {
		private int iOrientation = -1;

		public void onAccuracyChanged(Sensor sensor, int accuracy) {

		}

		public void onSensorChanged(SensorEvent event) {
			if (iOrientation < 0)
				iOrientation = ((WindowManager) mParentActivity.getSystemService(Context.WINDOW_SERVICE))
						.getDefaultDisplay().getOrientation();

			mCompassView.setAzimuth(event.values[0] + 90 * iOrientation);
			mCompassView.invalidate();

			if (mCompassEnabled)
				if (mNorthDirectionUp)
					if (mDrivingDirectionUp == false || mLastSpeed == 0) {
						mOsmv.setBearing(updateBearing(event.values[0]) + 90 * iOrientation);
						mOsmv.invalidate();
					}
		}

	};
	private Handler mCallbackHandler = new Handler() {
		@Override
		public void handleMessage(final Message msg) {
			final int what = msg.what;
			switch(what){
			case R.id.user_moved_map:
				setAutoFollow(false);
				break;
			case R.id.set_title:
//				setTitle();
				break;
			case R.id.add_yandex_bookmark:
//				showDialog(R.id.add_yandex_bookmark);
				break;
			case OpenStreetMapTileFilesystemProvider.ERROR_MESSAGE:
				if(msg.obj != null)
					Toast.makeText(mParentActivity, msg.obj.toString(), Toast.LENGTH_LONG).show();
				break;
			}
		}
	};
	private boolean mAutoFollow = true;
	private ImageView ivAutoFollow;
	private CompassView mCompassView;
	private SensorManager mOrientationSensorManager;
	private boolean mCompassEnabled;
	private boolean mDrivingDirectionUp;
	private boolean mNorthDirectionUp;
	private int mScreenOrientation;
	private float mLastSpeed, mLastBearing;
	private PoiManager mPoiManager;
	private String ACTION_SHOW_POINTS = "com.robert.maps.action.SHOW_POINTS";
	
	// Constructor
	public TiMapView(final TiViewProxy proxy) {
		super(proxy);
		mParentActivity = ((TiContext)proxy.getTiContext()).getActivity();
        mPoiManager = new PoiManager(mParentActivity);
        mOrientationSensorManager = (SensorManager)mParentActivity.getSystemService(Context.SENSOR_SERVICE);
//       	final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);

 		final RelativeLayout rl = new RelativeLayout(mParentActivity);
        OpenStreetMapRendererInfo RendererInfo = new OpenStreetMapRendererInfo(mParentActivity.getResources(), "");

        this.mOsmv = new OpenStreetMapView(mParentActivity, RendererInfo);
        this.mOsmv.setMainActivityCallbackHandler(mCallbackHandler);
        rl.addView(this.mOsmv, new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        
        
       	final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mParentActivity);
        
        /* SingleLocation-Overlay */
        {
	        /* Create a static Overlay showing a single location. (Gets updated in onLocationChanged(Location loc)! */
        	if(RendererInfo.YANDEX_TRAFFIC_ON == 1)
        		mOsmv.getOverlays().add(new YandexTrafficOverlay(mParentActivity, mOsmv));

	        mMyLocationOverlay = new MyLocationOverlay(mParentActivity);
	        mOsmv.getOverlays().add(mMyLocationOverlay);

	        mSearchResultOverlay = new SearchResultOverlay(mParentActivity);
	        mOsmv.getOverlays().add(mSearchResultOverlay);
        }

        /* Itemized Overlay */
		{
			mTrackOverlay = new TrackOverlay(mParentActivity, mPoiManager);
			mOsmv.getOverlays().add(mTrackOverlay);

			mCurrentTrackOverlay = new CurrentTrackOverlay(mParentActivity, mPoiManager, mOsmv);
			mOsmv.getOverlays().add(mCurrentTrackOverlay);

			mPoiOverlay = new PoiOverlay(mParentActivity, mPoiManager, null, pref.getBoolean("pref_hidepoi", false));
			mOsmv.getOverlays().add(mPoiOverlay);
		}

		{
        	final int sideBottom = Integer.parseInt(pref.getString("pref_zoomctrl", "1"));

            /* Compass */
        	mCompassView = new CompassView(mParentActivity, sideBottom == 2 ? false : true);
	        mCompassView.setVisibility(mCompassEnabled ? View.VISIBLE : View.INVISIBLE);
	        /* Create RelativeLayoutParams, that position in in the top right corner. */
	        final RelativeLayout.LayoutParams compassParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
	        compassParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
	        compassParams.addRule(!(sideBottom == 2 ? false : true) ? RelativeLayout.ALIGN_PARENT_BOTTOM : RelativeLayout.ALIGN_PARENT_TOP);
	        rl.addView(mCompassView, compassParams);

	        /* AutoFollow */
	        ivAutoFollow = new ImageView(mParentActivity);
	        ivAutoFollow.setImageResource(R.drawable.autofollow);
	        ivAutoFollow.setVisibility(ImageView.INVISIBLE);
	        /* Create RelativeLayoutParams, that position in in the top right corner. */
	        final RelativeLayout.LayoutParams followParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
	        followParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
	        followParams.addRule(!(sideBottom == 2 ? false : true) ? RelativeLayout.ALIGN_PARENT_BOTTOM : RelativeLayout.ALIGN_PARENT_TOP);
	        rl.addView(ivAutoFollow, followParams);

	        ivAutoFollow.setOnClickListener(new OnClickListener(){
	        	// @Override
	        	public void onClick(View v) {
	        		setAutoFollow(true);
	        		mSearchResultOverlay.Clear();
	        		setLastKnownLocation();
	        	}
	        });
        }

        /*ScaleBarView*/
        if(pref.getBoolean("pref_showscalebar", true)){
	        final ImageView ivZoomOut2 = new ImageView(mParentActivity);
	        final ScaleBarDrawable dr = new ScaleBarDrawable(mParentActivity, mOsmv, Integer.parseInt(pref.getString("pref_units", "0")));
	        ivZoomOut2.setImageDrawable(dr);
	        final RelativeLayout.LayoutParams scaleParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
	        scaleParams.addRule(RelativeLayout.RIGHT_OF, R.id.whatsnew);
	        scaleParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
	        rl.addView(ivZoomOut2, scaleParams);
        };
        

		mDrivingDirectionUp = pref.getBoolean("pref_drivingdirectionup", true);
		mNorthDirectionUp = pref.getBoolean("pref_northdirectionup", true);

		mScreenOrientation = Integer.parseInt(pref.getString("pref_screen_orientation", "-1"));
//		setRequestedOrientation(mScreenOrientation);

//     	mFullScreen = pref.getBoolean("pref_showstatusbar", true);
//		if (mFullScreen)
//			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN,
//					WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
//		else
//			getWindow()
//					.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		pref.edit().putBoolean("pref_showtitle", false);
//		mShowTitle = pref.getBoolean("pref_showtitle", true);
//		if (mShowTitle)
//	        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
//		else
//			requestWindowFeature(Window.FEATURE_NO_TITLE);

//        setContentView(rl);

//        if(mShowTitle)
//	        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.main_title);

		restoreUIState();

        final Intent queryIntent = mParentActivity.getIntent();
        final String queryAction = queryIntent.getAction();

        if (Intent.ACTION_SEARCH.equals(queryAction)) {
            doSearchQuery(queryIntent);
        }else if(ACTION_SHOW_POINTS.equalsIgnoreCase(queryAction))
        	ActionShowPoints(queryIntent);
	}

	private void ActionShowPoints(Intent queryIntent) {
		final ArrayList<String> locations = queryIntent.getStringArrayListExtra("locations");
		if(!locations.isEmpty()){
			Ut.dd("Intent: "+ACTION_SHOW_POINTS+" locations: "+locations.toString());
			String [] fields = locations.get(0).split(";");
			String locns = "", title = "", descr = "";
			if(fields.length>0) locns = fields[0];
			if(fields.length>1) title = fields[1];
			if(fields.length>2) descr = fields[2];

			GeoPoint point = GeoPoint.fromDoubleString(locns);
			mPoiOverlay.setGpsStatusGeoPoint(point, title, descr);
			setAutoFollow(false);
			mOsmv.setMapCenter(point);
		}
	}
	
	private float updateBearing(float newBearing) {
		float dif = newBearing - mLastBearing;
		// find difference between new and current position
		if (Math.abs(dif) > 180)
			dif = 360 - dif;
		// if difference is bigger than 180 degrees,
		// it's faster to rotate in opposite direction
		if (Math.abs(dif) < 1)
			return mLastBearing;
		// if difference is less than 1 degree, leave things as is
		if (Math.abs(dif) >= 90)
			return mLastBearing = newBearing;
		// if difference is bigger than 90 degress, just update it
		mLastBearing += 90 * Math.signum(dif) * Math.pow(Math.abs(dif) / 90, 2);
		// bearing is updated proportionally to the square of the difference
		// value
		// sign of difference is paid into account
		// if difference is 90(max. possible) it is updated exactly by 90
		while (mLastBearing > 360)
			mLastBearing -= 360;
		while (mLastBearing < 0)
			mLastBearing += 360;
		// prevent bearing overrun/underrun
		return mLastBearing;
	}
	
	private void setLastKnownLocation() {
		final LocationManager lm = (LocationManager) mParentActivity.getSystemService(Context.LOCATION_SERVICE);
		final Location loc1 = lm.getLastKnownLocation("gps");
		final Location loc2 = lm.getLastKnownLocation("network");

		boolean boolGpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
		boolean boolNetworkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		String str = "";
		Location loc = null;

		if(loc1 == null && loc2 != null)
			loc = loc2;
		else if (loc1 != null && loc2 == null)
			loc = loc1;
		else if (loc1 == null && loc2 == null)
			loc = null;
		else
			loc = loc1.getTime() > loc2.getTime() ? loc1 : loc2;

		if(boolGpsEnabled){}
		else if(boolNetworkEnabled)
			str = mParentActivity.getString(R.string.message_gpsdisabled);
		else if(loc == null)
			str = mParentActivity.getString(R.string.message_locationunavailable);
		else
			str = mParentActivity.getString(R.string.message_lastknownlocation);

		if(str.length() > 0)
			Toast.makeText(mParentActivity, str, Toast.LENGTH_LONG).show();

		if (loc != null)
			this.mOsmv
					.getController()
					.animateTo(
							TypeConverter.locationToGeoPoint(loc),
							OpenStreetMapViewController.AnimationType.MIDDLEPEAKSPEED,
							OpenStreetMapViewController.ANIMATION_SMOOTHNESS_HIGH,
							OpenStreetMapViewController.ANIMATION_DURATION_DEFAULT);
	}
	
	private void setAutoFollow(boolean autoFollow) {
		setAutoFollow(autoFollow, false);
	}

	private void setAutoFollow(boolean autoFollow, final boolean supressToast) {
		if (mAutoFollow != autoFollow) {
			mAutoFollow = autoFollow;

			if (autoFollow) {
				ivAutoFollow.setVisibility(ImageView.INVISIBLE);
				if(!supressToast)
					Toast.makeText(mParentActivity, R.string.auto_follow_enabled, Toast.LENGTH_SHORT).show();
			} else {
				ivAutoFollow.setVisibility(ImageView.VISIBLE);
				if(!supressToast)
					Toast.makeText(mParentActivity, R.string.auto_follow_disabled, Toast.LENGTH_SHORT).show();
			}
		}
	}
	
	private OpenStreetMapRendererInfo getRendererInfo(final Resources aRes, final SharedPreferences aPref, final String aName){
		OpenStreetMapRendererInfo RendererInfo = new OpenStreetMapRendererInfo(aRes, aName);
		RendererInfo.LoadFromResources(aName, PreferenceManager.getDefaultSharedPreferences(mParentActivity));


		return RendererInfo;
	}

	private void restoreUIState() {
		SharedPreferences settings = mParentActivity.getPreferences(Activity.MODE_PRIVATE);

		OpenStreetMapRendererInfo RendererInfo = getRendererInfo(mParentActivity.getResources(), settings, settings.getString("MapName", "mapnik"));
		if(!mOsmv.setRenderer(RendererInfo))
			mOsmv.setRenderer(getRendererInfo(mParentActivity.getResources(), settings, "mapnik"));

		mOsmv.getOverlays().clear();
		if(RendererInfo.YANDEX_TRAFFIC_ON == 1){
       		mOsmv.getOverlays().add(new YandexTrafficOverlay(mParentActivity, mOsmv));
		}
        if(mTrackOverlay != null)
        	mOsmv.getOverlays().add(mTrackOverlay);
        if(mCurrentTrackOverlay != null)
        	mOsmv.getOverlays().add(mCurrentTrackOverlay);
        if(mPoiOverlay != null)
        	mOsmv.getOverlays().add(mPoiOverlay);
        mOsmv.getOverlays().add(mMyLocationOverlay);
        mOsmv.getOverlays().add(mSearchResultOverlay);

		mOsmv.setZoomLevel(settings.getInt("ZoomLevel", 0));
		mOsmv.setMapCenter(settings.getInt("Latitude", 0), settings.getInt("Longitude", 0));

		mCompassEnabled = settings.getBoolean("CompassEnabled", false);
		mCompassView.setVisibility(mCompassEnabled ? View.VISIBLE : View.INVISIBLE);

		mAutoFollow = settings.getBoolean("AutoFollow", true);
		ivAutoFollow.setVisibility(mAutoFollow ? ImageView.INVISIBLE : View.VISIBLE);

//		setTitle();

		if(mPoiOverlay != null)
			mPoiOverlay.setTapIndex(settings.getInt("curShowPoiId", -1));

		mSearchResultOverlay.fromPref(settings);

		if(settings.getString("error", "").length() > 0){
//			showDialog(R.id.error);
		}

		if(!settings.getString("app_version", "").equalsIgnoreCase(Ut.getAppVersion(mParentActivity)))
//			showDialog(R.id.whatsnew);

		if (settings.getBoolean("add_yandex_bookmark", true))
			if (mParentActivity.getResources().getConfiguration().locale.toString()
					.equalsIgnoreCase("ru_RU")) {
				SharedPreferences uiState = mParentActivity.getPreferences(0);
				SharedPreferences.Editor editor = uiState.edit();
				editor.putBoolean("add_yandex_bookmark", false);
				editor.commit();

				Message msg = Message.obtain(mCallbackHandler,
						R.id.add_yandex_bookmark);
				mCallbackHandler.sendMessageDelayed(msg, 2000);
			}
	}
	
	private void doSearchQuery(Intent queryIntent) {
		mSearchResultOverlay.Clear();
		this.mOsmv.invalidate();

		final String queryString = queryIntent.getStringExtra(SearchManager.QUERY);

        // Record the query string in the recent queries suggestions provider.
        SearchRecentSuggestions suggestions = new SearchRecentSuggestions(mParentActivity, SearchSuggestionsProvider.AUTHORITY, SearchSuggestionsProvider.MODE);
        suggestions.saveRecentQuery(queryString, null);

		InputStream in = null;
		OutputStream out = null;

		try {
			URL url = new URL(
					"http://ajax.googleapis.com/ajax/services/search/local?v=1.0&sll="
							+ this.mOsmv.getMapCenter().toDoubleString()
							+ "&q=" + URLEncoder.encode(queryString, "UTF-8")
							+ "");
			Ut.dd(url.toString());
			in = new BufferedInputStream(url.openStream(), StreamUtils.IO_BUFFER_SIZE);

			final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
			out = new BufferedOutputStream(dataStream, StreamUtils.IO_BUFFER_SIZE);
			StreamUtils.copy(in, out);
			out.flush();

			String str = dataStream.toString();
			JSONObject json = new JSONObject(str);
			//Ut.dd(json.toString(4)); //
			JSONArray results = (JSONArray) ((JSONObject) json.get("responseData")).get("results");
			//Ut.dd("results.length="+results.length());
			if(results.length() == 0){
				Toast.makeText(mParentActivity, R.string.no_items, Toast.LENGTH_SHORT).show();
				return;
			}
			JSONObject res = results.getJSONObject(0);
			//Ut.dd(res.toString(4));
			//Toast.makeText(this, res.getString("titleNoFormatting"), Toast.LENGTH_LONG).show();
			final String address = res.getString("addressLines").replace("\"", "").replace("[", "").replace("]", "").replace(",", ", ").replace("  ", " ");
			//Toast.makeText(this, address, Toast.LENGTH_LONG).show();
			//Toast.makeText(this, ((JSONObject) json.get("addressLines")).toString(), Toast.LENGTH_LONG).show();

			setAutoFollow(false, true);
			this.mSearchResultOverlay.setLocation(new GeoPoint((int)(res.getDouble("lat")* 1E6), (int)(res.getDouble("lng")* 1E6)), address);
			this.mOsmv.setZoomLevel((int) (2 * res.getInt("accuracy")));
			this.mOsmv.getController().animateTo(new GeoPoint((int)(res.getDouble("lat")* 1E6), (int)(res.getDouble("lng")* 1E6)), OpenStreetMapViewController.AnimationType.MIDDLEPEAKSPEED, OpenStreetMapViewController.ANIMATION_SMOOTHNESS_HIGH, OpenStreetMapViewController.ANIMATION_DURATION_DEFAULT);

//			setTitle();

		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(mParentActivity, R.string.no_inet_conn, Toast.LENGTH_LONG).show();
		} finally {
			StreamUtils.closeStream(in);
			StreamUtils.closeStream(out);
		}
	}
}