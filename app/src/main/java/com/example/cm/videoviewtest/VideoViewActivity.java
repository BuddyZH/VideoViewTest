package com.example.cm.videoviewtest;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.gesture.GestureOverlayView;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import android.view.View.OnClickListener;
import android.view.GestureDetector.OnGestureListener;

import com.example.cm.videoviewtest.utils.DensityUtil;
import com.example.cm.videoviewtest.utils.MyGestures;
import com.example.cm.videoviewtest.utils.StringHelper;
import com.example.cm.videoviewtest.utils.VerticalSeekBar;

import java.io.File;
import java.text.BreakIterator;

/**
 * Created by cm on 2015/12/30.
 */
public class VideoViewActivity extends Activity implements OnClickListener{

    private final String TAG = "main";
    private final String VIDEOPATH = "/storage/emulated/0/03-Handler和Message.mp4";

    private BatteryReceiver batteryReceiver;
    private AudioManager mAM;
    private int mMaxVolume, mCurrentVolume;
    private int mTopLayoutHeight,mBottomLayoutHeight;
    private RelativeLayout rootLayout;

    private LinearLayout mTopLayout;
    private TextView mDateTime;
    private ImageButton mBackButton;
    private ImageView mBatteryLevel;
    private TextView mVideoName;
    private LinearLayout mBottomLayout;
    private ImageView mPlayPauseButton;
    private TextView mCurrentTime;
    private TextView mTotalTime;
    private SeekBar mBottomPlaySeekBar;
    private VerticalSeekBar mVolumeSeekBar;
    private ImageButton mVolumeMuteButton;
    private VideoView videoView;
    private ImageButton mScreenLockButton;
    private boolean mDragging;
    private boolean mShowing;
    private boolean mIsLockScreen;
    private static final int DEFAULT_SEEKBAR_VALUE = 1000;
    private static final int TIME_TICK_INTERVAL = 1000;
    private static final int DEFAULT_TIMEOUT = 3000;

    private static final int FADE_OUT = 1;
    private static final int SHOW_PROGRESS = 2;
    private static final int MSG_TIME_TICK = 3;

    private RelativeLayout gesture_volume_layout, gesture_bright_layout, gesture_progress_layout;
    private TextView gesture_tv_volume_percentage, gesture_tv_bright_percentage, gesture_tv_progress_time;
    private ImageView gesture_iv_player_volume, gesture_iv_player_bright, gesture_iv_progress;
    private GestureDetector gestureDetector;
    private float mBrightness = -1f;

    private ObjectAnimator mTopFadeInObjectAnimator;
    private ObjectAnimator mBottomFadeInObjectAnimator;
    private ObjectAnimator mTopFadeOutObjectAnimator;
    private ObjectAnimator mBottomFadeOutObjectAnimator;

