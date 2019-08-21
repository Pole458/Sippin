package com.pole.sippin;

import org.zoolu.net.SocketAddress;

/** Listens for AndroidRtpStreamReceiver events.*/
public interface AndroidReceiverListener {
    /** When the remote socket address (source) is changed. */
    void onRemoteSoAddressChanged(AndroidReceiver rr, SocketAddress remote_soaddr);
}
