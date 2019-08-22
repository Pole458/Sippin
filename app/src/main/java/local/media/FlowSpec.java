package local.media;


import android.net.rtp.AudioCodec;

/** Flow(s) specification.
 */
public class FlowSpec {

    /** Media spec */
    private AudioCodec audioCodec;

    /** Local port */
    private int local_port;

    /** Remote address */
    private String remoteAddress;

    /** Remote port */
    private int remote_port;

    /** Flow direction */
    private int direction;


    /** Creates a new FlowSpec */
    public FlowSpec(AudioCodec audioCodec, int local_port, String remote_addr, int remote_port, int direction) {
        this.audioCodec = audioCodec;
        this.local_port = local_port;
        this.remoteAddress = remote_addr;
        this.remote_port = remote_port;
        this.direction = direction;
    }

    /** Gets media specification. */
    public AudioCodec getAudioCodec() {
        return audioCodec;
    }


    /** Gets local port. */
    public int getLocalPort() {
        return local_port;
    }


    /** Gets remote address. */
    public String getRemoteAddress() {
        return remoteAddress;
    }


    /** Gets remote port. */
    public int getRemotePort() {
        return remote_port;
    }


    /** Gets direction. */
    public int getDirection() {
        return direction;
    }

}
