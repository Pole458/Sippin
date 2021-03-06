package com.pole.sippin;

import android.media.AudioRecord;
import android.util.Log;
import local.net.RtpPacket;
import local.net.RtpSocket;
import org.zoolu.net.IpAddress;
import org.zoolu.net.SocketAddress;
import org.zoolu.net.UdpSocket;


/** AndroidRtpStreamSender is a Android RTP sender.
 * It takes media from a given AudioRecord and sends it through RTP packets to a remote destination.
 */
public class AndroidRtpStreamSender extends AndroidSender {

    private static final String TAG = "Sip:AndrRtpStrmSender";

    /** The RtpSocket */
    private RtpSocket rtp_socket = null;

    /** Whether the socket has been created here */
    private boolean socket_is_local_attribute=false;

    /** Payload type */
    private int p_type;

    /** Number of frame per second */
    private long frame_rate;

    /** Number of bytes per frame */
    private int frame_size;

    /** Whether it works synchronously with a local clock, or it it acts as slave of the InputStream  */
    private boolean do_sync;

    /** Synchronization correction value, in milliseconds.
     * It accellarates the sending rate respect to the nominal value,
     * in order to compensate program latencies. */
    private int sync_adj = 0;

    /** Constructs a RtpStreamSender.
     * @param audio_record the stream to be sent
     * @param payload_type the type type
     * @param frame_rate the frame rate, i.e. the number of frames that should be sent per second;
     *        it is used to calculate the nominal packet time and, in case of do_sync==true,
    the next departure time
     * @param frame_size the size of the type
     * @param src_socket the socket used to send the RTP packet
     * @param dest_addr the destination address
     * @param dest_port the destination port */
    AndroidRtpStreamSender(AudioRecord audio_record, int payload_type, long frame_rate, int frame_size, UdpSocket src_socket, String dest_addr, int dest_port) {
        super(audio_record, dest_addr, dest_port);
        this.p_type = payload_type;
        this.frame_rate = frame_rate;
        this.frame_size = frame_size;
        this.do_sync = false;
        try {
            if (src_socket == null) {
                src_socket = new UdpSocket(0);
                socket_is_local_attribute = true;
            }
            rtp_socket = new RtpSocket(src_socket, IpAddress.getByName(dest_addr),dest_port);
        }
        catch (Exception e) {  e.printStackTrace();  }
    }

    /** Gets the local port. */
    public int getLocalPort() {
        if (rtp_socket!=null) return rtp_socket.getUdpSocket().getLocalPort();
        else return 0;
    }

    /** Changes the remote socket address. */
    void setRemoteSoAddress(SocketAddress remote_soaddr) {
        if (remote_soaddr!=null && rtp_socket!=null)
            try {
                rtp_socket = new RtpSocket(rtp_socket.getUdpSocket(),IpAddress.getByName(remote_soaddr.getAddress().toString()),remote_soaddr.getPort());
            }
            catch (Exception e) {  e.printStackTrace();  }
    }

    /** Gets the remote socket address. */
    public SocketAddress getRemoteSoAddress() {
        if (rtp_socket!=null) return new SocketAddress(rtp_socket.getRemoteAddress().toString(),rtp_socket.getRemotePort());
        else return null;
    }

    /** Sets the synchronization adjustment time (in milliseconds). */
    public void setSyncAdj(int millisecs) {
        sync_adj=millisecs;
    }

    /** Whether is running */
    public boolean isRunning() {
        return running;
    }

    /** Stops running */
    public void halt() {
        running = false;
    }

    /** Runs it in a new Thread. */
    public void run() {
        if (rtp_socket == null || audio_record == null) return;

        byte[] packet_buffer = new byte[12+frame_size];
        RtpPacket rtp_packet = new RtpPacket(packet_buffer,0);
        rtp_packet.setHeader(p_type);
        int seqn = 0;
        long time = 0;
        long start_time = System.currentTimeMillis();
        long byte_rate = frame_rate*frame_size;

        audio_record.startRecording();

        running = true;

        Log.v(TAG,"RTP: localhost:"+rtp_socket.getUdpSocket().getLocalPort()+" --> "+rtp_socket.getRemoteAddress().toString()+":"+rtp_socket.getRemotePort());
        Log.v(TAG, "RTP: sending packets of " + (packet_buffer.length-12) + " bytes of RTP type");

        try {
            while (running) {
                int num = read(audio_record, packet_buffer,12,packet_buffer.length-12);
                if (num > 0) {
                    rtp_packet.setSequenceNumber(seqn++);
                    rtp_packet.setTimestamp(time);
                    rtp_packet.setPayloadLength(num);
                    rtp_socket.send(rtp_packet);
                    // update rtp timestamp (in milliseconds)
                    long frame_time = (num * 1000) / byte_rate;
                    time += frame_time;
                    // wait for next departure
                    if (do_sync || sync_adj>0) {
                        // wait before next departure..
                        long sleep_time = start_time + time - System.currentTimeMillis();
                        // compensate possible inter-time reduction due to the approximated time obtained by System.currentTimeMillis()
                        long min_time = frame_time / 2;
                        // compensate possible program latency
                        sleep_time -= sync_adj;
                        if (sleep_time < min_time) sleep_time = min_time;
                        if (sleep_time > 0) try {
                            Log.v(TAG,"Sleep time: " + sleep_time);
                            Thread.sleep(sleep_time);  } catch (Exception e) {}
                    }
                } else if (num < 0) {
                    Log.v(TAG,"Error reading from InputStream");
                    running = false;
                }
            }
        }
        catch (Exception e) {  running=false;  e.printStackTrace();  }

        //if (DEBUG) println("rtp time:  "+time);
        //if (DEBUG) println("real time: "+(System.currentTimeMillis()-start_time));

        // close RtpSocket and local UdpSocket
        UdpSocket socket = rtp_socket.getUdpSocket();
        rtp_socket.close();
        if (socket_is_local_attribute && socket!=null) socket.close();

        // free all
        audio_record.stop();
        audio_record = null;
        rtp_socket = null;
    }


    /** Reads a block of bytes from an AudioRecord and put it into a given buffer.
     * This method is used by the RtpStreamSender to compose RTP packets,
     * and can be re-defined by a class that extends RtpStreamSender in order to
     * implement new RTP encoding mechanisms.
     * @return It returns the number of bytes read. */
    protected int read(AudioRecord audio_record, byte[] buff, int off, int len) throws Exception {
        return audio_record.read(buff, off, len);
//        return audio_record.read(buff,12,buff.length-12);
    }
}