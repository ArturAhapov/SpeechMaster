package com.arturagapov.speechmaster;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.SoundPool;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;

public class StartActivity extends Activity {
    //Подключаем звуки
    private SoundPool mSoundPool;
    private int mSoundId = 1;
    private int mStreamId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        //Tracking by Facebook
        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(this);

        //Устанавливаем размеры элементов экрана
        setScreenSize();
        setText();

        //Подключаем звуки
        /*
        try {
            mSoundPool = new SoundPool(4, AudioManager.STREAM_MUSIC, 100);
            mSoundPool.load(this, R.raw.chpok, 1);
            playStart();
        } catch (Exception e) {
            e.printStackTrace();
        }
        */
    }

    private void setText(){
        TextView appNameText = (TextView) findViewById(R.id.app_name);
        //Меняем шрифт
        Typeface font = Typeface.createFromAsset(getAssets(), "9844.otf");
        appNameText.setTypeface(font);
    }
    private void setScreenSize(){
        Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        Point p = new Point();
        display.getSize(p);
        int screenWidth = p.x;
        int screenHeight = p.y;
        int imageSize = (int)(screenWidth/2);

        ImageView logo =(ImageView)findViewById(R.id.logo);
        ViewGroup.LayoutParams logoParams = logo.getLayoutParams();
        logoParams.height = imageSize;
        logoParams.width = imageSize;
    }
    /*private void playStart(){
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        float curVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        float maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        float leftVolume = curVolume / maxVolume;
        float rightVolume = curVolume / maxVolume;
        int priority = 1;
        int no_loop = 0;
        float normal_playback_rate = 1f;
        mStreamId = mSoundPool.play(mSoundId, leftVolume, rightVolume, priority, no_loop,
                normal_playback_rate);
    }*/

    public void onClickGetStarted(View view) {
        Intent intent = new Intent(this, SpeechMasterActivity.class);
        startActivity(intent);
    }
}
