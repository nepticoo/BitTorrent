package tracker.app;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import static tracker.app.TrackerApp.TIMEOUT_MILLIS;

public class ListenerThread extends Thread {
    private final ServerSocket serverSocket;

    public ListenerThread(int port) throws IOException {
        serverSocket = new ServerSocket(port);
    }

    private void handleConnection(Socket socket) {
        if (socket == null) return;
        try {
            new PeerConnectionThread(socket).start();
        } catch (IOException e) {
            try {
                socket.close();
            } catch (IOException ex) {
            }
        }
    }

    @Override
    public void run() {
        try {
            serverSocket.setSoTimeout(TIMEOUT_MILLIS);
            while (!TrackerApp.isEnded()) {
                try {
                    Socket socket = serverSocket.accept();
                    handleConnection(socket);
                } catch (SocketTimeoutException e) {
                    continue;
                } catch (Exception e) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            serverSocket.close();
        } catch (Exception ignored) {
        }
    }
}
