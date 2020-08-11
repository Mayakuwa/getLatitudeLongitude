package com.websarva.wings.android.getlatitudelongitude;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListViewFragment extends Fragment {

    //データベースヘルパーフィールド
    private DatabaseHelper databaseHelper;

    private SQLiteDatabase db;

    //このクラスが所属するアクティビティオブジェクトを取得
    private Activity _parentActivity;


    public ListViewFragment() {
        // Required empty public constructor
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //所属するアクティビティクラスを取得
        _parentActivity = getActivity();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //フラグメントで表示する画面をxmlからインフレートする
        View view = inflater.inflate(R.layout.fragment_list_view, container,false);

        //リストビュー取得
        ListView listView = view.findViewById(R.id.listViewFragment);

        SimpleAdapter returnAdapter = readData();

        listView.setAdapter(returnAdapter);

        // インフレートされた画面を戻り値として返す
        return view;
    }


    //データベース表示処理
    private SimpleAdapter readData() {
        // DBHelper作成
        databaseHelper = new DatabaseHelper(_parentActivity);
        db = databaseHelper.getReadableDatabase();

        //クエリー発行
        Cursor cursor = db.query(
                "LatitudeLongitude",
                new String[]{"longitude", "latitude"},
                null,
                null,
                null,
                null,
                null);

        cursor.moveToFirst();


        List<Map<String, Double>> resultList = new ArrayList<>();

        Map<String, Double> result = new HashMap<>();

        try {
            for(int i=0; i < cursor.getCount(); i++) {

                Double longitude = cursor.getDouble(0);
                Double latitude = cursor.getDouble(1);

                result.put("longitude", longitude);
                result.put("latitude", latitude);
                resultList.add(result);

                result = new HashMap<>();
                cursor.moveToNext();

            }

            String[] from = {"longitude", "latitude"};

            //変える
            int[] to = {R.id.text_latitude, R.id.text_longitude};

            //第三引数をカスタムで作成したxmlにする
            SimpleAdapter adapter = new SimpleAdapter(_parentActivity,
                    resultList,
                    R.layout.list_view,
                    from,
                    to);

            return adapter;

        } finally {
            cursor.close();
        }
    }

}