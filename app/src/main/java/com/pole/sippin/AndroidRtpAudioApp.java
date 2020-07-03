package com.pole.sippin;

import android.media.*;
import android.net.rtp.RtpStream;
import android.os.Build;
import android.util.Log;
import local.media.FlowSpec;
import local.media.MediaApp;
import org.zoolu.net.UdpSocket;

public class AndroidRtpAudioApp implements MediaApp {

    private static final String TAG = "Sip:AndroidAudioApp";

    private AudioFormat audio_format_in;
    private AudioFormat audio_format_out;

    /** UDP socket */
    private UdpSocket socket = null;

    /** RtpStreamSender */
    private AndroidSender sender = null;
    /** RtpStreamReceiver */
    private AndroidReceiver receiver = null;

    /** Creates a new AndroidAudioApp */
    public AndroidRtpAudioApp(FlowSpec flow_spec) {

        AndroidAudioCodec audioCodec = flow_spec.getAudioCodec();

        Log.v(TAG, "Received audio codec: " + audioCodec.rtpmap);

        int local_port = flow_spec.getLocalPort();
        int remote_port = flow_spec.getRemotePort();

        String remote_addr = flow_spec.getRemoteAddress();

        int audioFormat;
        int sample_rate = 8000;
        int payload_type = audioCodec.type;
        int frame_size = 1;
        int frame_rate = 8000;
        int packet_size;
        int packet_rate = 50;

        if(audioCodec == AndroidAudioCodec.L8) {
            packet_size = 160;
            audioFormat = AudioFormat.ENCODING_PCM_8BIT;
        } else if(audioCodec == AndroidAudioCodec.L16) {
            packet_size = 320;
            audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        } else {
            Log.e(TAG, "Codec not supported!");
            return;
        }

        Log.v(TAG, "sample rate: " + sample_rate);
        Log.v(TAG, "type type: " + payload_type);
        Log.v(TAG, "frame size: " + frame_size);
        Log.v(TAG, "frame rate: " + frame_rate);
        Log.v(TAG, "packet size: " + packet_size);
        Log.v(TAG, "packet rate: " + packet_rate);

        // Build AudioFormat
        if(Build.VERSION.SDK_INT >= 23) {
            audio_format_in = new AudioFormat.Builder().setSampleRate(sample_rate).setEncoding(audioFormat).
                    setChannelMask(AudioFormat.CHANNEL_IN_MONO).build();

            audio_format_out = new AudioFormat.Builder().setSampleRate(sample_rate).setEncoding(audioFormat).
                    setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build();
        }

        try {
            // 5) udp socket
            socket = new UdpSocket(local_port);

            // 6) sender
            if (flow_spec.getDirection() == RtpStream.MODE_NORMAL || flow_spec.getDirection() == RtpStream.MODE_SEND_ONLY) {

                AudioRecord audio_record;
                if(Build.VERSION.SDK_INT >= 23) {
                    audio_record = new AudioRecord.Builder()
                            .setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                            .setAudioFormat(audio_format_in)
                            .setBufferSizeInBytes(AudioRecord.getMinBufferSize(sample_rate, AudioFormat.CHANNEL_IN_MONO, audioFormat))
                            .build();


                } else {
                    audio_record = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, sample_rate,
                    AudioFormat.CHANNEL_IN_MONO, audioFormat, AudioRecord.getMinBufferSize(
                            sample_rate, AudioFormat.CHANNEL_IN_MONO, audioFormat));
                }

                // sender
                sender = new AndroidRtpStreamSender(audio_record, payload_type, packet_rate, packet_size, socket, remote_addr, remote_port);
                Log.v(TAG, "audio_record state: " + audio_record.getState());
            }

            // 7) receiver
            if (flow_spec.getDirection() == RtpStream.MODE_NORMAL || flow_spec.getDirection() == RtpStream.MODE_RECEIVE_ONLY) {

                AudioTrack audio_track;
                if(Build.VERSION.SDK_INT >= 23) {

                    AudioAttributes audioAttributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build();

                    int buff_size = AudioTrack.getMinBufferSize(sample_rate, AudioFormat.CHANNEL_OUT_MONO, audioFormat);
                    Log.v(TAG, "audio track buff size: " + buff_size);

                    audio_track = new AudioTrack.Builder()
                            .setAudioAttributes(audioAttributes)
                            .setAudioFormat(audio_format_out)
                            .setBufferSizeInBytes(AudioTrack.getMinBufferSize(sample_rate, AudioFormat.CHANNEL_OUT_MONO, audioFormat))
                            .build();
                } else {

                    audio_track = new AudioTrack(AudioManager.STREAM_VOICE_CALL, sample_rate, AudioFormat.CHANNEL_OUT_MONO,
                            audioFormat, AudioTrack.getMinBufferSize(
                            sample_rate, AudioFormat.CHANNEL_OUT_MONO, audioFormat),
                            AudioTrack.MODE_STREAM);
                }

                // receiver
                receiver = new AndroidRtpStreamReceiver(audio_track, socket);
                Log.v(TAG, "audio track state: " + audio_track.getState());
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    @Override
    public boolean startApp() {
        Log.v(TAG, "starting Android audio");
        if (sender != null) {
            Log.v(TAG, "start sending");
            sender.start();
        }
        if (receiver != null) {
            Log.v(TAG, "start receiving");
            receiver.start();
        }
        return true;
    }

    @Override
    public boolean stopApp() {
        Log.v(TAG, "stopping Android audio");
        if (sender != null) {
            sender.halt();
            sender = null;
            Log.v(TAG, "sender halted");
        }

        if (receiver != null) {
            receiver.halt();
            receiver = null;
            Log.v(TAG, "receiver halted");
        }

        // try to take into account the resilience of RtpStreamSender
        try {
            Thread.sleep(AndroidRtpStreamReceiver.SO_TIMEOUT); } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        socket.close();
        return true;
    }
}
