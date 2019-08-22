package local.ua;


import android.net.rtp.AudioCodec;
import android.util.Log;
import local.media.MediaDesc;
import org.zoolu.net.SocketAddress;
import org.zoolu.sip.address.NameAddress;
import org.zoolu.sip.address.SipURL;
import org.zoolu.sip.provider.SipProvider;
import org.zoolu.sip.provider.SipStack;
import org.zoolu.tools.Configure;
import org.zoolu.tools.Parser;

import java.util.Vector;



/** UserAgentProfile maintains the user configuration.
 */
public class UserAgentProfile extends Configure {

    private static final String TAG = "Sip:UserAgentProfile";

    // ********************** user configurations *********************

    /** Display name for the user.
     * It is used in the user's AOR registered to the registrar server
     * and used as From URL. */
    public String display_name = null;

    /** User's name.
     * It is used to build the user's AOR registered to the registrar server
     * and used as From URL. */
    public String user = null;

    /** Fully qualified domain name (or address) of the proxy server.
     * It is part of the user's AOR registered to the registrar server
     * and used as From URL.
     * <p/>
     * If <i>proxy</i> is not defined, the <i>registrar</i> value is used in its place.
     * <p/>
     * If <i>registrar</i> is not defined, the <i>proxy</i> value is used in its place. */
    public String proxy = null;

    /** Fully qualified domain name (or address) of the registrar server.
     * It is used as recipient for REGISTER requests.
     * <p/>
     * If <i>registrar</i> is not defined, the <i>proxy</i> value is used in its place.
     * <p/>
     * If <i>proxy</i> is not defined, the <i>registrar</i> value is used in its place. */
    public String registrar = null;

    /** MyUA address.
     * It is the SIP address of the MyUA and is used to form the From URL if no proxy is configured. */
    public String ua_address = null;

    /** User's name used for server authentication. */
    public String auth_user = null;
    /** User's realm used for server authentication. */
    public String auth_realm = null;
    /** User's passwd used for server authentication. */
    public String auth_passwd = null;

    /** Whether registering with the registrar server */
    public boolean do_register = false;
    /** Whether unregistering the contact address */
    public boolean do_unregister = false;
    /** Whether unregistering all contacts before registering the contact address */
    public boolean do_unregister_all = false;
    /** Expires time (in seconds). */
    public int expires = 3600;

    /** Rate of keep-alive tokens (datagrams) sent toward the outbound proxy
     * (if present) or toward the registrar server.
     * Its value specifies the delta-time (in millesconds) between two
     * keep-alive tokens. <br/>
     * Set keepalive_time=0 for not sending keep-alive datagrams. */
    public long keepalive_time=0;

    /** Automatic call a remote user secified by the 'call_to' value.
     * Use value 'NONE' for manual calls (or let it undefined).  */
    public NameAddress call_to = null;

    /** Response time in seconds; it is the maximum time the user can wait before responding to an incoming call; after such time the call is automatically declined (refused). */
    public int refuse_time = 20;
    /** Automatic answer time in seconds; time<0 corresponds to manual answer mode. */
    public int accept_time = -1;
    /** Automatic hangup time (call duartion) in seconds; time<=0 corresponds to manual hangup mode. */
    public int hangup_time = -1;
    /** Automatic call transfer time in seconds; time<0 corresponds to no auto transfer mode. */
    public int transfer_time = -1;
    /** Automatic re-inviting time in seconds; time<0 corresponds to no auto re-invite mode.  */
    public int re_invite_time = -1;

    /** Redirect incoming call to the specified url.
     * Use value 'NONE' for not redirecting incoming calls (or let it undefined). */
    public NameAddress redirect_to = null;

    /** Transfer calls to the specified url.
     * Use value 'NONE' for not transferring calls (or let it undefined). */
    public NameAddress transfer_to = null;

    /** No offer in the invite */
    public boolean no_offer = false;
    /** Do not use prompt */
    public boolean no_prompt = false;

