package sr;

import timerPackage.Model;
import timerPackage.Timer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class SRClient {
    //共用端口号
    private final int port = 80;
    //发送的数据的数量
    private final int num = 10;
    //socket
    private DatagramSocket datagramSocket = new DatagramSocket();
    //数据报
    private DatagramPacket datagramPacket;
    //IP
    private InetAddress inetAddress;

    private Model model;

    private static SRClient srClient;
    //定时器
    private Timer timer;
    //下一个待发送的数据报序号
    private int nextSeq = 1;
    //最早未被接受的数据报序号
    private int base = 1;
    //用来保存发送窗口中序号（1~10）的数据报是否收到了对应ACK
    private boolean[] mark;
    //滑动窗口大小
    private int N = 5;


    public SRClient() throws Exception {
        mark = new boolean[num + 1];
        model = new Model();
        timer = new Timer(this, model);
        model.setTime(0);
        timer.start();
        while (true) {
            //向服务器端发送数据
            sendData();
            //从socket接受ACK数据报并存入数组
            byte[] bytes = new byte[4096];
            datagramPacket = new DatagramPacket(bytes, bytes.length);
            datagramSocket.receive(datagramPacket);

            //接收到的ACK转换为String
            String fromServer = new String(bytes, 0, bytes.length);
            //接收到的分组序号
            int ack = Integer.parseInt(fromServer.substring(fromServer.indexOf("ack:") + 4).trim());
            //对应序号数据报的ACK标记为收到
            mark[ack] = true;

            System.out.println("从服务器获得的数据:" + fromServer);
            System.out.println("\n");

            //如果收到base的ACK
            if (base == ack && base < num) {
                base++;
                //把base值移到顺序收到的分组的下一个序号处
                for (int i = base; i < nextSeq; i++) {
                    if (mark[i] == true) {
                        base = i + 1;
                    }
                }
            } else if (base == ack && base == num) {
                //发送最后一个数据报
                timer.interrupt();
                sendEnd();
                break;
            }

            if (base == nextSeq) {
                //停止计时器
                model.setTime(3);
            } else {
                //开始计时器
                model.setTime(0);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        srClient = new SRClient();
    }

    /**
     * 向服务器发送数据
     *
     * @throws Exception
     */
    private void sendData() throws Exception {
        inetAddress = InetAddress.getLocalHost();
        //下一个待发送的数据报序号小于窗口和待发送的数据量大小
        while (nextSeq < base + N && nextSeq <= num) {
            //不发编号为3,5,6的数据，模拟数据或者ACK丢失
            if (nextSeq == 3 || nextSeq == 5 || nextSeq == 6) {
                nextSeq++;
                continue;
            }

            String clientData = "客户端发送的数据编号:" + nextSeq;
            System.out.println("向服务器发送的数据:" + nextSeq);

            byte[] data = clientData.getBytes();
            DatagramPacket datagramPacket = new DatagramPacket(data, data.length, inetAddress, port);
            datagramSocket.send(datagramPacket);

            if (nextSeq == base) {
                //开始计时
                model.setTime(3);
            }
            nextSeq++;
        }
    }

    /**
     * 超时数据重传,选择重传发送窗口内未被确认序号的报文
     */
    public void timeOut() throws Exception {
        for (int i = base; i < base + N && i <= num; i++) {
            if (mark[i] == false) {
                String clientData = "客户端重新发送的数据编号:" + i;
                System.out.println("向服务器重新发送的数据:" + i);
                byte[] data = clientData.getBytes();
                DatagramPacket datagramPacket = new DatagramPacket(data, data.length, inetAddress, port);
                datagramSocket.send(datagramPacket);
            }
        }
    }

    /**
     * 向服务器发送结束信号
     */
    public void sendEnd() throws IOException {
        inetAddress = InetAddress.getLocalHost();
        int end = -1;
        String clientData = "客户端发送的数据编号:" + end;
        System.out.println("向服务器发送结束信号");

        byte[] data = clientData.getBytes();
        DatagramPacket datagramPacket = new DatagramPacket(data, data.length, inetAddress, port);
        datagramSocket.send(datagramPacket);
    }
}
