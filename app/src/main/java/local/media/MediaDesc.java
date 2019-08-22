package local.media;



import android.net.rtp.AudioCodec;
import org.zoolu.sdp.*;
import org.zoolu.tools.Parser;

import java.util.Vector;



/** Media description.
 */
public class MediaDesc {

    /** Media type (e.g. audio, video, message, etc.) */
    private String media;

    /** Port */
    private int port;

    /** Transport */
    private String transport;

    /** Vector of media specifications */
    private Vector<AudioCodec> audioCodecs;

    /** Creates a new MediaDesc.
     * @param media Media type
     * @param port Port
     * @param transport Transport protocol
     * @param audioCodecs Vector of media specifications (MediaSpec) */
    public MediaDesc(String media, int port, String transport, Vector<AudioCodec> audioCodecs) {
        this.media = media;
        this.port = port;
        this.transport = transport;
        this.audioCodecs = audioCodecs;
    }


    /** Creates a new MediaDesc.
     * @param md MediaDescriptor (org.zoolu.tools.sdp.MediaDescriptor) used to create this MediaDesc */
    public MediaDesc(MediaDescriptor md) {

        MediaField mf = md.getMedia();
        String media = mf.getMedia();
        int port = mf.getPort();
        String transport = mf.getTransport();

        Vector<AttributeField> attributes = md.getAttributes("rtpmap");
        Vector<AudioCodec> audioCodecs = new Vector<>(attributes.size());

        for (int i = 0; i < attributes.size(); i++) {
            Parser par = new Parser(attributes.elementAt(i).getAttributeValue());
            int avp = par.getInt();
            String codec = null;
            if (par.skipChar().hasMore()) {
                codec = par.getWord(new char[]{'/'});
            }
            audioCodecs.addElement(AudioCodec.getCodec(avp, codec, null));
        }

        this.media = media;
        this.port = port;
        this.transport = transport;
        this.audioCodecs = audioCodecs;
    }


    /** Gets media type. */
    public String getMedia() {
        return media;
    }


    /** Gets port. */
    public int getPort() {
        return port;
    }


    /** Sets port. */
    public void setPort(int port) {
        this.port=port;
    }


    /** Gets transport protocol. */
    public String getTransport() {
        return transport;
    }

    /** Gets Audio codecs */
    public Vector<AudioCodec> getAudioCodecs() {
        return audioCodecs;
    }

    /** Sets media specifications. */
    public void setMediaSpecs(Vector<AudioCodec> media_specs) {
        this.audioCodecs = media_specs;
    }


    /** Adds a new media specification. */
    public void addMediaSpec(AudioCodec media_spec) {
        if (audioCodecs ==null) audioCodecs = new Vector<>();
        audioCodecs.addElement(media_spec);
    }

    /** Gets the corresponding MediaDescriptor. */
    public MediaDescriptor toMediaDescriptor() {
        Vector<Integer> formats = new Vector<>();
        Vector<AttributeField> attributes = new Vector<>();
        if (audioCodecs !=null) {
            for (int i = 0; i< audioCodecs.size(); i++) {
                AudioCodec codec = audioCodecs.elementAt(i);
                formats.addElement(codec.type);
                attributes.addElement(new AttributeField("rtpmap", codec.type + " " + codec.rtpmap));
            }
        }
        return new MediaDescriptor(new MediaField(media, port,0, transport, formats),null, attributes);
    }

    /** Gets a String representation of this object. */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(media).append(" ").append(port).append(" ").append(transport);
        if (audioCodecs != null) {
            sb.append(" {");
            for (int i = 0; i < audioCodecs.size(); i++) {
                if (i>0) sb.append(",");
                sb.append(" ").append(audioCodecs.elementAt(i).toString());
            }
            sb.append(" }");
        }
        return sb.toString();
    }

}
