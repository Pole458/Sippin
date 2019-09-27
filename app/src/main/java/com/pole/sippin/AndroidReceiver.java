package com.pole.sippin;

import android.media.AudioTrack;

public abstract class AndroidReceiver extends Thread {

    protected boolean running;

    AudioTrack audio_track;

    AndroidReceiver(AudioTrack audio_track) {
        this.audio_track = audio_track;
    }

    /** Stops running */
    public void halt() {
        running = false;
    }

    void setRED(int red_rate) {
    }
}