package com.pole.sippin;

import android.media.AudioTrack;

public abstract class AndroidReceiver extends Thread {

    protected boolean running;

    AudioTrack audio_track;

    protected AndroidReceiverListener listener;

    AndroidReceiver(AudioTrack audio_track, AndroidReceiverListener listener) {
        this.audio_track = audio_track;
        this.listener = listener;
    }

    /** Stops running */
    public void halt() {
        running = false;
    }

    void setRED(int red_rate) {
    }
}
