package com.pole.sippin;

import android.media.AudioRecord;
import org.zoolu.net.SocketAddress;

public abstract class AndroidSender extends Thread {

    protected AudioRecord audio_record;

    protected String dest_addr;

    protected int dest_port;

    protected boolean running;

    public AndroidSender(AudioRecord audio_record, String dest_addr, int dest_port) {
        this.audio_record = audio_record;
        this.dest_addr = dest_addr;
        this.dest_port = dest_port;
    }

    public void halt() {
        running = false;
    }

    void setRemoteSoAddress(SocketAddress remote_soaddr) {
    }
}
