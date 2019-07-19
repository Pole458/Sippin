package com.pole.sippin;

import android.util.Log;
import local.media.*;
import mg.dida.javax.sound.share.classes.javax.sound.sampled.AudioFormat;
import mg.dida.javax.sound.share.classes.javax.sound.sampled.AudioInputStream;
import mg.dida.javax.sound.share.classes.javax.sound.sampled.AudioSystem;
import org.zoolu.net.SocketAddress;
import org.zoolu.net.UdpSocket;
import org.zoolu.sound.AudioOutputStream;
import org.zoolu.sound.ExtendedAudioSystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class AndroidAudioApp implements MediaApp, RtpStreamReceiverListener {

    private static final String TAG = "Sip: AndroidAudioApp" ;
    
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
    public static final String TONE="TONE";
    /** Test tone frequency [Hz] */
    private static int TONE_FREQ = 100;
    /** Test tone ampliture (from 0.0 to 1.0) */
    private static double TONE_AMP = 1.0;
    /** Test tone sample size [bits] */
    private static int TONE_SAMPLE_SIZE = 8;

    /** Audio format */
    AudioFormat audio_format;

    /** Stream direction */
    FlowSpec.Direction dir;

    /** UDP socket */
    UdpSocket socket=null;

    /** RtpStreamSender */
    RtpStreamSender sender=null;
    /** RtpStreamReceiver */
    RtpStreamReceiver receiver=null;

    /** Whether using system audio capture */
    boolean audio_input=false;
    /** Whether using system audio playout */
    boolean audio_output=false;

    /** Whether using symmetric_rtp */
    boolean symmetric_rtp=SYMMETRIC_RTP;

    /** Creates a new AndroidAudioApp */
    public AndroidAudioApp(RtpStreamSender sender, RtpStreamReceiver receiver, boolean symmetric_rtp) {
        this.sender=sender;
        this.receiver=receiver;
        this.symmetric_rtp=symmetric_rtp;
        Log.v(TAG,"codec: [unknown]");
    }

    /** Creates a new AndroidAudioApp */
    public AndroidAudioApp(FlowSpec flow_spec, String audiofile_in, String audiofile_out, boolean direct_convertion, boolean do_sync, int red_rate, boolean symmetric_rtp) {
        MediaSpec audio_spec=flow_spec.getMediaSpec();

        this.dir=flow_spec.getDirection();
        this.symmetric_rtp=symmetric_rtp;

        String codec = audio_spec.getCodec();
        int payload_type = audio_spec.getAVP();
        int sample_rate = audio_spec.getSampleRate();
        int packet_size = audio_spec.getPacketSize();

        int local_port = flow_spec.getLocalPort();
        int remote_port = flow_spec.getRemotePort();

        String remote_addr = flow_spec.getRemoteAddress();

        // 1) in case not defined, use default values
        if (codec==null) codec=DEFAULT_CODEC;
        if (payload_type<0) payload_type=DEFAULT_PAYLOAD_TYPE;
        if (sample_rate<=0) sample_rate=DEFAULT_SAMPLE_RATE;
        if (packet_size<=0) packet_size=DEFAULT_PACKET_SIZE;

        // 2) codec name translation
        codec=codec.toUpperCase();
        String codec_orig=codec;

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
                codec = "G726_24";
                break;
            case "G726-32":
                codec = "G726_32";
                break;
            case "G726-40":
                codec = "G726_40";
                break;
            case "ADPCM24":
                codec = "G726_24";
                break;
            case "ADPCM32":
                codec = "G726_32";
                break;
            case "ADPCM40":
                codec = "G726_40";
                break;
            case "GSM":
                codec = "GSM0610";
                break;
        }

        Log.v(TAG, "codec: "+codec_orig);
        if (!codec.equals(codec_orig)) Log.v(TAG, "codec mapped to: "+codec);

        // 3) frame_size, frame_rate, packet_rate
        int frame_size=DEFAULT_FRAME_SIZE;
        int frame_rate=DEFAULT_FRAME_RATE;

        if (codec.equals("ULAW") || codec.equals("G711_ULAW"))
        {  payload_type=0;
            frame_size=1;
            frame_rate=sample_rate;
        }
        else
        if (codec.equals("ALAW") || codec.equals("G711_ALAW"))
        {  payload_type=8;
            frame_size=1;
            frame_rate=sample_rate;
        }
        else
        if (codec.equals("G726_24"))
        {  payload_type=101;
            frame_size=3;
            frame_rate=sample_rate/8;
        }
        else
        if (codec.equals("G726_32"))
        {  payload_type=101;
            frame_size=4;
            frame_rate=sample_rate/8;
        }
        else
        if (codec.equals("G726_40"))
        {  payload_type=101;
            frame_size=5;
            frame_rate=sample_rate/8;
        }
        else
        if (codec.equals("GSM0610"))
        {  payload_type=3;
            frame_size=33;
            frame_rate=sample_rate/160; // = 50 frames/sec in case of sample rate = 8000 Hz
        }

        int packet_rate=frame_rate*frame_size/packet_size;
        Log.v(TAG, "packet size: "+packet_size);
        Log.v(TAG, "packet rate: "+packet_rate);

        // 4) find the proper supported AudioFormat
        Log.v(TAG, "base audio format: "+ ExtendedAudioSystem.getBaseAudioFormat().toString());
        AudioFormat.Encoding encoding=null;
        AudioFormat.Encoding[] supported_encodings= AudioSystem.getTargetEncodings(ExtendedAudioSystem.getBaseAudioFormat());
        for (int i=0; i<supported_encodings.length ; i++)
        {  if (supported_encodings[i].toString().equalsIgnoreCase(codec))
        {  encoding=supported_encodings[i];
            break;
        }
        }
        if (encoding!=null)
        {  // get the first available target format
            AudioFormat[] available_formats=AudioSystem.getTargetFormats(encoding,ExtendedAudioSystem.getBaseAudioFormat());
            audio_format=available_formats[0];
            Log.v(TAG, "encoding audio format: "+audio_format);
            //Log.v(TAG, "DEBUG: frame_size: "+audio_format.getFrameSize());
            //Log.v(TAG, "DEBUG: frame_rate: "+audio_format.getFrameRate());
            //Log.v(TAG, "DEBUG: big_endian: "+audio_format.isBigEndian());
        }
        else Log.v(TAG, "WARNING: codec '"+codec+"' not natively supported");

        try
        {  // 5) udp socket
            socket=new UdpSocket(local_port);

            // 6) sender
            if ((dir==FlowSpec.SEND_ONLY || dir==FlowSpec.FULL_DUPLEX))
            {  Log.v(TAG, "new audio sender to "+remote_addr+":"+remote_port);
                if (audiofile_in!=null && audiofile_in.equals(AndroidAudioApp.TONE))
                {  // tone generator
                    Log.v(TAG, "Tone generator: "+TONE_FREQ+" Hz");
                    ToneInputStream tone=new ToneInputStream(TONE_FREQ,TONE_AMP, sample_rate, TONE_SAMPLE_SIZE, ToneInputStream.PCM_LINEAR_UNSIGNED,DEFAULT_BIG_ENDIAN);
                    // sender
                    sender=new RtpStreamSender(tone,true,payload_type,packet_rate,packet_size,socket,remote_addr,remote_port);
                }
                else
                if (audiofile_in!=null)
                {  // input file
                    File file=new File(audiofile_in);
                    if (audiofile_in.indexOf(".wav")==(audiofile_in.length()-4))
                    {  // known file format
                        Log.v(TAG, "File audio format: " + AudioSystem.getAudioFileFormat(file));
                        // get AudioInputStream
                        AudioInputStream audio_input_stream = AudioSystem.getAudioInputStream(file);
                        // apply audio conversion
                        if (audio_format!=null) audio_input_stream=AudioSystem.getAudioInputStream(audio_format,audio_input_stream);
                        // sender
                        sender=new RtpStreamSender(audio_input_stream,true,payload_type,packet_rate,packet_size,socket,remote_addr,remote_port);
                    }
                    else
                    {  // sender
                        sender=new RtpStreamSender(new FileInputStream(file),true,payload_type,packet_rate,packet_size,socket,remote_addr,remote_port);
                    }
                }
                else
                {  // javax sound
                    AudioInputStream audio_input_stream=null;
                    if (!direct_convertion || codec.equalsIgnoreCase("ULAW") || codec.equalsIgnoreCase("ALAW"))
                    {  // use embedded conversion provider
                        audio_input_stream=ExtendedAudioSystem.getInputStream(audio_format);
                    }
                    else
                    {  // use explicit conversion provider
                        Class audio_system=Class.forName("com.zoopera.sound.ConverterAudioSystem");
                        java.lang.reflect.Method get_input_stream=audio_system.getMethod("convertAudioInputStream",new Class[]{ String.class, float.class, AudioInputStream.class });
                        audio_input_stream=(AudioInputStream)get_input_stream.invoke(null,new Object[]{ codec, new Integer(sample_rate), ExtendedAudioSystem.getInputStream(ExtendedAudioSystem.getBaseAudioFormat()) });
                        Log.v(TAG, "send x-format: "+audio_input_stream.getFormat());
                    }
                    // sender
                    if (!do_sync) sender=new RtpStreamSender(audio_input_stream,false,payload_type,packet_rate,packet_size,socket,remote_addr,remote_port);
                    else sender=new RtpStreamSender(audio_input_stream,true,payload_type,packet_rate,packet_size,socket,remote_addr,remote_port);
                    //if (sync_adj>0) sender.setSyncAdj(sync_adj);
                    audio_input=true;
                }
            }

            // 7) receiver
            if (dir==FlowSpec.RECV_ONLY || dir==FlowSpec.FULL_DUPLEX)
            {  Log.v(TAG, "new audio receiver on "+local_port);
                if (audiofile_out!=null)
                {  // output file
                    File file=new File(audiofile_out);
                    FileOutputStream output_stream=new FileOutputStream(file);
                    // receiver
                    receiver=new RtpStreamReceiver(output_stream,socket);
                }
                else
                {  // javax sound
                    AudioOutputStream audio_output_stream=null;
                    if (!direct_convertion || codec.equalsIgnoreCase("ULAW") || codec.equalsIgnoreCase("ALAW"))
                    {  // use embedded conversion provider
                        audio_output_stream=ExtendedAudioSystem.getOutputStream(audio_format);
                    }
                    else
                    {  // use explicit conversion provider
                        Class audio_system=Class.forName("com.zoopera.sound.ConverterAudioSystem");
                        java.lang.reflect.Method get_output_stream=audio_system.getMethod("convertAudioOutputStream",new Class[]{ String.class, float.class, AudioOutputStream.class });
                        audio_output_stream=(AudioOutputStream)get_output_stream.invoke(null,new Object[]{ codec, new Integer(sample_rate), ExtendedAudioSystem.getOutputStream(ExtendedAudioSystem.getBaseAudioFormat()) });
                        Log.v(TAG, "recv x-format: "+audio_output_stream.getFormat());
                    }
                    // receiver
                    receiver=new RtpStreamReceiver(audio_output_stream,socket,this);
                    receiver.setRED(red_rate);
                    audio_output=true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "", e);
        }
    }
    
    @Override
    public boolean startApp() {
        Log.v(TAG, "starting java audio");
        if (sender!=null)
        {  Log.v(TAG, "start sending");
            if (audio_input) ExtendedAudioSystem.startAudioInputLine();
            sender.start();
        }
        if (receiver!=null)
        {  Log.v(TAG, "start receiving");
            if (audio_output) ExtendedAudioSystem.startAudioOutputLine();
            receiver.start();
        }
        return true;
    }

    @Override
    public boolean stopApp() {
        Log.v(TAG, "stopping java audio");
        if (sender!=null)
        {  sender.halt();
            sender=null;
            Log.v(TAG, "sender halted");
        }
        if (audio_input) ExtendedAudioSystem.stopAudioInputLine();

        if (receiver!=null)
        {  receiver.halt();
            receiver=null;
            Log.v(TAG, "receiver halted");
        }
        if (audio_output) ExtendedAudioSystem.stopAudioOutputLine();

        // try to take into account the resilience of RtpStreamSender
        try { Thread.sleep(RtpStreamReceiver.SO_TIMEOUT); } catch (Exception e) {
            Log.e(TAG, "", e);
        }
        socket.close();
        return true;
    }

    /** Sets symmetric RTP mode. */
    public void setSymmetricRtp(boolean symmetric_rtp)
    {  this.symmetric_rtp=symmetric_rtp;
    }


    /** whether symmetric RTP mode is set. */
    public boolean isSymmetricRtp()
    {  return symmetric_rtp;
    }


    /** From RtpStreamReceiverListener. When the remote socket address (source) is changed. */
    public void onRemoteSoAddressChanged(RtpStreamReceiver rr, SocketAddress remote_soaddr)
    {  if (symmetric_rtp && sender!=null) sender.setRemoteSoAddress(remote_soaddr);
    }
}
