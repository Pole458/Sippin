package com.pole.sippin;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import local.ua.UserAgent;
import local.ua.UserAgentListener;
import local.ua.UserAgentProfile;
import org.zoolu.sip.address.NameAddress;
import org.zoolu.sip.provider.SipProvider;
import org.zoolu.sip.provider.SipStack;
////import org.zoolu.tools.ScheduledWork;

import java.util.Vector;

public class MainActivity extends AppCompatActivity /*implements UserAgentListener*/ {

    private static final String TAG = "Sip: MainActivity";

    // ********************** UserAgent logic **********************

    /** SipProvider. */
    private SipProvider sip_provider;
//
//    /** User Agent */
//    private UserAgent ua;
//
//    /** UserAgentProfile */
//    private UserAgentProfile ua_profile;

    // ********************** Activity logic **********************

    private TextView myNumberTextView;

    private EditText numberTextView;

    private ImageButton callButton;

    private ImageButton hangUpButton;

    private TextView statusTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myNumberTextView = findViewById(R.id.myNumberTextView);
        numberTextView = findViewById(R.id.numberEditText);
        callButton = findViewById(R.id.callButton);
        hangUpButton = findViewById(R.id.closeCallButton);
        statusTextView = findViewById(R.id.statusTextView);

//        callButton.setOnClickListener(v -> call(numberTextView.getText().toString()));
//        hangUpButton.setOnClickListener(v -> hangUp());
//
        sip_provider = new SipProvider(SipProvider.AUTO_CONFIGURATION, SipStack.default_port);
//        ua_profile = new UserAgentProfile();
//
//        ua = new UserAgent(sip_provider,ua_profile, this);
//        changeStatus(UA_IDLE);
//
//        // Set the re-invite
//        if (ua_profile.re_invite_time > 0)
//            reInvite(ua_profile.re_invite_time);
//
//        // Set the transfer (REFER)
//        if (ua_profile.transfer_to != null && ua_profile.transfer_time > 0)
//            callTransfer(ua_profile.transfer_to,ua_profile.transfer_time);
//
//        // Unregisters ALL contact URLs
//        if (ua_profile.do_unregister_all){
////            Log.v(TAG, "UNREGISTER ALL contact URLs");
//            ua.unregisterall();
//        }
//
//        // unregisters the contact URL
//        if (ua_profile.do_unregister){
////            Log.v(TAG, "UNREGISTER the contact URL");
//            ua.unregister();
//        }
//
//        // registers the contact URL with the registrar server
//        if (ua_profile.do_register) {
////            Log.v(TAG, "REGISTRATION");
//            ua.loopRegister(ua_profile.expires,ua_profile.expires / 2, ua_profile.keepalive_time);
//        }

//        if (!ua_profile.audio && !ua_profile.video)
//            Log.v(TAG, "ONLY SIGNALING, NO MEDIA");

    }

//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        hangUp();
//    }

//    /** When the call/accept button is pressed. */
//    private void call(String url) {
//
//        if (call_state == UA_IDLE) {
//            if (url != null && url.length() > 0) {
//                ua.hangup();
//                ua.call(url);
//                changeStatus(UA_OUTGOING_CALL);
//            }
//        } else if (call_state == UA_INCOMING_CALL) {
//            ua.accept();
//            changeStatus(UA_ON_CALL);
//        }
//    }

//    /** When the refuse/hangup button is pressed. */
//    private void hangUp() {
//        if (call_state != UA_IDLE) {
//            ua.hangup();
//            changeStatus(UA_IDLE);
//        }
//    }

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
        call_state = state;
        switch (state) {
            case UA_IDLE:
                statusTextView.setText("Idle");
                break;
            case UA_INCOMING_CALL:
                statusTextView.setText("Incoming Call");
                break;
            case UA_OUTGOING_CALL:
                statusTextView.setText("Outgoing Call");
                break;
            case UA_ON_CALL:
                statusTextView.setText("On Call");
                break;
        }
    }

    /** Gets the call state */
    protected int getStatus() {
        return call_state;
    }

    // ********************** MyUA callback functions **********************

