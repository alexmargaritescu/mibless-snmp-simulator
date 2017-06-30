package ro.happyhyppo.sim.pcap;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.pcap4j.core.BpfProgram.BpfCompileMode;
import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PacketListener;
import org.pcap4j.core.PcapAddress;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.PcapNetworkInterface.PromiscuousMode;
import org.pcap4j.core.Pcaps;
import org.pcap4j.packet.EthernetPacket;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.UdpPacket;
import org.pcap4j.packet.namednumber.UdpPort;

public class SimpleTest {

    private static String ipAddr = "192.168.45.73";

    private static final String virtualNetwork = "20.0.0";

    private static final String virtualRouterAddr = "100.0.0.1";

    private static final short portBase = 10000;

    private static final int virtualPort = 161;

    public static void main(String[] args) {
        if (args != null && args.length != 0) {
            ipAddr = args[0];
        }
        System.out.println("IP Address to use: " + ipAddr);
        try {
            PcapNetworkInterface nif1 = selectNif(ipAddr);
            PcapNetworkInterface nif2 = selectNif(ipAddr);
            System.out.println(nif1);
            final PcapHandle captureHandle = nif1.openLive(65536, PromiscuousMode.PROMISCUOUS, 10);
            captureHandle.setFilter("udp", BpfCompileMode.OPTIMIZE);
            final PcapHandle sendHandle = nif2.openLive(65536, PromiscuousMode.PROMISCUOUS, 10);
            final Inet4Address destination = (Inet4Address) Inet4Address.getByName(ipAddr);
            final PacketListener listener = new PacketListener() {
                public void gotPacket(Packet packet) {
                    if (packet.contains(UdpPacket.class)) {
                        IpV4Packet ipV4Packet = packet.get(IpV4Packet.class);
                        if (ipV4Packet == null) {
                            return;
                        }
                        try {
                            Inet4Address dstAddr = ipV4Packet.getHeader().getDstAddr();
                            Inet4Address routerAddr = (Inet4Address) Inet4Address.getByName(virtualRouterAddr);
                            if (isSimulatedNetwork(dstAddr)) {
                                // packet to or from our simulated network
                                EthernetPacket inPacket = packet.get(EthernetPacket.class);
                                System.out.println(inPacket);
                                UdpPacket udpPacket = packet.get(UdpPacket.class);
                                IpV4Packet.Builder ipv4Builder = ipV4Packet.getBuilder();
                                UdpPacket.Builder udpBuilder = udpPacket.getBuilder();
                                UdpPort srcPort = udpPacket.getHeader().getSrcPort();
                                UdpPort dstPort = udpPacket.getHeader().getDstPort();
                                // destination is our real IP address, no matter what
                                ipv4Builder.dstAddr(destination);
                                udpBuilder.dstAddr(destination);
                                if (!dstAddr.equals(routerAddr)) {
                                    // packet going to our simulated devices
                                    ipv4Builder.srcAddr(routerAddr);
                                    udpBuilder.srcAddr(routerAddr);
                                    udpBuilder.srcPort(srcPort);
                                    udpBuilder.dstPort(UdpPort.getInstance((short) mapPort(dstAddr.getHostAddress())));
                                } else {
                                    // packet coming from our simulated devices
                                    String srcAddress = mapAddress(srcPort.valueAsInt());
                                    ipv4Builder.srcAddr((Inet4Address) Inet4Address.getByName(srcAddress));
                                    udpBuilder.srcAddr((Inet4Address) Inet4Address.getByName(srcAddress));
                                    udpBuilder.srcPort(UdpPort.getInstance((short) virtualPort));
                                    udpBuilder.dstPort(dstPort);
                                }
                                udpBuilder.correctLengthAtBuild(true);
                                udpBuilder.correctChecksumAtBuild(true);
                                ipv4Builder.ttl((byte) 0);
                                ipv4Builder.payloadBuilder(udpBuilder);
                                ipv4Builder.correctLengthAtBuild(true);
                                ipv4Builder.correctChecksumAtBuild(true);
                                EthernetPacket.Builder ethBuilder = inPacket.getBuilder();
                                ethBuilder.dstAddr(inPacket.getHeader().getSrcAddr());
                                ethBuilder.srcAddr(inPacket.getHeader().getDstAddr());
                                ethBuilder.payloadBuilder(ipv4Builder);
                                EthernetPacket outPacket = ethBuilder.build();
                                System.out.println(outPacket);
                                sendHandle.sendPacket(outPacket);

                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(new Runnable() {
                public void run() {
                    while (true) {
                        try {
                            captureHandle.loop(-1, listener);
                        } catch (PcapNativeException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            break;
                        } catch (NotOpenException e) {
                            break;
                        }
                    }
                }
            });
            //captureHandle.breakLoop();
            //sendHandle.close();
            //sendHandle.close();
            //executor.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static PcapNetworkInterface selectNif(String ipAddress) throws Exception {
        List<PcapNetworkInterface> allDevs = Pcaps.findAllDevs();
        for (PcapNetworkInterface nif : allDevs) {
            List<PcapAddress> addresses = nif.getAddresses();
            for (PcapAddress address : addresses) {
                if (address.getAddress().equals((Inet4Address) InetAddress.getByName(ipAddress))) {
                    System.out.println("Using " + nif.getDescription());
                    return nif;
                }
            }
        }
        return null;
    }

    private static boolean isSimulatedNetwork(Inet4Address dstAddr) {
        String ipAddress = dstAddr.getHostAddress();
        return ipAddress.startsWith(virtualNetwork) || ipAddress.equals(virtualRouterAddr);
    }

    private static int mapPort(String ipAddress) {
        short offset = Short.parseShort(ipAddress.substring(ipAddress.lastIndexOf('.') + 1));
        return portBase + offset;
    }

    private static String mapAddress(Integer port) {
        return virtualNetwork + "." + String.format("%02d", port - portBase);
    }

}