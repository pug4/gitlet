package gitlet;

import java.io.File;
import java.io.Serializable;

/**
 * Driver class for Gitlet, the tiny stupid version-control system.
 *
 * @author Jayu Patel.
 */
public class Main implements Serializable {
    /**
     * repo.
     */
    private static Repository repo = new Repository();

    /**
     * Usage: java gitlet.Main ARGS, where ARGS contains
     * <COMMAND> <OPERAND> ....
     */
    public static void main(String... args) {

        File gitFolder = new File(".gitlet");
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            return;
        }
        if (!args[0].equals("init") && !gitFolder.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }
        switch (args[0]) {
        case "commit":
            commiting(args);
            break;
        case "add":
            adding(args);
            break;
        case "log":
            loging(args);
            break;
        case "rm":
            removing(args);
            break;
        case "init":
            initing(args);
            break;
        case "global-log":
            globalLoging(args);
            break;
        case "find":
            findinging(args);
            break;
        case "status":
            statusing(args);
            break;
        case "checkout":
            checkingOut(args);
            break;
        case "branch":
            branching(args);
            break;
        case "rm-branch":
            rmbranching(args);
            break;
        case "reset":
            resting(args);
            break;
        case "merge":
            merging(args);
            break;
        default:
            System.out.println("No command with that name exists.");
        }
        repo.setRepo();
        repo.setStagingArea();
        repo.saveStagingArea();
        repo.saveRepo();
        return;
    }

    public static boolean argumentSizeChecker
    (int len, boolean checkout, String... args) {
        if (args.length == len) {
            return true;
        }
        if (!checkout) {
            System.out.println("Incorrect Operands.");
        }

        return false;
    }

    public static void commiting(String... args) {
        if (args[1].equals("")) {
            System.out.println("Please enter a commit message.");
        } else if (argumentSizeChecker(2, false, args)) {
            repo.commit(args[1]);
        }
    }

    public static void adding(String... args) {
        if (argumentSizeChecker(2, false, args)) {
            repo.add(args[1]);
        }
    }

    public static void loging(String... args) {
        if (argumentSizeChecker(1, false, args)) {
            repo.log();
        }
    }

    public static void removing(String... args) {
        if (argumentSizeChecker(2, false, args)) {
            repo.remove(args[1]);
        }
    }

    public static void initing(String... args) {
        if (argumentSizeChecker(1, false, args)) {
            repo.init();
        }
    }

    public static void findinging(String... args) {
        if (argumentSizeChecker(2, false, args)) {
            repo.find(args[1]);
        }
    }

    public static void checkingOut(String... args) {
        if (argumentSizeChecker(3, true, args)) {
            repo.checkOutFile(args[2]);
        } else if (argumentSizeChecker(4, true, args)) {
            if (args[2].equals("--")) {
                repo.checkOutFileCommitId(args[1], args[3]);
            } else {
                System.out.println("Incorrect operands.");
            }
        } else if (argumentSizeChecker(2, true, args)) {
            repo.checkOutBranches(args[1]);
        }
    }

    public static void statusing(String... args) {
        if (argumentSizeChecker(1, false, args)) {
            repo.status();
        }
    }

    public static void globalLoging(String... args) {
        if (argumentSizeChecker(1, false, args)) {
            repo.globalLog();
        }
    }
    public static void branching(String... args) {
        if (argumentSizeChecker(2, false, args)) {
            repo.branch(args[1]);
        }
    }
    public static void rmbranching(String... args) {
        if (argumentSizeChecker(2, false, args)) {
            repo.rmBranch(args[1]);
        }
    }
    public static void resting(String... args) {
        if (argumentSizeChecker(2, false, args)) {
            repo.reset(args[1]);
        }
    }
    public static void merging(String... args) {
        if (argumentSizeChecker(2, false, args)) {
            repo.merge(args[1]);
        }
    }
}
