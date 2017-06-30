package ro.happyhyppo.sim.pcap;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPClient {
    public static void main(String args[]) throws Exception {
        try {
            DatagramSocket clientSocket = new DatagramSocket(null);
            String sentence = "lala";
            byte[] sendData = sentence.getBytes();
            byte[] receiveData = new byte[1024];
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
                            InetAddress.getByName("20.0.0.100"), 161);
            clientSocket.send(sendPacket);
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            clientSocket.receive(receivePacket);
            InetAddress ipAddress = receivePacket.getAddress();
            int port = receivePacket.getPort();
            String modifiedSentence = new String(receivePacket.getData(), 0, receivePacket.getLength());
            System.out.println("RECEIVED: " + modifiedSentence + " from " + ipAddress + ":" + port);
            clientSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
