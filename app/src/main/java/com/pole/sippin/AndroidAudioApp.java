package com.pole.sippin;

import android.net.rtp.AudioCodec;
import android.net.rtp.AudioGroup;
import android.net.rtp.AudioStream;
import android.util.Log;
import local.media.FlowSpec;
import local.media.MediaApp;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class AndroidAudioApp implements MediaApp {

    private static final String TAG = "Sip:AndroidAudioApp" ;

    private AudioStream audioStream;

    private AudioGroup audioGroup;

    /** Creates a new AndroidAudioApp */
    public AndroidAudioApp(FlowSpec flow_spec, AudioStream audioStream) {

        this.audioStream = audioStream;

        int remotePort = flow_spec.getRemotePort();

        String remoteAddress = flow_spec.getRemoteAddress();

        AudioCodec audioCodec = flow_spec.getAudioCodec().getAudioCodec();
        Log.v(TAG, "Received audio spec: " + audioCodec.rtpmap);
        this.audioStream.setCodec(audioCodec);
        this.audioStream.setMode(flow_spec.getDirection());

        audioGroup = new AudioGroup();
        audioGroup.setMode(AudioGroup.MODE_ECHO_SUPPRESSION);

        try {
            this.audioStream.associate(InetAddress.getByName(remoteAddress), remotePort);
        } catch (UnknownHostException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }
    
    @Override
    public boolean startApp() {
        Log.v(TAG, "starting Android audio");
        audioStream.join(audioGroup);
        return true;
    }

    @Override
    public boolean stopApp() {
        Log.v(TAG, "stopping Android audio");
        audioStream.join(null);
        audioGroup.clear();
        return true;
    }
}
