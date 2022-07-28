package gitlet;



import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Map.Entry;

public class Repository implements Serializable {
    /**
     * staging.
     */
    private StagingArea staging = new StagingArea();
    /**
     * commitArrayList.
     */
    private ArrayList<Commit> commitArrayList = new ArrayList<>();
    /**
     * branches.
     */
    private HashMap<String, Commit> branches = new HashMap<String, Commit>();
    /**
     * head.
     */
    private Commit head;
    /**
     * cwd.
     */
    private File cwd = new File(System.getProperty("user.dir"));
    /**
     * current Branch.
     */
    private String currBranch;

    public static void main(String... files) {

    }

    public String timeFormatter() {
        LocalDateTime curr = LocalDateTime.now();
        DateTimeFormatter f = DateTimeFormatter.ofPattern
                ("EEE MMM dd kk:mm:ss yyyy");
        return curr.format(f) + " -0800";
    }

    public void init() {

        File gitFolder = new File(".gitlet");
        Commit init = new Commit("initial commit",
                null, timeFormatter(), new HashMap<>(), "master");

        File gitBlobs = new File(".gitlet/blob");
        File gitBranches = new File(".gitlet/branches");
        File gitCommits = new File(".gitlet/commit");

        if (gitFolder.exists()) {
            System.out.println("A Gitlet version-control "
                    + "system already exists in the current directory.");
            saveStagingArea();
            saveRepo();
            return;
        }
        commitArrayList.add(init);
        branches.put("master", init);
        head = init;
        currBranch = "master";
        gitFolder.mkdir();
        gitBlobs.mkdir();
        gitBranches.mkdir();
        gitCommits.mkdir();
        Utils.writeObject(new File(gitCommits
                + "/" + init.getCommitHash() + ".txt"), init);
        saveStagingArea();
        saveRepo();
    }

    public void reset(String commitHash) {
        setRepo();
        setStagingArea();
        boolean found = false;
        List<String> allFiles = Utils.plainFilenamesIn(cwd);
        for (String file : allFiles) {
            String blobContent = Utils.sha1
                    (Utils.readContentsAsString(new File(file)));
            if (!staging.getFilesAdded().containsKey(file)
                    && !staging.getFilesAdded().containsValue(blobContent)
                    && !head.getBlob().containsValue(blobContent)) {
                System.out.println("There is an untracked file in "
                        + "the way; delete it, or add and commit it first.");
                saveStagingArea();
                saveRepo();
                return;
            }
        }
        for (Commit commit : commitArrayList) {
            if (commit.getCommitHash().equals(commitHash)) {
                found = true;
                for (String file : allFiles) {
                    if (!commit.getBlob().containsKey(file)) {
                        new File(cwd + "/" + file).delete();
                    }
                }
                for (Entry<String, String> blobEntry
                        : commit.getBlob().entrySet()) {
                    String blobContent = Utils.readContentsAsString(
                            new File(".gitlet/blob/"
                                    + blobEntry.getValue() + ".txt"));
                    Utils.writeContents(new File(cwd + "/"
                                    + blobEntry.getKey()),
                            blobContent);
                    staging.getFilesAdded().clear();
                    staging.getFilesRemoved().clear();
                    saveStagingArea();
                    saveRepo();
                    break;
                }
                head = commit;
                branches.put(head.getBranch(), head);
            }
        }
        if (!found) {
            System.out.println("No commit with that id exists.");
        }
        saveStagingArea();
        saveRepo();
    }

