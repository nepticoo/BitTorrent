package peer.app;

import common.models.Message;
import common.utils.JSONUtils;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import static peer.app.PeerApp.TIMEOUT_MILLIS;

public class P2PListenerThread extends Thread {
    private final ServerSocket serverSocket;

    public P2PListenerThread(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
    }

    private void handleConnection(Socket socket) throws Exception {
        if (socket == null) return;

        socket.setSoTimeout(TIMEOUT_MILLIS);

        DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
        String receivedStr = dataInputStream.readUTF();
        Message message = JSONUtils.fromJson(receivedStr);

        if (!message.getType().equals(Message.Type.download_request)) {
            socket.close();
            return;
        }
        String filename = message.getFromBody("name");
        String md5 = message.getFromBody("md5");
        String receiverIP = message.getFromBody("receiver_ip");
        int receiverPort = message.getIntFromBody("receiver_port");

        File file = new File(PeerApp.getSharedFolderPath(), filename);

        if(!file.exists()) {
            socket.close();
            return;
        }

        TorrentP2PThread torrentThread = new TorrentP2PThread(socket, file, receiverIP + ":" + receiverPort);
        torrentThread.start();
    }

    @Override
    public void run() {
        try {
            serverSocket.setSoTimeout(TIMEOUT_MILLIS);
            while (!PeerApp.isEnded()) {
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
