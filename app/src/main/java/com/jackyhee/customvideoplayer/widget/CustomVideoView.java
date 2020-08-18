package com.jackyhee.customvideoplayer.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.transition.TransitionManager;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.DrawableRes;

import com.jackyhee.customvideoplayer.R;
import com.jackyhee.customvideoplayer.utils.Utils;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author hexj
 * @createDate 2020/8/7 10:31
 **/
public class CustomVideoView extends RelativeLayout implements View.OnClickListener {

    private static final String TAG = CustomVideoView.class.getSimpleName();

    private static final int TIME_CONTROLS_SHOW = 3000;// time controls show lasts
    private Point mVideoSize = null;
    private SurfaceView mSurfaceView;
    private View mTitleLayout;//title bar
    private View mCtrlLayout;//layout of the controls bar
    private TextView mTvCurrentTime;//current play time
    private TextView mTvDuration;//duration of the video
    private ImageView mIvToggle;//toggle btn for play/pause
    private ImageView mIvZoom;//zoom btn for full screen or not
    private SeekBar mSeekBar;
    private View mLoadingView;//loading view
    private MediaPlayer mMediaPlayer;
    private String mVideoUrl;//online video url
    /**
     * The title of the video
     */
    private String mTitle;
    private int mCurrentPosition;//current play position
    private int mDuration;//duration of video
    /**
     * Handles timeout for media controls.
     */
    UIHandler mHandler = new UIHandler(this);

    /**
     * The listener for all the events we publish.
     */
    VideoListener videoListener;

    private Context mContext;

    public CustomVideoView(Context context) {
        this(context, null, 0);
    }

