package Messages;

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
    private InetAddress roomGroup;



    public static String getNewGroupName() {
        return "";
    }


    public void sendPacket(String message) throws IOException {
        DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), this.roomGroup, GROUP_PORT);
        socket.send(packet);
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
                    found = true;
                    break;
                }
            }
        }
    }

    public MyMulticastSocketWrapper(String GROUPNAME) {
        try {
            this.roomGroup = InetAddress.getByName(GROUPNAME);
            boolean socketCreated = false;
            while (!socketCreated) {
                try {
                    SocketAddress socketAddress = new InetSocketAddress(this.roomGroup, GROUP_PORT);
                    socket = new MulticastSocket(GROUP_PORT);
                    socket.joinGroup(socketAddress, networkInterface);
                    socketCreated = true;
                    connected = true;
                } catch (SocketException e) {
                    connected = false;
                    exit(1);
                }
            }
        } catch (Exception e) {

        }
    }

}
