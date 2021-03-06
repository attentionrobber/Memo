package com.hyunseok.android.memo.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.hyunseok.android.memo.domain.Memo;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;

/**
 * Created by Administrator on 2017-02-14.
 */

public class DBHelper extends OrmLiteSqliteOpenHelper {

    public static final String DB_NAME = "database.db"; // DBMS의 Schema 단위
    public static final int DB_VERSION = 1; // Field를 수정했을 경우 이것을 바꿔주면 onUpgrade 호출됨.

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    /**
     * 생성자에서 호출되는 super(cotext, ...) 에서 database.db 파일이 생성되어있지 않으면 호출됨.
     * @param database
     * @param connectionSource
     */
    @Override
    public void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {
        try {
            // Bbs.class에 정의된 테이블을 생성한다.
            TableUtils.createTable(connectionSource, Memo.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 생성자에서 호출되는 super()에서 database.db파일이 존재하고 DB_VERSION이 증가하면 호출된다.
     * @param database
     * @param connectionSource
     * @param oldVersion
     * @param newVersion
     */
    @Override
    public void onUpgrade(SQLiteDatabase database, ConnectionSource connectionSource, int oldVersion, int newVersion) {
        try {
            // Bbs.class에 정의된 테이블 삭제하고
            TableUtils.dropTable(connectionSource, Memo.class, false);

            // TODO 데이터를 보존해야될 필요성이 있으면 중간처리를 해줘야한다.
            // TODO 임시테이블을 생성한 후 데이터를 먼저 저장하고 onCreate 이후에 데이터를 입력해준다.

            // onCreate를 호출해서 테이블을 생성한다.
            onCreate(database, connectionSource);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Dao<Memo, Integer> memoDao = null; // Generic에 두개 타입은 HashMap형태
    // Data Access Object
    // DBHelper를 사용하기 때문에 dao객체도 열어놓고 사용가능하다.
    public Dao<Memo, Integer> getMemoDao() throws SQLException {
        if(memoDao == null) {
            memoDao = getDao(Memo.class);
        }
        return memoDao;
    }
    public void releaseBbsDao() {
        if (memoDao != null) {
            memoDao = null;
        }
    }
}
