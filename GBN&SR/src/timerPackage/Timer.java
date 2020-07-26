package timerPackage;

import gbn.GBNClinet;
import sr.SRClient;

/**
 * 计时器
 */
public class Timer extends Thread {

    private Model model;
    private GBNClinet gbnClient;
    private SRClient srClient;

    //GBN协议的定时器
    public Timer(GBNClinet gbnClient, Model model) {
        this.gbnClient = gbnClient;
        this.model = model;
    }

    //SR协议的定时器
    public Timer(SRClient srClient, Model model) {
        this.srClient = srClient;
        this.model = model;
    }

    @Override
    public void run() {
        do {
            int time = model.getTime();
            if (time > 0) {
                try {
                    Thread.sleep(time * 1000);

                    System.out.println("\n");

                    if (gbnClient == null) {
                        System.out.println("SR客户端等待ACK超时");
                        srClient.timeOut();
                    }

                    if (srClient == null) {
                        System.out.println("GBN客户端等待ACK超时");
                        gbnClient.timeOut();
                    }

                    model.setTime(0);

                } catch (Exception e) {

                }
            }
        } while (true);
    }
}
