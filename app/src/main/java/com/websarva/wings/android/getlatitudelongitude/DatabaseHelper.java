package com.websarva.wings.android.getlatitudelongitude;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

//データベース保存処理をまとめたクラス
public class DatabaseHelper extends SQLiteOpenHelper {

    //データベースのバージョン
    public static final int DATABASE_VERSION = 1;

    //データベース名
    public static final String DATABASE_NAME = "LatitudeLongitude.db";

    //デーブル名
    private static final String TABLE_NAME = "LatitudeLongitude";

    //データベースプライマリーキー
    private static final String _ID = "_id";

    //データベースカラム
    private static final String COLUMN_NAME_LONGITUDE = "longitude";
    private static final String COLUMN_NAME_LATITUDE = "latitude";
    private static final String COLUMN_NAME_TIME = "time";


    //テーブル作成のクエリー実行文
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    _ID + "INTEGER PRIMARY KEY," +
                    COLUMN_NAME_LONGITUDE + " DOUBLE," +
                    COLUMN_NAME_LATITUDE + " DOUBLE," +
                    COLUMN_NAME_TIME + " STRING)";

    private static final String SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS" + TABLE_NAME;

    //コンストラクタ
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //テーブル作成
        //SQLファイルがなければSQLiteファイルが作成される
        db.execSQL(
               SQL_CREATE_ENTRIES
        );

    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if(oldVersion == 1 && newVersion == 2) {
            //バージョンを変える際の処理

        }
    }

}
