package local.ua;


import android.content.Context;
import android.content.SharedPreferences;
import android.net.rtp.AudioCodec;
import android.util.Log;
import local.media.MediaDesc;
import org.zoolu.sip.address.NameAddress;
import org.zoolu.sip.address.SipURL;
import org.zoolu.sip.provider.SipProvider;
import org.zoolu.sip.provider.SipStack;
import org.zoolu.tools.Configurable;

import java.util.Vector;



/** UserAgentProfile maintains the user configuration.
 */
public class UserAgentProfile implements Configurable {

    private static final String TAG = "Sip:UserAgentProfile";

    // ********************** user configurations *********************

    /** Display rtpmap for the user.
     * It is used in the user's AOR registered to the registrar server
     * and used as From URL. */
    private String display_name = null;

    /** User's rtpmap.
     * It is used to build the user's AOR registered to the registrar server
     * and used as From URL. */
    public String user = null;

    /** Fully qualified domain rtpmap (or address) of the proxy server.
     * It is part of the user's AOR registered to the registrar server
     * and used as From URL.
     * <p/>
     * If <i>proxy</i> is not defined, the <i>registrar</i> value is used in its place.
     * <p/>
     * If <i>registrar</i> is not defined, the <i>proxy</i> value is used in its place. */
    public String proxy = null;

    /** Fully qualified domain rtpmap (or address) of the registrar server.
     * It is used as recipient for REGISTER requests.
     * <p/>
     * If <i>registrar</i> is not defined, the <i>proxy</i> value is used in its place.
     * <p/>
     * If <i>proxy</i> is not defined, the <i>registrar</i> value is used in its place. */
    String registrar = null;

    /** MyUA address.
     * It is the SIP address of the MyUA and is used to form the From URL if no proxy is configured. */
    private String ua_address = null;

    /** User's rtpmap used for server authentication. */
    String auth_user = null;
    /** User's realm used for server authentication. */
    String auth_realm = null;
    /** User's passwd used for server authentication. */
    String auth_passwd = null;

    /** Whether registering with the registrar server */
    private boolean do_register = false;
    /** Whether unregistering the contact address */
    private boolean do_unregister = false;
    /** Whether unregistering all contacts before registering the contact address */
    private boolean do_unregister_all = false;
    /** Expires time (in seconds). */
    public int expires = 3600;

    /** Rate of keep-alive tokens (datagrams) sent toward the outbound proxy
     * (if present) or toward the registrar server.
     * Its value specifies the delta-time (in millesconds) between two
     * keep-alive tokens. <br/>
     * Set keepalive_time=0 for not sending keep-alive datagrams. */
    private long keepalive_time=0;

    /** Automatic call a remote user specified by the 'call_to' value.
     * Use value 'NONE' for manual calls (or let it undefined).  */
    private NameAddress call_to = null;

    /** Response time in seconds; it is the maximum time the user can wait before responding to an incoming call; after such time the call is automatically declined (refused). */
    int refuse_time = 20;
    /** Automatic answer time in seconds; time<0 corresponds to manual answer mode. */
    private int accept_time = -1;
    /** Automatic hangup time (call duartion) in seconds; time<=0 corresponds to manual hangup mode. */
    private int hangup_time = -1;
    /** Automatic call transfer time in seconds; time<0 corresponds to no auto transfer mode. */
    private int transfer_time = -1;
    /** Automatic re-inviting time in seconds; time<0 corresponds to no auto re-invite mode.  */
    private int re_invite_time = -1;

    /** Redirect incoming call to the specified url.
     * Use value 'NONE' for not redirecting incoming calls (or let it undefined). */
    private NameAddress redirect_to = null;

    /** Transfer calls to the specified url.
     * Use value 'NONE' for not transferring calls (or let it undefined). */
    private NameAddress transfer_to = null;

    /** No offer in the invite */
    boolean no_offer = false;
    /** Do not use prompt */
    private boolean no_prompt = false;

    /** Whether using audio */
    boolean audio = true;
    /** Whether using video */
    boolean video = false;

    /** Whether looping the received media streams back to the sender. */
    private boolean loopback = false;
    /** Whether playing in receive only mode */
    boolean recv_only = false;
    /** Whether playing in send only mode */
    boolean send_only = false;
    /** Whether playing a test tone in send only mode */
    private boolean send_tone = false;

