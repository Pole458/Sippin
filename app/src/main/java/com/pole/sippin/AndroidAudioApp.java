package com.pole.sippin;

import android.media.*;
import android.os.Build;
import android.util.Log;
import local.media.FlowSpec;
import local.media.MediaApp;
import local.media.MediaSpec;

import org.zoolu.net.SocketAddress;
import org.zoolu.net.UdpSocket;

//import mg.dida.javax.sound.share.classes.javax.sound.sampled.AudioFormat;
//import mg.dida.javax.sound.share.classes.javax.sound.sampled.AudioInputStream;
//import mg.dida.javax.sound.share.classes.javax.sound.sampled.AudioSystem;
//import org.zoolu.sound.AudioOutputStream;
//import org.zoolu.sound.ExtendedAudioSystem;
//import local.media.RtpStreamReceiver;

public class AndroidAudioApp implements MediaApp, AndroidRtpStreamReceiverListener {

    private static final String TAG = "Sip:AndroidAudioApp" ;
    
    /** Whether using symmetric RTP by default */
    private static final boolean SYMMETRIC_RTP = false;

    /** Codec */
    private static final String DEFAULT_CODEC = "ULAW";
    /** Payload type */
    private static final int DEFAULT_PAYLOAD_TYPE = 0;
    /** Sample rate [samples/sec] */
    private static final int DEFAULT_SAMPLE_RATE = 8000;
    /** Codec frame size [bytes] */
    private static final int DEFAULT_FRAME_SIZE = 1;
    /** Codec frame rate [frames/sec] */
    private static final int DEFAULT_FRAME_RATE = 8000;
    /** Packet size [bytes] */
    private static final int DEFAULT_PACKET_SIZE = 160;
    /** whether using big-endian rule for byte ordering */
    private static final boolean DEFAULT_BIG_ENDIAN = false;

    /** Test tone */
    public static final String TONE = "TONE";
    /** Test tone frequency [Hz] */
    private static int TONE_FREQ = 100;
    /** Test tone ampliture (from 0.0 to 1.0) */
    private static double TONE_AMP = 1.0;
    /** Test tone sample size [bits] */
    private static int TONE_SAMPLE_SIZE = 8;

    /** Audio format */
    private AudioFormat audio_format_in;
    private AudioFormat audio_format_out;

    /** Stream direction */
    FlowSpec.Direction dir;

    /** UDP socket */
    UdpSocket socket = null;

    /** RtpStreamSender */
    private AndroidRtpStreamSender sender = null;
    /** RtpStreamReceiver */
    private AndroidRtpStreamReceiver receiver = null;

    /** Whether using system audio capture */
    boolean audio_input = false;
    /** Whether using system audio playout */
    boolean audio_output = false;

    /** Whether using symmetric_rtp */
    boolean symmetric_rtp = SYMMETRIC_RTP;

