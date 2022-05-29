package test;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.BasicConfigurator;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import sagex.ISageAPIProvider;
import sagex.SageAPI;
import sagex.phoenix.Phoenix;
import sagex.phoenix.util.BaseBuilder;
import sagex.stub.StubSageAPI;

public class InitPhoenix {
    private static boolean initialized = false;

    public static File PHOENIX_HOME;
    public static File PROJECT_ROOT;

    public static synchronized void init(boolean deleteOld, boolean stubapi) throws IOException {
        init(deleteOld, stubapi, false);
    }

    public static synchronized void init(boolean deleteOld, boolean stubapi, boolean force) throws IOException {
        init(deleteOld, (stubapi)?new StubSageAPI():null, force);
    }

    public static synchronized void init(boolean deleteOld, ISageAPIProvider api, boolean force) throws IOException {
        // allow for xml parsing errors
        BaseBuilder.failOnError = true;

        if (api!=null) {
            SageAPI.setProvider(api);
        }

        System.out.println("Configure Logging");
        BasicConfigurator.resetConfiguration();
        BasicConfigurator.configure();
        LogManager.getRootLogger().setLevel(Level.DEBUG);
        System.out.println("Copying Phoenix Configuration to Testing Area...");
        //File baseDir = new File("."); // test runner should put us into target/testing

        if (!force && initialized) {
            System.out.println("InitPhoenix: already done.");

            // for testing we are clearing out the phoenix state after each test
            Phoenix.getInstance().reinit();
            return;
        }

        PROJECT_ROOT = getProjectRoot(new File("."));
        if (!new File(PROJECT_ROOT,"src").exists()) {
            throw new RuntimeException("PROJECT ROOT doesn't appear correct: " + PROJECT_ROOT.getAbsolutePath());
        }
        PHOENIX_HOME = new File(PROJECT_ROOT, "target/testing");
        PHOENIX_HOME.mkdirs();

        if (deleteOld) {
            if ("testing".equals(PHOENIX_HOME.getCanonicalFile().getName())) {
                FileUtils.cleanDirectory(PHOENIX_HOME);
            } else {
                throw new RuntimeException("Trying clean baseDir that is not the testing dir: " + PHOENIX_HOME.getAbsolutePath());
            }
        }

        FileUtils.copyDirectory(new File(PROJECT_ROOT,"src/plugins/phoenix-core/STVs"), new File(PHOENIX_HOME, "STVs"), new FileFilter() {
            public boolean accept(File pathname) {
                System.out.println("Copy: " + pathname);
                return !(pathname.getName().startsWith("."));
            }
        });

        System.out.println("Initializing Phoneix with testing dir: " + PHOENIX_HOME.getAbsolutePath());
        System.setProperty("phoenix/sagetvHomeDir", PHOENIX_HOME.getAbsolutePath());
        System.setProperty("phoenix/testing", "true");

        // for testing we are clearing out the phoenix state after each test
        Phoenix.getInstance().reinit();

        System.out.println("Phoenix has been initialized.");

        System.out.println("Configure Logging Again...");
        BasicConfigurator.resetConfiguration();
        BasicConfigurator.configure();
        LogManager.getRootLogger().setLevel(Level.DEBUG);
        Logger.getLogger(InitPhoenix.class).debug("Logging Configured");
        initialized = true;
    }

    private static File getProjectRoot(File dir) {
        File d = new File(dir, "src");
        if (d.exists() && d.isDirectory()) return dir;
        return getProjectRoot(dir.getAbsoluteFile().getParentFile());
    }

    public static File ProjectHome(String path) {
        if (PROJECT_ROOT==null) throw new RuntimeException("InitPhoenix.init() must be run before you can access files");
        if (path.startsWith("/") || path.startsWith("..")) throw new RuntimeException("Paths must be relative to the ProjectRoot; PATH: " + path);
        return new File(PROJECT_ROOT, path);
    }

    public static File PhoenixHome(String path) {
        if (PROJECT_ROOT==null) throw new RuntimeException("InitPhoenix.init() must be run before you can access files");
        if (path.startsWith("/") || path.startsWith("..")) throw new RuntimeException("Paths must be relative to the ProjectRoot; PATH: " + path);
        return new File(PHOENIX_HOME, path);
    }
}