    /** Whether using audio */
    public boolean audio = true;
    /** Whether using video */
    public boolean video = false;

    /** Whether looping the received media streams back to the sender. */
    public boolean loopback = false;
    /** Whether playing in receive only mode */
    public boolean recv_only = false;
    /** Whether playing in send only mode */
    public boolean send_only = false;
    /** Whether playing a test tone in send only mode */
    public boolean send_tone = false;
    /** Audio file to be streamed */
    public String send_file = null;
    /** Audio file to be recorded */
    public String recv_file = null;
    /** Video file to be streamed */
    public String send_video_file = null;
    /** Video file to be recorded */
    public String recv_video_file = null;

    /** Media address (use it if you want to use a media address different from the via address) */
    public String media_addr = null;
    /** First media port (use it if you want to use media ports different from those specified in mediaDescs) */
    public int media_port = -1;

    /** Whether using symmetric_rtp */
    public boolean symmetric_rtp = false;

    /** Vector of media descriptions (MediaDesc) */
    public Vector<MediaDesc> mediaDescs = new Vector<>();


    // ******************** undocumented parametes ********************

    /** Whether running the UAS (User Agent Server), or acting just as UAC (User Agent Client). In the latter case only outgoing calls are supported. */
    public boolean ua_server = true;
    /** Whether running an Options Server, that automatically responds to OPTIONS requests. */
    public boolean options_server = true;
    /** Whether running an Null Server, that automatically responds to not-implemented requests. */
    public boolean null_server = true;

    /** Fixed audio multicast socket address; if defined, it forces the use of this maddr+port for audio session */
    public SocketAddress audio_mcast_soaddr = null;

    /** Fixed video multicast socket address; if defined, it forces the use of this maddr+port for video session */
    public SocketAddress video_mcast_soaddr = null;


    // ********************* historical parametes *********************

    /** Default audio port */
    private int audio_port = 4000;
    /** Default video port */
    private int video_port = 4002;

    // ************************** costructors *************************

    /** Constructs a void UserAgentProfile */
    public UserAgentProfile() {

        if (proxy!=null && proxy.equalsIgnoreCase(Configure.NONE)) proxy=null;
        if (registrar!=null && registrar.equalsIgnoreCase(Configure.NONE)) registrar=null;
        if (display_name!=null && display_name.equalsIgnoreCase(Configure.NONE)) display_name=null;
        if (user!=null && user.equalsIgnoreCase(Configure.NONE)) user=null;
        if (auth_realm!=null && auth_realm.equalsIgnoreCase(Configure.NONE)) auth_realm=null;
        if (send_file!=null && send_file.equalsIgnoreCase(Configure.NONE)) send_file=null;
        if (recv_file!=null && recv_file.equalsIgnoreCase(Configure.NONE)) recv_file=null;

        Vector<AudioCodec> audioCodecs = new Vector<>();
        for(AudioCodec codec : AudioCodec.getCodecs()) {
           audioCodecs.addElement(codec);
        }

        mediaDescs.addElement(new MediaDesc("audio", audio_port, "RTP/AVP", audioCodecs));

        setUnconfiguredAttributes(null);
    }


    // ************************ public methods ************************

    /** Gets the user's AOR (Address Of Record) registered to the registrar server
     * and used as From URL.
     * <p/>
     * In case of <i>proxy</i> and <i>user</i> parameters have been defined
     * it is formed as "<i>display_name</i>"&lt;sip:<i>user</i>@<i>proxy</i>&rt,
     * otherwhise the local MyUA address (obtained by the SipProvider) is used. */
    public NameAddress getUserURI() {
        if (proxy != null && user != null)
            return new NameAddress(display_name, new SipURL(user, proxy));
        else
            return new NameAddress(display_name, new SipURL(user, ua_address));
    }

