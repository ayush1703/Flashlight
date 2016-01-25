package com.example.flashlight;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.ToggleButton;

import java.util.ArrayList;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.


        return super.onOptionsItemSelected(item);
    }

     public static class PlaceholderFragment extends Fragment {
        private Camera cam;
        private Camera.Parameters camParams;
        private boolean hasCam;
        private int freq;
        private StroboRunner sr;
        private Thread t;
        private boolean isChecked = false;
         public static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;
         public Button speakButton;
        public PlaceholderFragment() {
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            try {
                //Log.d("TORCH", "Check cam");
                // Get CAM reference
                cam = Camera.open();
                camParams = cam.getParameters();
                cam.startPreview();
                hasCam = true;
                //Log.d("TORCH", "HAS CAM ["+hasCam+"]");
            }
            catch(Throwable t) {
                t.printStackTrace();
            }

        }
         @TargetApi(Build.VERSION_CODES.HONEYCOMB)
         public void startVoiceRecognitionActivity() {
             Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
             intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                     RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
             intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                     "Speech recognition demo");
             startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
         }

         public void onActivityResult(int requestCode, int resultCode, Intent data) {
             super.onActivityResult(requestCode, resultCode, data);

             if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == RESULT_OK) {
                 // Fill the list view with the strings the recognizer thought it
                 // could have heard
                 ArrayList matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                 if (matches.contains("stop")) {
                     isChecked=false;
                     turnOnOff(isChecked);
                 }else{
                     isChecked=true;
                     turnOnOff(isChecked);

                 }
             }
         }


         @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            // Let's get the reference to the toggle
            final ImageView tBtn = (ImageView) rootView.findViewById(R.id.iconLight);
            tBtn.setImageResource(R.mipmap.off);
            speakButton = (Button) rootView.findViewById(R.id.btn_speak);
            speakButton.setOnClickListener(new View.OnClickListener(){

                @Override
                public void onClick(View v) {
                    startVoiceRecognitionActivity();
                }

            });
            tBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    isChecked = !isChecked;

                    if (isChecked)
                        tBtn.setImageResource(R.mipmap.on);
                    else
                        tBtn.setImageResource(R.mipmap.off);

                    turnOnOff(isChecked);
                }
            });


            // Seekbar
            SeekBar skBar = (SeekBar) rootView.findViewById(R.id.seekBar);
            skBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    freq = progress;
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });
            return rootView;
        }

        private void turnOnOff(boolean on) {

            if (on) {
                if (freq != 0) {
                    sr = new StroboRunner();
                    sr.freq = freq;
                    t = new Thread(sr);
                    t.start();
                    return ;
                }
                else
                    camParams.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            }
            if (!on) {
                if (t != null) {
                    sr.stopRunning = true;
                    t = null;
                    return ;
                }
                else
                    camParams.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            }

            cam.setParameters(camParams);
            cam.startPreview();
        }



        private class StroboRunner implements Runnable {

            int freq;
            boolean stopRunning = false;

            @Override
            public void run() {
                Camera.Parameters paramsOn = PlaceholderFragment.this.cam.getParameters();
                Camera.Parameters paramsOff = PlaceholderFragment.this.camParams;
                paramsOn.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                paramsOff.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                try {
                    while (!stopRunning) {
                        PlaceholderFragment.this.cam.setParameters(paramsOn);
                        PlaceholderFragment.this.cam.startPreview();
                        // We make the thread sleeping
                        Thread.sleep(100 - freq);
                        PlaceholderFragment.this.cam.setParameters(paramsOff);
                        PlaceholderFragment.this.cam.startPreview();
                        Thread.sleep(freq);
                    }
                }
                catch(Throwable t) {}
            }
        }


    }


}