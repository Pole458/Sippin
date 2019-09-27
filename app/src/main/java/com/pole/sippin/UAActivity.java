package com.pole.sippin;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.*;
import local.ua.UserAgent;
import local.ua.UserAgentListener;
import local.ua.UserAgentProfile;
import org.zoolu.sip.address.NameAddress;
import org.zoolu.sip.provider.SipProvider;

import java.util.Vector;


public class UAActivity extends AppCompatActivity implements UserAgentListener {

    private static final String TAG = "Sip: UAActivity";

//    private static final String addres = "<sip:alice@192.168.1.2:5070>";
//    private static final String addres = "<sip:alice@160.78.237.132:5070>";
    private static final String addres = "<sip:echo@mjsip.org>";

    // ********************** UserAgent logic **********************

    /** SipProvider. */
    private SipProvider sip_provider;

    /** User Agent */
    private UserAgent ua;

    /** UserAgentProfile */
    private UserAgentProfile ua_profile;

    // ********************** Activity logic **********************

    private TextView myNumberTextView;

    private EditText numberTextView;

    private ImageButton callButton;

    private ImageButton hangUpButton;

    private TextView statusTextView;
    
    private CheckBox l8CheckBox;
    private CheckBox l16CheckBox;
    private CheckBox pcmaCheckBox;
    private CheckBox amrCheckBox;
    private CheckBox gsmCheckBox;
    private CheckBox gsmEfrCheckBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myNumberTextView = findViewById(R.id.myNumberTextView);
        numberTextView = findViewById(R.id.numberEditText);
        callButton = findViewById(R.id.callButton);
        hangUpButton = findViewById(R.id.closeCallButton);
        statusTextView = findViewById(R.id.statusTextView);

        l8CheckBox = findViewById(R.id.l8CheckBox);
        l16CheckBox = findViewById(R.id.l16CheckBox);
        pcmaCheckBox = findViewById(R.id.pcmaCheckBox);
        amrCheckBox = findViewById(R.id.amrCheckBox);
        gsmCheckBox = findViewById(R.id.gsmCheckBox);
        gsmEfrCheckBox = findViewById(R.id.gsmEfrCheckBox);

        l8CheckBox.setOnCheckedChangeListener((v, b) -> ua.setShouldUseCodecs(AndroidAudioCodec.L8, b));
        l16CheckBox.setOnCheckedChangeListener((v, b) -> ua.setShouldUseCodecs(AndroidAudioCodec.L16, b));
        pcmaCheckBox.setOnCheckedChangeListener((v, b) -> ua.setShouldUseCodecs(AndroidAudioCodec.PCMA, b));
        amrCheckBox.setOnCheckedChangeListener((v, b) -> ua.setShouldUseCodecs(AndroidAudioCodec.AMR, b));
        gsmCheckBox.setOnCheckedChangeListener((v, b) -> ua.setShouldUseCodecs(AndroidAudioCodec.GSM, b));
        gsmEfrCheckBox.setOnCheckedChangeListener((v, b) -> ua.setShouldUseCodecs(AndroidAudioCodec.GSM_EFR, b));
        
        callButton.setOnClickListener(v -> call(numberTextView.getText().toString()));
        hangUpButton.setOnClickListener(v -> hangUp());

        sip_provider = new SipProvider(getApplicationContext());
        ua_profile = new UserAgentProfile(getApplicationContext());

        ua = new UserAgent(sip_provider, ua_profile,this);
        changeStatus(UA_IDLE);

        numberTextView.setText(addres);
        myNumberTextView.setText(ua_profile.getUserURI().toString());

