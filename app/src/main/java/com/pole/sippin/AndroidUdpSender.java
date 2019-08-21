package com.pole.sippin;

import android.media.AudioRecord;
import android.util.Log;
import org.zoolu.net.SocketAddress;

import java.io.IOException;
import java.net.*;

public class AndroidUdpSender extends AndroidSender {

    private static final String TAG = "Sip:AndUdpSender";

    private AudioRecord audio_record;

    private int BUF_SIZE;

    private int local_port;

    private boolean running;

    public AndroidUdpSender(AudioRecord audio_record, int buff_size, String remote_adrr, int remote_port, int local_port) {
        super(audio_record, remote_adrr, remote_port);
        this.audio_record = audio_record;
        BUF_SIZE = buff_size;
        this.local_port = local_port;
    }


    @Override
    public void run() {

        Log.v(TAG, "UdpSender started");

        running = true;
        int bytes_read;
        byte[] buf = new byte[BUF_SIZE];
        try {
            final InetAddress serverAddress = InetAddress.getByName(dest_addr);

            // Create a socket and start recording
            DatagramSocket socket = new DatagramSocket();
            audio_record.startRecording();
            while(running) {
                // Capture audio from the mic and transmit it
                bytes_read = audio_record.read(buf, 0, BUF_SIZE);
                DatagramPacket packet = new DatagramPacket(buf, bytes_read, serverAddress, dest_port);
                socket.send(packet);
                Thread.sleep(20, 0);
            }
            // Stop recording and release resources
            audio_record.stop();
            audio_record.release();
            socket.disconnect();
            socket.close();
            running = false;
        } catch(Exception e) {
            Log.e(TAG, e.getMessage(), e);
            running = false;
        }

        Log.v(TAG, "UdpSender stopped");
    }


    public void halt() {
        running = false;
    }

}
