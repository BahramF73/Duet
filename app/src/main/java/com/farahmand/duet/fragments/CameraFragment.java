package com.farahmand.duet.fragments;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.ExecuteCallback;
import com.downloader.Error;
import com.downloader.OnCancelListener;
import com.downloader.OnDownloadListener;
import com.downloader.OnPauseListener;
import com.downloader.OnProgressListener;
import com.downloader.OnStartOrResumeListener;
import com.downloader.PRDownloader;
import com.downloader.PRDownloaderConfig;
import com.downloader.Progress;
import com.downloader.Status;
import com.farahmand.duet.R;
import com.farahmand.duet.ShowActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;
import static com.farahmand.duet.Helper.toast;

import com.arthenica.mobileffmpeg.FFmpeg;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.VideoResult;
import com.otaliastudios.cameraview.controls.Audio;
import com.otaliastudios.cameraview.controls.Facing;
import com.otaliastudios.cameraview.controls.Flash;

public class CameraFragment extends Fragment implements View.OnClickListener {

    //SharedPreferences sharedPreferences;
    String targetFileName ="";
    String recordedFileName="";
    private static final String TAG = "CameraFragment";
    private static final String VIDEO_DIRECTORY_NAME = "DuetApp";
    private ImageView mRecordVideo,switchCamera,flash;
    private VideoView mPlayerViewRight;
    private TextView showProgress;
    private String mOutputFilePath;
    private boolean targetIsReady =false;
    long duration;
    private ProgressDialog mProgressDialog;
    public static final int DIALOG_DOWNLOAD_PROGRESS = 0;
    private ProgressBar percentageProgressBar;
    private ConstraintLayout progressBar,progressLayout;
    private FloatingActionButton next;
    private CameraView camera;
    private boolean isRecording =false;
    //private String targetVideoLink="http://persianeducation.org/mercedes-benz-ampamp-amg.mp4";
    private String targetVideoLink="http://d1cet4i8k165wj.cloudfront.net/1594223713_1394608067.mp4";
    private boolean hasFrontFlash=false;
    private boolean hasBackFlash=false;
    private String quality="720:1280";
    public CameraFragment() {
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_camera, container, false);
        camera =view.findViewById(R.id.cameraView);
        camera.setLifecycleOwner(getViewLifecycleOwner());
        camera.setPreviewFrameRateExact(true);
        camera.setPreviewFrameRate(25.0F);
        camera.setAudio(Audio.OFF);
        camera.addCameraListener(new CameraListener() {
            @Override
            public void onPictureTaken(@NonNull PictureResult result) {
                super.onPictureTaken(result);
            }

            @Override
            public void onVideoTaken(@NonNull VideoResult result) {
                super.onVideoTaken(result);
                getChangedRecordedRatioPath();
            }

            @Override
            public void onVideoRecordingStart() {
                super.onVideoRecordingStart();
            }

            @Override
            public void onVideoRecordingEnd() {
                super.onVideoRecordingEnd();
            }
        });

        progressBar=view.findViewById(R.id.progressBar);
        showProgress=view.findViewById(R.id.showPercent);
        progressLayout=view.findViewById(R.id.progressLayout);
        percentageProgressBar=view.findViewById(R.id.progressBarPercentage);
        mPlayerViewRight =view.findViewById(R.id.playerViewRight);
        next=view.findViewById(R.id.btnNext);
        next.setOnClickListener(this);
        mRecordVideo=view.findViewById(R.id.recordVideo);
        flash=view.findViewById(R.id.flash);
        switchCamera=view.findViewById(R.id.switchCamera);
        requestPermission();