    /** Sets the user's AOR (Address Of Record) registered to the registrar server
     * and used as From URL.
     * <p/>
     * It actually sets the <i>display_name</i>, <i>user</i>, and <i>proxy</i> parameters.
     * <p/>
     * If <i>registrar</i> is not defined, the <i>proxy</i> value is used in its place. */
    public void setUserURI(NameAddress naddr) {
        SipURL url=naddr.getAddress();
        if (display_name==null) display_name=naddr.getDisplayName();
        if (user==null) user=url.getUserName();
        if (proxy==null) proxy=(url.hasPort())? url.getHost()+":"+url.getPort() : url.getHost();
        if (registrar==null) registrar=proxy;
    }

    /** Sets server and authentication attributes (if not already done).
     * It actually sets <i>ua_address</i>, <i>registrar</i>, <i>proxy</i>, <i>auth_realm</i>,
     * and <i>auth_user</i> attributes.
     * <p/>
     * Note: this method sets such attributes only if they haven't still been initilized. */
    public void setUnconfiguredAttributes(SipProvider sip_provider) {
        if (registrar==null && proxy!=null) registrar=proxy;
        if (proxy==null && registrar!=null) proxy=registrar;
        if (auth_realm==null && proxy!=null) auth_realm=proxy;
        if (auth_realm==null && registrar!=null) auth_realm=registrar;
        if (auth_user==null && user!=null) auth_user=user;
        if (ua_address==null && sip_provider!=null)
        {  ua_address=sip_provider.getViaAddress();
            if (sip_provider.getPort()!=SipStack.default_port) ua_address+=":"+sip_provider.getPort();
        }
    }

    // *********************** protected methods **********************

