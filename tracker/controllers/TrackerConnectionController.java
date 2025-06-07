package tracker.controllers;

import common.models.Message;
import tracker.app.PeerConnectionThread;
import tracker.app.TrackerApp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static tracker.app.TrackerApp.TIMEOUT_MILLIS;

public class TrackerConnectionController {
    public static Message handleCommand(Message message) {
        if (!message.getType().equals(Message.Type.file_request)) {
            return null;
        }
        String targetFileName = message.getFromBody("name");
        String foundFileHash = null;
        String foundPeerIP = null;
        int foundPearPort = -999999;

        Message.Type type = Message.Type.response;
        HashMap<String, Object> body = new HashMap<>();

        for (PeerConnectionThread connection : TrackerApp.getConnections()) {
            for (String fileName : connection.getFileAndHashes().keySet()) {
                if (fileName.equals(targetFileName)) {
                    String MD5Hash = connection.getFileAndHashes().get(fileName);
                    if (foundPearPort != -999999 && !foundFileHash.equals(MD5Hash)) {
                        body.put("response", "error");
                        body.put("error", "multiple_hash");
                        return new Message(body, type);
                    }
                    foundFileHash = MD5Hash;
                    foundPeerIP = connection.getOtherSideIP();
                    foundPearPort = connection.getOtherSidePort();
                }
            }
        }

        if(foundPearPort == -999999) {
            body.put("response", "error");
            body.put("error", "not_found");
            return new Message(body, type);
        }

        body.put("response", "peer_found");
        body.put("md5", foundFileHash);
        body.put("peer_have", foundPeerIP);
        body.put("peer_port", foundPearPort);
        return new Message(body, type);
    }

    public static Map<String, List<String>> getSends(PeerConnectionThread connection) {
        Message command = new Message(new HashMap<String, Object>(){{
            put("command", "get_sends");
        }}, Message.Type.command);
        Message response = connection.sendAndWaitForResponse(command, TIMEOUT_MILLIS);

        return response.getFromBody("sent_files");
    }

    public static Map<String, List<String>> getReceives(PeerConnectionThread connection) {
        Message command = new Message(new HashMap<String, Object>(){{
            put("command", "get_receives");
        }}, Message.Type.command);
        Message response = connection.sendAndWaitForResponse(command, TIMEOUT_MILLIS);

        return response.getFromBody("received_files");
    }
}