    public CustomVideoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        setBackgroundColor(Color.BLACK);
        // Inflate the content
        inflate(context, R.layout.custom_video_view, this);
        initView();
    }

    private void initView() {
        mSurfaceView = (SurfaceView) findViewById(R.id.custom_video_surface);
        mCtrlLayout = findViewById(R.id.layout_controls);
        mTitleLayout = findViewById(R.id.llyt_title_bar);
        mIvToggle = (ImageView) findViewById(R.id.iv_play_pause);
        mIvZoom = (ImageView) findViewById(R.id.iv_full_mode);
        mTvCurrentTime = (TextView) findViewById(R.id.tv_current);
        mTvDuration = (TextView) findViewById(R.id.tv_duration);
        mSeekBar = (SeekBar) findViewById(R.id.seekbar_playback);
        mLoadingView = findViewById(R.id.layout_loading);
        mSurfaceView.setOnClickListener(this);
        mIvToggle.setOnClickListener(this);
        mIvZoom.setOnClickListener(this);
        findViewById(R.id.img_video_view_back).setOnClickListener(this);
        mSeekBar.setThumbOffset(0);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    showControls();
                }

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

                Log.d(TAG, "onStartTrackingTouch");
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.d(TAG, "onStopTrackingTouch");
                if (mMediaPlayer != null) {
                    if (mMediaPlayer.isPlaying()) {
                        mMediaPlayer.pause();
                    }
                    int progress = seekBar.getProgress();
                    if(progress > mDuration) {
                        progress = mDuration ;
                    }
                    Log.d(TAG,"Seek to:"+seekBar.getProgress()+",max:"+seekBar.getMax()+"duration:"+mMediaPlayer.getDuration()/1000);
                    mMediaPlayer.seekTo(progress * 1000);
                    mCurrentPosition = progress;
                }

            }
        });

        // Prepare video playback
        mSurfaceView.getHolder().addCallback(
                new SurfaceHolder.Callback() {
                    @Override
                    public void surfaceCreated(SurfaceHolder holder) {
                        openVideo(holder.getSurface());
                    }

                    @Override
                    public void surfaceChanged(
                            SurfaceHolder holder, int format, int width, int height) {
                        // Do nothing
                    }

                    @Override
                    public void surfaceDestroyed(SurfaceHolder holder) {
                        if (mMediaPlayer != null) {
                            mCurrentPosition = mMediaPlayer.getCurrentPosition()/1000;
                            Log.d(TAG, "surfaceDestroyed mSavedCurrentPosition = " + mCurrentPosition);
                        }
                        closeVideo();
                    }
                });

        mHandler.sendEmptyMessageDelayed(UIHandler.MESSAGE_HIDE_CONTROLS, TIME_CONTROLS_SHOW);
    }

    public void setZoomIco(@DrawableRes int resId) {
        mIvZoom.setImageResource(resId);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if(changed && mVideoSize != null) {
            Log.d(TAG,"onLayout");
            adjustVideoSize();
        }
        super.onLayout(changed, l, t, r, b);
    }

    private void resetControlsLayout() {
        mTvCurrentTime.setText("00:00");
        if (mDuration > 0) {
            mTvDuration.setText(Utils.second2TimeStr(mDuration));
        }
    }

    public void setVideoListener(VideoListener listener) {
        videoListener = listener;
    }

    public void setVideoUrl(String url) {
        mVideoUrl = url;
    }

    public void setDuration(int duration) {
        mDuration = duration;
        mTvDuration.setText(Utils.second2TimeStr(mDuration));
        if (mDuration > 0) {
            mSeekBar.setMax(mDuration);
        }
    }

    public int getDuration() {
        return mDuration;
    }

    public void setCurrPosition(int position) {
        mCurrentPosition = position;
        if(position > 0 && mTvCurrentTime != null) {
            mSeekBar.setProgress(mCurrentPosition);
            mTvCurrentTime.setText(Utils.second2TimeStr(mCurrentPosition));
        }
    }

    public int getCurrentPosition() {
        if (mMediaPlayer == null) {
            return 0;
        }
        return mMediaPlayer.getCurrentPosition()/1000;
    }

    public boolean isPlaying() {
        return mMediaPlayer != null && mMediaPlayer.isPlaying();
    }

    public void play() {
        if (mMediaPlayer == null) {
            return;
        }
        mMediaPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);
        mMediaPlayer.start();
        adjustToggleState();
        setKeepScreenOn(true);
        if (videoListener != null) {
            videoListener.onVideoStarted();
        }
        startPlayTimer();
    }

    public void pause() {
        if (mMediaPlayer == null) {
            adjustToggleState();
            return;
        }
        mMediaPlayer.setOnBufferingUpdateListener(null);
        mMediaPlayer.pause();
        adjustToggleState();
        setKeepScreenOn(false);
        if (videoListener != null) {
            videoListener.onVideoStopped();
        }
        stopPlayTimer();
    }

    void toggle() {
        if (mMediaPlayer == null) {
            return;
        }
        if (mMediaPlayer.isPlaying()) {
            pause();
        } else {
            play();
        }
    }

    void toggleControls() {
        if (mCtrlLayout.getVisibility() == View.VISIBLE) {
            hideControls();
        } else {
            showControls();
        }
    }

    void adjustToggleState() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mIvToggle.setImageResource(R.drawable.ic_video_pause);
        } else {
            mIvToggle.setImageResource(R.drawable.ic_video_play);
        }
    }

    void closeVideo() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    void openVideo(Surface surface) {
        if (TextUtils.isEmpty(mVideoUrl)) {
            return;
        }
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setSurface(surface);
        startVideo();
    }

    /**
     * Restarts playback of the video.
     */
    public void startVideo() {
        showLoadingView(getResources().getString(R.string.video_loading), false);
        mMediaPlayer.reset();
        try {
            mMediaPlayer.setDataSource(mVideoUrl);
            mMediaPlayer.setOnPreparedListener(mOnPreparedListener);
            mMediaPlayer.setOnCompletionListener(
                    new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mediaPlayer) {
                            adjustToggleState();
                            setKeepScreenOn(false);
                            if (videoListener != null) {
                                videoListener.onVideoCompleted();
                            }
                        }
                    });
            mMediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                @Override
                public void onSeekComplete(MediaPlayer mp) {
                    play();
                }
            });
            mMediaPlayer.setOnVideoSizeChangedListener(mVideoSizeChangedListener);
            mMediaPlayer.prepareAsync();
        } catch (IOException e) {
            Log.e(TAG, "Failed to open video", e);
        }
    }

    /**
     * Shows all the controls.
     */
    public void showControls() {

        mHandler.removeMessages(UIHandler.MESSAGE_HIDE_CONTROLS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            TransitionManager.beginDelayedTransition(this);
        }
        if(mCtrlLayout.getVisibility() != View.VISIBLE) {
            mCtrlLayout.setVisibility(View.VISIBLE);
        }
        if(mTitleLayout.getVisibility() != View.VISIBLE) {
            mTitleLayout.setVisibility(View.VISIBLE);
        }
        mHandler.sendEmptyMessageDelayed(UIHandler.MESSAGE_HIDE_CONTROLS, TIME_CONTROLS_SHOW);
    }

    /**
     * Hides all the controls.
     */
    @SuppressLint("NewApi")
    public void hideControls() {
        TransitionManager.beginDelayedTransition(this);
        mCtrlLayout.setVisibility(View.INVISIBLE);
        mTitleLayout.setVisibility(View.INVISIBLE);

    }

    private void showLoadingView(String info, boolean showOptBtn) {
        if (mLoadingView != null) {
            mLoadingView.setVisibility(View.VISIBLE);
            TextView tvContent = (TextView) mLoadingView.findViewById(R.id.txt_content_loading);
            tvContent.setText(info);
            TextView tvExit = (TextView) mLoadingView.findViewById(R.id.txt_ok_loading);
            LinearLayout llytBtn = (LinearLayout) mLoadingView.findViewById(R.id.llyt_btn_loading);
            View progressbar = mLoadingView.findViewById(R.id.progressbar);
            llytBtn.setVisibility(View.GONE);
            if (showOptBtn) {
                progressbar.setVisibility(View.GONE);
            } else {
                progressbar.setVisibility(View.VISIBLE);
            }
            RelativeLayout.LayoutParams lp = (LayoutParams) mLoadingView.getLayoutParams();
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                //横屏
                lp.width = getHeight() / 2;
            } else {
                lp.width = getWidth() / 2;
            }
            lp.height = (int) (lp.width * 0.8);
            tvContent.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            tvExit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);

        }
    }

    private void hideLoadingView() {
        if (mLoadingView != null && mLoadingView.getVisibility() == View.VISIBLE) {
            mLoadingView.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mHandler != null) {
            mHandler.removeMessages(UIHandler.MESSAGE_HIDE_CONTROLS);
            mHandler = null;
        }
        mOnPreparedListener = null;
        mVideoSizeChangedListener = null;
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        super.onDetachedFromWindow();
    }

    Timer mTimer;//timer for update play progress

    class PlayTimerTask extends TimerTask {
        @Override
        public void run() {
            if (mMediaPlayer != null) {
                int currPos = mMediaPlayer.getCurrentPosition();//此处为mm
                int progress = currPos/1000;
                if(progress > mDuration) {
                    progress = mDuration;
                    mTimer.cancel();
                }
                Message msg = Message.obtain();
                msg.what = UIHandler.MESSAGE_UPDATE_PROGRESS;
                msg.arg1 = progress;
                Log.d(TAG,"PlayTimerTask currPos:"+msg.arg1);
                mHandler.sendMessage(msg);
            }
        }
    }

    ;

    /**
     * @param position 秒
     */
    private void updatePlayPosition(int position) {
        mSeekBar.setProgress(position);
        mTvCurrentTime.setText(Utils.second2TimeStr(position));
    }

    /**
     * @param duration 秒
     */
    private void updateDuration(int duration) {
        mSeekBar.setMax(duration);
        mTvDuration.setText(Utils.second2TimeStr(duration));
    }

    private void startPlayTimer() {
        if(mTimer != null) {
            mTimer.cancel();
            mTimer.purge();
        }
        mTimer = new Timer();
        mTimer.schedule(new PlayTimerTask(), 0, 1000);
    }

    private void stopPlayTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    MediaPlayer.OnPreparedListener mOnPreparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mediaPlayer) {
            int videoWidth = mediaPlayer.getVideoWidth();
            int videoHeight = mediaPlayer.getVideoHeight();
            if(videoWidth > 0 && videoHeight > 0) {
                mVideoSize = new Point(videoWidth,videoHeight);
            }
            int duration = mediaPlayer.getDuration();
            if (duration > 0 && mHandler != null) {
                mDuration = duration / 1000;
                Message msg = Message.obtain();
                msg.what = UIHandler.MESSAGE_UPDATE_DURATION;
                msg.arg1 = mDuration;
                mHandler.sendMessage(msg);
            }
            Log.d(TAG, "onPrepared 视频时长：" + duration+",videoWidth:"+videoWidth+",videoHeight:"+videoHeight);
            hideLoadingView();
            // Adjust the aspect ratio of this view
            requestLayout();
            if (mCurrentPosition > 0) {
                mediaPlayer.seekTo(mCurrentPosition*1000);
                mCurrentPosition = 0;
            } else {
                // Start automatically
                play();
            }
        }
    };

    /**
     * 调整播放窗口大小
     */
    private void adjustVideoSize() {
        if(mVideoSize == null)
            return;
        int videoWidth = mVideoSize.x;
        int videoHeight = mVideoSize.y;
        if (videoWidth != 0 && videoHeight != 0) {
            final float aspectRatio = (float) videoHeight / videoWidth;
            int width = getWidth();
            int height = getHeight();
            Log.d(TAG, "onVideoSizeChanged width:" + width + ",height:" + height + ",videoWidth:" + videoWidth + ",videoHeight:" + videoHeight);
            final float viewRatio = (float) height / width;
            RelativeLayout.LayoutParams lp = (LayoutParams) mSurfaceView.getLayoutParams();
            if (aspectRatio > viewRatio) {
                int padding = (int) ((width - height / aspectRatio) / 2);
                lp.setMargins(padding, 0, padding, 0);
            } else {
                int padding = (int) ((height - width * aspectRatio) / 2);
                lp.setMargins(0, padding, 0, padding);
            }
        }
    }

    MediaPlayer.OnBufferingUpdateListener mBufferingUpdateListener = new MediaPlayer.OnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(MediaPlayer mp, int percent) {
            if (mp.isPlaying() || percent == 100) {
                hideLoadingView();
            } else {
                Log.d(TAG, "onBufferingUpdate 缓冲中：" + percent);
                showLoadingView(getResources().getString(R.string.video_loading), false);
            }
        }
    };
    MediaPlayer.OnVideoSizeChangedListener mVideoSizeChangedListener = new MediaPlayer.OnVideoSizeChangedListener() {
        @Override
        public void onVideoSizeChanged(MediaPlayer mp, int videoWidth, int videoHeight) {
            adjustVideoSize();
        }
    };

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.iv_play_pause) {
            toggle();
        } else if (id == R.id.iv_full_mode) {
            if (videoListener != null) {
                videoListener.onZoomPressed();
            }
        } else if (id == R.id.custom_video_surface) {
            toggleControls();
        } else if (id == R.id.img_video_view_back) {
            if (videoListener != null) {
                videoListener.onBackPressed();
            }
        }
    }

    public abstract static class VideoListener {

        /**
         * Called when the video is started or resumed.
         */
        public void onVideoStarted() {
        }

        /**
         * Called when the video is paused or finished.
         */
        public void onVideoStopped() {
        }

        /**
         * Called when video is playing to the end
         */
        public void onVideoCompleted() {
        }

        /**
         * full screen mode
         */
        public void onZoomPressed() {
        }

        public void onBackPressed() {
        }
    }

    private static class UIHandler extends Handler {

        static final int MESSAGE_HIDE_CONTROLS = 0x01;
        static final int MESSAGE_UPDATE_PROGRESS = 0x02;
        static final int MESSAGE_UPDATE_DURATION = 0x03;

        private final WeakReference<CustomVideoView> mVideoViewRef;

        UIHandler(CustomVideoView view) {
            mVideoViewRef = new WeakReference<>(view);
        }

        @Override
        public void handleMessage(Message msg) {
            CustomVideoView videoView = mVideoViewRef.get();
            if (videoView == null) {
                return;
            }
            switch (msg.what) {
                case MESSAGE_HIDE_CONTROLS:
                    videoView.hideControls();
                    break;
                case MESSAGE_UPDATE_PROGRESS: {
                    videoView.updatePlayPosition(msg.arg1);
                }
                break;
                case MESSAGE_UPDATE_DURATION: {
                    videoView.updateDuration(msg.arg1);
                }
                break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}