    public void commit(String newMessage) {
        setStagingArea();
        setRepo();
        File gitFolder = new File(".gitlet");
        if (staging.getFilesAdded().isEmpty()
                && staging.getFilesRemoved().isEmpty()) {
            System.out.println("No changes added to the commit.");
            saveStagingArea();
            saveRepo();
            return;
        }

        ArrayList<String> filesToCommit =
                new ArrayList<>(staging.getFilesAdded().keySet());
        HashMap<String, String> blob =
                new HashMap<String, String>(head.getBlob());
        for (String file : filesToCommit) {
            blob.put(file, staging.getFilesAdded().get(file));
            staging.getFilesAdded().remove(file);
        }
        for (String file : staging.getFilesRemoved()) {
            if (head.getBlob().containsKey(file)) {
                blob.remove(file);
                Utils.restrictedDelete(
                        new File(cwd + "/" + file));
            }
        }
        staging.getFilesRemoved().clear();
        Commit parentNewCommit = head;
        Commit newCommit = new Commit(newMessage,
                parentNewCommit, timeFormatter(),
                blob, head.getBranch());
        branches.put(head.getBranch(), newCommit);
        if (commitArrayList.contains(newCommit)) {
            saveStagingArea();
            saveRepo();
            System.out.println("already commited");
            return;
        }

        commitArrayList.add(newCommit);
        Utils.writeObject(new File(".gitlet/commit/"
                + newCommit.getCommitHash() + ".txt"), newCommit);
        head = newCommit;
        saveStagingArea();
        saveRepo();

    }

    public void add(String fileName) {

        setStagingArea();
        setRepo();

        File addFile = new File(fileName);
        if (!addFile.exists()) {
            System.out.println("File does not exist.");
            saveStagingArea();
            saveRepo();
            return;
        }
        byte[] blobContent = Utils.readContents(addFile);
        String hash = Utils.sha1(blobContent);

        if (staging.getFilesRemoved().contains(fileName)) {
            staging.getFilesRemoved().remove(fileName);
            Utils.writeContents(
                    new File(".gitlet/blob/"
                            + hash + ".txt"), blobContent);
        } else if (head.getBlob()
                .get(fileName) != null
                && head.getBlob().get(fileName).equals(hash)) {
            saveStagingArea();
            saveRepo();
            return;
        } else if (!staging.getFilesAdded().containsValue(hash)) {
            staging.getFilesAdded().put(fileName, hash);
            Utils.writeContents(
                    new File(".gitlet/blob/" + hash
                            + ".txt"), blobContent);
        } else if (staging.getFilesAdded().containsValue(hash)
                && !staging.getFilesAdded()
                .getOrDefault(fileName, "").equals(hash)) {
            staging.getFilesAdded().put(fileName, hash);
            Utils.writeContents(
                    new File(".gitlet/blob/" + hash + ".txt"), blobContent);
        }

        saveStagingArea();
        saveRepo();
    }

    public void remove(String fileName) {
        setStagingArea();
        setRepo();
        String blobContent = "";
        File removeFile = new File(fileName);
        if (!removeFile.exists() && head.getBlob().containsKey(fileName)) {
            staging.getFilesRemoved().add(fileName);
            saveStagingArea();
            saveRepo();
            return;
        }
        if (!removeFile.exists()) {
            System.out.println("File does not exist.");
            saveStagingArea();
            saveRepo();
            return;
        }
        blobContent = Utils.sha1(Utils.readContentsAsString(removeFile));
        if (!new File(".gitlet/blob/" + blobContent + ".txt").exists()) {
            System.out.println("No reason to remove the file.");
            saveStagingArea();
            saveRepo();
            return;
        }
        if (staging.getFilesAdded().containsKey(fileName)) {
            staging.getFilesAdded().remove(fileName);
            staging.getFilesRemoved().remove(fileName);
            new File(".gitlet/blob/" + blobContent + ".txt").delete();
        }
        if (head.getBlob().containsKey(fileName)) {
            staging.getFilesRemoved().add(fileName);
            new File(cwd + "/" + fileName).delete();
        }
        saveStagingArea();
        saveRepo();
    }

    public void branch(String branchName) {
        setStagingArea();
        setRepo();
        if (!branches.containsKey(branchName)) {
            branches.put(branchName, head);
            currBranch = branchName;
        } else {
            System.out.println("A branch with that name already exists.");
        }
        saveStagingArea();
        saveRepo();
    }

