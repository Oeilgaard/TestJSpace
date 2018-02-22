package common.src.main;

import java.io.BufferedReader;
        import java.io.IOException;
        import java.io.InputStreamReader;
        import java.net.Socket;

        import javax.swing.JOptionPane;

/**
 * Trivial client for the date server.
 */
public class TestSimpleJavaClient {

        /**
     * Runs the client as an application.  First it displays a dialog
     * box asking for the IP address or hostname of a host running
     * the date server, then connects to it and displays the date that
     * it serves.
     */
    public static void main(String[] args) throws IOException {
        String serverAddress = JOptionPane.showInputDialog(
                "Enter IP Address of a machine that is\n" +
                        "running the date service on port 9090:");
        Socket s = new Socket(serverAddress, 25565);

        System.exit(0);
    }
}