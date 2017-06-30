package ro.happyhyppo.sim.pcap;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;

public class UDPServer {

    private static String ipAddr = "192.168.45.73";

    public static void main(String args[]) {
        DatagramSocket serverSocket = null;
        try {
            serverSocket = new DatagramSocket(10100, (Inet4Address) Inet4Address.getByName(ipAddr));
            //serverSocket = new DatagramSocket(10100);
            System.out.println(serverSocket.getLocalAddress() + ":" + serverSocket.getLocalPort());
            byte[] receiveData = new byte[1024];
            byte[] sendData = new byte[1024];
            while (true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivePacket);
                String sentence = new String(receivePacket.getData(), 0, receivePacket.getLength());
                InetAddress ipAddress = receivePacket.getAddress();
                int port = receivePacket.getPort();
                String capitalizedSentence = sentence.toUpperCase();
                System.out.println("RECEIVED: " + sentence + " from " + ipAddress + ":" + port);
                sendData = capitalizedSentence.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ipAddress, port);
                serverSocket.send(sendPacket);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (serverSocket != null) {
                serverSocket.close();
            }
        }
    }
}