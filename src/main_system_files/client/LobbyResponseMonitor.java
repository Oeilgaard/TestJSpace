package main_system_files.client;

public class LobbyResponseMonitor {

    private Boolean ok = false;

    synchronized void sync() throws InterruptedException {

        while (!ok) wait();

        ok = false;
    }

    synchronized void okay(){
        ok = true;
        notifyAll();
    }


}
