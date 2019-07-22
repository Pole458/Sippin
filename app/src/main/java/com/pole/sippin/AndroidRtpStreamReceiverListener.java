package com.pole.sippin;

import org.zoolu.net.SocketAddress;

/** Listens for AndroidRtpStreamReceiver events.*/
public interface AndroidRtpStreamReceiverListener {
    /** When the remote socket address (source) is changed. */
    public void onRemoteSoAddressChanged(AndroidRtpStreamReceiver rr, SocketAddress remote_soaddr);
}
