package com.pole.sippin;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class CallService extends Service {

    public CallService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }


}
