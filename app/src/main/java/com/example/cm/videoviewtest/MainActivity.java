package com.example.cm.videoviewtest;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.File;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;

public class MainActivity extends Activity implements OnClickListener {

    private Button btn_videoview, btn_controller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_videoview = (Button)findViewById(R.id.btn_videoview);
        btn_controller = (Button)findViewById(R.id.btn_controller);

        btn_videoview.setOnClickListener(this);
        btn_controller.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Intent intent = null;
        switch (v.getId()){
            case R.id.btn_videoview:
                intent = new Intent(MainActivity.this,VideoViewActivity.class);
                startActivity(intent);
                break;
            case R.id.btn_controller:
                intent = new Intent(MainActivity.this, ControllerActivity.class);
                startActivity(intent);
                break;
            default:
                break;
        }
    }
}
