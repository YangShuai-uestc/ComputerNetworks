package gbn;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 * 服务器端
 */
public class GBNServer {
    //端口
    private final int port = 80;
    //套接字
    private DatagramSocket datagramSocket;
    //数据报
    private DatagramPacket datagramPacket;
    //期待接收到的数据包序号
    private int exceptedSeq = 1;

    public GBNServer() throws IOException {

        try {
            //套接字与端口绑定，IP地址等收到数据报才知道
            datagramSocket = new DatagramSocket(port);
            while (true) {
                //用来接收数据报的数组
                byte[] receivedData = new byte[4096];

                datagramPacket = new DatagramPacket(receivedData, receivedData.length);
                //从socket接受数据并放入数据报
                datagramSocket.receive(datagramPacket);
                //收到的数据
                String received = new String(receivedData, 0, receivedData.length);//offset是初始偏移量
                System.out.println(received);

                //当前收到数据报的编号
                int i = Integer.parseInt(received.substring(received.indexOf("编号:") + 3).trim());
                //如果收到了预期的数据
                if (i == exceptedSeq) {
                    //发送ack
                    sendAck(exceptedSeq);
                    System.out.println("True===服务端期待的数据编号:" + exceptedSeq + ",收到的数据编号为" + i);
                    //期待的序号加1
                    exceptedSeq++;
                    System.out.println('\n');
                } else {
                    System.out.println("False！！！未收到预期数据编号：" + exceptedSeq + "，当前收到的编号为：" + i);
                    //仍发送之前的ack
                    sendAck(exceptedSeq - 1);
                    System.out.println('\n');
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public static final void main(String[] args) throws IOException {
        new GBNServer();
    }

    //向客户端发送ack
    public void sendAck(int ack) throws IOException {
        //ACK的内容
        String response = " ack:" + ack;
        //ACK转换为数组
        byte[] responseBytes = response.getBytes();
        //从接受到的数据报中取出IP地址
        InetAddress responseAddress = datagramPacket.getAddress();
        //接受到的数据报中取出端口号
        int responsePort = datagramPacket.getPort();
        //通过ACK数组、IP地址、端口号创建待发送的数据报
        datagramPacket = new DatagramPacket(responseBytes, responseBytes.length, responseAddress, responsePort);
        datagramSocket.send(datagramPacket);
    }
}
