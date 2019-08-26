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

package local.ua;

import android.net.rtp.AudioStream;
import android.util.Log;
import com.pole.sippin.AndroidAudioApp;
import local.media.FlowSpec;
import local.media.MediaApp;



/** Media agent.
 * A media agent is used to start and stop multimedia sessions
 * (e.g. audio and/or video), by means of embedded media applications.
 */
class MediaAgent {

    private static final String TAG = "MediaAgent";

    /** Active media applications */
    private MediaApp active_media_app;

    /** Starts a media session */
    boolean startMediaSession(FlowSpec flow_spec, AudioStream audioStream) {

        Log.v(TAG, "start("+flow_spec.getAudioCodec()+")");
        Log.v(TAG, "new flow: "+flow_spec.getLocalPort()+((flow_spec.getDirection()==AudioStream.MODE_SEND_ONLY)? "=-->" : ((flow_spec.getDirection()==AudioStream.MODE_RECEIVE_ONLY)? "<--=" : "<-->" ))+flow_spec.getRemoteAddress()+":"+flow_spec.getRemotePort());

        // start new media_app
        MediaApp media_app;

        String media = "audio";

        // stop previous media_app (just in case something was wrong..)
        if (active_media_app != null) {
            active_media_app.stopApp();
        }

        media_app = newAudioApp(flow_spec, audioStream);

        if (media_app != null) {
            if (media_app.startApp()) {
                active_media_app = media_app;
                return true;
            } else return false;
        } else {
            Log.v(TAG, "WARNING: no " + media +" application has been found: " + media + " not started");
            return false;
        }
    }

    /** Stops a media session.  */
    void stopMediaSession(String media) {
        Log.v(TAG, "stop("+media+")");

        if (active_media_app != null) {
            active_media_app.stopApp();
        } else {
            Log.v(TAG, "WARNING: no running "+media+" application has been found.");
        }
    }

    // ********************** media applications *********************

    /** Creates a new audio application. */
    private MediaApp newAudioApp(FlowSpec audio_flow, AudioStream audioStream) {

        MediaApp audio_app;

        audio_app = new AndroidAudioApp(audio_flow, audioStream);

        return audio_app;
    }

}
