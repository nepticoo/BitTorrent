package peer.controllers;

import common.models.Message;
import common.utils.FileUtils;
import peer.app.P2TConnectionThread;
import peer.app.PeerApp;

import java.io.FileNotFoundException;
import java.util.HashMap;

import static peer.app.PeerApp.TIMEOUT_MILLIS;

public class P2TConnectionController {
    public static Message handleCommand(Message message) {
        String command = message.getFromBody("command");
        switch (command) {
            case "status": {
                return status();
            }
            case "get_files_list": {
                return getFilesList();
            }
            case "get_sends": {
                return getSends();
            }
            case "get_receives": {
                return getReceives();
            }
            default: {
                return null;
            }
        }
    }

    private static Message getReceives() {
        Message.Type type = Message.Type.response;
        HashMap<String, Object> body = new HashMap<>();
        body.put("command", "get_receives");
        body.put("response", "ok");
        body.put("received_files", PeerApp.getReceivedFiles());

        return new Message(body, type);
    }

    private static Message getSends() {
        Message.Type type = Message.Type.response;
        HashMap<String, Object> body = new HashMap<>();
        body.put("command", "get_sends");
        body.put("response", "ok");
        body.put("sent_files", PeerApp.getSentFiles());

        return new Message(body, type);
    }

    public static Message getFilesList() {
        Message.Type type = Message.Type.response;
        HashMap<String, Object> body = new HashMap<>();
        body.put("command", "get_files_list");
        body.put("response", "ok");
        body.put("files", FileUtils.listFilesInFolder(PeerApp.getSharedFolderPath()));

        return new Message(body, type);
    }

    public static Message status() {
        Message.Type type = Message.Type.response;
        HashMap<String, Object> body = new HashMap<>();
        body.put("command", "status");
        body.put("response", "ok");
        body.put("peer", PeerApp.getPeerIP());
        body.put("listen_port", PeerApp.getPeerPort());

        return new Message(body, type);
    }

    public static Message sendFileRequest(P2TConnectionThread tracker, String filename) throws Exception {
        Message commandMessage = new Message(new HashMap<String, Object>() {{
            put("name", filename);
        }}, Message.Type.file_request);
        Message response = tracker.sendAndWaitForResponse(commandMessage, TIMEOUT_MILLIS);
        if(response.getFromBody("response").equals("error")) {
            if(response.getFromBody("error").equals("not_found")) {
                throw new FileNotFoundException();
            }
            if(response.getFromBody("error").equals("multiple_hash")) {
                throw new IllegalStateException();
            }
        }
        return response;
    }
}
