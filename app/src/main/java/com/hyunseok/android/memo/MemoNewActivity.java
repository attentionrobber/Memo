package com.hyunseok.android.memo;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;
import com.hyunseok.android.memo.data.DBHelper;
import com.hyunseok.android.memo.domain.Memo;
import com.hyunseok.android.memo.utility.Logger;
import com.hyunseok.android.memo.utility.PermissionControl;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.Date;
import java.util.Locale;

public class MemoNewActivity extends AppCompatActivity {

    private static final String TAG = "MemoNewActivity";
    private boolean keypad_toggle = false;

    // 권한 관련
    private final int PERM_GRANT = 1;
    private final int PERM_DENY = 2;
    private int PERM_RESULT = 0;
    private final int REQ_PERMISSION = 1; // 요청 코드
    private final int REQ_PLACE_PICKER = 98; // PlacePicker(장소 선택) 요청 코드
    private final int REQ_LOCATION = 99; // 내 위치 요청 코드
    private final int REQ_CAMERA = 101; // 카메라 요청 코드
    private final int REQ_GALLERY = 102; // 갤러리 요청 코드

    // 이미지 Uri 관련
    Uri imageUri;
    String strUri = "";

    // Location 관련
    Intent intent; // 지도 인텐트
    Bundle bundle;