    private MyGestures myGestures;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_videoview);

        mAM = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mMaxVolume = mAM.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        videoView = (VideoView) findViewById(R.id.videoview);


        /*顶栏*/
        mTopLayout = (LinearLayout) findViewById(R.id.mediacontroller_top_layout);
        mDateTime = (TextView) findViewById(R.id.date_time);
        mBackButton = (ImageButton) findViewById(R.id.mediacontroller_back);
        mBatteryLevel = (ImageView) findViewById(R.id.battery_level);
        mBatteryLevel.setImageLevel(0);
        mVideoName = (TextView) findViewById(R.id.mediacontroller_file_name);


         /*底部控制栏*/
        mBottomLayout = (LinearLayout) findViewById(R.id.mediacontroller_bottom_layout);
        mPlayPauseButton = (ImageView) findViewById(R.id.mediacontroller_play_pause);
        mCurrentTime = (TextView) findViewById(R.id.mediacontroller_time_current);
        mTotalTime = (TextView) findViewById(R.id.mediacontroller_time_total);
        mBottomPlaySeekBar = (SeekBar) findViewById(R.id.seekBar);
        mVolumeMuteButton = (ImageButton) findViewById(R.id.mediacontroller_volume);
        mVolumeSeekBar = (VerticalSeekBar) findViewById(R.id.mediacontroller_volume_controller);

        mScreenLockButton = (ImageButton) findViewById(R.id.mediacontroller_screen_lock);

        /*监听事件*/
        mBackButton.setOnClickListener(this);
        mPlayPauseButton.setOnClickListener(this);
        mVolumeMuteButton.setOnClickListener(this);
        mScreenLockButton.setOnClickListener(this);

        mVolumeSeekBar.setOnSeekBarChangeListener(mVolumeSeekBarListener);
        mVolumeSeekBar.setMax(DEFAULT_SEEKBAR_VALUE);
        mBottomPlaySeekBar.setOnSeekBarChangeListener(mSeekListener);

        /*手势音量/进度/亮度*/
        gesture_volume_layout = (RelativeLayout) findViewById(R.id.gesture_volume_layout);
        gesture_bright_layout = (RelativeLayout) findViewById(R.id.gesture_bright_layout);
        gesture_progress_layout = (RelativeLayout) findViewById(R.id.gesture_progress_layout);
        gesture_tv_progress_time = (TextView) findViewById(R.id.gesture_tv_progress_time);
        gesture_tv_volume_percentage = (TextView) findViewById(R.id.gesture_tv_volume_percentage);
        gesture_tv_bright_percentage = (TextView) findViewById(R.id.gesture_tv_bright_percentage);
        gesture_iv_progress = (ImageView) findViewById(R.id.gesture_iv_progress);
        gesture_iv_player_bright = (ImageView) findViewById(R.id.gesture_iv_player_bright);
        gesture_iv_player_volume = (ImageView) findViewById(R.id.gesture_iv_player_volume);

        myGestures = new MyGestures(this);
        myGestures.setTouchListener(mGustureTouchListener);


        mTopLayoutHeight = mTopLayout.getLayoutParams().height;
        mBottomLayoutHeight = mBottomLayout.getLayoutParams().height;
        Log.i(TAG,"mTopLayoutHeight = " + mTopLayoutHeight);
        Log.i(TAG,"mBottomLayoutHeight = "+ mBottomLayoutHeight);

        /*开始播放本地文件*/
        Log.i(TAG, "获取视频文件地址");
        String path = VIDEOPATH;
        File file = new File(path);
        if (!file.exists()) {
            Toast.makeText(this, "视频文件路径错误", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] temp = path.split("/");
        final String videoName = temp[temp.length - 1];
        mVideoName.setText(videoName);

        Log.i(TAG, "指定视频源路径:"+path+";"+"视频名称:"+videoName);
        videoView.setVideoPath(file.getAbsolutePath());

        registerBatteryReceiver();

        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                Log.i(TAG, "视频时长：" + StringHelper.stringForTime(videoView.getDuration()));
                mTotalTime.setText(StringHelper.stringForTime(videoView.getDuration()));
                mHandler.sendEmptyMessage(SHOW_PROGRESS);
                mHandler.sendEmptyMessage(MSG_TIME_TICK);
                mPlayPauseButton.setImageResource(R.drawable.akbm_mediacontroller_pause);
                View placeholder = findViewById(R.id.placedholder);
                placeholder.setVisibility(View.GONE);
                Log.i(TAG, "开始播放");
                videoView.start();
                mShowing = true;
                show(DEFAULT_TIMEOUT);
            }
        });
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mPlayPauseButton.setImageResource(R.drawable.akbm_mediacontroller_play);
            }
        });
        videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                videoView.seekTo(0);
                Log.i(TAG, "MediaPlayer Error");
                return false;
            }
        });
    }

    private void registerBatteryReceiver() {
        IntentFilter batteryFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        batteryReceiver = new BatteryReceiver();
        registerReceiver(batteryReceiver, batteryFilter);
    }

    private void unregisterBatteryReceiver(){
        if (batteryReceiver == null)
            return;
        try{
            unregisterReceiver(batteryReceiver);
            batteryReceiver = null;
        }catch (IllegalArgumentException e){
            Log.e(TAG,"unregister BatteryReceiver illegalArgumentException.",e);
        }
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (mShowing) {
                    mHandler.sendEmptyMessage(FADE_OUT);
                    Log.i(TAG, "ACTION_DOWN  hide()");
                } else {
                    if (videoView.isPlaying()) {
                        show(DEFAULT_TIMEOUT);
                        Log.i(TAG, "ACTION_DOWN  show(DEFAULT_TIMEOUT)");
                    } else {
                        show(0);
                        Log.i(TAG, "ACTION_DOWN  show(0)");
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                gesture_volume_layout.setVisibility(View.GONE);
                gesture_progress_layout.setVisibility(View.GONE);
                gesture_bright_layout.setVisibility(View.GONE);
                break;
            default:
                break;
        }
        return myGestures.onTouchEvent(event);
    }


    private SeekBar.OnSeekBarChangeListener mSeekListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            mDragging = true;
            mHandler.removeMessages(SHOW_PROGRESS);
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            long duration = videoView.getDuration();
            long newposition = (duration * progress) / DEFAULT_SEEKBAR_VALUE;
            if (mCurrentTime != null) {
                mCurrentTime.setText(StringHelper.stringForTime((int) newposition));
            }

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            mDragging = false;
            int progress = seekBar.getProgress();
            long duration = videoView.getDuration();
            if (videoView != null) {
                if (!videoView.isPlaying()) {
                    videoView.start();
                }
                videoView.seekTo((int) (duration * progress / DEFAULT_SEEKBAR_VALUE));
            }
            updatePausePlay();
            show(DEFAULT_TIMEOUT);
            mHandler.sendEmptyMessage(SHOW_PROGRESS);
        }
    };

    private VerticalSeekBar.OnSeekBarChangeListener mVolumeSeekBarListener = new VerticalSeekBar.OnSeekBarChangeListener() {
        int startVolume = 0;
        int endVolume = 0;

        @Override
        public void onProgressChanged(VerticalSeekBar vBar, int progress, boolean fromUser) {
            if (!fromUser) {
                return;
            }
            int v = (mMaxVolume * progress) / DEFAULT_SEEKBAR_VALUE;
            updateVolume(v);
        }

        @Override
        public void onStartTrackingTouch(VerticalSeekBar vBar) {
            startVolume = mAM.getStreamVolume(AudioManager.STREAM_MUSIC);
            mHandler.removeMessages(FADE_OUT);
        }

        @Override
        public void onStopTrackingTouch(VerticalSeekBar vBar) {
            mHandler.removeMessages(FADE_OUT);
            Message msg = mHandler.obtainMessage(FADE_OUT);
            mHandler.sendMessageDelayed(msg, DEFAULT_TIMEOUT);
            endVolume = mAM.getStreamVolume(AudioManager.STREAM_MUSIC);
        }
    };

    private void updateVolume(int v) {
        if (mAM == null || mVolumeMuteButton == null || mVolumeSeekBar == null) return;

        if (v > mMaxVolume) {
            v = mMaxVolume;
        } else if (v < 0) {
            v = 0;
        }

        try {
            mAM.setStreamVolume(AudioManager.STREAM_MUSIC, v, 0);
        } catch (Exception e) {
            Log.i(TAG, "Exception", e);
        }

        updateVolumeMuteButton(v);
    }

    private void updateVolumeMuteButton(int v) {
        if (v <= 0) {
            mVolumeMuteButton.setImageResource(R.drawable.akbm_mute_selector);
            mVolumeSeekBar.setProgress(0);
        } else {
            mVolumeMuteButton.setImageResource(R.drawable.akbm_volume_selector);
            int progress = (int) ((v * 1.0 / mMaxVolume) * mVolumeSeekBar.getMax());
            mVolumeSeekBar.setProgress(progress);
        }
    }

    private void doPauseResume() {
        if (videoView.isPlaying()) {
            videoView.pause();
            mHandler.removeMessages(FADE_OUT);
            show(0);
        } else {
            videoView.start();
            show(DEFAULT_TIMEOUT);
        }
        updatePausePlay();
    }

    private void updatePausePlay() {
        if (mPlayPauseButton == null) {
            return;
        }
        if (videoView.isPlaying()) {
            mPlayPauseButton.setImageResource(R.drawable.akbm_mediacontroller_pause);
        } else {
            mPlayPauseButton.setImageResource(R.drawable.akbm_mediacontroller_play);
        }
    }


    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int pos;
            switch (msg.what) {
                case FADE_OUT:
                    hide();
                    break;
                case SHOW_PROGRESS:
                    pos = setProgress();
                    if (!mDragging && videoView.isPlaying()) {
                        msg = obtainMessage(SHOW_PROGRESS);
                        sendMessageDelayed(msg, 1000 - (pos % 1000));
                    }
                    break;
                case MSG_TIME_TICK:
                    mDateTime.setText(StringHelper.stringForCurrentTime());
                    mHandler.sendEmptyMessageDelayed(MSG_TIME_TICK, TIME_TICK_INTERVAL);
                    break;

            }
        }
    };

    public void show(int timeout) {
        if (!mShowing) {
            setProgress();
            if (mPlayPauseButton != null) {
                mPlayPauseButton.requestFocus();
            }
            startTopFadeInAnimator();
            startBottomFadeInAnimator();
            mVolumeMuteButton.setEnabled(true);
            mScreenLockButton.setVisibility(View.VISIBLE);
            mShowing = true;
        }
        updatePausePlay();
        mHandler.sendEmptyMessage(SHOW_PROGRESS);
        if (timeout != 0) {
            mHandler.removeMessages(FADE_OUT);
            Message msg = mHandler.obtainMessage(FADE_OUT);
            mHandler.sendMessageDelayed(msg, timeout);
        }
    }

    public void hide() {
        if (mShowing) {
            try {
                mHandler.removeMessages(SHOW_PROGRESS);
                mVolumeMuteButton.setEnabled(false);
                startTopFadeOutAnimator();
                startBottomFadeOutAnimator();
                mVolumeSeekBar.setVisibility(View.GONE);
                mScreenLockButton.setVisibility(View.GONE);
            } catch (IllegalArgumentException ex) {
                Log.w("MediaController", "already removed");
            }
        }
        mShowing = false;
    }

    private int setProgress() {
        if (videoView == null || mDragging) {
            return 0;
        }
        int position = videoView.getCurrentPosition();
        int duration = videoView.getDuration();
        if (mBottomPlaySeekBar != null) {
            if (duration > 0) {
                long pos = 1000L * position / duration;
                mBottomPlaySeekBar.setProgress((int) pos);
            }
        }
        if (mCurrentTime != null) {
            mCurrentTime.setText(StringHelper.stringForTime(position));
        }
        return position;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.mediacontroller_play_pause:
                doPauseResume();
                break;
            case R.id.mediacontroller_back:
                Intent intent = new Intent(VideoViewActivity.this, MainActivity.class);
                startActivity(intent);
                break;
            case R.id.mediacontroller_volume:
                if (mVolumeSeekBar.getVisibility() != View.VISIBLE) {
                    mVolumeSeekBar.setVisibility(View.VISIBLE);
                } else {
                    mVolumeSeekBar.setVisibility(View.GONE);
                }
                int volume = mAM.getStreamVolume(AudioManager.STREAM_MUSIC);
                updateVolume(volume);
                break;
            case R.id.mediacontroller_screen_lock:
                lockOrUnlockScreen();
                break;
            default:
                break;
        }
    }

    private void lockOrUnlockScreen() {
        mBackButton.setEnabled(mIsLockScreen);
        mPlayPauseButton.setEnabled(mIsLockScreen);
        mBottomPlaySeekBar.setEnabled(mIsLockScreen);
        mVolumeSeekBar.setEnabled(mIsLockScreen);
        mVolumeMuteButton.setEnabled(mIsLockScreen);
        if (mIsLockScreen) {
            mScreenLockButton.setImageResource(R.drawable.akbm_mediacontroller_unlock);
        } else {
            mScreenLockButton.setImageResource(R.drawable.akbm_mediacontroller_lock);
        }
        mIsLockScreen = !mIsLockScreen;
    }

    private void setBatteryLevel(int percent) {
        if (mBatteryLevel != null) {
            mBatteryLevel.setImageLevel(percent);
        }
    }

    private MyGestures.TouchListener mGustureTouchListener = new MyGestures.TouchListener() {

        @Override
        public void onGestureBegin() {

        }

        @Override
        public void onGestureEnd() {

        }

        @Override
        public void onLeftSlide(float percent) {
            gesture_progress_layout.setVisibility(View.GONE);
            gesture_volume_layout.setVisibility(View.GONE);
            gesture_bright_layout.setVisibility(View.VISIBLE);
            gesture_iv_player_bright.setImageResource(R.mipmap.souhu_player_bright);
            mBrightness = getWindow().getAttributes().screenBrightness;
            WindowManager.LayoutParams lpa = getWindow().getAttributes();
            lpa.screenBrightness = mBrightness + percent;
            if (lpa.screenBrightness > 1.0f){
                lpa.screenBrightness = 1.0f;
            } else if (lpa.screenBrightness < 0.01f){
                lpa.screenBrightness = 0.01f;
            }
            mBrightness = lpa.screenBrightness;
            getWindow().setAttributes(lpa);
            gesture_tv_bright_percentage.setText((int) (lpa.screenBrightness * 100) + "%");
        }

        @Override
        public void onRightSlide(float percent,boolean isIncrease) {
            gesture_progress_layout.setVisibility(View.GONE);
            gesture_volume_layout.setVisibility(View.VISIBLE);
            gesture_bright_layout.setVisibility(View.GONE);
            mCurrentVolume = mAM.getStreamVolume(AudioManager.STREAM_MUSIC);
            if (isIncrease){
                gesture_iv_player_volume.setImageResource(R.mipmap.souhu_player_volume);
                if (mCurrentVolume < mMaxVolume){
                    mCurrentVolume++;
                }
            }else {
                if (mCurrentVolume > 0){
                    mCurrentVolume--;
                    if (mCurrentVolume == 0){
                        gesture_iv_player_volume.setImageResource(R.mipmap.souhu_player_silence);
                    }
                }
            }
            int percentage = (mCurrentVolume * 100) / mMaxVolume;
            gesture_tv_volume_percentage.setText(percentage + "%");
            mAM.setStreamVolume(AudioManager.STREAM_MUSIC, mCurrentVolume, 0);
        }

        @Override
        public void onHorizonSlide(float percent, boolean isForward) {
            gesture_progress_layout.setVisibility(View.VISIBLE);
            gesture_volume_layout.setVisibility(View.GONE);
            gesture_bright_layout.setVisibility(View.GONE);
            int playingTime = videoView.getCurrentPosition() / 1000;
            int videoTotalTime = videoView.getDuration() / 1000;
            if (isForward){
                gesture_iv_progress.setImageResource(R.mipmap.souhu_player_forward);
                if (playingTime < videoTotalTime - 16){
                    playingTime += 3;
                } else {
                    playingTime = videoTotalTime - 10;
                }
            }else {
                gesture_iv_progress.setImageResource(R.mipmap.souhu_player_backward);
                if (playingTime > 3){
                    playingTime -= 3;
                } else{
                    playingTime = 0;
                }
            }
            if (playingTime < 0){
                playingTime = 0;
            }
            videoView.seekTo(playingTime * 1000);
            gesture_tv_progress_time.setText(StringHelper.stringForTime(playingTime * 1000) + "/" + StringHelper.stringForTime(videoTotalTime * 1000));
        }

        @Override
        public void onSingleTap() {

        }

        @Override
        public void onDoubleTap() {

        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return false;
        }
    };

    class BatteryReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
                int level = intent.getIntExtra("level", 0);
                int scale = intent.getIntExtra("scale", 100);
                int percent = level * 100 / scale;
                Log.i(TAG, "currentBatteryLevel: " + percent);
                setBatteryLevel(percent);
            }

        }
    }
    private ObjectAnimator makeAnimator(final Object target, String propertyName, int duration,
                                        float... values){
        ObjectAnimator mAnimator = ObjectAnimator.ofFloat(target, propertyName, values);
        mAnimator.setDuration(duration);
        return  mAnimator;
    }

    private void startTopFadeInAnimator(){
        mTopFadeInObjectAnimator = makeAnimator(mTopLayout, "transparentY", 500, mTopLayoutHeight, 0);
        mTopFadeInObjectAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                Log.i(TAG,"mTopFadeInObjectAnimator value:"+value);
                mTopLayout.setAlpha(1 - value / mTopLayoutHeight);
            }
        });
        mTopFadeInObjectAnimator.start();
    }
    private void startTopFadeOutAnimator(){
        mTopFadeOutObjectAnimator = makeAnimator(mTopLayout, "transparentY", 500, 0, -mTopLayoutHeight);
        mTopFadeOutObjectAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                Log.i(TAG,"mTopFadeOutObjectAnimator value:"+value);
                mTopLayout.setAlpha(1 + value / mTopLayoutHeight);
            }
        });
        mTopFadeOutObjectAnimator.start();
    }
    private void startBottomFadeInAnimator(){
        mBottomFadeInObjectAnimator = makeAnimator(mBottomLayout, "transparentY", 500, mBottomLayoutHeight, 0);
        mBottomFadeInObjectAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                Log.i(TAG,"mBottomFadeInObjectAnimator value:"+value);
                mBottomLayout.setAlpha(1 - value / mBottomLayoutHeight);
            }
        });
        mBottomFadeInObjectAnimator.start();
    }
    private void startBottomFadeOutAnimator(){
        mBottomFadeOutObjectAnimator = makeAnimator(mBottomLayout, "transparentY", 500, 0, -mBottomLayoutHeight);
        mBottomFadeOutObjectAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                Log.i(TAG,"mBottomFadeOutObjectAnimator value:"+value);
                mBottomLayout.setAlpha(1 + value / mBottomLayoutHeight);
            }
        });
        mBottomFadeOutObjectAnimator.start();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterBatteryReceiver();
    }

}
