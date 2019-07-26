/*
 * Copyright (C) 2006 Luca Veltri - University of Parma - Italy
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

package org.zoolu.net;

import android.net.wifi.WifiManager;
import android.util.Log;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;


/** IpAddress is an IP address.
 */
public class IpAddress {

    public static final String TAG = "Sip: IpAddress";

    /** The host address/name */
    private String address;

    /** The InetAddress */
    private InetAddress inet_address;

    // ********************* Protected *********************

    /** Creates an IpAddress */
    IpAddress(InetAddress iaddress) {
        init(null, iaddress);
    }

    /** Inits the IpAddress */
    private void init(String address, InetAddress iaddress) {
        this.address=address;
        this.inet_address=iaddress;
    }

    /** Gets the InetAddress */
    InetAddress getInetAddress() {
        if (inet_address==null) try { inet_address=InetAddress.getByName(address); } catch (java.net.UnknownHostException e) {}
        return inet_address;
    }

    // ********************** Public ***********************

    /** Creates an IpAddress */
    public IpAddress(String address) {
        init(address,null);
    }

    /** Creates an IpAddress */
    public IpAddress(IpAddress ipaddr) {
        init(ipaddr.address,ipaddr.inet_address);
    }

    /** Makes a copy */
    public Object clone()
    {  return new IpAddress(this);
    }

    /** Whether it is equal to Object <i>obj</i> */
    public boolean equals(Object obj)
    {  try
    {  IpAddress ipaddr=(IpAddress)obj;
        if (!toString().equals(ipaddr.toString())) return false;
        return true;
    }
    catch (Exception e) {  return false;  }
    }

    /** Gets a String representation of the Object */
    public String toString()
    {  if (address==null && inet_address!=null) address = inet_address.getHostAddress();
        return address;
    }


    // *********************** Static ***********************

    /** Gets the IpAddress for a given fully-qualified host name. */
    public static IpAddress getByName(String host_addr) throws java.net.UnknownHostException
    {  InetAddress iaddr=InetAddress.getByName(host_addr);
        return new IpAddress(iaddr);
    }


    /** Detects the default IP address of this host. */
    public static IpAddress getLocalHostAddress() {

        try {

            Enumeration<NetworkInterface> networks = NetworkInterface.getNetworkInterfaces();
            while (networks.hasMoreElements()) {
                Enumeration<InetAddress> iaddrs = networks.nextElement().getInetAddresses();
                while (iaddrs.hasMoreElements()) {
                    InetAddress iaddr = iaddrs.nextElement();
                    if(!iaddr.isLoopbackAddress() && iaddr instanceof Inet4Address && isPrivateIP(iaddr.getHostAddress()))
                        return new IpAddress(iaddr);
                }
            }

        } catch (SocketException e) {
            e.printStackTrace();
            return null;
        }

        return null;

    }

    private static boolean isPrivateIP(String ipAddress) {
        boolean isValid = false;

        if (ipAddress != null && !ipAddress.isEmpty()) {
            String[] ip = ipAddress.split("\\.");
            short[] ipNumber = new short[] {
                    Short.parseShort(ip[0]),
                    Short.parseShort(ip[1]),
                    Short.parseShort(ip[2]),
                    Short.parseShort(ip[3])
            };

            /*if (ipNumber[0] == 10) { // Class A
                isValid = true;
            } else if (ipNumber[0] == 172 && (ipNumber[1] >= 16 && ipNumber[1] <= 31)) { // Class B
                isValid = true;
            } else*/ if (ipNumber[0] == 192 && ipNumber[1] == 168) { // Class C
                isValid = true;
            }
        }

        return isValid;
    }
}
