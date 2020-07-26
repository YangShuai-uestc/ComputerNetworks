package gbn;

import timerPackage.Model;
import timerPackage.Timer;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * 客户端
 */
public class GBNClinet {
    //端口
    private final int port = 80;
    //套接字
    private DatagramSocket datagramSocket = new DatagramSocket();
    //代表数据报
    private DatagramPacket datagramPacket;

    private InetAddress inetAddress;

    private Model model;
    //GBN客户端
    private static GBNClinet gbnClient;
    //定时器
    private Timer timer;
    //下一个待发送的序号
    private int nextSeq = 1;
    //最早未ACK的序号
    private int base = 1;
    //滑动窗口大小
    private int N = 5;


    public GBNClinet() throws Exception {
        model = new Model();
        timer = new Timer(this, model);
        model.setTime(0);
        timer.start();
        while (true) {
            //向服务器端发送数据
            sendData();
            //用bytes数组来接收数据
            byte[] bytes = new byte[4096];
            datagramPacket = new DatagramPacket(bytes, bytes.length);
            //从服务器端接受ACK并交给socket
            datagramSocket.receive(datagramPacket);
            //接受的数据转换成String
            String fromServer = new String(bytes, 0, bytes.length);
            //从ACK报文中提取出ack的序号
            int ack = Integer.parseInt(fromServer.substring(fromServer.indexOf("ack:") + 4).trim());
            //base = ack+1，累计确认机制
            base = ack + 1;
            if (base == nextSeq) {
                //停止计时器
                model.setTime(0);
            } else {
                //开始计时器
                model.setTime(3);
            }
            System.out.println("从服务器获得的数据:" + fromServer + "\n");
        }

    }

    public static void main(String[] args) throws Exception {
        gbnClient = new GBNClinet();

    }

    /**
     * 向服务器发送数据
     *
     * @throws Exception
     */
    private void sendData() throws Exception {
        //本地IP地址
        inetAddress = InetAddress.getLocalHost();
        //下一个要发送的数据序号在窗口内进入循环，模拟发送10个数据
        while (nextSeq < base + N && nextSeq <= 10) {
            //不发编号为3的数据，模拟数据丢失
            if (nextSeq == 3) {
                nextSeq++;
                continue;
            }

            String clientData = "客户端发送的数据编号:" + nextSeq;

            System.out.println("向服务器发送的数据:" + nextSeq + "\n");
            //数据转换成字节流
            byte[] data = clientData.getBytes();
            //放入数据报
            DatagramPacket datagramPacket = new DatagramPacket(data, data.length, inetAddress, port);
            //发送
            datagramSocket.send(datagramPacket);

            if (nextSeq == base) {
                //开始计时
                model.setTime(3);
            }
            nextSeq++;
        }
    }

    /**
     * 超时数据重传
     */
    public void timeOut() throws Exception {
        //BGN协议把base到nextSeq-1序号的数据报全部重新传了一遍
        for (int i = base; i < nextSeq; i++) {
            String clientData = "客户端重新发送了数据报->编号:" + i;
            System.out.println("向服务器重新发送的数据:" + i + "\n");
            byte[] data = clientData.getBytes();
            DatagramPacket datagramPacket = new DatagramPacket(data, data.length, inetAddress, port);
            datagramSocket.send(datagramPacket);
        }
    }
}

