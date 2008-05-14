/**
 * Copyright 2008 The Apache Software Foundation
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sf.katta.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;

public class NetworkUtil {
  public static String[] getLocalHostNames() {
    final ArrayList<String> hostNames = new ArrayList<String>();
    try {
      final Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
      for (final Enumeration ifaces = networkInterfaces; ifaces.hasMoreElements();) {
        final NetworkInterface iface = (NetworkInterface) ifaces.nextElement();
        InetAddress ia = null;
        for (final Enumeration ips = iface.getInetAddresses(); ips.hasMoreElements();) {
          ia = (InetAddress) ips.nextElement();
          hostNames.add(ia.getCanonicalHostName());
          hostNames.add(ipToString(ia.getAddress()));
        }
      }
    } catch (final SocketException e) {
      throw new RuntimeException("unable to retrieve host names of localhost");
    }
    return hostNames.toArray(new String[hostNames.size()]);
  }

  private static String ipToString(final byte[] bytes) {
    final StringBuffer addrStr = new StringBuffer();
    for (int cnt = 0; cnt < bytes.length; cnt++) {
      final int uByte = bytes[cnt] < 0 ? bytes[cnt] + 256 : bytes[cnt];
      addrStr.append(uByte);
      if (cnt < 3)
        addrStr.append('.');
    }
    return addrStr.toString();
  }

  public static boolean hostNamesInList(final String serverList, final String[] hostNames) {
    final String[] serverNames = serverList.split(",");
    for (final String hostname : hostNames) {
      for (final String serverNameAndPort : serverNames) {
        final String serverName = serverNameAndPort.split(":")[0];
        if (serverName.equalsIgnoreCase(hostname)) {
          return true;
        }
      }
    }
    return false;
  }

  public static boolean hostNameInArray(final String[] hostNames, final String hostName) {
    for (final String name : hostNames) {
      if (name.equalsIgnoreCase(hostName)) {
        return true;
      }
    }
    return false;
  }

  public static String getLocalhostName() {
    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (final UnknownHostException e) {
      throw new RuntimeException("unable to retrieve localhost name");
    }
  }
}