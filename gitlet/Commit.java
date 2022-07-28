package gitlet;


import java.io.Serializable;
import java.util.HashMap;

public class Commit implements Serializable {


    /** message. */
    private String message;



    /** timeStamp. */
    private String timeStamp;

    /** parent. */
    private Commit parent;



    /** blob. */
    private HashMap<String, String> blob;


    /** commitHash. */
    private String commitHash;


    /** branch. */
    private String branch;




    public Commit(String msg, Commit par, String time,
                  HashMap<String, String> blo, String branchName) {
        message = msg;
        parent = par;
        timeStamp = time;
        commitHash = getCommitHash1();
        blob = blo;
        branch = branchName;
    }
    public String getCommitHash1() {
        byte[] val = Utils.serialize(this);
        return Utils.sha1(val);
    }
    public String getCommitHash() {
        return commitHash;
    }
    public String getMessage() {
        return message;
    }



    public String getTimeStamp() {
        return timeStamp;
    }



    public Commit getParent() {
        return parent;
    }


    public HashMap<String, String> getBlob() {
        return blob;
    }


    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch2) {
        this.branch = branch2;
    }

    public int len() {
        int count = 0;
        Commit iter = this;
        while (iter.parent != null) {
            count = count + 1;
            iter = iter.parent;
        }
        return count;
    }
}