        mRecordVideo.setOnClickListener(this);
        switchCamera.setOnClickListener(this);
        flash.setOnClickListener(this);
        mRecordVideo.setClickable(false);
        return view;
    }

    @Override
    public void onClick(View v) {
        if (targetIsReady){
            switch (v.getId()){
                case R.id.recordVideo:{
                    if (isRecording) {
                        stopRecordingVideo();
                    } else {
                        startRecordingVideo();
                    }
                }
                break;
                case R.id.btnNext:
                    Intent intent=new Intent(getActivity(), ShowActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra("Target",targetFileName);
                    intent.putExtra("Recorded",recordedFileName);
                    Objects.requireNonNull(getActivity()).startActivity(intent);
                    break;
            }
        }else {
            toast(getActivity(),"Target video is not ready");
        }
        if(!isRecording){
            if (v.getId() == R.id.switchCamera) {
                if (camera.getFacing() == Facing.BACK) {
                    camera.setFacing(Facing.FRONT);
                } else if (camera.getFacing() == Facing.FRONT) {
                    camera.setFacing(Facing.BACK);
                }
            }
        }
        if(v.getId()==R.id.flash){
            if(camera.getFlash()== Flash.OFF){
                camera.setFlash(Flash.TORCH);
            }else if(camera.getFlash()==Flash.TORCH){
                camera.setFlash(Flash.OFF);
            }
        }
    }

    private void requestPermission() {
        Dexter.withContext(getActivity()).withPermissions(Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        // check if all permissions are granted or not
                        if (report.areAllPermissionsGranted()) {
                            preparePlayerRight(targetVideoLink);
                        }
                        // check for permanent denial of any permission show alert dialog
                        if (report.isAnyPermissionPermanentlyDenied()) {
                            // open Settings activity
                            showSettingsDialog();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).withErrorListener(error -> Toast.makeText(Objects.requireNonNull(getActivity()).getApplicationContext(), "Error occurred! ", Toast.LENGTH_SHORT).show())
                .onSameThread()
                .check();
    }

    //Show dialog for go to settings
    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(Objects.requireNonNull(getActivity()));
        builder.setTitle(getString(R.string.message_need_permission));
        builder.setMessage(R.string.message_permission);
        builder.setPositiveButton(getString(R.string.title_go_to_setting), (dialog, which) -> {
            dialog.cancel();
            openSettings();
        });
        builder.show();
    }

    //Open settings for get permissions
    private void openSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", Objects.requireNonNull(getActivity()).getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, 101);
    }

    private void autoStop() {
        mPlayerViewRight.setOnCompletionListener(mp -> {
            try {
                stopRecordingVideo();
                mRecordVideo.setClickable(false);
                mRecordVideo.setImageResource(R.drawable.ic_record);
                mPlayerViewRight.pause();
                toast(getContext(),"Target"+duration+"");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void startRecordingVideo() {
        mRecordVideo.setImageResource(R.drawable.ic_stop);
        autoStop();
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory(),
                VIDEO_DIRECTORY_NAME);
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date());
        String recordingFileName=mediaStorageDir.getPath() + File.separator
                + "RECORDED_VID_" + timeStamp + ".mp4";
        recordedFileName=recordingFileName;
        File recordingFile=new File(recordingFileName);
        mPlayerViewRight.start();
        camera.takeVideo(recordingFile);
        isRecording=true;
    }
    private void stopRecordingVideo(){
        progressBar.setVisibility(View.VISIBLE);
        mPlayerViewRight.pause();
        mRecordVideo.setClickable(false);
        mRecordVideo.setImageResource(R.drawable.ic_record);
        camera.stopVideo();
    }

    private void preparePlayerRight(String url){

        new Thread(new Runnable() {
            @Override
            public void run() {
                File file=new File(targetFileName);
                if(targetFileName.equals("")||!file.exists()){
                    getTargetVideo(url);
                    Log.e(TAG,"going target download");
                }else{
                    Objects.requireNonNull(getActivity()).runOnUiThread(() -> {
                        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                        retriever.setDataSource(targetFileName);
                        duration = Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                        mPlayerViewRight.setVideoPath(targetFileName);
                        mPlayerViewRight.setOnPreparedListener(mp -> {
                            targetIsReady =true;
                            mRecordVideo.setClickable(true);
                            mPlayerViewRight.seekTo(0);
                            progressBar.setVisibility(View.GONE);
                        });
                    });
                }
            }
        }).start();
    }



    private void getChangedRecordedRatioPath() {
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory(),
                VIDEO_DIRECTORY_NAME);
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date());
        File originFile=new File(recordedFileName);
        String destFileName=mediaStorageDir.getPath() + File.separator
                + "RECORDED_VID_" + timeStamp + ".mp4";
        File destinationFile=new File(destFileName);
        FFmpeg.executeAsync("-i " + originFile + " -r 25 -b:v 8M -minrate 6M -maxrate 8M -bufsize 4M " + destinationFile, new ExecuteCallback() {
            @Override
            public void apply(long executionId, int returnCode) {
                if (returnCode == RETURN_CODE_SUCCESS) {
                    if(originFile.delete()){
                        Log.e(TAG,"Old recorded deleted");
                        recordedFileName=destFileName;
                        next.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                    }
                    toast(getActivity(),"Convert finished");
                    Log.i(Config.TAG, "Change recorded Command execution completed successfully.");
                } else if (returnCode == RETURN_CODE_CANCEL) {
                    Log.i(Config.TAG, "Command execution cancelled by user.");
                } else {
                    Log.i(Config.TAG, String.format("Command execution failed with rc=%d and the output below.", returnCode));
                    Config.printLastCommandOutput(Log.INFO);
                }
            }
        });
    }


    //get target video and convert it to acceptable aspect ratio
    private void getTargetVideo(String fileURL) {
        progressLayout.setVisibility(View.VISIBLE);
        percentageProgressBar.setProgress(0);
        PRDownloaderConfig config = PRDownloaderConfig.newBuilder()
                .setReadTimeout(30_000)
                .setConnectTimeout(30_000)
                .build();
        PRDownloader.initialize(getContext(), config);

        File mediaStorageDir = new File(Environment.getExternalStorageDirectory(),
                VIDEO_DIRECTORY_NAME);

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date());
        Log.e(TAG,"downloaded target video 2");
        //Target video path address
        String fileName="TARGET_VID_" + timeStamp + ".mp4";
        String fileDirectory=mediaStorageDir.getPath() + File.separator
                + "TARGET_VID_" + timeStamp + ".mp4";
        //FileOutputStream f = new FileOutputStream(fileName);

        int downloadId = PRDownloader.download(fileURL,mediaStorageDir.getPath(),fileName)
                .build()
                .setOnStartOrResumeListener(new OnStartOrResumeListener() {
                    @Override
                    public void onStartOrResume() {

                    }
                })
                .setOnPauseListener(new OnPauseListener() {
                    @Override
                    public void onPause() {

                    }
                })
                .setOnCancelListener(new OnCancelListener() {
                    @Override
                    public void onCancel() {

                    }
                })
                .setOnProgressListener(new OnProgressListener() {
                    @Override
                    public void onProgress(Progress progress) {
                        float c = ((float)progress.currentBytes/progress.totalBytes)*100;
                        percentageProgressBar.setProgress(Math.round(c));
                        showProgress.setText(("Loading "+Math.round(c)+"%"));
                    }
                })
                .start(new OnDownloadListener() {
                    @Override
                    public void onDownloadComplete() {
                        progressLayout.setVisibility(View.GONE);
                        changeTargetRatio(fileDirectory);
                    }

                    @Override
                    public void onError(Error error) {

                    }

                });

        Status status=PRDownloader.getStatus(downloadId);
        /*Log.e(TAG,"get target video");
        try {
            File mediaStorageDir = new File(Environment.getExternalStorageDirectory(),
                    VIDEO_DIRECTORY_NAME);
            URL url = new URL(getFinalLocation(fileURL));
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setRequestMethod("GET");
            c.setDoOutput(true);
            Log.e(TAG,"downloaded target video 1");
            c.connect();
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                    Locale.getDefault()).format(new Date());
            Log.e(TAG,"downloaded target video 2");
            //Target video path address
            String fileName=mediaStorageDir.getPath() + File.separator
                    + "TARGET_VID_" + timeStamp + ".mp4";
            FileOutputStream f = new FileOutputStream(fileName);
            Log.e(TAG,"downloaded target video 3");
            InputStream in = c.getInputStream();
            byte[] buffer = new byte[1024];
            int len1 = 0;
            while ((len1 = in.read(buffer)) > 0) {
                f.write(buffer, 0, len1);
            }
            f.close();
            Log.e(TAG,"downloaded target video 4");
            changeTargetRatio(fileName);
        } catch (IOException e) {
            Log.d("Error....", e.toString());
        }*/
    }

    private void changeTargetRatio(String fileName) {
        progressBar.setVisibility(View.VISIBLE);
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory(),
                VIDEO_DIRECTORY_NAME);
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date());
        String destFileName=mediaStorageDir.getPath() + File.separator
                + "TARGET_VID_" + timeStamp + ".mp4";
        File originFile=new File(fileName);
        File destinationFile=new File(destFileName);


        FFmpeg.executeAsync("-i " + originFile + " -vf scale="+quality+":force_original_aspect_ratio=decrease,pad="+quality+":(ow-iw)/2:(oh-ih)/2 -r 25 -b:v 8M -minrate 6M -maxrate 8M -bufsize 4M " + destinationFile, new ExecuteCallback() {
            @Override
            public void apply(long executionId, int returnCode) {
                if (returnCode == RETURN_CODE_SUCCESS) {
                    if(originFile.delete()){
                        Log.e(TAG,"Deleted");
                        toast(getContext(),"Deleted");
                        targetFileName=destFileName;
                        preparePlayerRight(targetVideoLink);
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
        Log.e(TAG,"change target ratio");
    }

    @Override
    public void onStop() {
        if (mPlayerViewRight.isPlaying())
            mPlayerViewRight.pause();
        super.onStop();
    }

    @Override
    public void onPause() {
        camera.close();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        camera.open();
    }

    @Override
    public void onDestroy() {
        camera.destroy();
        super.onDestroy();
    }
    public static String getFinalLocation(String address) throws IOException{
        URL url = new URL(address);
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        int status = conn.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK)
        {
            if (status == HttpURLConnection.HTTP_MOVED_TEMP
                    || status == HttpURLConnection.HTTP_MOVED_PERM
                    || status == HttpURLConnection.HTTP_SEE_OTHER)
            {
                String newLocation = conn.getHeaderField("Location");
                return getFinalLocation(newLocation);
            }
        }
        return address;
    }
}

