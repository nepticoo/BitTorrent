package peer.app;

import common.models.Message;
import common.utils.JSONUtils;
import common.utils.MD5Hash;

import java.io.*;
import java.net.Socket;
import java.nio.file.FileAlreadyExistsException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PeerApp {
    public static final int TIMEOUT_MILLIS = 5000;

    private static String peerIP;
    private static int peerPort;
    private static String sharedFolderPath;
    private static final Map<String, List<String>> sentFiles = new HashMap<>();
    private static final Map<String, List<String>> receivedFiles = new HashMap<>();
    private static P2TConnectionThread P2TConnection;
    private static P2PListenerThread P2PListenerThread;
    private static final ArrayList<TorrentP2PThread> torrentP2PThreads = new ArrayList<>();


    private static boolean exitFlag = false;

    public static boolean isEnded() {
        return exitFlag;
    }

    public static void initFromArgs(String[] args) throws Exception {
        try {
            String[] peerParts = args[0].split(":");
            peerIP = peerParts[0];
            peerPort = Integer.parseInt(peerParts[1]);

            String[] trackerParts = args[1].split(":");
            String trackerIP = trackerParts[0];
            int trackerPort = Integer.parseInt(trackerParts[1]);

            sharedFolderPath = args[2];
            if (args.length > 3 || peerParts.length > 2 || trackerParts.length > 2) {
                throw new IllegalAccessException();
            }
            File sharedFolder = new File(sharedFolderPath);
            if(!sharedFolder.exists() || !sharedFolder.isDirectory()) {
                throw new IllegalAccessException();
            }

            P2PListenerThread = new P2PListenerThread(peerPort);

            Socket socket = new Socket(trackerIP, trackerPort);
            P2TConnection = new P2TConnectionThread(socket);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void endAll() {
        exitFlag = true;
        P2TConnection.end();
        for(TorrentP2PThread thread : torrentP2PThreads) {
            thread.end();
        }
        torrentP2PThreads.clear();
        sentFiles.clear();
        receivedFiles.clear();
    }

    public static void connectTracker() {
        if(P2TConnection != null && !P2TConnection.isAlive()) {
            P2TConnection.start();
        }else  {
            throw new IllegalStateException("Tracker connection thread is already running or not set");
        }
    }

    public static void startListening() {
        if(P2PListenerThread != null && !P2PListenerThread.isAlive()) {
            P2PListenerThread.start();
        }else {
            throw new IllegalStateException("Listener thread is already running or not set.");
        }
    }

    public static void removeTorrentP2PThread(TorrentP2PThread torrentP2PThread) {
        if (torrentP2PThread != null) {
            torrentP2PThreads.remove(torrentP2PThread);
            torrentP2PThread.end();
        }
    }

    public static void addTorrentP2PThread(TorrentP2PThread torrentP2PThread) {
        if(torrentP2PThread != null && !torrentP2PThreads.contains(torrentP2PThread)) {
            torrentP2PThreads.add(torrentP2PThread);
        }
    }

    public static String getSharedFolderPath() {
        return sharedFolderPath;
    }

    public static void addSentFile(String receiver, String fileNameAndHash) {
        if(!sentFiles.containsKey(receiver)) {
            sentFiles.put(receiver, new ArrayList<>());
        }
        sentFiles.get(receiver).add(fileNameAndHash);
    }

    public static void addReceivedFile(String sender, String fileNameAndHash) {
        if(!receivedFiles.containsKey(sender)) {
            receivedFiles.put(sender, new ArrayList<>());
        }
        receivedFiles.get(sender).add(fileNameAndHash);
    }

    public static String getPeerIP() {
        return peerIP;
    }

    public static int getPeerPort() {
        return peerPort;
    }

    public static Map<String, List<String>> getSentFiles() {
        return sentFiles;
    }

    public static Map<String, List<String>> getReceivedFiles() {
        return receivedFiles;
    }

    public static P2TConnectionThread getP2TConnection() {
        return P2TConnection;
    }

    public static void requestDownload(String ip, int port, String filename, String md5) throws Exception {
        Message downloadRequest = new Message(new HashMap<String, Object>(){{
            put("name", filename);
            put("md5", md5);
            put("receiver_ip", peerIP);
            put("receiver_port", peerPort);
        }}, Message.Type.download_request);

        Socket socket = new Socket(ip, port);
        socket.setSoTimeout(TIMEOUT_MILLIS);
        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());

        String JSONString = JSONUtils.toJson(downloadRequest);
        try {
            dataOutputStream.writeUTF(JSONString);
        } catch (IOException e) {
            e.printStackTrace();
        }


        File outputFile = new File(sharedFolderPath, filename);
        try (
                BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
                FileOutputStream fos = new FileOutputStream(outputFile)
        ) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        } catch (Exception e) {
            outputFile.delete();
            throw new IOException();
        } finally {
            socket.close();
        }

        if(!outputFile.exists() || !MD5Hash.HashFile(outputFile.getPath()).equals(md5)) {
            outputFile.delete();
            throw new IllegalStateException();
        }

        addReceivedFile(filename, md5);
    }
}
