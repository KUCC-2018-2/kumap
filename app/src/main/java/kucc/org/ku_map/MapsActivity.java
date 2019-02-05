package kucc.org.ku_map;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.TranslateAnimation;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

import kucc.org.ku_map.dijkstra.Dijkstra;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener, GoogleMap.OnMapClickListener{

    /** LOG TAG **/
    private static final String TAG = "MapsActivity";

    /** DB **/
    private SQLiteDatabase db;

    private GoogleMap mMap;
    private View mapView;

    private ImageButton btn_pathfind;
    private Button btn_set_source;
    private Button btn_set_dest;
    private TextView tv_title;
    private AutoCompleteTextView tv_source;
    private AutoCompleteTextView tv_dest;
    private ConstraintLayout markerWindow;

    private String[] arr_latlng;
    private String[] arr_flag;
    private Dijkstra dijkstra;

    /** Current system time in milliseconds when back button is pressed **/
    private static long backbtn_pressed_time;

    private static LatLng l0,l1,l2,l3,l4,l5,l6,l7,l8,l9,l10,l11,l12,l13,l14,l15,l16,l17,l18,l19,
                   l20,l21,l22,l23,l24,l25,l26,l27,l28,l29,l30,l31,l32,l33,l34,l35,l36,l37,l38,l39,
                   l40,l41,l42,l43,l44,l45,l46,l47,l48,l49,l50,l51,l52,l53,l54,l55,l56,l57,l58,l59,
                   l60,l61,l62,l63,l64,l65,l66,l67,l68,l69,l70,l71,l72,l73,l74,l75,l76,l77,l78,l79,
                   l80,l81,l82,l83,l84,l85,l86,l87,l88,l89,l90,l91,l92,l93,l94,l95,l96,l97,l98,l99,
                   l100,l101,l102,l103,l104,l105,l106,l107,l108,l109;
    private static LatLng[] latlngs = {
            l0,l1,l2,l3,l4,l5,l6,l7,l8,l9,l10,l11,l12,l13,l14,l15,l16,l17,l18,l19,
            l20,l21,l22,l23,l24,l25,l26,l27,l28,l29,l30,l31,l32,l33,l34,l35,l36,l37,l38,l39,
            l40,l41,l42,l43,l44,l45,l46,l47,l48,l49,l50,l51,l52,l53,l54,l55,l56,l57,l58,l59,
            l60,l61,l62,l63,l64,l65,l66,l67,l68,l69,l70,l71,l72,l73,l74,l75,l76,l77,l78,l79,
            l80,l81,l82,l83,l84,l85,l86,l87,l88,l89,l90,l91,l92,l93,l94,l95,l96,l97,l98,l99,
            l100,l101,l102,l103,l104,l105,l106,l107,l108,l109};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        /** database **/
        db = openOrCreateDatabase("kumap", MODE_PRIVATE, null);

        /** **/
        tv_title = findViewById(R.id.tv_title);
        tv_source = findViewById(R.id.actv_source);
        tv_dest = findViewById(R.id.actv_dest);
        btn_pathfind = findViewById(R.id.btn_pathfind);
        btn_set_source = findViewById(R.id.setSource);
        btn_set_dest = findViewById(R.id.setDest);
        markerWindow = findViewById(R.id.markerWindow);

        /** Check location permission **/
        checkPermission();
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            startLocationService();
        }

        /** Instantiate markers **/
        arr_latlng = getResources().getStringArray(R.array.latlng);
        for(int i = 0; i < latlngs.length; i++){
            latlngs[i] = new LatLng(Double.valueOf(arr_latlng[2*i]),Double.valueOf(arr_latlng[2*i+1]));
        }

        /** Instantiate marker_flag array **/
        arr_flag = getResources().getStringArray(R.array.marker_flag);

        /** markerWindow set INVISIBLE **/
        markerWindow.setVisibility(View.INVISIBLE);

        /** Get suggestion array from resource **/
        String[] suggestions = getResources().getStringArray(R.array.suggestion);

        /** Instantiate ArrayAdapter object with suggestion array **/
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.custom_select_dialog_item, suggestions);

        /** Set adapter & threshold of auto-complete text view **/
        tv_source.setThreshold(1);
        tv_source.setAdapter(arrayAdapter);
        tv_dest.setThreshold(1);
        tv_dest.setAdapter(arrayAdapter);

        /** Add TextChangedListener and set clear drawable on the right side of text view if text length > 0 **/
        tv_source.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(count > 0){
                    tv_source.setCompoundDrawablesWithIntrinsicBounds(null, null, getResources().getDrawable(R.drawable.clear), null);
                }else{
                    tv_source.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
                }
            }
            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        tv_dest.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(count > 0){
                    tv_dest.setCompoundDrawablesWithIntrinsicBounds(null, null, getResources().getDrawable(R.drawable.clear), null);
                }else{
                    tv_dest.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
                }
            }
            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        /** Add OnTouchListener on drawable in the text view **/
        tv_source.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int DRAWABLE_RIGHT = 2;
                if(tv_source.getText().length() > 0 && event.getAction() == MotionEvent.ACTION_UP) {
                    if(event.getRawX() >= (tv_source.getRight() - tv_source.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                        tv_source.setText("");
                        return true;
                    }
                }
                return false;
            }
        });
        tv_dest.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int DRAWABLE_RIGHT = 2;
                if(tv_dest.getText().length() > 0 && event.getAction() == MotionEvent.ACTION_UP) {
                    if(event.getRawX() >= (tv_dest.getRight() - tv_dest.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                        tv_dest.setText("");
                        return true;
                    }
                }
                return false;
            }
        });

        /** Obtain the SupportMapFragment and get notified when the map is ready to be used. **/
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapView = mapFragment.getView();

        mapFragment.getMapAsync(this);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        /** Reference to a google map object and animate camera to main building of KU **/
        mMap = googleMap;
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(37.589503, 127.032323),17));

        /** set OnMarkerClickListener & OnMapClickListener **/
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapClickListener(this);

        /** Set zoom controller **/
        mMap.getUiSettings().setZoomControlsEnabled(true);

        /** Set compass functionality **/
        mMap.getUiSettings().setCompassEnabled(true);

        /** Set minimum zoom to 15 (동-scale) **/
        mMap.setMinZoomPreference(15);

        /** Add markers **/
        init_markers();

        /** Current location button relocation **/
        View locationButton = ((View) mapView.findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));
        RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
        rlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,RelativeLayout.TRUE);
        rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
        rlp.setMargins(0,0,50,300);

        /** Permission check & enable my location **/
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            mMap.setMyLocationEnabled(true);
        }

        /** Add path-find button and onClickListener **/
        dijkstra = new Dijkstra();
        btn_pathfind.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                findPath();
            }
        });

        btn_set_source.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                setSourceFromMarker();
            }
        });
        btn_set_dest.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                setDestFromMarker();
            }
        });

    }

    private void checkPermission(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            Log.i(TAG, "permission checked : granted.");
        }else{
            Log.i(TAG, "permission checked : not granted. requesting users location permission...");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
        }
    }

    private void startLocationService(){

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED){
            return;
        }

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        GPSListener gpsListener = new GPSListener();
        long minTime = 5000;
        float minDistance = 0;
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistance, gpsListener);
    }

    private class GPSListener implements LocationListener{

        @Override
        public void onLocationChanged(Location location) {
            /** Custom code on location changed : nothing for now **/
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    }

    /** Convert vector image into bitmap format **/
//    private BitmapDescriptor bitmapDescriptorFromVector(Context context, int vectorResId) {
//        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
//        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
//        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
//        Canvas canvas = new Canvas(bitmap);
//        vectorDrawable.draw(canvas);
//        return BitmapDescriptorFactory.fromBitmap(bitmap);
//    }

    private int retrieve_index(String location){
        int i = -1;
        if(db != null){
            String retrieve_index = "SELECT LOCATION_INDEX FROM LOCATION_INFO WHERE NAME = " + "'" + location + "'";
            Cursor c = db.rawQuery(retrieve_index,null);
            if(c.moveToNext()){
                i = c.getInt(0);
            }
        }
        return i;
    }

    /** Check if EditText is empty or not **/
    private boolean isEmpty(EditText et){
       if(et.getText().toString().trim().length() > 0){
           return false;
       }else{
           return true;
       }
    }

    /** Initiate Markers **/
    private void init_markers(){
        for(int i = 0; i < latlngs.length; i++){
            if(!arr_flag[i].equals("false")) {
                mMap.addMarker(new MarkerOptions().position(latlngs[i]).title(String.valueOf(i)).title(arr_flag[i]));
            }
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        mMap.animateCamera(CameraUpdateFactory.newLatLng(marker.getPosition()));

        markerWindow.setVisibility(View.VISIBLE);
        TranslateAnimation animate = new TranslateAnimation(
                0,0,markerWindow.getHeight(),0
        );
        animate.setDuration(500);
        markerWindow.startAnimation(animate);
        tv_title.setText(marker.getTitle());
        return true;
    }

    @Override
    public void onMapClick(LatLng latLng) {
        markerWindowSlideDown(null);
    }

    public void findPath(){
        if(!isEmpty(tv_source) && !isEmpty(tv_dest )){
            /** retrieve source and destination marker indeces from database**/
            int source = retrieve_index(tv_source.getText().toString());
            int dest = retrieve_index(tv_dest.getText().toString());
            Log.i(TAG, "source : " + tv_source.getText().toString() + " index = " + source);
            Log.i(TAG, "destination : " + tv_dest.getText().toString() + " index = " + dest);
            if(source == -1 || dest == -1) return;

            /** Clear google map & add markers **/
            mMap.clear();

            /** Get marker indeces of markers on the path **/
            ArrayList<Integer> paths = dijkstra.DA(source,dest);

            /** Draw path from source to destination using dijkstra algorithm **/
            PolylineOptions polyLine = new PolylineOptions().width(20).color(0xFF368AFF);
            for(int i = 0; i < paths.size()-1; i++){
                polyLine.add(latlngs[paths.get(i)], latlngs[paths.get(i+1)]);
            }
            mMap.addPolyline(polyLine);
        }
    }

    /** Slide-down marker window **/
    public void markerWindowSlideDown(View v){
        if(markerWindow.getVisibility() == View.VISIBLE) {
            markerWindow.setVisibility(View.INVISIBLE);
            TranslateAnimation animate = new TranslateAnimation(
                    0, 0, 0, markerWindow.getHeight()
            );
            animate.setDuration(500);
            markerWindow.startAnimation(animate);
        }
    }

    /** switch source and destination **/
    public void source_dest_switch(View v){
        Editable source = tv_source.getText();
        Editable dest = tv_dest.getText();
        tv_source.setText(dest);
        tv_dest.setText(source);
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        /** Terminate when interval between first and second click of back button is less than2000 milliseconds **/
        if(backbtn_pressed_time + 2000 > System.currentTimeMillis()){
            super.onBackPressed();
        }else{
            Toast.makeText(getApplicationContext(), "뒤로가기 버튼을 한 번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT).show();
            backbtn_pressed_time = System.currentTimeMillis();
        }

    }

    public void setSourceFromMarker(){
        tv_source.setText(tv_title.getText());
    }

    public void setDestFromMarker(){
        tv_dest.setText(tv_title.getText());
    }

}

