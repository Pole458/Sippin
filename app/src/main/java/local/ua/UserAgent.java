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

import local.media.*;
import org.zoolu.sip.call.*;
import org.zoolu.sip.address.*;
import org.zoolu.sip.provider.*;
import org.zoolu.sip.message.Message;
import org.zoolu.sdp.*;
import org.zoolu.net.SocketAddress;
import org.zoolu.tools.*;

import java.net.InetAddress;
import java.util.Enumeration;
import java.util.Vector;



/** Simple SIP call agent (signaling and media).
 * It supports both audio and video sessions, by means of embedded media applications
 * that can use the default Java sound support (javax.sound.sampled.AudioSystem)
 * and/or the Java Multimedia Framework (JMF).
 * <p>
 * As media applications it can also use external audio/video tools.
 * Currently only support for RAT (Robust Audio Tool) and VIC has been implemented.
 */
public class UserAgent extends CallListenerAdapter implements CallWatcherListener, RegistrationClientListener, TimerListener {

    private static final String TAG = "Sip:UserAgent";

    private AudioStream audioStream;

    // ***************************** attributes ****************************

    /** UserAgentProfile */
    protected UserAgentProfile ua_profile;

    /** SipProvider */
    protected SipProvider sip_provider;

    /** RegistrationClient */
    private RegistrationClient rc=null;

    /** SipKeepAlive daemon */
    private SipKeepAlive keep_alive;

    /** Call */
    protected ExtendedCall call;

    /** Call transfer */
    private ExtendedCall call_transfer;

    /** UAS */
    private CallWatcher ua_server;

    /** OptionsServer */
    private OptionsServer options_server;

    /** MediaAgent */
    private MediaAgent media_agent;

    /** List of active media sessions */
    private Vector<String> media_sessions = new Vector<>();

    /** Current local media descriptions, as Vector of MediaDesc */
    private Vector media_descs = null;

    /** MyUA listener */
    protected UserAgentListener listener = null;

    /** Response timeout */
    private Timer response_to = null;

    /** Whether the outgoing call is already in progress */
    private boolean progress;

    /** Whether the outgoing call is already ringing */
    private boolean ringing;

    // **************************** constructors ***************************

    /** Creates a new MyUA. */
    public UserAgent(SipProvider sip_provider, UserAgentProfile ua_profile, UserAgentListener listener) {


        this.sip_provider=sip_provider;
        this.listener=listener;
        this.ua_profile=ua_profile;

        // update user profile information
        ua_profile.setUnconfiguredAttributes(sip_provider);

        // start call server (that corresponds to the UAS part)
        if (ua_profile.ua_server)
            ua_server = new CallWatcher(sip_provider,this);

        // start OPTIONS server
        if (ua_profile.options_server)
            options_server = new OptionsServer(sip_provider,"INVITE, ACK, CANCEL, OPTIONS, BYE","application/sdp");

        // init media agent
        media_agent = new MediaAgent(ua_profile);

    }

    // ************************** private methods **************************


    public int getAudioStreamPort() {
        if(audioStream == null) startAudioStream();
        return audioStream.getLocalPort();
    }

