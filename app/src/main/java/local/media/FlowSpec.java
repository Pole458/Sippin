package local.media;




/** Flow(s) specification.
 */
public class FlowSpec
{
    /** Interface for characterizing media direction */
    public static class Direction {}

    /** Send only mode */
    public static final Direction SEND_ONLY=new Direction();

    /** Receive only mode */
    public static final Direction RECV_ONLY=new Direction();

    /** Full duplex mode */
    public static final Direction FULL_DUPLEX=new Direction();

    /** Media spec */
    private MediaSpec media_spec;

    /** Local port */
    private int local_port;

    /** Remote address */
    private String remote_addr;

    /** Remote port */
    private int remote_port;

    /** Flow direction */
    private int direction;


    /** Creates a new FlowSpec */
    public FlowSpec(MediaSpec media_spec, int local_port, String remote_addr, int remote_port, int direction) {
        this.media_spec=media_spec;
        this.local_port=local_port;
        this.remote_addr=remote_addr;
        this.remote_port=remote_port;
        this.direction=direction;
    }

    /** Gets media specification. */
    public MediaSpec getMediaSpec() {
        return media_spec;
    }


    /** Gets local port. */
    public int getLocalPort() {
        return local_port;
    }


    /** Gets remote address. */
    public String getRemoteAddress() {
        return remote_addr;
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
