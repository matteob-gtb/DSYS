package Networking;

import Messages.CommonMulticastMessages.AbstractMessage;
import Messages.CommonMulticastMessages.AnonymousMessages.AckMessage;
import Messages.CommonMulticastMessages.AnonymousMessages.ProbeMessage;
import Peer.ChatClient;

import java.io.IOException;
import java.net.*;
import java.util.*;

import static utils.Constants.GROUP_PORT;
import static utils.Constants.SOCKET_DEFAULT_TIMEOUT_MS;

//self-contained socket
public class MyMulticastSocketWrapper {
    private static NetworkInterface networkInterface;
    private MulticastSocket socket; //multicast or normal UDP
    private boolean connected = false;
    private static Set<String> usedGroupNames = new HashSet<>();
    public static String hostAddress;

    public static void addUsedGroupName(String groupName) {
        usedGroupNames.add(groupName);
    }

    public InetAddress getMCastAddress() {
        return roomGroup;
    }

    private InetAddress roomGroup;

    private static String firstAvailableGroupName = "224.1.1.2";

    public static String getNextIPAddress(String ipAddress) {
        String[] octets = ipAddress.split("\\.");
        if (octets.length != 4) {
            throw new IllegalArgumentException("Invalid IPv4 address format");
        }

        int[] intOctets = new int[4];
        for (int i = 0; i < 4; i++) {
            intOctets[i] = Integer.parseInt(octets[i]);
            if (intOctets[i] < 0 || intOctets[i] > 255) {
                throw new IllegalArgumentException("Each octet must be between 0 and 255");
            }
        }

        intOctets[3] += 1;

        for (int i = 3; i > 0; i--) {
            if (intOctets[i] > 255) {
                intOctets[i] = 0;
                intOctets[i - 1] += 1;
            }
        }

        if (intOctets[0] > 255) {
            throw new IllegalArgumentException("IP address overflow, cannot increment further");
        }

        return intOctets[0] + "." + intOctets[1] + "." + intOctets[2] + "." + intOctets[3];
    }

    public static String getNewGroupName() {
        String currentGroupName = firstAvailableGroupName;
        while (usedGroupNames.contains(currentGroupName))
            currentGroupName = getNextIPAddress(currentGroupName);
        addUsedGroupName(currentGroupName);
        firstAvailableGroupName = currentGroupName;
        return currentGroupName;
    }


    public boolean receive(DatagramPacket packet) {
        try {
            socket.receive(packet);
            return true;
        } catch (SocketTimeoutException e) {
            return false;
        } catch (IOException e) {

            return false;
        }

    }


    public void close() {
        try {
            socket.close();
        } catch (Exception e) {
            System.out.println("IO exception");
        }
    }


    public boolean sendPacket(AbstractMessage message) {
        String msg = message.toJSONString();
        DatagramPacket packet;
        InetAddress destination = null;
        if (!message.isUnicast())
            destination = this.roomGroup;
        else destination = message.getDestinationAddress();

        packet = new DatagramPacket(msg.getBytes(), msg.length(), destination, GROUP_PORT);

        try {
            socket.send(packet);
        } catch (IOException e) {
            this.connected = false;
            message.setSent(false);
            return false;
        }
        message.setSent(true);
        return true;
    }

    @Deprecated
    //if this fails it means we can't and will never connect (no interfaces available)
    public static void setupInterfaces() throws SocketException {
        networkInterface = null;
        boolean found = false;
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements() && !found) {
            NetworkInterface netInt = interfaces.nextElement();
            Enumeration<InetAddress> inetAddresses = netInt.getInetAddresses();
            for (Iterator<InetAddress> it = inetAddresses.asIterator(); it.hasNext(); ) {
                InetAddress address = it.next();
                if (address instanceof Inet4Address && netInt.supportsMulticast()) {
                    networkInterface = NetworkInterface.getByInetAddress(address);
                    System.out.println("Selected interface: " + networkInterface.getName());
                    System.out.println("Client address : " + address.getHostAddress());
                    hostAddress = address.getHostAddress();
                    found = true;
                    break;
                }
            }
        }
    }


    public boolean isConnected() {
        return connected;
    }


    public MyMulticastSocketWrapper(String GROUPNAME) {
        try {
            this.roomGroup = InetAddress.getByName(GROUPNAME);
            boolean socketCreated = false;
            while (!socketCreated) {
                try {

                    socket = new MulticastSocket(GROUP_PORT);
                    socket.setSoTimeout(SOCKET_DEFAULT_TIMEOUT_MS);

                    Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                    while (interfaces.hasMoreElements()) {
                        NetworkInterface networkInterface = interfaces.nextElement();

                        // Check if the interface supports multicast, thanks macOS
                        if (networkInterface.supportsMulticast()) {
                            System.out.println("Joining group on interface: " + networkInterface.getName());
                            socket.joinGroup(new InetSocketAddress(roomGroup, GROUP_PORT), networkInterface);
                        }
                    }


                    socketCreated = true;
                    connected = true;
                } catch (SocketException e) {
                    connected = false;

                }
            }
        } catch (Exception e) {
            System.out.println("Failed to create socket, no matching interface found");
            System.out.println(e.getMessage());
            connected = false;

        }
    }


    public void probeConnection() throws IOException {
        byte[] buffer = new ProbeMessage(ChatClient.ID).toJSONString().getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, this.roomGroup, GROUP_PORT);
        socket.send(packet);
    }
}
