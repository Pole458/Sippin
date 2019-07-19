/*
 * Copyright (C) 2009 Luca Veltri - University of Parma - Italy
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



import android.util.Log;
import org.zoolu.net.*;
import org.zoolu.sip.message.Message;
import org.zoolu.tools.*;
import java.util.Hashtable;
import java.util.Enumeration;
import java.io.IOException;



/** ConnectedTransport is a generic Connection Oriented (CO) transport service for SIP.
 */
abstract class ConnectedTransport implements Transport, TcpServerListener, TransportConnListener
{

    private static final String TAG = "Sip: ConnectedTransport";
    /** Max number of (contemporary) open connections */
    int nmax_connections=0;

    /** Table of active connections, as table:TransportConnId-->TransportConn */
    Hashtable connections=null;

    /** Transport listener */
    TransportListener listener;




    /** Creates a new ConnectedTransport */
    public ConnectedTransport(int local_port, int nmax_connections) throws IOException
    {  this.nmax_connections=nmax_connections;
        connections=new Hashtable();
    }


    /** Gets protocol type */
    abstract public String getProtocol();


    /** Creates a proper transport connection to the remote end-point. */
    abstract protected TransportConn createTransportConn(IpAddress dest_ipaddr, int dest_port)  throws IOException;


    /** Sets transport listener */
    public void setListener(TransportListener listener)
    {  this.listener=listener;
    }


    /** Sends a Message through the connection. Parameters <i>dest_addr</i>/<i>dest_addr</i>
     * are not used, and the message is addressed to the connection remote peer.
     * <p>Better use sendMessage(Message msg) method instead. */
    public TransportConn sendMessage(Message msg, IpAddress dest_ipaddr, int dest_port, int ttl) throws IOException
    {  TransportConnId conn_id=new TransportConnId(getProtocol(),dest_ipaddr,dest_port);
        if (!connections.containsKey(conn_id)) {
            Log.v(TAG, "no active connection found matching "+conn_id);
            Log.v(TAG, "open "+getProtocol()+" connection to "+dest_ipaddr+":"+dest_port);
            TransportConn conn=null;
            try
            {  conn=createTransportConn(dest_ipaddr,dest_port);
            }
            catch (Exception e)
            {  Log.v(TAG, "connection setup FAILED");
                return null;
            }
            Log.v(TAG, "connection "+conn+" opened");
            addConnection(conn);
        }
        else
        {  Log.v(TAG, "active connection found matching "+conn_id);
        }
        TransportConn conn=(TransportConn)connections.get(conn_id);
        if (conn!=null)
        {  Log.v(TAG, "sending data through conn "+conn);
            try
            {  conn.sendMessage(msg);
                return conn;
            }
            catch (IOException e)
            {  Log.e(TAG, "", e);
                return null;
            }
        }
        else
        {  // this point has not to be reached
            Log.v(TAG, "ERROR: conn "+conn_id+" not found: abort.");
            return null;
        }
    }


    /** Sends the message <i>msg</i> using the specified connection. */
    public TransportConn sendMessage(Message msg, TransportConnId conn_id)
    {  if (conn_id!=null && connections.containsKey(conn_id))
    {  // connection exists
        Log.v(TAG, "active connection found matching "+conn_id);
        TransportConn conn=(TransportConn)connections.get(conn_id);
        try
        {  conn.sendMessage(msg);
            return conn;
        }
        catch (Exception e)
        {  Log.e(TAG, "", e);
        }
    }
        //else
        Log.v(TAG, "no active connection found matching "+conn_id);
        return null;
    }


    /** Stops running */
    public void halt()
    {  // close all connections
        if (connections!=null)
        {  Log.v(TAG, "connections are going down");
            for (Enumeration e=connections.elements(); e.hasMoreElements(); )
            {  TransportConn c=(TransportConn)e.nextElement();
                c.halt();
            }
            connections=null;
        }
    }


    /** From TcpServerListener. When a new incoming connection is established */
    abstract public void onIncomingConnection(TcpServer tcp_server, TcpSocket socket);



    /** From TcpServerListener. When TcpServer terminates. */
    public void onServerTerminated(TcpServer tcp_server, Exception error)
    {  Log.v(TAG, "tcp server "+tcp_server+" terminated");
    }


    /** From TransportConnListener. When a new SIP message is received. */
    public void onReceivedMessage(TransportConn conn, Message msg)
    {  if (listener!=null) listener.onReceivedMessage(this,msg);
    }


    /** From TransportConnListener. When TransportConn terminates. */
    public void onConnectionTerminated(TransportConn conn, Exception error)
    {  TransportConnId conn_id=new TransportConnId(conn);
        removeConnection(conn_id);
        if (error!=null) Log.e(TAG, "", error);
    }


    /** Adds a new Connection */
    protected synchronized void addConnection(TransportConn conn)
    {  TransportConnId conn_id=new TransportConnId(conn);
        if (connections.containsKey(conn_id))
        {  // remove the previous connection
            Log.v(TAG, "trying to add the already established connection "+conn_id);
            Log.v(TAG, "connection "+conn_id+" will be replaced");
            TransportConn old_conn=(TransportConn)connections.get(conn_id);
            old_conn.halt();
            connections.remove(conn_id);
        }
        else
        if (connections.size()>=nmax_connections)
        {  // remove the older unused connection
            Log.v(TAG, "reached the maximum number of connection: removing the older unused connection");
            long older_time=System.currentTimeMillis();
            TransportConnId older_id=null;
            for (Enumeration e=connections.elements(); e.hasMoreElements(); )
            {  TransportConn co=(TransportConn)e.nextElement();
                if (co.getLastTimeMillis()<older_time) older_id=new TransportConnId(co);
            }
            if (older_id!=null) removeConnection(older_id);
        }
        connections.put(conn_id,conn);
        conn_id=new TransportConnId(conn);
        conn=(TransportConn)connections.get(conn_id);
        // DEBUG log:
        Log.v(TAG, "active connenctions:");
        for (Enumeration e=connections.keys(); e.hasMoreElements(); )
        {  TransportConnId id=(TransportConnId)e.nextElement();
            Log.v(TAG, "conn-id="+id+": "+((TransportConn)connections.get(id)).toString());
        }
    }


    /** Removes a Connection */
    protected synchronized void removeConnection(TransportConnId conn_id)
    {  if (connections.containsKey(conn_id))
    {  TransportConn conn=(TransportConn)connections.get(conn_id);
        conn.halt();
        connections.remove(conn_id);
        // DEBUG log:
        Log.v(TAG, "active connenctions:");
        for (Enumeration e=connections.elements(); e.hasMoreElements(); )
        {  TransportConn co=(TransportConn)e.nextElement();
            Log.v(TAG, "conn "+co.toString());
        }
    }
    }

}
