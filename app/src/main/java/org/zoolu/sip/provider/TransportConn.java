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


import org.zoolu.sip.message.Message;
import org.zoolu.net.IpAddress;
import java.io.IOException;


/** TransportConn is a generic transport connection for SIP, established between two CO transport entities.
 */
interface TransportConn
{
    /** Gets protocol type */
    public String getProtocol();

    /** Gets the remote IpAddress */
    public IpAddress getRemoteAddress();

    /** Gets the remote port */
    public int getRemotePort();

    /** Gets the last time the TransportConn has been used (in millisconds) */
    public long getLastTimeMillis();

    /** Sends a Message */
    public void sendMessage(Message msg) throws IOException;

    /** Stops and closes the connection */
    public void halt();

}
