package tracker.app;

import common.models.ConnectionThread;
import common.models.Message;
import tracker.controllers.TrackerConnectionController;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import static tracker.app.TrackerApp.TIMEOUT_MILLIS;

public class PeerConnectionThread extends ConnectionThread {
    private Map<String, String> fileAndHashes;

    public PeerConnectionThread(Socket socket) throws IOException {
        super(socket);
    }

    @Override
    public boolean initialHandshake() {
        try {
            if (!refreshStatus(true)) {
                return false;
            }
            if (!refreshFileList(true)) {
                return false;
            }
            TrackerApp.addPeerConnection(this);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean refreshStatus(boolean isInitial) {
        Message command = new Message(new HashMap<String, Object>() {{
            put("command", "status");
        }}, Message.Type.command);
        Message response = sendAndWaitForResponse(command, TIMEOUT_MILLIS);

        if (response == null) {
            if (!isInitial) {
                end();
            }
            return false;
        }

        setOtherSideIP(response.getFromBody("peer"));
        setOtherSidePort(response.getIntFromBody("listen_port"));
        return true;
    }

    public boolean refreshFileList(boolean isInitial) {
        Message command = new Message(new HashMap<String, Object>() {{
            put("command", "get_files_list");
        }}, Message.Type.command);
        Message response = sendAndWaitForResponse(command, TIMEOUT_MILLIS);

        if (response == null) {
            if (!isInitial) {
                end();
            }
            return false;
        }

        fileAndHashes = response.getFromBody("files");
        return true;
    }

    @Override
    protected boolean handleMessage(Message message) {
        if (message.getType() == Message.Type.file_request) {
            sendMessage(TrackerConnectionController.handleCommand(message));
            return true;
        }
        return false;
    }

    @Override
    public void run() {
        super.run();
        TrackerApp.removePeerConnection(this);
    }

    public Map<String, String> getFileAndHashes() {
        return Map.copyOf(fileAndHashes);
    }
}