    /** Creates a new AndroidAudioApp */
    public AndroidAudioApp(FlowSpec flow_spec, boolean do_sync, int red_rate, boolean symmetric_rtp) {

        MediaSpec audio_spec = flow_spec.getMediaSpec();

        Log.v(TAG, "Received audio spec: " + audio_spec.toString());

        this.dir = flow_spec.getDirection();
        this.symmetric_rtp = symmetric_rtp;

        String codec = audio_spec.getCodec();
        int payload_type = audio_spec.getAVP();
        int sample_rate = audio_spec.getSampleRate();
        int packet_size = audio_spec.getPacketSize();

        int local_port = flow_spec.getLocalPort();
        int remote_port = flow_spec.getRemotePort();

        String remote_addr = flow_spec.getRemoteAddress();

        // 1) in case not defined, use default values
        if (codec == null) codec = DEFAULT_CODEC;
        if (payload_type < 0) payload_type = DEFAULT_PAYLOAD_TYPE;
        if (sample_rate <= 0) sample_rate = DEFAULT_SAMPLE_RATE;
        if (packet_size <= 0) packet_size = DEFAULT_PACKET_SIZE;

        int encoding = AudioFormat.ENCODING_DEFAULT;

        // 2) codec name translation
        codec = codec.toUpperCase();
        String codec_orig = codec;

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

//        Log.v(TAG, "codec: " + codec_orig);
//        if (!codec.equals(codec_orig)) Log.v(TAG, "codec mapped to: " + codec);

        // 3) frame_size, frame_rate, packet_rate
        int frame_size = DEFAULT_FRAME_SIZE;
        int frame_rate = DEFAULT_FRAME_RATE;

        switch (codec) {
            case "ULAW":
            case "G711_ULAW":
                payload_type = 0;
                frame_size = 1;
                frame_rate = sample_rate;
                break;
            case "ALAW":
            case "G711_ALAW":
                payload_type = 8;
                frame_size = 1;
                frame_rate = sample_rate;
                break;
            case "G726_24":
                payload_type = 101;
                frame_size = 3;
                frame_rate = sample_rate / 8;
                break;
            case "G726_32":
                payload_type = 101;
                frame_size = 4;
                frame_rate = sample_rate / 8;
                break;
            case "G726_40":
                payload_type = 101;
                frame_size = 5;
                frame_rate = sample_rate / 8;
                break;
            case "GSM0610":
                payload_type = 3;
                frame_size = 33;
                frame_rate = sample_rate / 160; // = 50 frames/sec in case of sample rate = 8000 Hz
                break;
        }

        int packet_rate = frame_rate * frame_size / packet_size;


        // 4) find the proper supported AudioFormat
//        Log.v(TAG, "base audio format: " + ExtendedAudioSystem.getBaseAudioFormat().toString());
//        AudioFormat.Encoding encoding = null;
//        AudioFormat.Encoding[] supported_encodings = AudioSystem.getTargetEncodings(ExtendedAudioSystem.getBaseAudioFormat());
//        for (AudioFormat.Encoding supported_encoding : supported_encodings) {
//            if (supported_encoding.toString().equalsIgnoreCase(codec)) {
//                encoding = supported_encoding;
//                break;
//            }
//        }
//        if (encoding != null) {
//            // get the first available target format
//            AudioFormat[] available_formats = AudioSystem.getTargetFormats(encoding,ExtendedAudioSystem.getBaseAudioFormat());
//            audio_format_in = available_formats[0];
//            Log.v(TAG, "encoding audio format: "+audio_format_in);
//            //Log.v(TAG, "DEBUG: frame_size: "+audio_format_in.getFrameSize());
//            //Log.v(TAG, "DEBUG: frame_rate: "+audio_format_in.getFrameRate());
//            //Log.v(TAG, "DEBUG: big_endian: "+audio_format_in.isBigEndian());
//        }
//        else Log.v(TAG, "WARNING: codec '" + codec + "' not natively supported");


        // Build AudioFormat
        if(Build.VERSION.SDK_INT >= 21) {
            audio_format_in = new AudioFormat.Builder().setSampleRate(sample_rate).setEncoding(AudioFormat.ENCODING_PCM_16BIT).
                    setChannelMask(AudioFormat.CHANNEL_IN_MONO).build();

            audio_format_out = new AudioFormat.Builder().setSampleRate(sample_rate).setEncoding(AudioFormat.ENCODING_PCM_16BIT).
                    setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build();

            Log.v(TAG, "" + audio_format_in.getEncoding());

        } else {
            //todo implements for api < 21
        }

        Log.v(TAG, "frame size: " + frame_size);
        Log.v(TAG, "frame rate: " + frame_rate);
        Log.v(TAG, "packet size: " + packet_size);
        Log.v(TAG, "packet rate: " + packet_rate);
        Log.v(TAG, "payload type: " + payload_type);

        try {
            // 5) udp socket
            socket = new UdpSocket(local_port);

            // 6) sender
            if (dir == FlowSpec.SEND_ONLY || dir == FlowSpec.FULL_DUPLEX) {

//                Log.v(TAG, "new audio sender to " + remote_addr + ":" + remote_port);

                AudioRecord audio_record;
                if(Build.VERSION.SDK_INT >= 23) {
                    audio_record = new AudioRecord.Builder()
                            .setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                            .setAudioFormat(audio_format_in)
                            .build();

                } else {
                    audio_record = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, sample_rate,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, AudioRecord.getMinBufferSize(
                            sample_rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT));
                }

                // sender
                sender = new AndroidRtpStreamSender(audio_record, do_sync, payload_type, packet_rate, packet_size, socket, remote_addr, remote_port);
                audio_input = true;
            }

            // 7) receiver
//            if (dir == FlowSpec.RECV_ONLY || dir == FlowSpec.FULL_DUPLEX) {
////                Log.v(TAG, "new audio receiver on " + local_port);
//
//                AudioTrack audio_track;
//                if(Build.VERSION.SDK_INT >= 23) {
//
//                    AudioAttributes audioAttributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
//                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build();
//
////                    int buffer_size = AudioTrack.getMinBufferSize(audio_format_out.getSampleRate(), audio_format_out.getChannelMask(), audio_format_out.getEncoding());
//
////                    Log.v(TAG, "receiver buffer size : " + buffer_size);
//
//                    audio_track = new AudioTrack.Builder()
//                            .setAudioAttributes(audioAttributes)
//                            .setAudioFormat(audio_format_out)
////                            .setBufferSizeInBytes(buffer_size)
//                            .setBufferSizeInBytes(8000 /* 1 second buffer */)
//                            .build();
//                } else {
//
//                    audio_track = new AudioTrack(AudioManager.STREAM_VOICE_CALL, sample_rate, AudioFormat.CHANNEL_OUT_MONO,
//                            AudioFormat.ENCODING_PCM_16BIT, 8000 /* 1 second buffer */,
//                            AudioTrack.MODE_STREAM);
//                }
//
//                // receiver
//                receiver = new AndroidRtpStreamReceiver(audio_track, socket,this);
//                receiver.setRED(red_rate);
//                audio_output = true;
//            }
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
        if (receiver!=null) {
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

    /** From RtpStreamReceiverListener. When the remote socket address (source) is changed. */
    public void onRemoteSoAddressChanged(AndroidRtpStreamReceiver rr, SocketAddress remote_soaddr) {
        if (symmetric_rtp && sender != null) sender.setRemoteSoAddress(remote_soaddr);
    }
}
