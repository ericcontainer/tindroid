package co.tinode.tindroid;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import co.tinode.tindroid.media.VxCard;
import co.tinode.tinodesdk.ComTopic;
import co.tinode.tinodesdk.Tinode;
import co.tinode.tinodesdk.model.MsgServerInfo;

public class CallActivity extends AppCompatActivity  {
    private static final String TAG = "CallActivity";

    static final String FRAGMENT_ACTIVE = "active_call";
    static final String FRAGMENT_INCOMING = "incoming_call";

    public static final String INTENT_ACTION_CALL_INCOMING = "tindroidx.intent.action.call.INCOMING";
    public static final String INTENT_ACTION_CALL_START = "tindroidx.intent.action.call.START";
    public static final String INTENT_ACTION_CALL_CLOSE = "tindroidx.intent.action.call.CLOSE";

    private boolean mTurnScreenOffWhenDone;

    private Tinode mTinode;
    private InfoEventListener mListener;

    private String mTopicName;
    private int mSeq;
    private ComTopic<VxCard> mTopic;

    // Receives 'close' requests from FCM (e.g. upon remote hang-up).
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (INTENT_ACTION_CALL_CLOSE.equals(intent.getAction())) {
                String topicName = intent.getStringExtra("topic");
                int seq = intent.getIntExtra("seq", -1);
                if (mTopicName.equals(topicName) && mSeq == seq) {
                    finish();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        final String action = intent != null ? intent.getAction() : null;
        if (action == null) {
            Log.w(TAG, "No intent or no valid action, unable to proceed");
            finish();
            return;
        }

        // Using action once.
        intent.setAction(null);

        Bundle args = new Bundle();
        String fragmentToShow;
        switch (action) {
            case INTENT_ACTION_CALL_INCOMING:
                // Incoming call started by the ser
                fragmentToShow = FRAGMENT_INCOMING;
                args.putString("call_direction", "incoming");
                break;

            case INTENT_ACTION_CALL_START:
                // Call started by the current user.
                args.putString("call_direction", "outgoing");
                fragmentToShow = FRAGMENT_ACTIVE;
                break;

            default:
                Log.e(TAG, "Unknown call action '" + action + "'");
                finish();
                return;
        }

        mTopicName = intent.getStringExtra("topic");
        // Technically the call is from intent.getStringExtra("from")
        // but it's the same as "topic" for p2p topics;
        mSeq = intent.getIntExtra("seq", -1);

        setContentView(R.layout.activity_call);

        mTinode = Cache.getTinode();
        mListener = new InfoEventListener();
        mTinode.addListener(mListener);

        //noinspection unchecked
        mTopic = (ComTopic<VxCard>) mTinode.getTopic(mTopicName);

        // Handle external request to finish call.
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(INTENT_ACTION_CALL_CLOSE);
        lbm.registerReceiver(mBroadcastReceiver, mIntentFilter);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        boolean isScreenOff = !pm.isInteractive();

        mTurnScreenOffWhenDone = isScreenOff;
        if (isScreenOff) {
            // Turn screen on and unlock.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true);
                setTurnScreenOn(true);
            } else {
                getWindow().addFlags(
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
                );
            }

            KeyguardManager mgr = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mgr.requestDismissKeyguard(this, null);
            }
        }

        showFragment(fragmentToShow, args);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);

        mTinode.removeListener(mListener);

        if (mTurnScreenOffWhenDone) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(false);
                setTurnScreenOn(false);
            } else {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
            }
        }
        super.onDestroy();
    }

    void acceptCall() {
        Bundle args = new Bundle();
        args.putString("call_direction", "incoming");
        showFragment(FRAGMENT_ACTIVE, args);
    }

    void declineCall() {
        // Send message to server that the call is declined.
        if (mTopic != null) {
            mTopic.videoCall("hang-up", mSeq, null);
        }
        finish();
    }

    void finishCall() {
        finish();
    }

    void showFragment(String tag, Bundle args) {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        FragmentManager fm = getSupportFragmentManager();

        Fragment fragment = fm.findFragmentByTag(tag);
        if (fragment == null) {
            switch (tag) {
                case FRAGMENT_INCOMING:
                    fragment = new IncomingCallFragment();
                    break;
                case FRAGMENT_ACTIVE:
                    fragment = new CallFragment();
                    break;
                default:
                    throw new IllegalArgumentException("Failed to create fragment: unknown tag " + tag);
            }
        } else if (args == null) {
            // Retain old arguments.
            args = fragment.getArguments();
        }

        args = args != null ? args : new Bundle();
        args.putString("topic", mTopicName);
        args.putInt("call_seq", mSeq);

        if (fragment.getArguments() != null) {
            fragment.getArguments().putAll(args);
        } else {
            fragment.setArguments(args);
        }

        FragmentTransaction trx = fm.beginTransaction();
        if (!fragment.isAdded()) {
            trx = trx.replace(R.id.contentFragment, fragment, tag)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        } else if (!fragment.isVisible()) {
            trx = trx.show(fragment);
        }

        if (!trx.isEmpty()) {
            trx.commit();
        }
    }

    private class InfoEventListener extends Tinode.EventListener {
        @Override
        public void onInfoMessage(MsgServerInfo info) {
            if (mTopicName.equals(info.topic) && mSeq == info.seq) {
                if ("call".equals(info.what) && "hang-up".equals(info.event)) {
                    Log.d(TAG, "Remote hangup: " + info.topic + ":" + info.seq);
                    finish();
                }
            }
        }
    }
}