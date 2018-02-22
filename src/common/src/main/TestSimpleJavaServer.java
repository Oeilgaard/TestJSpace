package common.src.main;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

/**
 * A TCP server that runs on port 9090.  When a client connects, it
 * sends the client the current date and time, then closes the
 * connection with that client.  Arguably just about the simplest
 * server you can write.
 */
public class TestSimpleJavaServer {

        /**
     * Runs the server.
     */
    public static void main(String[] args) throws IOException {
        ServerSocket listener = new ServerSocket(25565);

        System.out.println(listener.getInetAddress());

        Socket socket = listener.accept();

        System.out.println("Someone has connected!");
        socket.close();

        listener.close();

    }
}