package com.farahmand.duet;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.VideoView;

import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.ExecuteCallback;
import com.arthenica.mobileffmpeg.FFmpeg;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;
import static com.farahmand.duet.Helper.toast;

public class ShowActivity extends AppCompatActivity {
    VideoView right,left,match;
    String recorded="",target="";
    ConstraintLayout progressBar;
    File recordedFile,targetFile;
    private static final String TAG = "ShowActivity";

    private static final String VIDEO_DIRECTORY_NAME = "DuetApp";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show);
        progressBar=findViewById(R.id.progressBar);
        match=findViewById(R.id.videoViewMatch);
        target=getIntent().getStringExtra("Target");
        recorded=getIntent().getStringExtra("Recorded");
        match.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(match.isPlaying()){
                    match.pause();
                }else{
                    match.start();
                }
            }
        });
        //toast(this,"Recorded\n"+recorded+"\nTarget\n"+target);

        File mediaStorageDir = new File(Environment.getExternalStorageDirectory(),
                VIDEO_DIRECTORY_NAME);
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date());
        String duetName=mediaStorageDir.getPath() + File.separator
                + "DUET_VID_" + timeStamp + ".mp4";

        FFmpeg.executeAsync("-i " + recorded + " -i " + target + " -filter_complex [0:v][1:v]hstack=inputs=2:shortest=1[outv]  -r 25 -b:v 8M -minrate 6M -maxrate 8M -bufsize 4M  -map 1:a -shortest -map [outv] " + duetName, new ExecuteCallback() {
            @Override
            public void apply(long executionId, int returnCode) {
                if (returnCode == RETURN_CODE_SUCCESS) {
                    toast(ShowActivity.this,"Duet saved");
                    progressBar.setVisibility(View.GONE);
                    match.setVideoPath(duetName);
                    match.setOnPreparedListener(mp -> mp.setLooping(true));
                    match.start();
                    recordedFile=new File(recorded);
                    targetFile=new File(target);
                    if(recordedFile.delete()&&targetFile.delete()){
                        Log.e(TAG,"Files Deleted");
                    }
                    Log.i(Config.TAG, "Command execution completed successfully.");
                } else if (returnCode == RETURN_CODE_CANCEL) {
                    Log.i(Config.TAG, "Command execution cancelled by user.");
                } else {
                    Log.i(Config.TAG, String.format("Command execution failed with rc=%d and the output below.", returnCode));
                    Config.printLastCommandOutput(Log.INFO);
                }
            }
        });
    }
}