    /** Parses a single line (loaded from the config file) */
    protected void parseLine(String line) {
        String attribute;
        Parser par;
        int index=line.indexOf("=");
        if (index>0) {  attribute=line.substring(0,index).trim(); par=new Parser(line,index+1);  }
        else {  attribute=line; par=new Parser("");  }

        if (attribute.equals("display_name"))   {  display_name=par.getRemainingString().trim();  return;  }
        if (attribute.equals("user"))           {  user=par.getString();  return;  }
        if (attribute.equals("proxy"))          {  proxy=par.getString();  return;  }
        if (attribute.equals("registrar"))      {  registrar=par.getString();  return;  }

        if (attribute.equals("auth_user"))      {  auth_user=par.getString();  return;  }
        if (attribute.equals("auth_realm"))     {  auth_realm=par.getRemainingString().trim();  return;  }
        if (attribute.equals("auth_passwd"))    {  auth_passwd=par.getRemainingString().trim();  return;  }

        if (attribute.equals("do_register"))    {  do_register=(par.getString().toLowerCase().startsWith("y"));  return;  }
        if (attribute.equals("do_unregister"))  {  do_unregister=(par.getString().toLowerCase().startsWith("y"));  return;  }
        if (attribute.equals("do_unregister_all")) {  do_unregister_all=(par.getString().toLowerCase().startsWith("y"));  return;  }
        if (attribute.equals("expires"))        {  expires=par.getInt();  return;  }
        if (attribute.equals("keepalive_time")) {  keepalive_time=par.getInt();  return;  }

        if (attribute.equals("call_to"))
        {  String naddr=par.getRemainingString().trim();
            if (naddr==null || naddr.length()==0 || naddr.equalsIgnoreCase(Configure.NONE)) call_to=null;
            else call_to=new NameAddress(naddr);
            return;
        }
        if (attribute.equals("redirect_to"))
        {  String naddr=par.getRemainingString().trim();
            if (naddr==null || naddr.length()==0 || naddr.equalsIgnoreCase(Configure.NONE)) redirect_to=null;
            else redirect_to=new NameAddress(naddr);
            return;
        }
        if (attribute.equals("transfer_to"))
        {  String naddr=par.getRemainingString().trim();
            if (naddr==null || naddr.length()==0 || naddr.equalsIgnoreCase(Configure.NONE)) transfer_to=null;
            else transfer_to=new NameAddress(naddr);
            return;
        }

        if (attribute.equals("refuse_time"))    {  refuse_time=par.getInt();  return;  }
        if (attribute.equals("accept_time"))    {  accept_time=par.getInt();  return;  }
        if (attribute.equals("hangup_time"))    {  hangup_time=par.getInt();  return;  }
        if (attribute.equals("transfer_time"))  {  transfer_time=par.getInt();  return;  }
        if (attribute.equals("re_invite_time")) {  re_invite_time=par.getInt();  return;  }
        if (attribute.equals("no_offer"))       {  no_offer=(par.getString().toLowerCase().startsWith("y"));  return;  }
        if (attribute.equals("no_prompt"))      {  no_prompt=(par.getString().toLowerCase().startsWith("y"));  return;  }

        if (attribute.equals("loopback"))       {  loopback=(par.getString().toLowerCase().startsWith("y"));  return;  }
        if (attribute.equals("recv_only"))      {  recv_only=(par.getString().toLowerCase().startsWith("y"));  return;  }
        if (attribute.equals("send_only"))      {  send_only=(par.getString().toLowerCase().startsWith("y"));  return;  }
        if (attribute.equals("send_tone"))      {  send_tone=(par.getString().toLowerCase().startsWith("y"));  return;  }
        if (attribute.equals("send_file"))      {  send_file=par.getRemainingString().trim();  return;  }
        if (attribute.equals("recv_file"))      {  recv_file=par.getRemainingString().trim();  return;  }
        if (attribute.equals("send_video_file")){  send_video_file=par.getRemainingString().trim();  return;  }
        if (attribute.equals("recv_video_file")){  recv_video_file=par.getRemainingString().trim();  return;  }

        if (attribute.equals("audio"))          {  audio=(par.getString().toLowerCase().startsWith("y"));  return;  }
        if (attribute.equals("video"))          {  video=(par.getString().toLowerCase().startsWith("y"));  return;  }

        if (attribute.equals("media_addr"))     {  media_addr=par.getString();  return;  }
        if (attribute.equals("media_port"))     {  media_port=par.getInt();  return;  }
        if (attribute.equals("symmetric_rtp"))  {  symmetric_rtp=(par.getString().toLowerCase().startsWith("y"));  return;  }


        if (attribute.equals("ua_server")) {  ua_server=(par.getString().toLowerCase().startsWith("y"));  return;  }
        if (attribute.equals("options_server")) {  options_server=(par.getString().toLowerCase().startsWith("y"));  return;  }
        if (attribute.equals("null_server")) {  null_server=(par.getString().toLowerCase().startsWith("y"));  return;  }
        if (attribute.equals("audio_mcast_soaddr")) {  audio_mcast_soaddr=new SocketAddress(par.getString());  return;  }
        if (attribute.equals("video_mcast_soaddr")) {  video_mcast_soaddr=new SocketAddress(par.getString());  return;  }

        // for backward compatibily
        if (attribute.equals("from_url"))         {  setUserURI(new NameAddress(par.getRemainingString().trim()));  return;  }
        if (attribute.equals("contact_user"))     {  user=par.getString();  return;  }
        if (attribute.equals("auto_accept"))      {  accept_time=((par.getString().toLowerCase().startsWith("y")))? 0 : -1;  return;  }

        if (attribute.equals("audio_port"))     {  audio_port=par.getInt();  return;  }
        if (attribute.equals("video_port"))     {  video_port=par.getInt();  return;  }

        // old parameters
        if (attribute.equals("contact_url")) System.err.println("WARNING: parameter 'contact_url' is no more supported.");
        if (attribute.equals("secure_contact_url")) System.err.println("WARNING: parameter 'secure_contact_url' is no more supported.");
    }


    /** Converts the entire object into lines (to be saved into the config file) */
    protected String toLines() {
        // currently not implemented..
        return getUserURI().toString();
    }
}