    public void status() {
        setStagingArea();
        setRepo();
        List<String> allFiles = Utils.plainFilenamesIn(cwd);
        System.out.println("=== Branches ===");
        ArrayList<String> branchSorting = new ArrayList<>();
        for (Entry<String, Commit> entry : branches.entrySet()) {
            if (head.getBranch().equals(entry.getKey())) {
                branchSorting.add("*" + entry.getKey());
            } else {
                branchSorting.add(entry.getKey());
            }
        }
        for (int i = branchSorting.size() - 1; i >= 0; i--) {
            System.out.println(branchSorting.get(i));
        }
        System.out.println();
        System.out.println("=== Staged Files ===");
        for (Entry<String, String> entry : staging.getFilesAdded().entrySet()) {
            System.out.println(entry.getKey());
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        for (String fileToBeRemoved : staging.getFilesRemoved()) {
            System.out.println(fileToBeRemoved);
        }
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        for (Entry<String, String> filesAddedStageing
                : head.getBlob().entrySet()) {
            if (new File(cwd + "/" + filesAddedStageing.getKey()).exists()) {
                String blobContent = Utils.sha1(Utils.readContentsAsString(
                        new File(cwd + "/"
                                + filesAddedStageing.getKey())));
                if (!new File(".gitlet/blob/"
                        + blobContent + ".txt").exists()
                        && head.getBlob()
                        .containsKey(filesAddedStageing.getKey())
                        && !blobContent.equals
                        (filesAddedStageing.getValue())) {
                    System.out.println(filesAddedStageing.getKey()
                            + " (modified)");
                }
            } else if (!staging.getFilesRemoved()
                    .contains(filesAddedStageing.getKey())
                    && head.getBlob()
                    .containsKey(filesAddedStageing.getKey())) {
                System.out.println(filesAddedStageing.getKey() + " (deleted)");
            }
        }
        System.out.println();
        System.out.println("=== Untracked Files ===");
        findUntracked();
        System.out.println();
        saveStagingArea();
        saveRepo();
    }
    public void findUntracked() {
        List<String> allFiles = Utils.plainFilenamesIn(cwd);
        for (String file : allFiles) {
            if (!staging.getFilesAdded().containsKey(file)
                    && !head.getBlob().containsKey(file)) {
                System.out.println(file);
            }
        }
    }
    public void checkOutFile(String fileName) {
        setRepo();
        setStagingArea();
        String blobHash = head.getBlob().get(fileName);
        String blobContent = Utils.readContentsAsString(
                new File(".gitlet/blob/" + blobHash + ".txt"));
        File fileToChange = new File(fileName);
        Utils.writeContents(fileToChange, blobContent);
        saveStagingArea();
        saveRepo();
    }

    public void checkOutBranches(String branchName) {
        setRepo();
        setStagingArea();

        if (!branches.containsKey(branchName)) {
            System.out.println("No such branch exists.");
            saveStagingArea();
            saveRepo();
            return;
        }
        if (head.getBranch().equals(branchName)) {
            System.out.println("No need to checkout the current branch.");
            saveStagingArea();
            saveRepo();
            return;
        }
        Commit x = branches.get(branchName);
        for (Entry<String, String> entry : head.getBlob().entrySet()) {
            if (x.getBlob().get(entry.getKey()) == null) {
                new File(entry.getKey()).delete();
            }
        }
        List<String> allFiles = Utils.plainFilenamesIn(cwd);
        for (String file : allFiles) {
            if (!staging.getFilesAdded().containsKey(file)
                    && !head.getBlob().containsKey(file)) {
                System.out.println("There is an untracked file "
                        +  "in the way; delete it, or"
                        + " add and commit it first.");
                saveStagingArea();
                saveRepo();
                return;
            }
        }
        for (Entry<String, String> stringStringEntry : x.getBlob().entrySet()) {
            String blobContent = Utils.readContentsAsString
                    (new File(".gitlet/blob/"
                            + stringStringEntry.getValue() + ".txt"));
            File fileToChange = new File(stringStringEntry.getKey());
            Utils.writeContents(fileToChange, blobContent);
        }
        head = x;
        head.setBranch(branchName);
        currBranch = branchName;
        staging.getFilesAdded().clear();
        staging.getFilesRemoved().clear();
        saveStagingArea();
        saveRepo();
    }

    public void style() {
        saveRepo();
        saveStagingArea();
    }
    public void style2() {
        setRepo();
        setStagingArea();
    }
    @SuppressWarnings("unchecked")
    public HashSet addSet(HashSet set,
                          Commit branchCName,
                          Commit splitPoint) {
        set.addAll(branchCName.getBlob().keySet());
        set.addAll(splitPoint.getBlob().keySet());
        set.addAll(head.getBlob().keySet());
        return set;
    }

    @SuppressWarnings("unchecked")
    public void merge(String branchName) {
        style2();
        if (mergeErrors(branchName)) {
            style();
            return;
        }
        Commit branchCommit = branches.get(branchName);
        Commit spiltPoint = splitDetechor(branchCommit, head);
        if (spiltPoint == null) {
            style();
            return;
        }
        HashSet<String> set = addSet(new HashSet<>(), branchCommit, spiltPoint);
        HashMap blobs = new HashMap<>();
        for (String file : set) {
            String splitPointBlobsContent = spiltPoint.getBlob()
                    .getOrDefault(file, "");
            String branchNameBlobsContent = branchCommit.getBlob()
                    .getOrDefault(file, "");
            String headBlobContent = head.getBlob().getOrDefault(file, "");
            if (!splitPointBlobsContent.equals(branchNameBlobsContent)
                    && headBlobContent.equals(branchNameBlobsContent)) {
                checkOutFileCommitId(branchCommit.getCommitHash(), file);
                staging.getFilesAdded().put(file, branchNameBlobsContent);
            }
            if (makeCheckMerge5(file, branchCommit, spiltPoint)) {
                blobs.put(file, headBlobContent);
            }
            if (makeCheckMerge3(file, branchCommit, spiltPoint)) {
                blobs.put(file, headBlobContent);
            }
            if (checkingMergeStuff(file, branchCommit, spiltPoint)) {
                blobs.put(file, head.getBlob().getOrDefault(file, ""));
            }
            if (makeCheckMerge(file, branchCommit, spiltPoint)) {
                String blobContent = Utils.readContentsAsString(
                        new File(".gitlet/blob/"
                                + branchCommit.getBlob().get(file) + ".txt"));
                Utils.writeContents(new File(cwd + "/" + file), blobContent);
                blobs.put(file, blobContent);
            }
            if (makeCheckMerge2(file, branchCommit, spiltPoint)) {
                new File(cwd + "/" + file).delete();
                if (blobs.containsKey(file)) {
                    blobs.remove(file);
                }
            }
            if (!branchNameBlobsContent.equals(headBlobContent)
                    && !headBlobContent.equals(splitPointBlobsContent)
                    && !branchNameBlobsContent.equals(splitPointBlobsContent)) {
                blobs = mergeConflict(file, branchCommit, blobs);
            }
        }
        @SuppressWarnings("unchecked")
        Commit mergeCommit = new Commit(makeCommitMessage(branchName),
                head, timeFormatter(), blobs, head.getBranch());
        commitArrayList.add(mergeCommit);
        head = mergeCommit;
        style();
    }


    public String makeCommitMessage(String branchToCommit) {
        String msg = "Merged "
                + branchToCommit + " into " + head.getBranch() + ".";
        return msg;
    }
    public boolean makeCheckMerge5(String f,
                                   Commit branchCName,
                                   Commit splitPoint) {
        if (splitPoint.getBlob().containsKey(f)
                && !splitPoint.getBlob().getOrDefault(f, "")
                .equals(branchCName.getBlob().getOrDefault(f, ""))
                && splitPoint.getBlob().getOrDefault(f, "")
                .equals(head.getBlob().getOrDefault(f, ""))) {
            return true;
        }
        return false;
    }
    public boolean makeCheckMerge3(String f,
                                  Commit branchCName,
                                  Commit splitPoint) {
        if ((!splitPoint.getBlob().getOrDefault(f, "")
                .equals(head.getBlob().getOrDefault(f, ""))
                && head.getBlob().getOrDefault(f, "")
                .equals(branchCName.getBlob().getOrDefault(f, "")))
                || (!splitPoint.getBlob().getOrDefault(f, "")
                .equals(head.getBlob().getOrDefault(f, ""))
                && head.getBlob().getOrDefault(f, "")
                .equals(branchCName.getBlob().getOrDefault(f, "")))) {
            return true;
        }
        return false;

    }
    public boolean checkingMergeStuff(String f,
                                  Commit branchCName,
                                  Commit splitPoint) {
        if (!branchCName.getBlob().containsKey(f)
                && !splitPoint.getBlob().containsKey(f)
                && head.getBlob().containsKey(f)) {
            return true;
        }
        return false;
    }
    public boolean makeCheckMerge(String f,
                                  Commit branchCName,
                                  Commit splitPoint) {
        if (!splitPoint.getBlob().containsKey(f)
                && !head.getBlob().containsKey(f)
                && branchCName.getBlob().containsKey(f)) {
            return true;
        }
        return false;
    }
    public boolean makeCheckMerge2(String f,
                                  Commit branchCName,
                                  Commit splitPoint) {
        if (splitPoint.getBlob().containsKey(f)
                && splitPoint.getBlob().getOrDefault(f, "")
                .equals(head.getBlob().getOrDefault(f, ""))
                && !branchCName.getBlob().containsKey(f)) {
            return true;
        }
        return false;
    }
    @SuppressWarnings("unchecked")
    public HashMap mergeConflict(String f, Commit branchCName, HashMap blobs) {
        System.out.println("Encountered a merge conflict.");
        if (head.getBlob().get(f) != null
                && branchCName.getBlob().get(f) != null) {
            String headContent = Utils.readContentsAsString(
                    new File(".gitlet/blob/"
                            + head.getBlob().get(f) + ".txt"));
            String branchNameContent = Utils.readContentsAsString(
                    new File(".gitlet/blob/"
                            + branchCName.getBlob().get(f) + ".txt"));
            Utils.writeContents(new File(cwd + "/" + f),
                    "<<<<<<< HEAD\n" + headContent
                            + "=======\n" + branchNameContent + ">>>>>>>\n");
            blobs.put(f, Utils.sha1(Utils.readContentsAsString(
                    new File(cwd + "/" + f))));
            Utils.writeContents(new File(".gitlet/blob/"
                    + Utils.sha1(Utils.readContentsAsString(
                        new File(cwd + "/" + f))) + ".txt"));
        } else {
            String headContent = Utils.readContentsAsString(
                    new File(".gitlet/blob/"
                            + head.getBlob().get(f) + ".txt"));
            Utils.writeContents(new File(cwd + "/" + f),
                    "<<<<<<< HEAD\n" + headContent
                            + "=======\n" + ">>>>>>>\n");
            blobs.put(f, Utils.sha1(Utils.readContentsAsString(
                    new File(cwd + "/" + f))));
            Utils.writeContents(new File(".gitlet/blob/"
                    + Utils.sha1(Utils.readContentsAsString(
                        new File(cwd + "/" + f))) + ".txt"));
        }
        return blobs;
    }

    public boolean mergeErrors(String branchName) {
        List<String> allFiles = Utils.plainFilenamesIn(cwd);
        for (String file : allFiles) {
            String blobContent = Utils.sha1
                    (Utils.readContentsAsString(new File(file)));
            if (!staging.getFilesAdded().containsValue(blobContent)
                    && !head.getBlob().containsKey(file)) {
                System.out.println("There is an untracked "
                        + "file in the way; delete it,"
                        + " or add and commit it first.");
                return true;
            }
        }
        if (!staging.getFilesAdded().isEmpty()
                || !staging.getFilesRemoved().isEmpty()) {
            System.out.println("You have uncommitted changes.");
            return true;
        } else if (branchName.equals(head.getBranch())) {
            System.out.println("Cannot merge a branch with itself.");
            return true;
        } else if (!branches.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
            return true;
        }

        return false;
    }

    public Commit splitDetechor(Commit branch1, Commit branch2) {
        Commit iter1 = branch1;
        Commit iter2 = branch2;
        while (iter1 != null
                && !iter1.getMessage().equals(iter2.getMessage())) {
            iter1 = iter1.getParent();
            iter2 = iter2.getParent();
        }
        if (branch1.getMessage() != null
                && iter2.getMessage().equals(branch1.getMessage())) {
            System.out.println("Given branch is an"
                    + " ancestor of the current branch.");
            return null;
        }
        return iter1;
    }

    public void checkOutFileCommitId(String commitID, String fileName) {
        setRepo();
        setStagingArea();
        String blobHash = "";
        boolean commitIdExists = false;
        File fileToChange = new File(fileName);
        for (Commit commit : commitArrayList) {
            if (commit.getCommitHash().contains(commitID)) {
                blobHash = blobHash + commit.getBlob().get(fileName);
                commitIdExists = true;
            }
        }
        if (!commitIdExists) {
            System.out.println("No commit with that id exists.");
            saveRepo();
            saveStagingArea();
            return;
        }
        if (head.getBlob().get(fileName) == null) {
            System.out.println("File does not exist in that commit.");
            saveRepo();
            saveStagingArea();
            return;
        }
        String blobContent = Utils.readContentsAsString
                (new File(".gitlet/blob/" + blobHash + ".txt"));
        Utils.writeContents(fileToChange, blobContent);
        saveStagingArea();
        saveRepo();
    }

    public void find(String commitMessage) {
        setRepo();
        setStagingArea();
        boolean found = false;
        for (Commit commit : commitArrayList) {
            if (commit.getMessage().equals(commitMessage)) {
                System.out.println(commit.getCommitHash());
                found = true;
            }
        }
        if (!found) {
            System.out.println("Found no commit with that message.");
        }
        saveRepo();
        saveStagingArea();
    }

    public void log() {
        setRepo();
        setStagingArea();
        Commit iter = head;
        while (iter != null) {
            System.out.println("===");
            System.out.println("commit " + iter.getCommitHash());
            System.out.println("Date: " + iter.getTimeStamp());
            System.out.println(iter.getMessage());
            System.out.println();
            iter = iter.getParent();
        }

        saveRepo();
        saveStagingArea();
    }

    public void rmBranch(String branchToRm) {
        setStagingArea();
        setRepo();
        if (!branches.containsKey(branchToRm)) {
            System.out.println("A branch with that name does not exist.");
            saveStagingArea();
            saveRepo();
            return;
        }
        if (head.getBranch().equals(branchToRm)) {
            System.out.println("Cannot remove the current branch.");
            saveStagingArea();
            saveRepo();
            return;
        }
        branches.remove(branchToRm);
        saveRepo();
        saveStagingArea();
    }

    public void globalLog() {
        setRepo();
        setStagingArea();

        for (int i = commitArrayList.size() - 1; i >= 0; i--) {
            Commit temp = commitArrayList.get(i);
            System.out.println("===");
            System.out.println("commit " + temp.getCommitHash());
            System.out.println("Date: " + temp.getTimeStamp());
            System.out.println(temp.getMessage());
            System.out.println();
        }

        saveRepo();
        saveStagingArea();
    }

    public void saveStagingArea() {
        File gitStaging = new File(".gitlet/StagingArea1.txt");
        Utils.writeObject(gitStaging, staging.getFilesAdded());
        File gitStaging2 = new File(".gitlet/StagingArea2.txt");
        Utils.writeObject(gitStaging2, staging.getFilesRemoved());

    }

    @SuppressWarnings("unchecked")
    public void setStagingArea() {
        File gitStaging = new File(".gitlet/StagingArea1.txt");
        staging.setFilesAdded(Utils.readObject(gitStaging, HashMap.class));
        File gitStaging2 = new File(".gitlet/StagingArea2.txt");
        staging.setFilesRemoved(Utils.readObject(gitStaging2, ArrayList.class));
    }
    public void saveRepo() {
        File gitRepo1 = new File(".gitlet/Repo1.txt");
        File gitRepo2 = new File(".gitlet/Repo2.txt");
        File gitRepo3 = new File(".gitlet/Repo3.txt");
        File gitRepo4 = new File(".gitlet/Repo4.txt");
        Utils.writeObject(gitRepo1, commitArrayList);
        Utils.writeObject(gitRepo2, head);
        Utils.writeObject(gitRepo3, branches);
        Utils.writeObject(gitRepo4, currBranch);

    }
    @SuppressWarnings("unchecked")
    public void setRepo() {
        File gitRepo1 = new File(".gitlet/Repo1.txt");
        File gitRepo2 = new File(".gitlet/Repo2.txt");
        File gitRepo3 = new File(".gitlet/Repo3.txt");
        File gitRepo4 = new File(".gitlet/Repo4.txt");
        commitArrayList = Utils.readObject(gitRepo1, ArrayList.class);
        head = Utils.readObject(gitRepo2, Commit.class);
        branches = Utils.readObject(gitRepo3, HashMap.class);
        currBranch = Utils.readObject(gitRepo4, String.class);
    }

}
