package tracker.controllers;

import common.utils.FileUtils;
import tracker.app.PeerConnectionThread;
import tracker.app.TrackerApp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TrackerCLIController {
    public static String processCommand(String command) {
        if (TrackerCommands.REFRESH_FILES.matches(command)) {
            return refreshFiles();
        }
        if (TrackerCommands.RESET_CONNECTIONS.matches(command)) {
            return resetConnections();
        }
        if (TrackerCommands.LIST_PEERS.matches(command)) {
            return listPeers();
        }
        if (TrackerCommands.LIST_FILES.matches(command)) {
            return listFiles(command);
        }
        if (TrackerCommands.GET_RECEIVES.matches(command)) {
            return getReceives(command);
        }
        if (TrackerCommands.GET_SENDS.matches(command)) {
            return getSends(command);
        }
        if (TrackerCommands.END.matches(command)) {
            return endProgram();
        } else {
            return TrackerCommands.invalidCommand;
        }
    }

    private static String getReceives(String command) {
        try {
            String ip = TrackerCommands.GET_RECEIVES.getGroup(command, "ip");
            int port = Integer.parseInt(TrackerCommands.GET_RECEIVES.getGroup(command, "port"));

            PeerConnectionThread connection = TrackerApp.getConnectionByIpPort(ip, port);
            if (connection == null) {
                return "Peer not found.";
            }

            Map<String, List<String>> receivedFiles = TrackerConnectionController.getReceives(connection);
            if (receivedFiles.isEmpty()) {
                return "No files received by " + ip + ":" + port;
            }
            ArrayList<String> lines = new ArrayList<>();
            for (String sender : receivedFiles.keySet()) {
                for (String fileAndHash : receivedFiles.get(sender)) {
                    String line = fileAndHash + " - " + sender;
                    lines.add(line);
                }
            }
            Collections.sort(lines);
            StringBuilder result = new StringBuilder();
            for (String line : lines) {
                result.append(line).append("\n");
            }
            result.deleteCharAt(result.length() - 1);
            return result.toString();
        } catch (Exception e) {
            return TrackerCommands.invalidCommand;
        }
    }

    private static String getSends(String command) {
        try {
            String ip = TrackerCommands.GET_SENDS.getGroup(command, "ip");
            int port = Integer.parseInt(TrackerCommands.GET_SENDS.getGroup(command, "port"));

            PeerConnectionThread connection = TrackerApp.getConnectionByIpPort(ip, port);
            if (connection == null) {
                return "Peer not found.";
            }

            Map<String, List<String>> sentFiles = TrackerConnectionController.getSends(connection);
            if (sentFiles == null || sentFiles.isEmpty()) {
                return "No files sent by " + ip + ":" + port;
            }
            ArrayList<String> lines = new ArrayList<>();
            for (String receiver : sentFiles.keySet()) {
                for (String fileAndHash : sentFiles.get(receiver)) {
                    String line = fileAndHash + " - " + receiver;
                    lines.add(line);
                }
            }
            Collections.sort(lines);
            StringBuilder result = new StringBuilder();
            for (String line : lines) {
                result.append(line).append("\n");
            }
            result.deleteCharAt(result.length() - 1);
            return result.toString();
        } catch (Exception e) {
            return TrackerCommands.invalidCommand;
        }
    }

    private static String listFiles(String command) {
        try {
            String ip = TrackerCommands.LIST_FILES.getGroup(command, "ip");
            int port = Integer.parseInt(TrackerCommands.LIST_FILES.getGroup(command, "port"));

            PeerConnectionThread connection = TrackerApp.getConnectionByIpPort(ip, port);

            if (connection == null) {
                return "Peer not found.";
            }

            String ret = FileUtils.getSortedFileList(connection.getFileAndHashes());
            if (ret.isEmpty()) {
                return "Repository is empty.";
            }
            return ret;
        } catch (Exception e) {
            return TrackerCommands.invalidCommand;
        }
    }

    private static String listPeers() {
        if (TrackerApp.getConnections().isEmpty()) {
            return "No peers connected.";
        }
        StringBuilder result = new StringBuilder();
        for (PeerConnectionThread connection : TrackerApp.getConnections()) {
            result.append(connection.getOtherSideIP() + ":" + connection.getOtherSidePort()).append("\n");
        }
        result.deleteCharAt(result.length() - 1);
        return result.toString();
    }

    private static String resetConnections() {
        for(int i = TrackerApp.getConnections().size() - 1; i >= 0; i--) {
            TrackerApp.getConnections().get(i).refreshStatus(false);
        }
//        for (PeerConnectionThread connection : TrackerApp.getConnections()) {
//            connection.refreshStatus();
//        }
        refreshFiles();
        return "";
    }

    private static String refreshFiles() {
        for (PeerConnectionThread connection : TrackerApp.getConnections()) {
            connection.refreshFileList(false);
        }
        return "";
    }

    private static String endProgram() {
        TrackerApp.endAll();
        return "";
    }
}
