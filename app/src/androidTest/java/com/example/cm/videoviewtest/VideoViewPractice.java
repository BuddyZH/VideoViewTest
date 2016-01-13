package com.example.cm.videoviewtest;

import android.content.Context;
import android.drm.DrmManagerClient;
import android.media.AudioManager;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.MediaController;
import android.widget.VideoView;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Vector;

/**
 * Created by cm on 2016/1/12.
 */
public class VideoViewPractice extends SurfaceView
        implements MediaController.MediaPlayerControl {

        private String TAG = "VideoView";
        private Uri mUri;
        private Map<String,String> mHeaders;

        private static final int STATE_ERROR = -1;
        private static final int STATE_IDLE  = 0;
        private static final int STATE_PREPARING  = 1;
        private static final int STATE_PREPARED  = 2;
        private static final int STATE_PLAYING  = 3;
        private static final int STATE_PAUSED  = 4;
        private static final int STATE_PLAYBACK_COMPLETED  = 5;

        private static int mCurrentState  = STATE_IDLE;
        private static int mTargetState  = STATE_IDLE;

        private SurfaceHolder mSurfaceHolder = null;
        private MediaPlayer mMediaPlayer = null;
        private int mAudiSession;
        private int mVideoWidth;
        private int mVideoHeight;
        private int mSurfaceWidth;
        private int mSurfaceHeight;
        private MediaController mMediaController;
        private MediaPlayer.OnCompletionListener mOnCompletionListener;
        private MediaPlayer.OnPreparedListener mOnPreparedListener;
        private int mCurrentBufferPercentage;
        private MediaPlayer.OnErrorListener mOnErrorListener;
        private MediaPlayer.OnInfoListener mOnInfoListener;
        private int mSeekWhenPrepared;
        private boolean mCanPause;
        private boolean mCanSeekBack;
        private boolean mCanSeekForward;

        public VideoViewPractice(Context context){
                super(context);
                initVideoView();
        }

        private Vector<Pair<InputStream, MediaFormat>> mPendingSubtitleTracks;

        private void initVideoView(){
                mVideoWidth = 0;
                mVideoHeight = 0;
                getHolder().addCallback(mSHCallback);
                getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
                setFocusable(true);
                setFocusableInTouchMode(true);
                requestFocus();

                // 字幕相关，用不到
                mPendingSubtitleTracks = new Vector<Pair<InputStream, MediaFormat>>();
                mCurrentState = STATE_IDLE;
                mTargetState = STATE_IDLE;
        }

        public void setVideoPath(String path){
                setVideoURI(Uri.parse(path));
        }

        public void setVideoURI(Uri uri){
                setVideoURI(uri, null);
        }

        public void setVideoURI(Uri uri, Map<String, String> headers){
                mUri = uri;
                mHeaders = headers;
                mSeekWhenPrepared = 0;
                openVideo();
                requestLayout();
                invalidate();
        }

        SurfaceHolder.Callback mSHCallback = new SurfaceHolder.Callback(){
                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                        mSurfaceWidth = width;
                        mSurfaceHeight = height;
                        boolean isValidState = (mTargetState == STATE_PLAYING);
                        boolean hasValidSize = (mVideoWidth == width && mVideoHeight == height);
                        if (mMediaPlayer != null && isValidState && hasValidSize){
                                if (mSeekWhenPrepared != 0){
                                        seekTo(mSeekWhenPrepared);
                                }
                                start();
                        }
                }

                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                        mSurfaceHolder = holder;
                        openVideo();
                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                        mSurfaceHolder = null;
                        if (mMediaController != null) mMediaController.hide();
                        release(true);
                }
        };
        public void surfaceCreated(SurfaceHolder holder){
                mSurfaceHolder = holder;
                openVideo();
        }

        private void openVideo(){
                if (mUri == null || mSurfaceHolder == null){
                        return;
                }
                release(false);
                AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
                am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
                try{
                        mMediaPlayer = new MediaPlayer();
                        final Context context = getContext();
                        if (mAudiSession != 0){
                                mMediaPlayer.setAudioSessionId(mAudiSession);
                        }else {
                                mAudiSession = mMediaPlayer.getAudioSessionId();
                        }
                        mMediaPlayer.setOnPreparedListener(mPreparedListener);
                        mMediaPlayer.setOnVideoSizeChangedListener(mSizeChangedListener);
                        mMediaPlayer.setOnCompletionListener(mCompletionListener);
                        mMediaPlayer.setOnErrorListener(mErrorListener);
                        mMediaPlayer.setOnInfoListener(mInfoListener);
                        mMediaPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);
                        mCurrentBufferPercentage = 0;
                        mMediaPlayer.setDataSource(mContext, mUri, mHeaders);
                        mMediaPlayer.setDisplay(mSurfaceHolder);
                        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                        mMediaPlayer.setScreenOnWhilePlaying(true);
                        mMediaPlayer.prepareAsync();

                        mCurrentState = STATE_PREPARING;
                        attachMediaController();
                }catch (IOException ex){
                        Log.w(TAG,"Unable to open content: "+ mUri, ex);
                        mCurrentState = STATE_ERROR;
                        mTargetState = STATE_ERROR;
                        mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
                        return;
                }catch (IllegalArgumentException ex){
                        Log.w(TAG,"Unable to open content:" + mUri, ex);
                        mCurrentState = STATE_ERROR;
                        mTargetState = STATE_ERROR;
                        mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
                        return;
                }finally{
                        mPendingSubtitleTracks.clear();
                }
        }

        public void setMediaController(MediaController controller){
                if (mMediaController != null){
                        mMediaController.hide();
                }
                mMediaController = controller;
                attachMediaController();
        }

        private void attachMediaController(){
                if (mMediaPlayer != null && mMediaController != null){
                        mMediaController.setMediaPlayer(this);
                        View anchorView = this.getParent() instanceof View ? (View)this.getParent() : this;
                        mMediaController.setAnchorView(anchorView);
                        mMediaController.setEnabled(isInPlayStack());

                }
        }

        MediaPlayer.OnVideoSizeChangedListener mSizeChangedListener =
                new MediaPlayer.OnVideoSizeChangedListener() {
                        @Override
                        public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                                mVideoHeight = mp.getVideoHeight();
                                mVideoWidth = mp.getVideoWidth();
                                if (mVideoWidth != 0 && mVideoHeight != 0){
                                        getHolder().setFixedSize(mVideoWidth, mVideoHeight);
                                         requestLayout();
                                }
                        }
                };
        MediaPlayer.OnPreparedListener mPreparedListener = new MediaPlayer.OnPreparedListener(){
                @Override
                public void onPrepared(MediaPlayer mp) {
                        mCurrentState = STATE_PREPARED;
                }
        };

        private MediaPlayer.OnCompletionListener mCompletionListener =
                new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {

                        }
                };
        private MediaPlayer.OnInfoListener mInfoListener =
                new MediaPlayer.OnInfoListener() {
                        @Override
                        public boolean onInfo(MediaPlayer mp, int what, int extra) {
                                return false;
                        }
                };
        private MediaPlayer.OnErrorListener mErrorListener =
                new MediaPlayer.OnErrorListener() {
                        @Override
                        public boolean onError(MediaPlayer mp, int what, int extra) {
                                return false;
                        }
                };

        private MediaPlayer.OnBufferingUpdateListener mBufferingUpdateListener =
                new MediaPlayer.OnBufferingUpdateListener() {
                        @Override
                        public void onBufferingUpdate(MediaPlayer mp, int percent) {
                                mCurrentBufferPercentage = percent;
                        }
                };
        private void release(boolean cleartargetstate){
                if (mMediaPlayer != null){
                        mMediaPlayer.reset();
                        mMediaPlayer.release();
                        mMediaPlayer = null;
                        mPendingSubtitleTracks.clear();
                        mCurrentState = STATE_IDLE;
                        if (cleartargetstate){
                                mTargetState = STATE_IDLE;
                        }
                        AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
                        am.abandonAudioFocus(null);
                }
        }

        public void setOnPreparedListener(MediaPlayer.OnPreparedListener l){
               mOnPreparedListener = l;
        }
        public void setOnErrorListener(MediaPlayer.OnErrorListener l){
                mOnErrorListener = l;
        }
        public void setOnInfoListener(MediaPlayer.OnInfoListener l){
                mOnInfoListener = l;
        }
        @Override
        public void start() {
                if (isInPlayStack()){
                        mMediaPlayer.start();
                        mCurrentState = STATE_PLAYING;
                }
                mTargetState = STATE_PLAYING;
        }

        @Override
        public void pause() {
                if (isInPlayStack()){
                        if (mMediaPlayer.isPlaying()){
                                mMediaPlayer.pause();
                                mCurrentState = STATE_PAUSED;
                        }
                }
                mTargetState = STATE_IDLE;
        }

        @Override
        public int getDuration() {
                if (isInPlayStack()){
                        return mMediaPlayer.getDuration();
                }
                return -1;
        }

        @Override
        public int getCurrentPosition() {
                if (isInPlayStack()){
                        return mMediaPlayer.getCurrentPosition();
                }
                return 0;
        }

        @Override
        public void seekTo(int msec) {
                if (isInPlayStack()){
                        mMediaPlayer.seekTo(msec);
                        mSeekWhenPrepared = 0;
                }else {
                        mSeekWhenPrepared = msec;
                }
        }

        @Override
        public boolean isPlaying() {
                return isInPlayStack() && mMediaPlayer.isPlaying();
        }

        @Override
        public int getBufferPercentage() {
                if (mMediaPlayer != null){
                        return mCurrentBufferPercentage;
                }
                return 0;
        }

        @Override
        public boolean canPause() {
                return mCanPause;
        }

        @Override
        public boolean canSeekBackward() {
                return mCanSeekBack;
        }

        @Override
        public boolean canSeekForward() {
                return mCanSeekForward;
        }

        @Override
        public int getAudioSessionId() {
                if (mAudiSession == 0){
                        MediaPlayer foo = new MediaPlayer();
                        mAudiSession = foo.getAudioSessionId();
                        foo.release();
                }
                return mAudiSession;
        }

        private boolean isInPlayStack(){
                return (mMediaPlayer != null &&
                        mCurrentState != STATE_ERROR &&
                        mCurrentState != STATE_IDLE &&
                        mCurrentState != STATE_PREPARING);
        }
}
