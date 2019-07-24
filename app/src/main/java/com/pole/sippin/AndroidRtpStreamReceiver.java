package com.pole.sippin;

import android.media.AudioTrack;
import android.util.Log;
import local.net.RtpPacket;
import local.net.RtpSocket;
import org.zoolu.net.SocketAddress;
import org.zoolu.net.UdpSocket;

import java.io.InterruptedIOException;


/** AndroidRtpStreamReceiver is an Android RTP receiver.
 * It receives packets from RTP and writes it to the target AudioTrack.
 */
public class AndroidRtpStreamReceiver extends Thread {

    private static final String TAG = "Sip: AndrRtpStrmRec";

    /** Time waited before starting playing out packets (in millisecs). All packet received in the meantime are dropped in order to reduce the effect of an eventual initial packet burst. */
    public static final int EARLY_DROP_TIME = 200;

    /** Size of the read buffer */
    public static final int BUFFER_SIZE = 32768;

    /** Maximum blocking time, spent waiting for reading new bytes [milliseconds] */
    public static final int SO_TIMEOUT = 200;

    /** The OutputStream */
    private AudioTrack audio_track;

    /** The RtpSocket */
    private RtpSocket rtp_socket = null;

    /** Whether the socket has been created here */
    private boolean socket_is_local_attribute = false;

    /** Remote socket address */
    private SocketAddress remote_soaddr = null;

    /** Whether it is running */
    private boolean running = false;

    /** Packet drop rate (actually it is the inverse of the packet drop rate) */
    private int packet_drop_rate=0;

    /** Packet counter (incremented only if packet_drop_rate>0) */
    private int packet_counter=0;

    /** Listener */
    private AndroidRtpStreamReceiverListener listener;


    /** Constructs a AndroidRtpStreamReceiver.
     * @param audio_track the stream sink
     * @param udp_socket the local receiver UdpSocket
     * @listener the RtpStreamReceiver listener */
    public AndroidRtpStreamReceiver(AudioTrack audio_track, UdpSocket udp_socket, AndroidRtpStreamReceiverListener listener) {
        this.audio_track = audio_track;
        this.listener = listener;
        if (udp_socket != null)
            rtp_socket=new RtpSocket(udp_socket);
    }

    /** Gets the local port. */
    public int getLocalPort() {
        if (rtp_socket!=null) return rtp_socket.getUdpSocket().getLocalPort();
        else return 0;
    }

    /** Whether is running */
    public boolean isRunning() {
        return running;
    }

    /** Stops running */
    public void halt() {
        audio_track.stop();
        running=false;
    }

    /** Runs it in a new Thread. */
    public void run() {
        if (rtp_socket == null) {
//           println("ERROR: RTP socket is null");
            return;
        }

        byte[] buffer = new byte[BUFFER_SIZE];
        RtpPacket rtp_packet = new RtpPacket(buffer,0);

        audio_track.play();

        running = true;

//        println("RTP: localhost:"+rtp_socket.getUdpSocket().getLocalPort()+" <-- remotesocket");
//        println("RTP: receiving pkts of MAXIMUM "+buffer.length+" bytes");

        try {

            rtp_socket.getUdpSocket().setSoTimeout(SO_TIMEOUT);

            long early_drop_to = (EARLY_DROP_TIME > 0 )? System.currentTimeMillis() + EARLY_DROP_TIME : -1;

            while (running) {
                try {
                    // read a block of data from the rtp socket
                    rtp_socket.receive(rtp_packet);

                    // drop the first packets in order to reduce the effect of an eventual initial packet burst
                    if (early_drop_to > 0 && System.currentTimeMillis() < early_drop_to) continue;
                    else early_drop_to = -1;

                    // write this block to the output_stream (only if still running..)
                    if (running)
                        write(audio_track, rtp_packet.getPacket(), rtp_packet.getHeaderLength(), rtp_packet.getPayloadLength());

                    // check if remote socket address is changed
                    String addr = rtp_socket.getRemoteAddress().toString();
                    int port = rtp_socket.getRemotePort();
                    if (remote_soaddr == null || !remote_soaddr.getAddress().toString().equals(addr) || remote_soaddr.getPort() != port) {
                        remote_soaddr = new SocketAddress(addr, port);
                        if (listener != null) listener.onRemoteSoAddressChanged(this, remote_soaddr);
                    }

                } catch (InterruptedIOException e) {
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            running = false;
            e.printStackTrace();
        }

        // close RtpSocket and local UdpSocket
        UdpSocket udp_socket = rtp_socket.getUdpSocket();
        rtp_socket.close();
        if (socket_is_local_attribute && udp_socket!=null) udp_socket.close();

        // free all
        audio_track = null;
        rtp_socket = null;

//        println("rtp receiver terminated");
    }


    /** Sets the random early drop (RED) rate. Actually it sets the inverse of the packet drop rate. */
    public void setRED(int rate) {
        this.packet_drop_rate=rate;
    }


    /** Gets the random early drop (RED) rate. Actually it gets the inverse of the packet drop rate. */
    public int getRED() {
        return packet_drop_rate;
    }


    /** Writes a block of bytes to an InputStream taken from a given buffer.
     * This method is used by the RtpStreamReceiver to process incoming RTP packets,
     * and can be re-defined by a class that extends RtpStreamReceiver in order to
     * implement new RTP decoding mechanisms. */
    protected void write(AudioTrack audio_track, byte[] buff, int off, int len) throws Exception {
        if (packet_drop_rate>0 && (++packet_counter)%packet_drop_rate==0) return;
        audio_track.write(buff, off, len);
    }


    public static int byte2int(byte b) {
        return (b+0x100)%0x100;
    }

    public static int byte2int(byte b1, byte b2) {
        return (((b1+0x100)%0x100)<<8)+(b2+0x100)%0x100;
    }
}
