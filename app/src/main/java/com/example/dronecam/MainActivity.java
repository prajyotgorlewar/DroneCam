package com.example.dronecam;
import android.app.PictureInPictureParams;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Rational;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import android.net.Uri;
import androidx.appcompat.app.AppCompatActivity;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.VLCVideoLayout;
import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private VLCVideoLayout videoLayout;
    private LibVLC libVLC;
    private MediaPlayer mediaPlayer;
    private boolean isRecording = false;
    private String currentRecordingPath = null;
    private EditText rtspUrlEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        videoLayout = findViewById(R.id.video_layout);
        rtspUrlEditText = findViewById(R.id.rtsp_url);
        ImageButton playButton = findViewById(R.id.btn_play);
        ImageButton recordButton = findViewById(R.id.btn_record);
        ImageButton pipButton = findViewById(R.id.btn_pip);

        ArrayList<String> options = new ArrayList<>();
        options.add("--no-drop-late-frames");
        options.add("--no-skip-frames");
        options.add("--rtsp-tcp");
        options.add("--no-video-title-show");
        options.add("--audio-time-stretch");

        libVLC = new LibVLC(this, options);
        mediaPlayer = new MediaPlayer(libVLC);
        mediaPlayer.attachViews(videoLayout, null, false, false);

        // Playing Stream Through URL
        playButton.setOnClickListener(v -> {
            String url = rtspUrlEditText.getText().toString().trim();
            if (url.isEmpty()) {
                Toast.makeText(this, "Please enter a valid RTSP URL", Toast.LENGTH_SHORT).show();
                return;
            }
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }

            Media media = new Media(libVLC, Uri.parse(url));
            media.setHWDecoderEnabled(true, false);

            media.addOption(":network-caching=150");
            media.addOption(":file-caching=1500");
            media.addOption(":live-caching=1500");
            media.addOption(":rtsp-frame-buffer-size=300000");
            media.addOption(":codec=avcodec");
            media.addOption(":no-spu");
            media.addOption(":no-sub-autodetect-file");

            mediaPlayer.setMedia(media);
            mediaPlayer.setSpuTrack(-1);
            mediaPlayer.play();
        });

        //Record and save Video Functionality
        recordButton.setOnClickListener(v -> {
            String url = rtspUrlEditText.getText().toString().trim();
            if (!isRecording) {
                if (url.isEmpty()) {
                    Toast.makeText(this, "Please enter a valid RTSP URL", Toast.LENGTH_SHORT).show();
                    return;
                }
                File file = new File(getExternalFilesDir(null), "recorded_" + System.currentTimeMillis() + ".mp4");
                currentRecordingPath = file.getAbsolutePath();
                Media media = new Media(libVLC, Uri.parse(url));
                media.setHWDecoderEnabled(true, false);
                media.addOption(":sout=#duplicate{dst=display,dst=file{dst=" + currentRecordingPath + "}}");
                media.addOption(":sout-keep");
                media.addOption(":no-sout-all");
                media.addOption(":network-caching=150");
                mediaPlayer.setMedia(media);
                mediaPlayer.play();
                isRecording = true;
                recordButton.setImageResource(R.drawable.ic_stop);
                Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show();
            }
            else {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                    Toast.makeText(this, "Recording saved to: " + currentRecordingPath, Toast.LENGTH_LONG).show();

                    // Restart streaming without recording
                    Media media = new Media(libVLC, Uri.parse(url));
                    media.setHWDecoderEnabled(true, false);

                    media.addOption(":network-caching=150");
                    media.addOption(":file-caching=1500");
                    media.addOption(":live-caching=1500");
                    media.addOption(":rtsp-frame-buffer-size=300000");
                    media.addOption(":codec=avcodec");
                    media.addOption(":no-spu");
                    media.addOption(":no-sub-autodetect-file");

                    mediaPlayer.setMedia(media);
                    mediaPlayer.setSpuTrack(-1);
                    mediaPlayer.play();
                }

                isRecording = false;
                currentRecordingPath = null;
                recordButton.setImageResource(R.drawable.ic_record);
            }
        });


        // Picture in Picture View Functionality
        pipButton.setOnClickListener(v -> {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                // Set the aspect ratio (e.g. 16:9)
                Rational aspectRatio = new Rational(videoLayout.getWidth(), videoLayout.getHeight());
                PictureInPictureParams params = new PictureInPictureParams.Builder()
                        .setAspectRatio(aspectRatio)
                        .build();

                enterPictureInPictureMode(params);
            } else {
                Toast.makeText(this, "PiP requires Android 8.0 or higher", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        int visibility = isInPictureInPictureMode ? View.GONE : View.VISIBLE;
        rtspUrlEditText.setVisibility(visibility);
        findViewById(R.id.btn_play).setVisibility(visibility);
        findViewById(R.id.btn_record).setVisibility(visibility);
        findViewById(R.id.btn_pip).setVisibility(visibility);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.detachViews();
            mediaPlayer.release();
        }
        if (libVLC != null) {
            libVLC.release();
        }
    }
}
