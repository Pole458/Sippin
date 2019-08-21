package com.pole.sippin;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.net.rtp.AudioStream;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class CallService extends Service {



    public CallService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }


}
