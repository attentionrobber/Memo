package com.hyunseok.android.memo;

import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.hyunseok.android.memo.data.DBHelper;
import com.hyunseok.android.memo.domain.Memo;
import com.hyunseok.android.memo.utility.PermissionControl;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // Widget 관련
    RecyclerView recyclerView;
    ImageButton imgbtn_new;

    // 메모 데이터 관련
    private static List<Memo> datas = new ArrayList<>();

    MemoAdapter adapter;

    // 권한 관련
    private final int REQ_MYLOCATION = 99; // 내 위치 요청 코드
    private final int REQ_SEARCHLOCATION = 100; // 검색 위치 요청 코드
    private final int REQ_CAMERA = 101; // 카메라 요청 코드
    private final int REQ_GALLERY = 102; // 갤러리 요청 코드


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. 위젯 세팅
        setWidget();
        // 2. 리스너 계열을 등록
        setListener();
        // 3. 권한처리
        //checkPermission();

        try {
            // 4. DB에서 데이터 로드
            loadData();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // 5. 어댑터 세팅
        init();
    }

    private void init() {

        // 2. Adapter 생성하기
        adapter = new MemoAdapter(datas, this);
        // 3. Recycler View에 Adapter 세팅하기
        recyclerView.setAdapter(adapter);
        // 4. Recycler View 매니저 등록하기(View의 모양(Grid, 일반, 비대칭Grid)을 결정한다.)
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    public void loadData() throws SQLException {
        DBHelper dbHelper = OpenHelperManager.getHelper(this, DBHelper.class); // static 불가
        //DBHelper dbHelper = new DBHelper(context);
        Dao<Memo, Integer> memoDao = dbHelper.getMemoDao();

        datas = memoDao.queryForAll();
    }

    private void setWidget() {
        // 1. Recycler View(ViewHolder까지 있음)
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        imgbtn_new = (ImageButton) findViewById(R.id.imgbtn_new);
    }

    private void setListener() {
        imgbtn_new.setOnClickListener(clickListener);
    }

    /**
     * Listener 계열
     */
    View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Intent intent = null;
            switch (v.getId()) {
                case R.id.imgbtn_new :
                    intent = new Intent(MainActivity.this, MemoNewActivity.class);
                    startActivity(intent);
                    break;
            }
        }
    };

    /**
     * 새 글을 쓰거나 수정하고난 뒤에 메인화면이 떴을 때 Refresh 해준다.
     */
    @Override
    protected void onResume() {
        super.onResume();
        try {
            loadData();
            init();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