        requestRecordAudioPermission();

    }
    
    private void setCheckBoxEnable(boolean b) {
        l8CheckBox.setEnabled(b);
        l16CheckBox.setEnabled(b);;
        pcmaCheckBox.setEnabled(b);
        amrCheckBox.setEnabled(b);
        gsmCheckBox.setEnabled(b);
        gsmEfrCheckBox.setEnabled(b);
    }

    @Override
    protected void onDestroy() {
        new Thread() {
            @Override
            public void run() {
                ua.hangup();
            }
        }.start();
        super.onDestroy();
    }

    private void requestRecordAudioPermission() {
        //check API version, do nothing if API version < 23!
        if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
            }
        }
    }

    /** When the call/accept button is pressed. */
    private void call(String url) {

        if (call_state == UA_IDLE) {
            if (url != null && url.length() > 0) {

                new Thread() {
                    @Override
                    public void run() {
                        try {
                            ua.hangup();
                            ua.call(url);
                        } catch (Exception e) {
                            Log.e(TAG, e.getMessage(), e);
                        }
                    }
                }.start();
                changeStatus(UA_OUTGOING_CALL);
            }
        } else if (call_state == UA_INCOMING_CALL) {
            new Thread() {
                @Override
                public void run() {
                    ua.accept();
                }
            }.start();
            changeStatus(UA_ON_CALL);
        }
    }

    /** When the refuse/hangup button is pressed. */
    private void hangUp() {
        if (call_state != UA_IDLE) {
            new Thread() {
                @Override
                public void run() {
                    ua.hangup();
                }
            }.start();
            changeStatus(UA_IDLE);
        }
    }

    // ************************* MyUA internal state *************************

    /** UA_IDLE=0 */
    private static final int UA_IDLE = 0;
    /** UA_INCOMING_CALL=1 */
    private static final int UA_INCOMING_CALL = 1;
    /** UA_OUTGOING_CALL=2 */
    private static final int UA_OUTGOING_CALL = 2;
    /** UA_ON_CALL=3 */
    private static final int UA_ON_CALL = 3;

    /** Call state: <P>UA_IDLE=0, <BR>UA_INCOMING_CALL=1, <BR>UA_OUTGOING_CALL=2, <BR>UA_ON_CALL=3 */
    private int call_state = UA_IDLE;

    /** Changes the call state */
    private void changeStatus(int state) {
        runOnUiThread(() ->    {
            call_state = state;
            switch (state) {
                case UA_IDLE:
                    statusTextView.setText("Idle");
                    setCheckBoxEnable(true);
                    break;
                case UA_INCOMING_CALL:
                    statusTextView.setText("Incoming Call");
                    setCheckBoxEnable(true);
                    break;
                case UA_OUTGOING_CALL:
                    statusTextView.setText("Outgoing Call");
                    setCheckBoxEnable(false);
                    break;
                case UA_ON_CALL:
                    statusTextView.setText("On Call");
                    setCheckBoxEnable(false);
                    break;
            }
        });
    }

    /** Gets the call state */
    protected int getStatus() {
        return call_state;
    }

    // ********************** MyUA callback functions **********************

    /** When a new call is incoming */
    public void onUaIncomingCall(UserAgent ua, NameAddress callee, NameAddress caller, Vector media_descs) {
        changeStatus(UA_INCOMING_CALL);

//        if (ua_profile.redirect_to != null) {// redirect the call
//            //  display.setText("CALL redirected to "+ua_profile.redirect_to);
//            ua.redirect(ua_profile.redirect_to);
//        }
//        else
//        if (ua_profile.accept_time>=0) // automatically accept the call
//        {  display.setText("ON CALL");
//            jComboBox1.setSelectedItem(null);
//            comboBoxEditor1.setItem(caller.toString());
//            //accept();
//            automaticAccept(ua_profile.accept_time);
//        }
//        else
//        {  display.setText("INCOMING CALL");
//            jComboBox1.setSelectedItem(null);
//            comboBoxEditor1.setItem(caller.toString());
//        }
    }

    /** When an outgoing call is stated to be in progress */
    public void onUaCallProgress(UserAgent ua) {
        statusTextView.setText("PROGRESS");
    }

    /** When an outgoing call is remotely ringing */
    public void onUaCallRinging(UserAgent ua) {
        runOnUiThread(() -> statusTextView.setText("RINGING"));
    }

    /** When an outgoing call has been accepted */
    public void onUaCallAccepted(UserAgent ua) {

        AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if(audio != null)
            audio.setMode(AudioManager.MODE_IN_COMMUNICATION);

        runOnUiThread(() -> changeStatus(UA_ON_CALL));
//        if (ua_profile.hangup_time>0) automaticHangup(ua_profile.hangup_time);
    }

    /** When an incoming call has been cancelled */
    public void onUaCallCancelled(UserAgent ua) {
        changeStatus(UA_IDLE);
    }

    /** When a call has been transferred */
    public void onUaCallTransferred(UserAgent ua) {
        changeStatus(UA_IDLE);
    }

    /** When an outgoing call has been refused or timeout */
    public void onUaCallFailed(UserAgent ua, String reason) {
        runOnUiThread(() -> changeStatus(UA_IDLE));
    }

    /** When a call has been locally or remotely closed */
    public void onUaCallClosed(UserAgent ua) {

        AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if(audio != null)
            audio.setMode(AudioManager.MODE_NORMAL);

        changeStatus(UA_IDLE);
    }

    /** When a new media session is started. */
    public void onUaMediaSessionStarted(UserAgent ua, String type, String codec)
    {  //printLog(type+" started "+codec);
    }

    /** When a media session is stopped. */
    public void onUaMediaSessionStopped(UserAgent ua, String type)
    {  //printLog(type+" stopped");
    }

    /** When registration succeeded. */
    public void onUaRegistrationSucceeded(UserAgent ua, String result) {
        myNumberTextView.setText(ua_profile.getUserURI().toString());
//        Log.v(TAG, "REGISTRATION SUCCESS: "+result);
    }

    /** When registration failed. */
    public void onUaRegistrationFailed(UserAgent ua, String result) {
//        this.setTitle(sip_provider.getContactAddress(ua_profile.user).toString());
//        Log.v(TAG, "REGISTRATION FAILURE: "+result);
    }

}
