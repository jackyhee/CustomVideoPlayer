package com.jackyhee.customvideoplayer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.jackyhee.customvideoplayer.widget.CustomVideoView;

public class MainActivity extends AppCompatActivity {
    private int mCurrPlayPos;//当前视频已播放时长
    private int mVideoDuration;//当前视频总时长
    private ConstraintLayout.LayoutParams mInitVideoViewLp;
    private CustomVideoView mVideoView;
    private static final String TAG = "MainActivity";

    private String testUrl = "https://vd4.bdstatic.com/mda-ken86ieq0wyvaf5k/mda-ken86ieq0wyvaf5k.mp4";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        mVideoView = (CustomVideoView) findViewById(R.id.custom_videoview);
        mInitVideoViewLp = (ConstraintLayout.LayoutParams) mVideoView.getLayoutParams();
        //set duration first
        if(mVideoDuration != 0) {
            mVideoView.setDuration(mVideoDuration);
        }
        //and then set curr
        if(mCurrPlayPos != 0) {
            mVideoView.setCurrPosition(mCurrPlayPos);
        }
        mVideoView.setVideoUrl(testUrl);
        mVideoView.setVideoListener(new CustomVideoView.VideoListener() {
            @Override
            public void onVideoStarted() {
                super.onVideoStarted();
            }

            @Override
            public void onVideoStopped() {
                super.onVideoStopped();
            }

            @Override
            public void onVideoCompleted() {
            }

            @Override
            public void onZoomPressed() {
                if (!isLand()) {
                    //to full screen mode
                    showFullScreenMode();
                } else {
                    // to normal mode
                    showPortraitMode();
                }
            }

            @Override
            public void onBackPressed() {
                if (!isLand()) {
                    //stop and finish
                    finish();
                } else {
                    // to normal mode
                    showPortraitMode();
                }
            }
        });
    }

    /**
     * 是否横屏
     *
     * @return
     */
    private boolean isLand() {
        return getResources().getConfiguration()
                .orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    private void showFullScreenMode() {
        //记录当前播放的片段
        //记录当前播放时间
        mCurrPlayPos = mVideoView.getCurrentPosition();
        mVideoDuration = mVideoView.getDuration();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    private void showPortraitMode() {
        //记录当前播放的片段
        //记录当前播放时间
        mCurrPlayPos = mVideoView.getCurrentPosition();
        mVideoDuration = mVideoView.getDuration();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            //设置横屏的UI
            Log.d(TAG, "onConfigurationChanged ORIENTATION_LANDSCAPE");
            //set full screen mode
            int uiOptions = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                    View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            getWindow().getDecorView().setSystemUiVisibility(uiOptions);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) mVideoView.getLayoutParams();
            lp.width = LinearLayout.LayoutParams.MATCH_PARENT;
            lp.height = LinearLayout.LayoutParams.MATCH_PARENT;
            mVideoView.setZoomIco(R.drawable.ic_video_zoom_out);
        } else {
            //设置竖屏的UI
            Log.d(TAG, "onConfigurationChanged ORIENTATION_PORTRAIT");
            //exit from full screen
            getWindow().getDecorView().setSystemUiVisibility(0);
            WindowManager.LayoutParams attrs = getWindow().getAttributes();
            attrs.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().setAttributes(attrs);

            mInitVideoViewLp.width = LinearLayout.LayoutParams.MATCH_PARENT;
            mInitVideoViewLp.height = 0;
            mVideoView.setLayoutParams(mInitVideoViewLp);
            mVideoView.setZoomIco(R.drawable.ic_video_zoom_in);
        }
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        if (!isLand()) {
            //stop and finish
            finish();
        } else {
            // to normal mode
            showPortraitMode();
        }
    }
}