package local.media;


import android.util.Log;
import local.net.UdpRelay;
import local.net.UdpRelayListener;


/** LoopbackMediaApp.
 */
public class LoopbackMediaApp implements MediaApp, UdpRelayListener {

    private static final String TAG = "Sip: LoopBackMediaApp";

    /** UdpRelay */
    private UdpRelay udp_relay = null;


    /** Creates a new LoopbackMediaApp */
    public LoopbackMediaApp(FlowSpec flow_spec) {
        try {
            udp_relay = new UdpRelay(flow_spec.getLocalPort(), flow_spec.getRemoteAddress(), flow_spec.getRemotePort(),this);
            Log.v(TAG, "relay " + udp_relay.toString() + " started");
        }
        catch (Exception e) {
            Log.e(TAG, "Could not create UdpRelay", e);
        }
    }


    /** Starts media application */
    public boolean startApp() {
        // do nothing, already started..
        return true;
    }


    /** Stops media application */
    public boolean stopApp() {
        if (udp_relay!=null) {
            udp_relay.halt();
            udp_relay=null;
            Log.v(TAG, "relay halted");
        }
        return true;
    }


    // *************************** Callbacks ***************************

    /** From UdpRelayListener. When the remote source address changes. */
    public void onUdpRelaySourceChanged(UdpRelay udp_relay, String remote_src_addr, int remote_src_port) {
        Log.v(TAG,"UDP relay: remote address changed: "+remote_src_addr+":"+remote_src_port);
    }

    /** From UdpRelayListener. When UdpRelay stops relaying UDP datagrams. */
    public void onUdpRelayTerminated(UdpRelay udp_relay) {
        Log.v(TAG, "UDP relay: terminated.");
    }

}