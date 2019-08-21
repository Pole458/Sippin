package com.pole.sippin;

import android.media.AudioTrack;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class AndroidUdpReceiver extends AndroidReceiver {

    private static final String TAG = "Sip:AndrUdpReceiver";

    private int local_port;

    private int BUF_SIZE;

    AndroidUdpReceiver(AudioTrack audio_track, int local_port, int buf_size, AndroidReceiverListener list) {
        super(audio_track, list);
        this.local_port = local_port;
        this.BUF_SIZE = buf_size;
    }


    public void run() {
        // Create an instance of AudioTrack, used for playing back audio

        Log.v(TAG, "UdpReceiver started");

        running = true;
        audio_track.play();
        try {
            // Define a socket to receive the audio
            DatagramSocket socket = new DatagramSocket(local_port);
            byte[] buf = new byte[BUF_SIZE];
            while (running) {
                // Play back the audio received from packets
                DatagramPacket packet = new DatagramPacket(buf, BUF_SIZE);
                socket.receive(packet);
                audio_track.write(packet.getData(), 0, BUF_SIZE);
            }
            // Stop playing back and release resources
            socket.disconnect();
            socket.close();
            audio_track.stop();
            audio_track.flush();
            audio_track.release();
            running = false;
        } catch (Exception e) {
            running = false;
            Log.e(TAG, e.getMessage(), e);
        }

        Log.v(TAG, "UdpReceiver stopped");
    }

}
