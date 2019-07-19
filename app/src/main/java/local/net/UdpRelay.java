/*
 * Copyright (C) 2008 Luca Veltri - University of Parma - Italy
 *
 * This source code is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * Author(s):
 * Luca Veltri (luca.veltri@unipr.it)
 */

package local.net;


import android.util.Log;
import org.zoolu.net.*;
import java.io.InterruptedIOException;


/** UdpRelay implements a direct UDP datagram relay agent. 
 * It receives UDP packets at a local port and relays them toward a remote UDP socket
 * (destination address/port).
 */
public class UdpRelay extends Thread {

    private static final String TAG = "Sip: UdpRelay" ;

    /** The maximum IP packet size */
    private static final int MAX_PKT_SIZE = 32000;

    /** Local receiver/sender port */
    private int local_port;

    /** Remote source address */
    private IpAddress src_addr;

    /** Remote source port */
    private int src_port;

    /** Destination address */
    private IpAddress dest_addr;

    /** Destination port */
    private int dest_port;

    /** Whether it is running */
    private boolean stop;

    /** Maximum time that the UDP relay can remain active after been halted (in milliseconds) */
    private int socket_to = 3000; // 3sec

    /** Maximum time that the UDP relay remains active without receiving UDP datagrams (in seconds) */
    private int alive_to = 60; // 1min

    /** UdpRelay listener */
    private UdpRelayListener listener;

    /** Creates a new UDP relay and starts it.
     * <p> The UdpRelay remains active until method halt() is called. */
    public UdpRelay(int local_port, String dest_addr, int dest_port, UdpRelayListener listener) {

        this.local_port=local_port;
        this.dest_addr = new IpAddress(dest_addr);
        this.dest_port = dest_port;
        this.listener = listener;
        src_addr = new IpAddress("0.0.0.0");
        src_port = 0;
        stop = false;

        try {
            UdpSocket socket = new UdpSocket(local_port);
            byte[] buf = new byte[MAX_PKT_SIZE];

            socket.setSoTimeout(socket_to);
            // datagram packet
            UdpPacket packet=new UdpPacket(buf, buf.length);

            // convert alive_to in milliseconds
            long keepalive_to=((1000)*(long)alive_to)-socket_to;

            // end time
            long expire=System.currentTimeMillis()+keepalive_to;
            // whether reset the receiver
            //boolean reset=true;

            while(!stop) {
                // non-blocking receiver
                try {
                    socket.receive(packet);
                } catch (InterruptedIOException ie) {
                    // if expired, stop relaying
                    if (alive_to>0 && System.currentTimeMillis()>expire) halt();
                    continue;
                }
                // check whether the source address and port are changed
                if (src_port!=packet.getPort() || !src_addr.equals(packet.getIpAddress())) {
                    src_port=packet.getPort();
                    src_addr=packet.getIpAddress();
                    if (listener != null)
                        listener.onUdpRelaySourceChanged(this,src_addr.toString(),src_port);
                }
                // relay
                packet.setIpAddress(this.dest_addr);
                packet.setPort(dest_port);
                socket.send(packet);
                // reset
                packet=new UdpPacket(buf, buf.length);
                expire=System.currentTimeMillis()+keepalive_to;
            }
            socket.close();
            if (listener != null)
                listener.onUdpRelayTerminated(this);
        }
        catch (Exception e) {
            Log.e(TAG, "Failed to start", e);
        }
    }

    /** Gets the local receiver/sender port */
    public int getLocalPort() {
        return local_port;
    }

    /** Whether the UDP relay is running */
    public boolean isRunning() {
        return !stop;
    }

    /** Stops the UDP relay */
    public void halt() {
        stop = true;
    }

    /** Redirect packets received from remote source addr/port to destination addr/port  */
    public void run() {

    }

    public String toString() {
        return "localhost:" + local_port + "-->" + dest_addr+ ":" + dest_port;
    }

}
 