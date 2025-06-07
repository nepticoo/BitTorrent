package peer.app;

import common.utils.MD5Hash;

import java.io.*;
import java.net.Socket;

public class TorrentP2PThread extends Thread {
    private final Socket socket;
    private final File file;
    private final String receiver;
    private final BufferedOutputStream dataOutputStream;

    public TorrentP2PThread(Socket socket, File file, String receiver) throws IOException {
        this.socket = socket;
        this.file = file;
        this.receiver = receiver;
        this.dataOutputStream = new BufferedOutputStream(socket.getOutputStream());
        PeerApp.addTorrentP2PThread(this);
    }

    @Override
    public void run() {
        try (BufferedInputStream fileInputStream = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                dataOutputStream.write(buffer, 0, bytesRead);
            }

            dataOutputStream.flush();

            String filename = file.getName();
            String md5 = MD5Hash.HashFile(file.getPath());
            PeerApp.addSentFile(receiver, filename + " " + md5);

        } catch (Exception e) {
            System.err.println("Error while sending file to peer: " + e.getMessage());
        } finally {
            try {
                dataOutputStream.close();
                socket.close();
            } catch (IOException ignored) {
            }

            PeerApp.removeTorrentP2PThread(this);
        }

        try {
            socket.close();
        } catch (Exception e) {
        }

        PeerApp.removeTorrentP2PThread(this);
    }

    public void end() {
        try {
            dataOutputStream.close();
            socket.close();
        } catch (Exception e) {
        }
    }
}
