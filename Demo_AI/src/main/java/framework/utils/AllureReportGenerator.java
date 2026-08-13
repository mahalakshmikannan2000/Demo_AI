package framework.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Triggers the Allure CLI (via the checked-in .bat scripts) to turn allure-results/ into
 * an HTML report - either a single shareable file or a full history-tracked report - the
 * Allure Report capability highlighted in this demo.
 */
public class AllureReportGenerator extends WebActions {

    private static final Logger logger = LoggerFactory.getLogger(AllureReportGenerator.class);

    private static final String CMD_EXE_PATH =
            (System.getenv("SystemRoot") != null ? System.getenv("SystemRoot") : "C:\\Windows")
                    + "\\System32\\cmd.exe";
    private static final String BASH_PATH = "/bin/bash";

    /*
     * Method Description : Generates the Allure report, choosing single-file or history mode
     * based on the "singleAllure" config flag. Never lets a report failure break the suite.
     * Input Parameter(s) if any : None
     * Output Parameter(s) if any : void
     */
    public static void generateAllureReport() {
        boolean singleAllure = Boolean.parseBoolean(getRunTimeVariables("singleAllure"));
        logger.info("Allure Mode = {}", singleAllure ? "SINGLE FILE" : "HISTORY");

        try {
            if (singleAllure) {
                generateAllureSingleFile();
            } else {
                generateAllureWithHistory();
            }
        } catch (Exception e) {
            logger.warn("Allure generation failed. Ignoring to protect pipeline & Extent.", e);
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private static void generateAllureSingleFile() {
        try {
            ProcessBuilder pb;
            if (isWindows()) {
                pb = new ProcessBuilder(CMD_EXE_PATH, "/c", "generate-allure-single-file.bat");
            } else {
                pb = new ProcessBuilder(BASH_PATH, "-c",
                        "allure generate allure-results --single-file --clean -o allure-report/single");
            }
            pb.inheritIO();
            pb.start().waitFor();
            logger.info("Allure single-file generation attempted");
        } catch (Exception e) {
            logger.warn("Single-file Allure failed (ignored)", e);
        }
    }

    private static void generateAllureWithHistory() {
        try {
            ProcessBuilder pb;
            if (isWindows()) {
                pb = new ProcessBuilder(CMD_EXE_PATH, "/c", "generate-allure-history-report.bat");
            } else {
                pb = new ProcessBuilder(BASH_PATH, "-c",
                        "allure generate allure-results -o allure-report/reports/allure_report --clean");
            }
            pb.inheritIO();
            pb.start().waitFor();
            logger.info("Allure history generation attempted");
        } catch (Exception e) {
            logger.warn("History Allure failed (ignored)", e);
        }
    }
}