    // Widget 관련
    EditText editText_title, editText_content;
    Button btn_OK, btn_cancle;
    ImageButton imgbtn_addimg, imgbtn_location;
    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memo_new);



        setWidget(); // 위젯 세팅
        setListener(); // 리스너 계열을 등록
        //checkPermission();
    }

    private Memo makeMemo() {
        Memo memo = new Memo();

        memo.setTitle(editText_title.getText().toString());
        memo.setContent(editText_content.getText().toString());
        memo.setCurrentDate(new Date(System.currentTimeMillis()));

        if(imageUri != null) {
            strUri = imageUri.toString();
        } else {
            strUri = "";
        }
        memo.setImgUri(strUri);

        return memo;
    }

    public void saveToDB(Memo memo) throws SQLException {
        DBHelper dbHelper = OpenHelperManager.getHelper(this, DBHelper.class);
        Dao<Memo, Integer> memoDao = dbHelper.getMemoDao();
        memoDao.create(memo);
    }

    private void setWidget() {
        editText_title = (EditText) findViewById(R.id.textView_title);
        // 키패드(키보드) 자동으로 띄우기
        displayKeypad();

        editText_content = (EditText) findViewById(R.id.editText_content);
        btn_OK = (Button) findViewById(R.id.btn_OK);
        btn_cancle = (Button) findViewById(R.id.btn_cancle);
        imgbtn_addimg = (ImageButton) findViewById(R.id.imgbtn_addimg);
        imgbtn_location = (ImageButton) findViewById(R.id.imgbtn_location);
        imageView = (ImageView) findViewById(R.id.imageView);
    }

    private void setListener() {
        btn_OK.setOnClickListener(clickListener);
        btn_cancle.setOnClickListener(clickListener);
        imgbtn_addimg.setOnClickListener(clickListener);
        imgbtn_location.setOnClickListener(clickListener);
        imageView.setOnClickListener(clickListener);
    }
    // 키패드(키보드) 띄우기
    private void displayKeypad() {
        InputMethodManager imm;
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        //imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }
    // 키패드(키보드) 없애기
    private void hideKeypad() {
        InputMethodManager immhide = (InputMethodManager) getSystemService(MemoNewActivity.INPUT_METHOD_SERVICE);
        View view = this.getCurrentFocus();
        if (view == null) {
            view = new View(this);
        }
        immhide.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void alertAddImage() {
        // 1. 팝업창 만들기
        AlertDialog.Builder alert_Imgbtn = new AlertDialog.Builder(MemoNewActivity.this);
        // 2. 팝업창 제목
        alert_Imgbtn.setTitle("Input Image");
        // 3. Items 만들기
        final CharSequence[] items_Imgbtn = {"Camera", "Gallery"};
        alert_Imgbtn.setItems(items_Imgbtn, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = null;
                switch (which) {
                    case 0 : // Camera
                        intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        // 카메라 촬영 후 미디어 컨텐트 Uri를 생성해서 외부저장소에 저장한다.
                        // 마시멜로 이상 버전은 아래 코드를 반영해야함.
                        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                            ContentValues values = new ContentValues(1);
                            values.put(MediaStore.Images.Media.MIME_TYPE, "memo/jpg");
                            imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            // 컨텐트 Uri강제 세팅
                        }
                        startActivityForResult(intent, REQ_CAMERA);
                        break;
                    case 1 : // Gallery
                        intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        intent.setType("image/*"); // 외부저장소에 있는 이미지만 가져오기위한 필터링.
                        startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQ_GALLERY); // createChooser로 타이틀을 붙여줄 수 있다.
                        break;
                }
            }
        });
        alert_Imgbtn.show(); // 4. show함수로 팝업창을 띄운다.
    }

    private void alertClickImageView() {
        AlertDialog.Builder alertImageView = new AlertDialog.Builder(MemoNewActivity.this);
        alertImageView.setTitle("Image Option");
        final CharSequence[] items_ImageView = {"Change", "Delete"};
        alertImageView.setItems(items_ImageView, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0 : // Change
                        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        intent.setType("image/*"); // 외부저장소에 있는 이미지만 가져오기위한 필터링.
                        startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQ_GALLERY); // createChooser로 타이틀을 붙여줄 수 있다.
                        break;
                    case 1 : // Delete
                        imageView.setImageResource(0);
                        imageUri = null;
                        break;
                }
            }
        });
        alertImageView.show(); // show함수로 팝업창을 띄운다.
    }

    private void alertLocation() {
        AlertDialog.Builder alertImageView = new AlertDialog.Builder(MemoNewActivity.this);
        alertImageView.setTitle("Location Option");
        final CharSequence[] items_Location = {"Current Location", "Search Location"};
        alertImageView.setItems(items_Location, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0 : // Current Location 선택시 구글맵으로 현재 위치를 띄워준다.
                        intent = new Intent(MemoNewActivity.this, MapsActivity.class);
                        intent.putExtra("case", 0);
                        startActivityForResult(intent, REQ_LOCATION);
                        break;
                    case 1 : // Search Location 선택시 구글맵으로 검색할 수 있게 한다.
                        // TODO 권한 요청하기
                        searchByPlacePicker();
//                        checkPermission(REQ_PLACE_PICKER);
//                        if(PERM_RESULT == PERM_GRANT) {
//
//                        }
//                        intent = new Intent(MemoNewActivity.this, MapsActivity.class);
//                        intent.putExtra("case", 1);
//                        startActivityForResult(intent, REQ_LOCATION);
                        break;
                }
            }
        });
        alertImageView.show(); // show함수로 팝업창을 띄운다.
    }

    private void searchByPlacePicker(){
        PlacePicker.IntentBuilder placepicker = new PlacePicker.IntentBuilder();

        try {
            startActivityForResult(placepicker.build(this), REQ_PLACE_PICKER);
        } catch (GooglePlayServicesRepairableException e) {
            e.printStackTrace();
        } catch (GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }
    }

    private void checkPermission(int permission) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // 안드로이드 버전 체크
            if (PermissionControl.checkPermission(this, permission)) { // 권한 체크
                //init();
                PERM_RESULT = PERM_GRANT;
                Logger.print("111115", "ssibal");
            }
        } else {
            //init(); // 프로그램 실행
            PERM_RESULT = PERM_GRANT;
            Logger.print("222225", "ssibal");
        }
    }

    // 권한 체크 후 콜백처리(사용자가 확인 후 시스템이 호출하는 함수)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == REQ_PLACE_PICKER) {
            if( PermissionControl.onCheckResult(grantResults) ) { // 권한이 GRANTED 될 경우
                //init(); // 프로그램 실행
                PERM_RESULT = PERM_GRANT;
                Logger.print("333335", "ssibal");
            } else {
                Toast.makeText(this, "권한을 실행하지 않으면 프로그램이 실행되지 않습니다.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
//        else if(requestCode == REQ_CAMERA) {
//        }
    }


    /**
     * Listener 계열
     */
    View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_OK :
                    hideKeypad();
                    try {
                        Memo memo = makeMemo();
                        saveToDB(memo);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    finish();
                    break;
                case R.id.btn_cancle :
                    hideKeypad();

                    // 제목이나 내용을 작성했을 경우에만 AlertDialog 나타나게함.
                    if( !(editText_title.getText().toString().equals("")) || !(editText_content.getText().toString().equals(""))) {
                        AlertDialog.Builder alert_delete = new AlertDialog.Builder(MemoNewActivity.this);
                        alert_delete.setTitle("FINISH WRITING A NOTE");
                        alert_delete.setMessage("Exit without saving.");
                        alert_delete.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                MemoNewActivity.super.onBackPressed();
                            }
                        });
                        alert_delete.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                        alert_delete.show();
                    } else {
                        MemoNewActivity.super.onBackPressed();
                    }
                    break;
                case R.id.imgbtn_addimg : // Add Image 버튼 클릭시
                    hideKeypad();
                    alertAddImage();
                    break;
                case R.id.imageView : // ImageView 클릭시
                    hideKeypad();
                    alertClickImageView();
                    break;
                case R.id.imgbtn_location: // Location 버튼 클릭시
                    hideKeypad();
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        PermissionControl.checkPermission(MemoNewActivity.this, REQ_LOCATION);
                    } else {
                        alertLocation(); // 내 위치 or 지도 검색할지 선택하는 alert
                    }
                    break;
            }
        }
    };

    //startActivityForResult() 후에 실행되는 콜백 함수
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Logger.print("requestCode==========================="+requestCode,"MemoNewActivity");

        switch (requestCode) {
            case REQ_CAMERA:
                // 마시멜로버전 이상인 경우에만 getData()에 null이 넘어올것임.
                if (resultCode == RESULT_OK) { // resultCode OK이면 완료되었다는 뜻.
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                        if (data != null && data.getData() != null) {
                            imageUri = data.getData();
                        }
                    }
                    if (imageUri != null) {
                        Glide.with(this).load(imageUri).into(imageView);
                    } else {

                    }
                } else { // reulstCode가 uri가 남아있는데 삭제처리해야함.

                }
                break;
            case REQ_GALLERY:
                //if(data != null && data.getData() != null) {
                if (resultCode == RESULT_OK) {
                    imageUri = data.getData();
                    Glide.with(this).load(imageUri).into(imageView);
                } else {

                }
                break;
            case REQ_LOCATION: // 내 위치
                if (resultCode == RESULT_OK) {
                    bundle = data.getExtras();
                    if (bundle != null) {
                        double latitude = bundle.getDouble("latitude");
                        double longitude = bundle.getDouble("longitude");
                        String url = "http://maps.google.com/?q="; // 구글맵 기본 url
                        String locationUrl = url + latitude + "," + longitude;
                        editText_content.append(locationUrl);
                    }
                }
                break;
            case REQ_PLACE_PICKER:
                if (resultCode == RESULT_OK) {
                    Place place = PlacePicker.getPlace(data, this);
                    String address = String.valueOf(place.getAddress()); // 선택한 place의 주소
                    String name = String.valueOf(place.getName()); // 선택한 place의 검색시 타이틀
                    String url = "http://maps.google.com/?q="; // 구글맵 기본 url
                    String latlngStr = String.valueOf(place.getLatLng()); // Latlng을 String으로 변환함
                    String split[] = latlngStr.split(",");
                    String lat = split[0].replaceAll("[^0-9|.]", ""); // 숫자와 .(dot)을 제외한 문자를 모두 없앤다.
                    String lng = split[1].replaceAll("[^0-9|.]", "");
                    editText_content.append("\n" + address+ " " + name);
                    editText_content.append("\n" + url + lat + "," + lng);
                }
                break;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Logger.print("onStart 시작", TAG);
    }
    @Override
    protected void onResume() {
        super.onResume();
        Logger.print("onResume 시작", TAG);
    }
    @Override
    protected void onPause() {
        super.onPause();
        Logger.print("onPause 시작", TAG);
    }
    @Override
    protected void onStop() {
        super.onStop();
        Logger.print("onStop 시작", TAG);
    }
    @Override
    protected void onRestart() {
        super.onRestart();
        Logger.print("onRestart 시작", TAG);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Logger.print("onDestroy 시작", TAG);
    }


}
