package com.example.cm.videoviewtest;

import android.app.Activity;
import android.view.View;
import android.widget.MediaController;
import android.os.Bundle;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.File;

/**
 * Created by cm on 2015/12/30.
 */
public class ControllerActivity extends Activity{
    private VideoView videoView;
    private MediaController mController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controller);
       // 实例化MediaController
        mController = new MediaController(this);
        videoView = (VideoView)findViewById(R.id.videoview);
        File file = new File("/storage/emulated/0/03-Handler和Message.mp4");
        if (file.exists()){
            // 设置播放视频源的路径
            videoView.setVideoPath(file.getAbsolutePath());
            // 为VideoView指定MediaController
            videoView.setMediaController(mController);
            // 为MediaController指定控制的VideoView
            mController.setMediaPlayer(videoView);
            /*// 增加监听上一个和下一个的切换事件，默认这两个按钮是不显示的
            mController.setPrevNextListeners(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(ControllerActivity.this,"下一个",Toast.LENGTH_SHORT).show();
                }
            }, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(ControllerActivity.this,"上一个",Toast.LENGTH_SHORT).show();
                }
            });*/
        }

    }
}
