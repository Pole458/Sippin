package com.pole.sippin;

import android.net.rtp.AudioCodec;

import java.util.Arrays;
import java.util.Vector;

public class AndroidAudioCodec {

    public static final AndroidAudioCodec PCMU = new AndroidAudioCodec(AudioCodec.PCMU);
    public static final AndroidAudioCodec GSM = new AndroidAudioCodec(AudioCodec.GSM);
    public static final AndroidAudioCodec PCMA = new AndroidAudioCodec(AudioCodec.PCMA);
    public static final AndroidAudioCodec L16 = new AndroidAudioCodec(11, "L16/8000");
    public static final AndroidAudioCodec GSM_EFR = new AndroidAudioCodec(AudioCodec.GSM_EFR);
    public static final AndroidAudioCodec AMR = new AndroidAudioCodec(AudioCodec.AMR);
    public static final AndroidAudioCodec L8 = new AndroidAudioCodec(98, "L8/8000");

    public static final Vector<AndroidAudioCodec> supportedCodecs = new Vector<>(Arrays.asList(L8, L16, PCMU, PCMA, GSM, GSM_EFR, AMR));

    public int type;

    public String rtpmap;

    public AndroidAudioCodec(int type, String rtpmap) {
        this.type = type;
        this.rtpmap = rtpmap;
    }

    private AndroidAudioCodec(AudioCodec audioCodec) {
        this.type = audioCodec.type;
        this.rtpmap = audioCodec.rtpmap;
    }

    AudioCodec getAudioCodec() {
        return AudioCodec.getCodec(type, rtpmap, null);
    }
}
