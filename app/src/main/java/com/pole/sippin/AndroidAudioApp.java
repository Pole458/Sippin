package com.pole.sippin;

import android.media.AudioFormat;
import android.net.rtp.AudioCodec;
import android.net.rtp.AudioGroup;
import android.net.rtp.AudioStream;
import android.net.rtp.RtpStream;
import android.util.Log;
import local.media.FlowSpec;
import local.media.MediaApp;
import local.media.MediaSpec;
import org.zoolu.net.SocketAddress;
import org.zoolu.net.UdpSocket;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class AndroidAudioApp implements MediaApp {

    private static final String TAG = "Sip:AndroidAudioApp" ;

    private AudioStream audioStream;
    private AudioGroup audioGroup;

    /** Whether using symmetric RTP by default */
    private static final boolean SYMMETRIC_RTP = false;

    /** Codec */
    private static final String DEFAULT_CODEC = "PCMU";

    /** Whether using system audio capture */
    boolean audio_input = false;
    /** Whether using system audio playout */
    boolean audio_output = false;

    /** Whether using symmetric_rtp */
    private boolean symmetric_rtp = SYMMETRIC_RTP;

    /** Creates a new AndroidAudioApp */
    public AndroidAudioApp(FlowSpec flow_spec, boolean do_sync, int red_rate, boolean symmetric_rtp, AudioStream audioStream) {

        this.audioStream = audioStream;

        MediaSpec audio_spec = flow_spec.getMediaSpec();

        Log.v(TAG, "Received audio spec: " + audio_spec.toString());

        this.symmetric_rtp = symmetric_rtp;

        String codec = audio_spec.getCodec();

        int remote_port = flow_spec.getRemotePort();

        String remote_addr = flow_spec.getRemoteAddress();

        // 1) in case not defined, use default values
        if (codec == null) codec = DEFAULT_CODEC;

        // 2) codec name translation
        codec = codec.toUpperCase();

        switch (codec) {
            case "PCMU":
                codec = "ULAW";
                break;
            case "PCMA":
                codec = "ALAW";
                break;
            case "G711-ulaw":
                codec = "G711_ULAW";
                break;
            case "G711-alaw":
                codec = "G711_ALAW";
                break;
            case "G726-24":
            case "ADPCM24":
                codec = "G726_24";
                break;
            case "G726-32":
            case "ADPCM32":
                codec = "G726_32";
                break;
            case "G726-40":
            case "ADPCM40":
                codec = "G726_40";
                break;
            case "GSM":
                codec = "GSM0610";
                break;
        }

        AudioCodec audioCodec = AudioCodec.PCMU;
        switch (codec) {
            case "ULAW":
            case "G711_ULAW":
                audioCodec = AudioCodec.PCMU;
                break;
            case "ALAW":
            case "G711_ALAW":
                audioCodec = AudioCodec.PCMA;
                break;
            case "G726_24":
            case "G726_32":
            case "G726_40":
            case "GSM0610":
                break;
        }

        this.audioStream.setCodec(audioCodec);
        this.audioStream.setMode(flow_spec.getDirection());

        audioGroup = new AudioGroup();
        audioGroup.setMode(AudioGroup.MODE_ECHO_SUPPRESSION);

        try {
            this.audioStream.associate(InetAddress.getByName(remote_addr), remote_port);
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
