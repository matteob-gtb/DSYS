package Networking;

import Messages.AbstractMessage;
import Messages.AnonymousMessages.ProbeMessage;
import Messages.Room.AbstractOrderedMessage;

import java.io.IOException;
import java.net.*;
import java.util.*;

import static java.lang.System.exit;
import static utils.Constants.GROUP_PORT;

//self-contained socket
public class MyMulticastSocketWrapper {
    private static NetworkInterface networkInterface;
    private MulticastSocket socket; //multicast or normal UDP
    private boolean connected = false;
    private static Set<String> usedGroupNames = new HashSet<>();
    private final static int DEFAULT_TIMEOUT = 250;

    private Long lastConnectionAttemptEpoch = -1L;


    public static void addUsedGroupName(String groupName) {
        usedGroupNames.add(groupName);
    }

    public InetAddress getMCastAddress() {
        return roomGroup;
    }

    private InetAddress roomGroup;

    private static String firstAvailableGroupName = "224.1.1.2";

    public static String getNextIPAddress(String ipAddress) {
        // Split the input IP address into its components
        String[] octets = ipAddress.split("\\.");
        if (octets.length != 4) {
            throw new IllegalArgumentException("Invalid IPv4 address format");
        }

        // Parse the octets into integers
        int[] intOctets = new int[4];
        for (int i = 0; i < 4; i++) {
            intOctets[i] = Integer.parseInt(octets[i]);
            if (intOctets[i] < 0 || intOctets[i] > 255) {
                throw new IllegalArgumentException("Each octet must be between 0 and 255");
            }
        }

        // Increment the last octet
        intOctets[3] += 1;

        // Handle overflow and carry to the next octet
        for (int i = 3; i > 0; i--) {
            if (intOctets[i] > 255) {
                intOctets[i] = 0;
                intOctets[i - 1] += 1;
            }
        }

        // Special case for overflow of the first octet
        if (intOctets[0] > 255) {
            throw new IllegalArgumentException("IP address overflow, cannot increment further");
        }

        // Construct the next IP address string
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
            System.out.println("IO exception");
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


        DatagramPacket packet = new DatagramPacket(msg.getBytes(), msg.length(), this.roomGroup, GROUP_PORT);
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
                    SocketAddress socketAddress = new InetSocketAddress(this.roomGroup, GROUP_PORT);
                    socket = new MulticastSocket(GROUP_PORT);
                    socket.setSoTimeout(DEFAULT_TIMEOUT);

                    socket.joinGroup(socketAddress, networkInterface);
                    socketCreated = true;
                    connected = true;
                } catch (SocketException e) {
                    System.out.println("Socket exception: " + e.getMessage());
                    connected = false;

                }
            }
        } catch (Exception e) {
            System.out.println("Failed to create socket");
            System.out.println(e.getMessage());
            connected = false;
        }
    }


    public void probeConnection() throws IOException {
        byte[] buffer = new ProbeMessage().toJSONString().getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, this.roomGroup, GROUP_PORT);
        socket.send(packet);
    }
}
