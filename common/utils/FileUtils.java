package common.utils;

import java.io.File;
import java.util.*;

public class FileUtils {

    public static Map<String, String> listFilesInFolder(String folderPath) {
        Map<String, String> map = new HashMap<String, String>();
        try {
            File folder = new File(folderPath);
            if(!folder.isDirectory()) {
                throw new IllegalArgumentException();
            }
            File[] files = folder.listFiles();
            for(File file : files) {
                if(file.isFile()) {
                    map.put(file.getName(), MD5Hash.HashFile(file.getPath()));
                }
            }

        }catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    public static String getSortedFileList(Map<String, String> files) {
        if(files == null || files.isEmpty()) {
            return "";
        }
        ArrayList<String> fileStrings = new ArrayList<>();
        for(Map.Entry<String, String> entry : files.entrySet()) {
            String str = entry.getKey() + " " + entry.getValue();
            fileStrings.add(str);
        }
        Collections.sort(fileStrings);
        StringBuilder ret = new StringBuilder();
        for(String fileString : fileStrings) {
            ret.append(fileString).append("\n");
        }
        ret.deleteCharAt(ret.length() - 1);
        return ret.toString();
    }
}