    /** Media address (use it if you want to use a media address different from the via address) */
    String media_addr = null;
    /** First media port (use it if you want to use media ports different from those specified in mediaDescs) */
    int media_port = -1;

    // ******************** undocumented parametes ********************

    /** Whether running the UAS (User Agent Server), or acting just as UAC (User Agent Client). In the latter case only outgoing calls are supported. */
    boolean ua_server = true;
    /** Whether running an Options Server, that automatically responds to OPTIONS requests. */
    boolean options_server = true;
    /** Whether running an Null Server, that automatically responds to not-implemented requests. */
    private boolean null_server = true;


    // ********************* historical parametes *********************

    /** Default audio port */
    int audio_port = 4000;
    /** Default video port */
    private int video_port = 4002;

    // ************************** costructors *************************

    /** Constructs a void UserAgentProfile */
    public UserAgentProfile(Context context) {

        readAll(context);

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
    void setUnconfiguredAttributes(SipProvider sip_provider) {
        if (registrar==null && proxy!=null) registrar=proxy;
        if (proxy==null && registrar!=null) proxy=registrar;
        if (auth_realm==null && proxy!=null) auth_realm=proxy;
        if (auth_user==null && user!=null) auth_user=user;
        if (ua_address==null && sip_provider!=null)
        {  ua_address=sip_provider.getViaAddress();
            if (sip_provider.getPort()!=SipStack.default_port) ua_address+=":"+sip_provider.getPort();
        }
    }

    // *********************** protected methods **********************

    public void readAll(Context context) {

        SharedPreferences prefs = context.getSharedPreferences("SipStack", Context.MODE_PRIVATE);

        display_name = prefs.getString("display_name", display_name);
        user = prefs.getString("user", user);
        proxy = prefs.getString("proxy", proxy);
        registrar = prefs.getString("registrar", registrar);
        auth_user = prefs.getString("auth_user", auth_user);
        auth_realm = prefs.getString("auth_realm", auth_realm);
        auth_passwd = prefs.getString("auth_passwd", auth_passwd);

        do_register = prefs.getBoolean("do_register", do_register);
        do_unregister = prefs.getBoolean("do_unregister", do_unregister);
        do_unregister_all = prefs.getBoolean("do_unregister_all", do_unregister_all);
        expires = prefs.getInt("expires", expires);
        keepalive_time = prefs.getLong("keepalive_time", keepalive_time);

        String naddr = prefs.getString("call_to", null);
        if (naddr==null || naddr.length()==0 || naddr.equalsIgnoreCase("NONE")) call_to=null;
        else call_to=new NameAddress(naddr);

        naddr = prefs.getString("redirect_to", null);
        if (naddr==null || naddr.length()==0 || naddr.equalsIgnoreCase("NONE")) redirect_to=null;
        else redirect_to=new NameAddress(naddr);

        naddr = prefs.getString("transfer_to", null);
        if (naddr==null || naddr.length()==0 || naddr.equalsIgnoreCase("NONE"))transfer_to=null;
        else transfer_to=new NameAddress(naddr);

        refuse_time = prefs.getInt("refuse_time", refuse_time);
        accept_time = prefs.getInt("accept_time", accept_time);
        hangup_time = prefs.getInt("hangup_time", hangup_time);
        transfer_time = prefs.getInt("transfer_time", transfer_time);
        re_invite_time = prefs.getInt("re_invite_time", re_invite_time);
        no_offer = prefs.getBoolean("no_offer", no_offer);
        no_prompt = prefs.getBoolean("no_prompt", no_prompt);
        loopback = prefs.getBoolean("loopback", loopback);
        recv_only = prefs.getBoolean("recv_only", recv_only);
        send_only = prefs.getBoolean("send_only", send_only);
        send_tone = prefs.getBoolean("send_tone", send_tone);

        media_addr = prefs.getString("media_addr", media_addr);
        media_port = prefs.getInt("media_port", media_port);


        ua_server = prefs.getBoolean("ua_server", ua_server);
        options_server = prefs.getBoolean("options_server", options_server);
        null_server = prefs.getBoolean("null_server", null_server);

    }
}