    private void startAudioStream() {
        try {
            audioStream = new AudioStream(InetAddress.getByName(ua_profile.getUserURI().getAddress().getHost()));
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }


    /** Inits the RegistrationClient */
    private void initRegistrationClient() {
        rc = new RegistrationClient(sip_provider, new SipURL(ua_profile.registrar), ua_profile.getUserURI(), ua_profile.getUserURI(),
                ua_profile.auth_user, ua_profile.auth_realm, ua_profile.auth_passwd,this);
    }

    /** Gets SessionDescriptor from Vector of MediaSpec. */
    private SessionDescriptor getSessionDescriptor(Vector media_descs) {

        String owner=ua_profile.user;
        String media_addr=(ua_profile.media_addr!=null)? ua_profile.media_addr : sip_provider.getViaAddress();
        int media_port=ua_profile.media_port;
        SessionDescriptor sd=new SessionDescriptor(owner,media_addr);

        for (int i = 0; i < media_descs.size(); i++) {
            MediaDesc md=(MediaDesc)media_descs.elementAt(i);
            // check if audio or video have been disabled
            if (md.getMedia().equalsIgnoreCase("audio") && !ua_profile.audio) continue;
            if (md.getMedia().equalsIgnoreCase("video") && !ua_profile.video) continue;
            // else
            if (media_port>0)
            {  // override the media_desc port
                md.setPort(media_port);
                media_port+=2;
            }
            sd.addMediaDescriptor(md.toMediaDescriptor());
        }
        return sd;
    }

    /** Gets a NameAddress based on an input string.
     * The input string can be a:
     * <br/> - user name,
     * <br/> - an address of type <i>user@address</i>,
     * <br/> - a complete address in the form of <i>"Name" &lt;sip:user@address&gt;</i>,
     * <p/>
     * In the former case, a SIP URL is constructed using the proxy address
     * if available. */
    private NameAddress completeNameAddress(String str) {
        if (str.contains("<sip:") || str.contains("<sips:"))
            return new NameAddress(str);
        else {
            SipURL url = completeSipURL(str);
            return new NameAddress(url);
        }
    }

    /** Gets a SipURL based on an input string. */
    private SipURL completeSipURL(String str) {
        // in case it is passed only the user field, add "@" + proxy address
        if (ua_profile.proxy!=null && !str.startsWith("sip:") && !str.startsWith("sips:") && !str.contains("@") && !str.contains(".") && !str.contains(":")) {
            // may be it is just the user name..
            return new SipURL(str,ua_profile.proxy);
        }
        else return new SipURL(str);
    }


    // *************************** public methods **************************

    /** Register with the registrar server
     * @param expire_time expiration time in seconds */
    public void register(int expire_time) {
        if (rc.isRegistering()) rc.halt();
        rc.register(expire_time);
    }

    /** Periodically registers the contact address with the registrar server.
     * @param expire_time expiration time in seconds
     * @param renew_time renew time in seconds
     * @param keepalive_time keep-alive packet rate (inter-arrival time) in milliseconds */
    public void loopRegister(int expire_time, int renew_time, long keepalive_time) {

        // create registration client
        if (rc == null) initRegistrationClient();

        // stop previous operation
        if (rc.isRegistering()) rc.halt();

        // start registering
        rc.loopRegister(expire_time,renew_time);

        // keep-alive
        if (keepalive_time>0) {
            SipURL target_url = (sip_provider.hasOutboundProxy()) ? sip_provider.getOutboundProxy() : rc.getTarget().getAddress();
            String target_host=target_url.getHost();
            int target_port=target_url.getPort();
            if (target_port<0) target_port=SipStack.default_port;
            SocketAddress target_soaddr=new SocketAddress(target_host,target_port);
            if (keep_alive!=null && keep_alive.isRunning()) keep_alive.halt();
            keep_alive=new SipKeepAlive(sip_provider,target_soaddr,null, keepalive_time);
        }
    }

    /** Unregisters with the registrar server */
    public void unregister() {
        // create registration client
        if (rc==null)
            initRegistrationClient();

        // stop registering
        if (keep_alive!=null && keep_alive.isRunning())
            keep_alive.halt();

        if (rc.isRegistering())
            rc.halt();

        // unregister
        rc.unregister();
    }


    /** Unregister all contacts with the registrar server */
    public void unregisterall() {
        // create registration client
        if (rc==null)
            initRegistrationClient();
        // stop registering
        if (keep_alive!=null && keep_alive.isRunning())
            keep_alive.halt();
        if (rc.isRegistering())
            rc.halt();
        // unregister
        rc.unregisterall();
    }


    /** Makes a new call (acting as UAC). */
    public void call(String callee) {
        call(callee,null);
    }

    /** Makes a new call (acting as UAC) with specific media description (Vector of MediaDesc). */
    public void call(String callee, Vector media_descs) {
        // in case of incomplete url (e.g. only 'user' is present), try to complete it
        call(completeNameAddress(callee), media_descs);
    }


    /** Makes a new call (acting as UAC). */
    public void call(NameAddress callee) {
        call(callee,null);
    }


    /** Makes a new call (acting as UAC) with specific media description (Vector of MediaDesc). */
    public void call(NameAddress callee, Vector media_descs) {
        // new media description
        if (media_descs == null)
            media_descs = ua_profile.media_descs;
        this.media_descs = media_descs;
        // new call
        call = new ExtendedCall(sip_provider,ua_profile.getUserURI(),ua_profile.auth_user,ua_profile.auth_realm,ua_profile.auth_passwd,this);
        if(ua_profile.no_offer)
            call.call(callee);
        else {
            SessionDescriptor local_sdp=getSessionDescriptor(media_descs);
            call.call(callee, local_sdp.toString());
        }
        progress = false;
        ringing = false;
    }

    /** Closes an ongoing, incoming, or pending call. */
    public void hangup() {

        // response timeout
        if (response_to!=null) response_to.halt();

        closeMediaSessions();

        if (call!=null)
            call.hangup();

        call=null;
    }


    /** Accepts an incoming call. */
    public void accept() {
        accept(null);
    }


    /** Accepts an incoming call with specific media description (Vector of MediaDesc). */
    public void accept(Vector media_descs) {

        // response timeout
        if (response_to!=null)
            response_to.halt();
        // return if no active call
        if (call == null)
            return;

        if (media_descs == null)
            media_descs = ua_profile.media_descs;
        this.media_descs = media_descs;
        // new sdp
        SessionDescriptor local_sdp=getSessionDescriptor(media_descs);
        SessionDescriptor remote_sdp=new SessionDescriptor(call.getRemoteSessionDescriptor());
        SessionDescriptor new_sdp=new SessionDescriptor(local_sdp.getOrigin(),remote_sdp.getSessionName(),local_sdp.getConnection(),remote_sdp.getTime());
        new_sdp.addMediaDescriptors(local_sdp.getMediaDescriptors());
        new_sdp=OfferAnswerModel.makeSessionDescriptorProduct(new_sdp,remote_sdp);
        // accept
        call.accept(new_sdp.toString());
    }


    /** Redirects an incoming call. */
    public void redirect(String redirect_to) {
        // in case of incomplete url (e.g. only 'user' is present), try to complete it
        redirect(completeNameAddress(redirect_to));
    }


    /** Redirects an incoming call. */
    public void redirect(NameAddress redirect_to) {

        // response timeout
        if (response_to!=null) response_to.halt();

        if (call!=null) call.redirect(redirect_to);
    }


    /** Modifies the current session. It re-invites the remote party changing the contact URL and SDP. */
    public void modify(String body) {
        if (call!=null && call.isActive()) {
            Log.v(TAG, "RE-INVITING/MODIFING");
            call.modify(body);
        }
    }

    /** Transfers the current call to a remote UserAgent. */
    public void transfer(String transfer_to)
    {  // in case of incomplete url (e.g. only 'user' is present), try to complete it
        transfer(completeNameAddress(transfer_to));
    }


    /** Transfers the current call to a remote UserAgent. */
    public void transfer(NameAddress transfer_to)
    {  if (call!=null && call.isActive())
    {  Log.v(TAG, "REFER/TRANSFER");
        call.transfer(transfer_to);
    }
    }


    // ********************** protected methods **********************

    /** Starts media sessions (audio and/or video). */
    private void startMediaSessions(AudioStream audioStream) {

        // exit if the media application is already running
        if (media_sessions.size() > 0) {
            Log.v(TAG, "DEBUG: media sessions already active");
            return;
        }

        // get local and remote rtp addresses and ports
        SessionDescriptor local_sdp = new SessionDescriptor(call.getLocalSessionDescriptor());
        SessionDescriptor remote_sdp = new SessionDescriptor(call.getRemoteSessionDescriptor());

        String local_address = local_sdp.getConnection().getAddress();
        String remote_address = remote_sdp.getConnection().getAddress();

        // calculate media descriptor product
        Vector md_list = OfferAnswerModel.makeMediaDescriptorProduct(local_sdp.getMediaDescriptors(), remote_sdp.getMediaDescriptors());
        // select the media direction (send_only, recv_ony, fullduplex)

        FlowSpec.Direction dir = FlowSpec.FULL_DUPLEX;
        if (ua_profile.recv_only)
            dir = FlowSpec.RECV_ONLY;
        else if (ua_profile.send_only)
            dir = FlowSpec.SEND_ONLY;
        // for each media

        for (Enumeration ei = md_list.elements(); ei.hasMoreElements(); ) {
            MediaField md = ((MediaDescriptor)ei.nextElement()).getMedia();
            String media=md.getMedia();
            // local and remote ports
            int local_port=md.getPort();
            int remote_port=remote_sdp.getMediaDescriptor(media).getMedia().getPort();
            remote_sdp.removeMediaDescriptor(media);
            // media and flow specifications
            String transport = md.getTransport();
            String format = (String)md.getFormatList().elementAt(0);
            int avp = Integer.parseInt(format);
            MediaSpec media_spec=null;


            for (int i=0; i<media_descs.size() && media_spec==null; i++) {
                MediaDesc media_desc=(MediaDesc)media_descs.elementAt(i);
                if (media_desc.getMedia().equalsIgnoreCase(media)) {
                    Vector media_specs=media_desc.getMediaSpecs();
                    for (int j=0; j<media_specs.size() && media_spec==null; j++) {
                        MediaSpec ms=(MediaSpec)media_specs.elementAt(j);
                        if (ms.getAVP()==avp) media_spec=ms;
                    }
                }
            }

            if (local_port!=0 && remote_port!=0 && media_spec!=null) {
                FlowSpec flow_spec = new FlowSpec(media_spec, local_port, remote_address, remote_port, dir);
                Log.v(TAG, media+" format: "+flow_spec.getMediaSpec().getCodec());
                boolean success=media_agent.startMediaSession(flow_spec, audioStream);
                if (success) {
                    media_sessions.addElement(media);
                    if (listener!=null)
                        listener.onUaMediaSessionStarted(this,media,format);
                }
            } else {
                Log.v(TAG, "DEBUG: media session cannot be started (local_port="+local_port+", remote_port="+remote_port+", media_spec="+media_spec+").");
            }
        }
    }

    /** Closes media sessions.  */
    private void closeMediaSessions() {
        for (int i=0; i<media_sessions.size(); i++) {
            String media= media_sessions.elementAt(i);
            media_agent.stopMediaSession(media);
            if (listener!=null) listener.onUaMediaSessionStopped(this,media);
        }
        media_sessions.removeAllElements();
    }

    // ************************* RA callbacks ************************

    /** From RegistrationClientListener. When it has been successfully (un)registered. */
    public void onRegistrationSuccess(RegistrationClient rc, NameAddress target, NameAddress contact, String result)
    {  Log.v(TAG, "Registration success: "+result);
        if (listener!=null) listener.onUaRegistrationSucceeded(this,result);
    }

    /** From RegistrationClientListener. When it failed on (un)registering. */
    public void onRegistrationFailure(RegistrationClient rc, NameAddress target, NameAddress contact, String result)
    {  Log.v(TAG, "Registration failure: "+result);
        if (listener!=null) listener.onUaRegistrationFailed(this,result);
    }


    // ************************ Call callbacks ***********************

    /** From CallWatcherListener. When the CallWatcher receives a new invite request that creates a new Call. */
    public void onNewIncomingCall(CallWatcher call_watcher, ExtendedCall call, NameAddress callee, NameAddress caller, String sdp, Message invite) {
        Log.v(TAG, "onNewIncomingCall()");
        if (this.call!=null && !this.call.isClosed()) {
            Log.v(TAG, "LOCALLY BUSY: INCOMING CALL REFUSED");
            call.refuse();
            return;
        }
        // else
        Log.v(TAG, "INCOMING");
        this.call=call;
        call.ring();

        // response timeout
        if (ua_profile.refuse_time>=0) response_to=new Timer(ua_profile.refuse_time*1000,this);
        response_to.start();

        Vector<MediaDesc> media_descs=null;
        if (sdp!=null)
        {  Vector md_list=(new SessionDescriptor(sdp)).getMediaDescriptors();
            media_descs=new Vector<MediaDesc>(md_list.size());
            for (int i=0; i<md_list.size(); i++) media_descs.addElement(new MediaDesc((MediaDescriptor)md_list.elementAt(i)));
        }
        if (listener!=null) listener.onUaIncomingCall(this,callee,caller,media_descs);
    }


    /** From CallListener. Callback function called when arriving a new INVITE method (incoming call) */
    public void onCallInvite(Call call, NameAddress callee, NameAddress caller, String sdp, Message invite)
    {  Log.v(TAG, "onCallInvite()");
        if (call!=this.call) {  Log.v(TAG, "NOT the current call");  return;  }
        // never called (the method onNewInocomingCall() is called instead): do nothing.
    }


    /** From CallListener. Callback function called when arriving a new Re-INVITE method (re-inviting/call modify) */
    public void onCallModify(Call call, String sdp, Message invite)
    {  Log.v(TAG, "onCallModify()");
        if (call!=this.call) {  Log.v(TAG, "NOT the current call");  return;  }
        Log.v(TAG, "RE-INVITE/MODIFY");
        // to be implemented.
        // currently it simply accepts the session changes (see method onCallModify() in CallListenerAdapter)
        super.onCallModify(call,sdp,invite);
    }


    /** From CallListener. Callback function called when arriving a 183 Session Progress */
    public void onCallProgress(Call call, Message resp)
    {  Log.v(TAG, "onCallProgress()");
        if (call!=this.call && call!=call_transfer) {  Log.v(TAG, "NOT the current call");  return;  }
        if (!progress)
        {  Log.v(TAG, "PROGRESS");
            progress=true;

            if (listener!=null) listener.onUaCallProgress(this);
        }
    }


    /** From CallListener. Callback function that may be overloaded (extended). Called when arriving a 180 Ringing */
    public void onCallRinging(Call call, Message resp)
    {  Log.v(TAG, "onCallRinging()");
        if (call!=this.call && call!=call_transfer) {  Log.v(TAG, "NOT the current call");  return;  }
        if (!ringing)
        {  Log.v(TAG, "RINGING");
            ringing=true;


            if (listener!=null) listener.onUaCallRinging(this);
        }
    }


    /** From CallListener. Callback function called when arriving a 2xx (call accepted) */
    public void onCallAccepted(Call call, String sdp, Message resp)
    {  Log.v(TAG, "onCallAccepted()");
        if (call!=this.call && call!=call_transfer) {  Log.v(TAG, "NOT the current call");  return;  }
        Log.v(TAG, "ACCEPTED/CALL");
        if (ua_profile.no_offer)
        {  // new sdp
            SessionDescriptor local_sdp=getSessionDescriptor(media_descs);
            SessionDescriptor remote_sdp=new SessionDescriptor(sdp);
            SessionDescriptor new_sdp=new SessionDescriptor(local_sdp.getOrigin(),remote_sdp.getSessionName(),local_sdp.getConnection(),remote_sdp.getTime());
            new_sdp.addMediaDescriptors(local_sdp.getMediaDescriptors());
            new_sdp=OfferAnswerModel.makeSessionDescriptorProduct(new_sdp,remote_sdp);
            // answer with the local sdp
            call.ackWithAnswer(new_sdp.toString());
        }

        if (listener!=null) listener.onUaCallAccepted(this);

        startMediaSessions(audioStream);

        if (call==call_transfer)
        {  this.call.notify(resp);
        }
    }


    /** From CallListener. Callback function called when arriving an ACK method (call confirmed) */
    public void onCallConfirmed(Call call, String sdp, Message ack)
    {  Log.v(TAG, "onCallConfirmed()");
        if (call!=this.call) {  Log.v(TAG, "NOT the current call");  return;  }
        Log.v(TAG, "CONFIRMED/CALL");

        startMediaSessions(audioStream);
    }


    /** From CallListener. Callback function called when arriving a 2xx (re-invite/modify accepted) */
    public void onCallReInviteAccepted(Call call, String sdp, Message resp)
    {  Log.v(TAG, "onCallReInviteAccepted()");
        if (call!=this.call) {  Log.v(TAG, "NOT the current call");  return;  }
        Log.v(TAG, "RE-INVITE-ACCEPTED/CALL");
    }


    /** From CallListener. Callback function called when arriving a 4xx (re-invite/modify failure) */
    public void onCallReInviteRefused(Call call, String reason, Message resp)
    {  Log.v(TAG, "onCallReInviteRefused()");
        if (call!=this.call) {  Log.v(TAG, "NOT the current call");  return;  }
        Log.v(TAG, "RE-INVITE-REFUSED ("+reason+")/CALL");
        if (listener!=null) listener.onUaCallFailed(this,reason);
    }


    /** From CallListener. Callback function called when arriving a 4xx (call failure) */
    public void onCallRefused(Call call, String reason, Message resp)
    {  Log.v(TAG, "onCallRefused()");
        if (call!=this.call && call!=call_transfer) {  Log.v(TAG, "NOT the current call");  return;  }
        Log.v(TAG, "REFUSED ("+reason+")");
        if (call==call_transfer)
        {  this.call.notify(resp);
            call_transfer=null;
        }
        else this.call=null;

        if (listener!=null) listener.onUaCallFailed(this,reason);
    }


    /** From CallListener. Callback function called when arriving a 3xx (call redirection) */
    public void onCallRedirected(Call call, String reason, Vector contact_list, Message resp)
    {  Log.v(TAG, "onCallRedirected()");
        if (call!=this.call) {  Log.v(TAG, "NOT the current call");  return;  }
        Log.v(TAG, "REDIRECTION ("+reason+")");
        NameAddress first_contact=new NameAddress((String)contact_list.elementAt(0));
        call.call(first_contact);
    }


    /** From CallListener. Callback function called when arriving a CANCEL request */
    public void onCallCancel(Call call, Message cancel)
    {  Log.v(TAG, "onCallCancel()");
        if (call!=this.call) {  Log.v(TAG, "NOT the current call");  return;  }
        Log.v(TAG, "CANCEL");
        this.call=null;
        // response timeout
        if (response_to!=null) response_to.halt();

        if (listener!=null) listener.onUaCallCancelled(this);
    }


    /** From CallListener. Callback function called when arriving a BYE request */
    public void onCallBye(Call call, Message bye)
    {  Log.v(TAG, "onCallBye()");
        if (call!=this.call && call!=call_transfer) {  Log.v(TAG, "NOT the current call");  return;  }
        if (call!=call_transfer && call_transfer!=null)
        {  Log.v(TAG, "CLOSE PREVIOUS CALL");
            this.call=call_transfer;
            call_transfer=null;
            return;
        }
        // else
        Log.v(TAG, "CLOSE");
        this.call=null;
        closeMediaSessions();

        if (listener!=null) listener.onUaCallClosed(this);
    }


    /** From CallListener. Callback function called when arriving a response after a BYE request (call closed) */
    public void onCallClosed(Call call, Message resp)
    {  Log.v(TAG, "onCallClosed()");
        if (call!=this.call) {  Log.v(TAG, "NOT the current call");  return;  }
        Log.v(TAG, "CLOSE/OK");
        if (listener!=null) listener.onUaCallClosed(this);
    }

    /** Callback function called when the invite expires */
    public void onCallTimeout(Call call)
    {  Log.v(TAG, "onCallTimeout()");
        if (call!=this.call) {  Log.v(TAG, "NOT the current call");  return;  }
        Log.v(TAG, "NOT FOUND/TIMEOUT");
        int code=408;
        String reason="Request Timeout";
        if (call==call_transfer)
        {  this.call.notify(code,reason);
            call_transfer=null;
        }


        if (listener!=null) listener.onUaCallFailed(this,reason);
    }


    // ******************* ExtendedCall callbacks ********************

    /** From ExtendedCallListener. Callback function called when arriving a new REFER method (transfer request) */
    public void onCallTransfer(ExtendedCall call, NameAddress refer_to, NameAddress refered_by, Message refer)
    {  Log.v(TAG, "onCallTransfer()");
        if (call!=this.call) {  Log.v(TAG, "NOT the current call");  return;  }
        Log.v(TAG, "transfer to "+refer_to.toString());
        call.acceptTransfer();
        call_transfer=new ExtendedCall(sip_provider,ua_profile.getUserURI(),this);
        call_transfer.call(refer_to,getSessionDescriptor(media_descs).toString());
    }

    /** From ExtendedCallListener. Callback function called when a call transfer is accepted. */
    public void onCallTransferAccepted(ExtendedCall call, Message resp)
    {  Log.v(TAG, "onCallTransferAccepted()");
        if (call!=this.call) {  Log.v(TAG, "NOT the current call");  return;  }
        Log.v(TAG, "transfer accepted");
    }

    /** From ExtendedCallListener. Callback function called when a call transfer is refused. */
    public void onCallTransferRefused(ExtendedCall call, String reason, Message resp)
    {  Log.v(TAG, "onCallTransferRefused()");
        if (call!=this.call) {  Log.v(TAG, "NOT the current call");  return;  }
        Log.v(TAG, "transfer refused");
    }

    /** From ExtendedCallListener. Callback function called when a call transfer is successfully completed */
    public void onCallTransferSuccess(ExtendedCall call, Message notify)
    {  Log.v(TAG, "onCallTransferSuccess()");
        if (call!=this.call) {  Log.v(TAG, "NOT the current call");  return;  }
        Log.v(TAG, "transfer successed");
        call.hangup();
        if (listener!=null) listener.onUaCallTransferred(this);
    }

    /** From ExtendedCallListener. Callback function called when a call transfer is NOT sucessfully completed */
    public void onCallTransferFailure(ExtendedCall call, String reason, Message notify)
    {  Log.v(TAG, "onCallTransferFailure()");
        if (call!=this.call) {  Log.v(TAG, "NOT the current call");  return;  }
        Log.v(TAG, "transfer failed");
    }

    // *********************** Timer callbacks ***********************

    /** When the Timer exceeds. */
    public void onTimeout(Timer t) {
        if (response_to == t) {
            Log.v(TAG, "response time expired: incoming call declined");
            if (call != null)
                call.refuse();
        }
    }

}
