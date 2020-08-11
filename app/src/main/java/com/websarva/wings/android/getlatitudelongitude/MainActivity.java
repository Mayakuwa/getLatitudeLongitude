package com.websarva.wings.android.getlatitudelongitude;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.IntentService;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.internal.Constants;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.text.SimpleDateFormat;


public class MainActivity extends AppCompatActivity {

    //PERMISSION_CODEを定数定義
    private final int PERMISSION_REQUEST_CODE = 1234;

    //位置情報サービスクライアントをフィールド定義
    private FusedLocationProviderClient fusedLocationClient;

    //コールバックフィールド定義
    private LocationCallback locationCallback;

    //緯度フィールド
    private double latitude;

    //経度フィールド
    private double longitude;

    //直近の緯度フィールド
    private double latest_latitude;

    //直近の経度フィールド
    private double latest_longitude;

    //データベースヘルパーフィールド
    private DatabaseHelper databaseHelper;

    //データーベースフィールド
    private SQLiteDatabase db;

    //カウンターフィールド
    private int count;

    //時間間隔フィールド
    private int period;


    //経過時間を測定するためのデータフォーマットフィールド
    private SimpleDateFormat dataFormat = new SimpleDateFormat("mm:ss.S", Locale.JAPAN);

    //タイマー実行ハンドラー
    private Handler handler = new Handler();

