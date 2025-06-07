package peer.controllers;

import common.models.Message;
import common.utils.FileUtils;
import peer.app.P2TConnectionThread;
import peer.app.PeerApp;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import static peer.app.PeerApp.TIMEOUT_MILLIS;

public class PeerCLIController {
    public static String processCommand(String command) {
        if (PeerCommands.DOWNLOAD.matches(command)) {
            return handleDownload(command);
        }
        if (PeerCommands.LIST.matches(command)) {
            return handleListFiles();
        }
        if (PeerCommands.END.matches(command)) {
            return endProgram();
        } else {
            return PeerCommands.invalidCommand;
        }
    }

    private static String handleListFiles() {
        Map<String, String> filesAndHashes = FileUtils.listFilesInFolder(PeerApp.getSharedFolderPath());
        String ret = FileUtils.getSortedFileList(filesAndHashes);
        if (ret.isEmpty()) {
            return "Repository is empty.";
        }
        return ret;
    }

    private static String handleDownload(String command) {
        try {
            String filename;
            try {
                filename = PeerCommands.DOWNLOAD.getGroup(command, "filename");
            } catch (Exception e) {
                return PeerCommands.invalidCommand;
            }
            if (FileUtils.listFilesInFolder(PeerApp.getSharedFolderPath()).containsKey(filename)) {
                return "You already have the file!";
            }

            try {
                Message response = P2TConnectionController.sendFileRequest(PeerApp.getP2TConnection(), filename);
                try{
                    PeerApp.requestDownload(response.getFromBody("peer_have"), response.getIntFromBody("peer_port"), filename, response.getFromBody("md5"));
                    return "File downloaded successfully: " + filename;
                }catch (Exception e){
                    return "The file has been downloaded from peer but is corrupted!";
                }
            } catch (FileNotFoundException e) {
                return "No peer has the file!";
            } catch (IllegalStateException e) {
                return "Multiple hashes found!";
            } catch (Exception e) {
                e.printStackTrace();
                return "";
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String endProgram() {
        PeerApp.endAll();
        return "";
    }
}
