package co.tinode.tindroid;

import android.Manifest;
import android.app.Activity;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.LinkedList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import co.tinode.tindroid.media.VxCard;
import co.tinode.tinodesdk.ComTopic;

/**
 * Incoming call view with accept/decline buttons.
 */
public class IncomingCallFragment extends Fragment
        implements MotionLayout.TransitionListener {
    private static final String TAG = "IncomingCallActivity";

    // Default call timeout in seconds.
    private static final long DEFAULT_CALL_TIMEOUT = 30;

    // Check if we have camera and mic permissions.
    private final ActivityResultLauncher<String[]> mMediaPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                for (Map.Entry<String, Boolean> e : result.entrySet()) {
                    if (!e.getValue()) {
                        declineCall();
                        return;
                    }
                }
            });

    private PreviewView mLocalCameraView;
    private MediaPlayer mMediaPlayer;
    private ProcessCameraProvider mCamera;
    private Timer mTimer;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View v = inflater.inflate(R.layout.fragment_incoming_call, container, false);
        ((MotionLayout) v.findViewById(R.id.incomingCallMainLayout)).setTransitionListener(this);

        mLocalCameraView = v.findViewById(R.id.cameraPreviewView);

        long timeout = Cache.getTinode().getServerLimit("callTimeout", DEFAULT_CALL_TIMEOUT) * 1_000;
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                declineCall();
            }
        }, timeout + 5_000);

        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstance) {
        final Activity activity = getActivity();
        final Bundle args = getArguments();
        if (args == null || activity == null) {
            Log.w(TAG, "Call fragment created with no arguments");
            // Reject the call.
            declineCall();
            return;
        }

        String topicName = args.getString("topic");

        // Technically the call is from args.getString("from")
        // but it's the same as "topic" for p2p topics;
        //noinspection unchecked
        ComTopic<VxCard> topic = (ComTopic<VxCard>) Cache.getTinode().getTopic(topicName);

        VxCard pub = topic.getPub();
        UiUtils.setAvatar(view.findViewById(R.id.imageAvatar), pub, topicName, false);
        String peerName = pub != null ? pub.fn : null;
        if (TextUtils.isEmpty(peerName)) {
            peerName = getResources().getString(R.string.unknown);
        }
        ((TextView) view.findViewById(R.id.peerName)).setText(peerName);

        // Check permissions.
        LinkedList<String> missing = UiUtils.getMissingPermissions(activity,
                new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO});
        if (!missing.isEmpty()) {
            mMediaPermissionLauncher.launch(missing.toArray(new String[]{}));
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        final Activity activity = getActivity();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            // We are done. Just quit.
            return;
        }

        mMediaPlayer = MediaPlayer.create(activity, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE));
        mMediaPlayer.start();
        startCamera(activity);
    }

    @Override
    public void onPause() {
        if (mCamera != null) {
            mCamera.unbindAll();
        }
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {
        mTimer.cancel();
        super.onDestroy();
    }

    @Override
    public void onTransitionStarted(MotionLayout motionLayout, int startId, int endId) {
        // Do nothing.
    }

    @Override
    public void onTransitionChange(MotionLayout motionLayout, int startId, int endId, float progress) {
        // Do nothing.
    }

    @Override
    public void onTransitionCompleted(MotionLayout motionLayout, int currentId) {
        if (currentId == R.id.answerActivated) {
            acceptCall();
        } else if (currentId == R.id.hangUpActivated) {
            declineCall();
        } else {
            Log.i(TAG, "Unknown transition (normal?)");
        }
    }

    @Override
    public void onTransitionTrigger(MotionLayout motionLayout, int triggerId, boolean positive, float progress) {
        // Do nothing.
    }

    private void startCamera(Activity activity) {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(activity);
        cameraProviderFuture.addListener(() -> {
            mLocalCameraView.setVisibility(View.VISIBLE);
            // Used to bind the lifecycle of cameras to the lifecycle owner
            try {
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(mLocalCameraView.getSurfaceProvider());

                mCamera = cameraProviderFuture.get();
                // Unbind use cases before rebinding.
                mCamera.unbindAll();
                // Bind use cases to front camera.
                mCamera.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, preview);
            } catch (ExecutionException | InterruptedException | IllegalStateException ex) {
                Log.e(TAG, "Failed to start camera", ex);
            }
        }, ContextCompat.getMainExecutor(activity));
    }

    private void declineCall() {
        final CallActivity activity = (CallActivity) getActivity();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            // We are done. Just quit.
            return;
        }
        activity.declineCall();
    }

    private void acceptCall() {
        final CallActivity activity = (CallActivity) getActivity();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            // We are done. Just quit.
            return;
        }
        activity.acceptCall();
    }
}