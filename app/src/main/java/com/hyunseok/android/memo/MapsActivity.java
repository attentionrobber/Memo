package com.hyunseok.android.memo;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = "MemoNewActivity";

    private GoogleMap mMap;

    LocationManager locationManager;

    // 상태 코드
    private int option = -1;

    double longitude; // 경도
    double latitude;   // 위도
    double altitude;   // 고도
    float accuracy;    // 정확도
    String provider;   // 위치제공자
    LatLng myPosition; // 내 위치

    Button btn_confirm, btn_cancle, btn_search;
    EditText et_searchMap;

    ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Location Manager 객체를 생성한다.
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);


        // 어떤 옵션으로 지도서비스를 이용하는지 받아온다.
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        option = bundle.getInt("case"); // case0: My Location, case1: Search Location

        setWidget();
        setListener();
        alertGPS(); // GPS 센서가 꺼져있을 때 alert Dialog로 GPS 켤지 묻기


        if(option == 0) { // 내 위치
            myLocation();
        } else if(option == 1) { // 위치 검색

        }
    }

    private void setWidget() {
        btn_confirm = (Button) findViewById(R.id.btn_confirm);
        btn_cancle = (Button) findViewById(R.id.btn_cancle);
        btn_search = (Button) findViewById(R.id.btn_search);
        et_searchMap = (EditText) findViewById(R.id.et_searchMap);
    }

    private void setListener() {
        btn_confirm.setOnClickListener(clickListener);
        btn_cancle.setOnClickListener(clickListener);
        btn_search.setOnClickListener(clickListener);
    }

    // GPS 센서가 꺼져있을 때 alert Dialog로 GPS 켤지 묻기
    private void alertGPS() {
        // GPS 센서가 켜져있는지 확인
        // 꺼져있다면 GPS를 켜는 페이지로 이동.
        if( gpsCheck() == false ) {
            // 0. 팝업창 만들기
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(MapsActivity.this);
            // 1. 팝업창 제목
            alertDialog.setTitle("GPS 켜기");
            // 2. 팝업창 메시지
            alertDialog.setMessage("GPS가 꺼져 있습니다.\n설정창으로 이동하시겠습니까?");
            // 3. Yes버튼 만들기
            alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                    startActivity(intent);
                }
            });
            // 4. Cancle 버튼 만들기
            alertDialog.setNegativeButton("Cancle", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            // 5. show함수로 팝업창을 띄운다.
            alertDialog.show();
        }
    }

    // GPS 센서가 켜져있는지 체크 롤리팝 이하버전
    private boolean gpsCheck() {

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } else {
            String gps = android.provider.Settings.Secure.getString(getContentResolver(),
                    Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            if(gps.matches(",*gps.*")) {
                return true;
            } else {
                return false;
            }
        }
    }

    private void myLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if ((ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                    && (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
                return;
            }
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, // 핸드폰의 GPS 센서로 받는 위치(정확도가 더 높음)
                3000, 10, locationListener); // 통지사이의 최소 시간간격(ms), 통지사이의 최소 변경거리(m)

        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, // 통신회사에서 받는 위치
                3000, 10, locationListener);
    }

    /**
     * 위도, 경도로 주소 획득
     * @param lat
     * @param lng
     * @return
     */
    private String latLngToAddress(double lat, double lng) {

        StringBuffer bf = new StringBuffer();
        Geocoder geocoder = new Geocoder(this, Locale.KOREA);
        List<android.location.Address> address;
        try {
            if (geocoder != null) {
                address = geocoder.getFromLocation(lat, lng, 1); // 세번째 인수는 최대 결과값인데 하나만 리턴받도록 설정함
                if (address != null && address.size() > 0) { // 설정한 데이터로 주소가 리턴된 데이터가 있으면
                    String currentLocationAddress = address.get(0).getAddressLine(0).toString(); // 주소

                    // 전송할 주소 데이터 (위도/경도 포함 편집)
                    bf.append(currentLocationAddress).append("#");
                    bf.append(lat).append("#");
                    bf.append(lng);
                }
            }
        } catch (IOException e) {
            Toast.makeText(this, "주소 취득 실패", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
        return bf.toString();
    }

    /**
     * 주소로부터 위치정보 취득
     * @param address 주소
     */
    private LatLng addressToLatLng(String address) {
        Geocoder geocoder = new Geocoder(this);
        android.location.Address addr;
        LatLng latLng = null;
        try {
            List<android.location.Address> listAddress = geocoder.getFromLocationName(address, 1);
            if (listAddress.size() > 0) { // 주소값이 존재 하면
                addr = listAddress.get(0); // Address형태로
                latitude = addr.getLatitude();
                longitude = addr.getLongitude();
                latLng = new LatLng(latitude, longitude);

                Log.d(TAG, "주소로부터 취득한 위도 : " + latitude + ", 경도 : " + longitude);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        mMap.addMarker(new MarkerOptions().position(latLng).title(address));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));

        return latLng;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // 서울 신사역 37.516066 127.019361
        myPosition = new LatLng(37.516066, 127.019361);
        mMap.addMarker(new MarkerOptions().position(myPosition).title("Sinsa"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myPosition, 15));

        dialog = new ProgressDialog(MapsActivity.this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setMessage("현재 위치를 찾는 중...");
        dialog.show(); // ProgressDialog 띄우기

        myLocation();
    }


    @Override
    protected void onResume() {
        super.onResume();
        if(option == 0) {
            myLocation();
        }
    }

    /**
     * 버튼 클릭 리스너
     */
    View.OnClickListener clickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_confirm:
                    if( latitude == 0 || longitude == 0 ) { // 아직 위치를 못찾았을 경우(위도 or 경도가 0일 경우)
                        Toast.makeText(MapsActivity.this, "아직 위치를 찾지 못했습니다.", Toast.LENGTH_SHORT).show();
                        break;
                    }
                    Intent intent = new Intent(MapsActivity.this, MemoNewActivity.class);
                    intent.putExtra("latitude", latitude);
                    intent.putExtra("longitude", longitude);
                    setResult(RESULT_OK, intent); // startActivityOnResult로 호출을 하면 setResult를 해줘야한다.
                    finish();
                    break;
                case R.id.btn_search:
                    String address = et_searchMap.getText().toString();
                    addressToLatLng(address);
                    break;
                case R.id.btn_cancle:
                    MapsActivity.super.onBackPressed();
                    break;
            }
        }
    };

    /**
     * 자신의 위치를 찾는 Location 리스너
     */
    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(android.location.Location location) {

            longitude = location.getLongitude(); // 경도
            latitude = location.getLatitude();   // 위도
            altitude = location.getAltitude();   // 고도
            accuracy = location.getAccuracy();    // 정확도
            provider = location.getProvider();   // 위치제공자

            // 내 위치
            myPosition = new LatLng(latitude, longitude); // 위도, 경도
            mMap.addMarker(new MarkerOptions().position(myPosition).title("My Location")); // 내 위치와 마커 클릭시 나오는 텍스트
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myPosition, 18)); // 화면을 내 위치로 이동시키는 함수, Zoom Level 설정

            dialog.dismiss(); // ProgressDialog 종료
        }

        @Override // Provider의 상태 변경시 호출
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override // GPS가 사용할 수 없었다가 사용할 수 있을 때 호출
        public void onProviderEnabled(String provider) {

        }

        @Override // GPS가 사용할 수 없을 때 호출
        public void onProviderDisabled(String provider) {

        }
    };


    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

}
