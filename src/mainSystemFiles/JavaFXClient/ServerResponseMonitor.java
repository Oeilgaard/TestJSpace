package mainSystemFiles.JavaFXClient;

public class ServerResponseMonitor {

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