//    /** When a new call is incoming */
//    public void onUaIncomingCall(UserAgent ua, NameAddress callee, NameAddress caller, Vector media_descs) {
//        changeStatus(UA_INCOMING_CALL);
//
////        if (ua_profile.redirect_to != null) {// redirect the call
////            //  display.setText("CALL redirected to "+ua_profile.redirect_to);
////            ua.redirect(ua_profile.redirect_to);
////        }
////        else
////        if (ua_profile.accept_time>=0) // automatically accept the call
////        {  display.setText("ON CALL");
////            jComboBox1.setSelectedItem(null);
////            comboBoxEditor1.setItem(caller.toString());
////            //accept();
////            automaticAccept(ua_profile.accept_time);
////        }
////        else
////        {  display.setText("INCOMING CALL");
////            jComboBox1.setSelectedItem(null);
////            comboBoxEditor1.setItem(caller.toString());
////        }
//    }
//
//    /** When an outgoing call is stated to be in progress */
//    public void onUaCallProgress(UserAgent ua) {
//        statusTextView.setText("PROGRESS");
//    }
//
//
//    /** When an outgoing call is remotly ringing */
//    public void onUaCallRinging(UserAgent ua) {
//        statusTextView.setText("RINGING");
//    }
//
//
//    /** When an outgoing call has been accepted */
//    public void onUaCallAccepted(UserAgent ua) {
//        changeStatus(UA_ON_CALL);
////        if (ua_profile.hangup_time>0) automaticHangup(ua_profile.hangup_time);
//    }
//
//    /** When an incoming call has been cancelled */
//    public void onUaCallCancelled(UserAgent ua) {
//        changeStatus(UA_IDLE);
//    }
//
//
//    /** When a call has been transferred */
//    public void onUaCallTransferred(UserAgent ua) {
//        changeStatus(UA_IDLE);
//    }
//
//    /** When an outgoing call has been refused or timeout */
//    public void onUaCallFailed(UserAgent ua, String reason) {
//        changeStatus(UA_IDLE);
//    }
//
//    /** When a call has been locally or remotely closed */
//    public void onUaCallClosed(UserAgent ua) {
//        changeStatus(UA_IDLE);
//    }
//
//    /** When a new media session is started. */
//    public void onUaMediaSessionStarted(UserAgent ua, String type, String codec)
//    {  //printLog(type+" started "+codec);
//    }
//
//    /** When a media session is stopped. */
//    public void onUaMediaSessionStopped(UserAgent ua, String type)
//    {  //printLog(type+" stopped");
//    }
//
//    /** When registration succeeded. */
//    public void onUaRegistrationSucceeded(UserAgent ua, String result) {
//        myNumberTextView.setText(ua_profile.getUserURI().toString());
////        Log.v(TAG, "REGISTRATION SUCCESS: "+result);
//    }
//
//    /** When registration failed. */
//    public void onUaRegistrationFailed(UserAgent ua, String result) {
////        this.setTitle(sip_provider.getContactAddress(ua_profile.user).toString());
////        Log.v(TAG, "REGISTRATION FAILURE: "+result);
//    }
//
//    // ************************ scheduled events ************************
//
//    /** Schedules a re-inviting after <i>delay_time</i> secs. It simply changes the contact address. */
//    private void reInvite(final int delay_time) {
////        Log.v(TAG, "AUTOMATIC RE-INVITING/MODIFYING: " + delay_time + " secs");
////        if (delay_time==0)
////            ua.modify(null);
////        else new ScheduledWork(delay_time*1000) {
////            public void doWork() {
////                ua.modify(null);
////            }
////        };
//    }
//
//    /** Schedules a call-transfer after <i>delay_time</i> secs. */
//    private void callTransfer(final NameAddress transfer_to, final int delay_time) {
////        Log.v(TAG, "AUTOMATIC REFER/TRANSFER: "+delay_time+" secs");
////        if (delay_time==0) ua.transfer(transfer_to);
////        else new ScheduledWork(delay_time*1000) {  public void doWork() {  ua.transfer(transfer_to);  }  };
//    }
//
//
////    /** Schedules a call-transfer after <i>delay_time</i> secs. */
////    private void callTransfer(final NameAddress transfer_to, final int delay_time) {
////        Log.v(TAG, "AUTOMATIC REFER/TRANSFER: "+delay_time+" secs");
////        if (delay_time==0) ua.transfer(transfer_to);
////        else new ScheduledWork(delay_time*1000) {  public void doWork() {  ua.transfer(transfer_to);  }  };
////    }
////
////
////    /** Schedules an automatic hangup after <i>delay_time</i> secs. */
////    private void automaticHangup(final int delay_time) {
////        Log.v(TAG, "AUTOMATIC HANGUP: "+delay_time+" secs");
////        if (delay_time == 0)
////            hangUp();
////        else
////            new ScheduledWork(delay_time*1000) {
////                public void doWork() {
////                    hangUp();
////                }
////            };
////    }
//
}
