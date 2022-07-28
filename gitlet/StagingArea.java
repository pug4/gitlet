package gitlet;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class StagingArea implements Serializable {


    /** Files Added. */
    private HashMap<String, String> filesAdded = new HashMap<>();
    /** Files Removed. */
    private ArrayList<String> filesRemoved = new ArrayList<>();

    public StagingArea() {
    }

    public HashMap<String, String> getFilesAdded() {
        return filesAdded;
    }

    public void setFilesAdded(HashMap<String, String> filesAdded1) {
        this.filesAdded = filesAdded1;
    }

    public ArrayList<String> getFilesRemoved() {
        return filesRemoved;
    }

    public void setFilesRemoved(ArrayList<String> filesRemoved1) {
        this.filesRemoved = filesRemoved1;
    }

}