    //実行関数
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            count++;
            //デバック
            Log.d("timer",  dataFormat.format(count*period));
            handler.postDelayed(this, period);
        }
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //テキストとボタン取得
        final Button buttonFinish = findViewById(R.id.finishMeasure);
        final Button buttonStart = findViewById(R.id.startMeasure);
        final Button buttonExport = findViewById(R.id.exportMeasure);
        final Button buttonShow = findViewById(R.id.showMeasure);
        final TextView textLatitude = findViewById(R.id.textFieldLatitude);
        final TextView textLongitude = findViewById(R.id.textFieldLongitude);
        final TextView textAddress = findViewById(R.id.textFieldAddress);
        final TextView textSentence = findViewById(R.id.measureLatitudeLongitude);
        final TextView textAddressSentence = findViewById(R.id.textAddress);

        //タイマー初期値セット
        count = 0;
        period =100;

        //アクセス権限をダイアログ表示
        requestPermission();

        //位置情報サービスクライアントを作成
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        //位置情報取得コールバック定義
        locationCallback = new LocationCallback() {

            @Override
            public void onLocationResult(LocationResult locationResult) {

                if (locationResult == null) {
                    return;
                }

                if(!Geocoder.isPresent()) {
                    return;
                }


                for (Location location : locationResult.getLocations()) {

                    // 直近の値を取得
                    getLatestLocations();


                    //緯度画面更新
                    latitude = location.getLatitude();
                    textLatitude.setText(Double.toString(latitude));

                    //経度画面更新
                    longitude = location.getLongitude();
                    textLongitude.setText(Double.toString(longitude));

                    //アドレス表示
                    Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());

                    try {
                        List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                        String prefecture = addresses.get(0).getAdminArea();
                        String city = addresses.get(0).getLocality();
                        String town  = addresses.get(0).getSubLocality();
                        String thoroughFare = addresses.get(0).getThoroughfare();
                        textAddress.setText(prefecture + city + town + thoroughFare);

                    } catch (IOException e) {
                        Log.e("tag", "サービスが使えません");
                    }


                    String time = dataFormat.format(count*period);
                    //データベース保存処理
                    insertData(db, latitude, longitude, time);

                    //値が変わったらトーストで表示
                    if(latitude != latest_latitude || longitude != latest_longitude) {
                        Toast setText = Toast.makeText(MainActivity.this, "値が変わりました", Toast.LENGTH_SHORT);
                        setText.show();
                    }
                }
            }
        };


        //計測開始ボタンが押された
        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonStart.setEnabled(false);
                buttonFinish.setEnabled(true);
                buttonShow.setEnabled(true);
                buttonExport.setEnabled(true);

                //テキストの表示変更
                textSentence.setText(R.string.measure_latitude_longitude_proceed);
                textAddressSentence.setVisibility(View.GONE);
                startLocationUpdates();

                //タイマースタート
                handler.post(runnable);
            }
        });

        //計測終了ボタンが押された
        buttonFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //タイマーストップ
                handler.removeCallbacks(runnable);
                count = 0;
                dataFormat.format(0);

                //ロケーションアップデート停止
                stopLocationUpdates();
                buttonFinish.setEnabled(false);
                buttonStart.setEnabled(true);


                //表示をもとに戻す
                textLatitude.setText(R.string.latitude_field);
                textLongitude.setText(R.string.longitude_field);
                textAddress.setText(R.string.address_field);
                textSentence.setText(R.string.measure_latitude_longitude);
                textAddressSentence.setVisibility(View.VISIBLE);
            }
        });

        //緯度・経度出力ボタンが押された
        buttonExport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertExport();
            }
        });

        //経度・緯度一覧を表示ボタンが押された
        buttonShow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //インテントオブジェクトを生成
                Intent intent = new Intent(MainActivity.this, ListViewActivity.class);

                //第二画面の起動
                startActivity(intent);
            }
        });

    }

    //ライフサイクルのonDestroyで削除する。
    @Override
    public void onDestroy() {
        super.onDestroy();
        //データベース内のデータ全削除
        //データベースが使えるかどうか確認
        if (databaseHelper == null) {
            databaseHelper = new DatabaseHelper(getApplicationContext());
        }

        if (db == null) {
            db = databaseHelper.getReadableDatabase();
        }

        try {
            deleteAllData(db);
        } finally {
            Log.d("debag", "destroy時にデータベースの中身全部削除");
        }

    }


    //データ内の全データを削除する
    private void deleteAllData(SQLiteDatabase database) {
        database.delete("LatitudeLongitude", null, null);
    }



    //計測値初期化
    protected LocationRequest createLocationRequest() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(2500);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }

    //ロケーションのアップデート開始
    private void startLocationUpdates() {
        //パーミッションチェック。これがないとエラーになる
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            //権限がない場合、許可ダイアログを表示
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION}, PERMISSION_REQUEST_CODE);
            return;
        }

        //初期値設定
        LocationRequest locationRequest = createLocationRequest();

        //緯度・経度測定値のアップデートを実施
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);

    }


    //計測終了
    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    //直近のロケーション取得
    private void getLatestLocations() {
        //パーミッションチェック。これがないとエラーになる
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if(location == null) {

                } else {
                    //直近のロケーションを取得
                    latest_latitude = location.getLatitude();
                    latest_longitude = location.getLongitude();
                }
            }
        });
    }


    //位置情報権限のパーミッションを求める
    private void requestPermission() {
        boolean permissionAccessCoarseLocationApproved =
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        if(permissionAccessCoarseLocationApproved) {
            boolean backgroundLocationPermissionApproved =
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;


            if(backgroundLocationPermissionApproved) {
                //ここに記述すると、フォアグランド、バックグラウンド両方で起動できる。
            } else  {
                //フォアグランドのみでokなので、バックグラウンドの許可求める
                ActivityCompat.requestPermissions(this, new String[] {
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION}, PERMISSION_REQUEST_CODE);
            }
        } else {
            //位置情報の権限がないので、アクセスを求める
            ActivityCompat.requestPermissions(this, new String[] {
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
            }, PERMISSION_REQUEST_CODE);
        }

    }



    //出力ボタンを押した際のダイアログ
    private void AlertExport() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("CSVファイル出力")
                .setMessage("緯度・経度の一覧をファイルをダウンロードしますか？")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //データを出力
                        boolean result = exportData();
                        //出力結果を表示
                        if (result) {
                            Toast text = Toast.makeText(MainActivity.this, "ファイル出力しました",Toast.LENGTH_SHORT);
                            text.show();
                        } else {
                            Toast text = Toast.makeText(MainActivity.this, "ファイル出力失敗",Toast.LENGTH_SHORT);
                            text.show();
                        }
                    }
                })
                .show();

        //デバッグ用コード。ファイルの中身読み込み。いちいちファイルを開いて確認すると面倒くさいのでLogで確認
        readFile();
    }

    //ファイル表示処理（保存）
    private boolean exportData() {

        File path = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        String testfile = "testfile.csv";
        File file = new File(path, testfile);

        //外部ストレージがマウントされているか確認
        String storageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(storageState)) {
            try {
                //内容を上書きしたいので、第二引数をfalseにする
                FileOutputStream fileOutputStream = new FileOutputStream(file, false);
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8);
                BufferedWriter bw = new BufferedWriter(outputStreamWriter);

                //データベースが使えるかどうか確認
                if(databaseHelper == null) {
                    databaseHelper = new DatabaseHelper(getApplicationContext());
                }

                if(db == null) {
                    db = databaseHelper.getReadableDatabase();
                }

                Cursor cursor = db.query(
                        "LatitudeLongitude",
                        new String[] {"longitude", "latitude", "time"},
                        null,
                        null,
                        null,
                        null,
                        null);

                //ファイルの一行目に書き込むタイトル
                bw.write("緯度・経度一覧\n");
                bw.flush();

                //データ行数分書きだす
                while(cursor.moveToNext()) {
                    Double longitude = cursor.getDouble(cursor.getColumnIndex("longitude"));
                    Double latitude = cursor.getDouble(cursor.getColumnIndex("latitude"));
                    String time = cursor.getString(cursor.getColumnIndex("time"));

                    //テキストをバッファに格納
                    bw.write(longitude + "," + latitude + "," + "測定時間" + time + "\n");
                    //バッファのデータをファイルに書き込む
                    bw.flush();
                }

                fileOutputStream.close();
                outputStreamWriter.close();
                bw.close();
                return true;

            } catch (Exception e) {
                Log.d("debag","ファイル書き込み失敗");
                e.printStackTrace();
                return false;
            }

        } else {
            return false;
        }

    }

    //ファイル読み込み。呼び出し時に、真偽値で戻り値を変返すことでエラーが発生したか判断する
    private boolean readFile() {
        File path = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        String testfile = "testfile.csv";
        File file = new File(path, testfile);


        //外部ストレージがマウントされているか確認
        String storageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(storageState) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(storageState)) {
          try {
                FileInputStream fileInputStream = new FileInputStream(file);
                InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
                BufferedReader reader = new BufferedReader(inputStreamReader);

                String lineBuff;
                String str = null;

                while((lineBuff = reader.readLine()) != null) {
                    str = lineBuff;
                    Log.d("debag","ファイルの中身は"+ str);
                }

                return true;

            } catch (Exception e) {
                Log.d("debag","ファイル読み込み失敗");
                return false;
            }

        } else {
            return false;
        }
    }


    //データベース保存処理
    private void insertData(SQLiteDatabase db, double latitude, double longitude, String time) {

        if(databaseHelper == null) {
            databaseHelper = new DatabaseHelper(getApplicationContext());
        }

        if(db == null) {
            db = databaseHelper.getWritableDatabase();
        }

        ContentValues values = new ContentValues();
        values.put("longitude", longitude);
        values.put("latitude", latitude);
        values.put("time", time);


        db.insert("LatitudeLongitude", null, values);

    }

    //データベース表示処理。これはデバックの際にデータ適切に挿入されているか確認するためのメゾット
    //どこかのボタンのonClickの処理として使うのがよい
    private void readData() {
        if(databaseHelper == null) {
            databaseHelper = new DatabaseHelper(getApplicationContext());
        }

        if(db == null) {
            db = databaseHelper.getWritableDatabase();
        }

        Log.d("debug", "*******Cursor");

        Cursor cursor = db.query(
                "LatitudeLongitude",
                new String[] {"longitude", "latitude", "time"},
                null,
                null,
                null,
                null,
                null);

        cursor.moveToFirst();

        StringBuilder sbuilder = new StringBuilder();


        try {
            for(int i=0; i < cursor.getCount(); i++) {
                sbuilder.append(cursor.getDouble(0));
                sbuilder.append(":");
                sbuilder.append(cursor.getDouble(1));
                sbuilder.append("\n");
                sbuilder.append(cursor.getString(2));
                sbuilder.append("\n");
                cursor.moveToNext();
            }
        }
        finally {
                cursor.close();
                Log.d("debag", "*******" + sbuilder.toString());
            }
        }

}
