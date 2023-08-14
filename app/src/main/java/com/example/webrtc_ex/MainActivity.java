package com.example.webrtc_ex;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.example.webrtc_ex.rtc.webrtcActivity;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    // 권한 요청 코드
    private static final int PERMISSION_REQUEST_CODE = 123;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 연결 버튼
        findViewById(R.id.btn_start).setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.btn_start) {
            String roomName = ((EditText) findViewById(R.id.et_room)).getText().toString();

            if(roomName.length() < 2) {
                Toast.makeText(MainActivity.this, "2글자 이상", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(this, webrtcActivity.class);
            Bundle dataBundle = new Bundle();
            dataBundle.putString("ROOM_NAME", roomName);
            intent.putExtras(dataBundle);
            startActivity(intent);
        }
    }



}

