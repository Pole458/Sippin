/*
 * Copyright (C) 2005 Luca Veltri - University of Parma - Italy
 *
 * This file is part of MjSip (http://www.mjsip.org)
 *
 * MjSip is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * MjSip is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MjSip; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * Author(s):
 * Luca Veltri (luca.veltri@unipr.it)
 */

package org.zoolu.sip.provider;


import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import org.zoolu.tools.Timer;


/** SipStack collects all static attributes used by the sip stack.
 * <p>
 * SipStack attributes are: the default SIP port, default supported transport protocols,
 * timeouts, log configuration, etc.
 */
public class SipStack {

    // ********************** private attributes **********************

    /** Whether SipStack configuration has been already loaded */
    private static boolean is_init = false;


    // *********************** software release ***********************

    /** Version */
    public static final String version = "1.0";
    /** Release */
    private static final String release = "Sippin "+ version;
    /** Authors */
    public static final String authors = "Luca Veltri - University of Parma (Italy), Paolo D'Alessandro";

    // ************* default sip provider configurations **************

    /** Default SIP port.
     * Note that this is not the port used by the running stack, but simply the standard default SIP port.
     * <br> Normally it sould be set to 5060 as defined by RFC 3261. Using a different value may cause
     * some problems when interacting with other unaware SIP UAs. */
    public static int default_port = 5060;
    /** Default SIP port for TLS transport (SIPS).
     * Note that this is not the port used by the running stack, but simply the standard default SIPS port.
     * <br> Normally it sould be set to 5061 as defined by RFC 3261. Using a different value may cause
     * some problems when interacting with other unaware SIP UAs. */
    static int default_tls_port = 5061;
    /** Default supported transport protocols. */
    static String[] default_transport_protocols = { SipProvider.PROTO_UDP, SipProvider.PROTO_TCP };
    /** Default max number of contemporary open transport connections. */
    static int default_nmax_connections = 32;
    /** Whether adding 'rport' parameter on via header fields of outgoing requests. */
    static boolean use_rport = true;
    /** Whether adding (forcing) 'rport' parameter on via header fields of incoming requests. */
    static boolean force_rport = false;

    // ********************* transaction timeouts *********************

    /** starting retransmission timeout (milliseconds); called T1 in RFC2361; they suggest T1=500ms */
    public static long retransmission_timeout = 500;
    /** maximum retransmission timeout (milliseconds); called T2 in RFC2361; they suggest T2=4sec */
    public static long max_retransmission_timeout = 4000;
    /** transaction timeout (milliseconds); RFC2361 suggests 64*T1=32000ms */
    public static long transaction_timeout = 32000;
    /** clearing timeout (milliseconds); T4 in RFC2361; they suggest T4=5sec */
    public static long clearing_timeout = 5000;

    // ******************** general configurations ********************

    /** default max-forwards value (RFC3261 recommends value 70) */
    private static int max_forwards=70;
    /** Whether using only one thread for all timer instances (less precise but more efficient). */
    private static boolean single_timer=true;
    /** Whether at UAS side automatically sending (by default) a 100 Trying on INVITE. */
    public static boolean auto_trying=true;
    /** Whether 1xx responses create an "early dialog" for methods that create dialog. */
    public static boolean early_dialog=false;
    /** Default 'expires' value in seconds. RFC2361 suggests 3600s as default value. */
    public static int default_expires=3600;
    /** MyUA info included in request messages in the 'User-Agent' header field.
     * Use "NONE" if the 'User-Agent' header filed must not be added. */
    public static String ua_info=release;
    /** Server info included in response messages in the 'Server' header field
     * Use "NONE" if the 'Server' header filed must not be added. */
    public static String server_info=release;

    // ************** registration client configurations **************

    /** starting registration timeout (msecs) after a registration failure due to request timeout */
    public static long regc_min_attempt_timeout=60*1000; // 1min
    /** maximum registration timeout (msecs) after a registration failure due to request timeout */
    public static long regc_max_attempt_timeout=900*1000; // 15min
    /** maximum number of consecutive registration authentication attempts before giving up */
    public static int regc_auth_attempts=3;

    // ************************** extensions **************************

    /** Whether forcing this node to stay within the dialog route as peer,
     * by means of the insertion of a RecordRoute header.
     * This is a non-standard behaviour and is normally not necessary. */
    public static boolean on_dialog_route = false;

    /** Whether using an alternative transaction id that does not include the 'sent-by' value. */
    static boolean alternative_transaction_id = false;

    /* ************************** costructor ************************** */

    /** Inits SipStack from the specified <i>file</i> */
    public static void init(Context context) {

        if(context != null)
            readAll(context);

        // user-agent info
        if (ua_info!=null && (ua_info.length()==0 || ua_info.equalsIgnoreCase("NONE") || ua_info.equalsIgnoreCase("NO-MyUA-INFO")))
            ua_info=null;

        // server info
        if (server_info!=null && (server_info.length()==0 || server_info.equalsIgnoreCase("NONE") || server_info.equalsIgnoreCase("NO-SERVER-INFO")))
            server_info=null;

        // timers
        Timer.SINGLE_THREAD = single_timer;

        is_init = true;
    }

    /** Whether SipStack has been already initialized */
    public static boolean isInit() {
        return is_init;
    }

    private static void readAll(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("SipStack", Context.MODE_PRIVATE);

        // default sip provider configurations
        default_port = prefs.getInt("default_port", default_port);
        default_tls_port = prefs.getInt("default_tls_port", default_tls_port);
        String dtp = prefs.getString("default_transport_protocols", TextUtils.join(",", default_transport_protocols));
        default_transport_protocols = dtp.split(",");
        default_nmax_connections = prefs.getInt("default_nmax_connections", default_nmax_connections);
        use_rport = prefs.getBoolean("use_rport", use_rport);
        force_rport = prefs.getBoolean("force_rport", force_rport);

        // transaction timeouts
        retransmission_timeout = prefs.getLong("retransmission_timeout", retransmission_timeout);
        max_retransmission_timeout = prefs.getLong("max_retransmission_timeout", max_retransmission_timeout);
        transaction_timeout = prefs.getLong("transaction_timeout", transaction_timeout);
        clearing_timeout = prefs.getLong("clearing_timeout", clearing_timeout);

        // general configurations
        max_forwards = prefs.getInt("max_forwards", max_forwards);
        single_timer = prefs.getBoolean("single_timer", single_timer);
        auto_trying = prefs.getBoolean("auto_trying", auto_trying);
        early_dialog = prefs.getBoolean("early_dialog", early_dialog);
        default_expires = prefs.getInt("default_expires", default_expires);
        ua_info = prefs.getString("ua_info", ua_info);
        server_info = prefs.getString("server_info", server_info);

        // registration client configurations
        regc_min_attempt_timeout = prefs.getLong("regc_min_attempt_timeout", regc_min_attempt_timeout);
        regc_max_attempt_timeout = prefs.getLong("regc_max_attempt_timeout", regc_max_attempt_timeout);
        regc_auth_attempts = prefs.getInt("regc_auth_attempts", regc_auth_attempts);

        // extensions
        on_dialog_route = prefs.getBoolean("on_dialog_route", on_dialog_route);
        alternative_transaction_id = prefs.getBoolean("alternative_transaction_id", alternative_transaction_id);
    }

}
