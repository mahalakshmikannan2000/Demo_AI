package framework.utils;

import framework.config.PropertyReader;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IExecutionListener;
import org.testng.TestNG;
import org.testng.xml.*;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;

import static framework.utils.WebActions.getRunTimeVariables;

/**
 * Reads the Excel test matrix (src/main/resources/TestNGXML.xlsx) and dynamically builds +
 * runs a TestNG XmlSuite from it - the "dynamic TestNG XML generation" capability highlighted
 * in this demo. Run via: mvn exec:java -DappName=CSP_Regression (or ECS_Android / ECS_IOS / API_Regression).
 * If the Excel matrix does not exist yet, a demo matrix covering all four sample test cases is
 * generated automatically so the feature can be exercised out of the box.
 */
public class TestNGXMLGenerator {

    private static final Logger logger = LoggerFactory.getLogger(TestNGXMLGenerator.class);
    static String appName = getRunTimeVariables("appName");
    static String[] suiteArray = appName.split("_");
    static String appModule = suiteArray[0];
    static String suiteType = suiteArray[1];
    private final String suiteName;
    private final String testName;
    private final String className;
    private final String testCaseID;
    private final String methodName;
    private final String[] paramName;
    private final String[] paramValue;
    private final String executeFlag;
    public static String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    public static String resultsDir = PropertyReader.readProperty("allure.results.directory");

    private static final CountDownLatch EXECUTION_LATCH = new CountDownLatch(1);

    private TestNGXMLGenerator(Builder builder) {
        suiteName = builder.suiteName;
        testName = builder.testName;
        className = builder.className;
        testCaseID = builder.testCaseID;
        methodName = builder.methodName;
        paramName = builder.paramName;
        paramValue = builder.paramValue;
        executeFlag = builder.executeFlag;
    }

    public static class Builder {
        private String suiteName;
        private String testName;
        private String className;
        private String testCaseID;
        private String methodName;
        private String[] paramName;
        private String[] paramValue;
        private String executeFlag;

        public Builder setSuiteName(String suiteName) { this.suiteName = suiteName; return this; }
        public Builder setTestName(String testName) { this.testName = testName; return this; }
        public Builder setClassName(String className) { this.className = className; return this; }
        public Builder setTestCaseID(String testCaseID) { this.testCaseID = testCaseID; return this; }
        public Builder setMethodName(String methodName) { this.methodName = methodName; return this; }
        public Builder setParamName(String[] paramName) { this.paramName = paramName; return this; }
        public Builder setParamValue(String[] paramValue) { this.paramValue = paramValue; return this; }
        public Builder setExecuteFlag(String executeFlag) { this.executeFlag = executeFlag; return this; }

        public TestNGXMLGenerator build() { return new TestNGXMLGenerator(this); }
    }

    public String getTestCaseID() {
        return this.testCaseID;
    }

    /*
     * Method Description : Main entry to read Excel, generate XML, and run suite
     * Input Parameter(s) if any : args (String[])
     * Output Parameter(s) if any : void
     */
    public static void main(String[] args) {
        logger.info("[MAIN] Starting TestNG execution");
        boolean executionFailed = false;
        try {
            String outputPath = "generated.xml";
            String path = PropertyReader.readProperty("testNGPath");
            ensureExcelExists(path);
            List<TestNGXMLGenerator> testData = readExcel(path);
            if (testData.isEmpty()) {
                throw new RuntimeException("No executable test data found in Excel");
            }
            System.setProperty("allure.results.directory", resultsDir);
            XmlSuite suite = buildSuite(testData);
            if (suite.getTests().isEmpty()) {
                throw new RuntimeException("Generated TestNG suite has no tests. Aborting execution.");
            }

            writeSuiteToXmlFile(suite, outputPath);

            TestNG testng = new TestNG();
            testng.setXmlSuites(Collections.singletonList(suite));
            testng.addListener(new IExecutionListener() {
                @Override
                public void onExecutionFinish() {
                    logger.info("[MAIN] TestNG execution finished (IExecutionListener)");
                    EXECUTION_LATCH.countDown();
                }
            });
            testng.run();
            boolean hasFailure = testng.hasFailure();
            logger.info("TestNG completed. Failure = {}", hasFailure);
            EXECUTION_LATCH.await();
        } catch (Exception e) {
            executionFailed = true;
            logger.error("Framework execution error: {}", e.getMessage(), e);
        }

        if (executionFailed) {
            logger.info("[MAIN] Execution failed due to framework error");
            System.exit(1);
        } else {
            logger.info("[MAIN] Execution completed (test failures ignored)");
            System.exit(0);
        }
    }

    /*
     * Method Description : Generates a demo Excel test matrix (CSP/ECS/API sheets) at the given
     * path if one does not already exist, so the dynamic-XML feature works out of the box
     * Input Parameter(s) if any : path (String) - Excel file path
     * Output Parameter(s) if any : void
     */
    static void ensureExcelExists(String path) {
        File file = new File(path);
        if (file.exists()) {
            return;
        }
        logger.info("Excel test matrix not found at {}. Generating a demo matrix.", path);
        file.getParentFile().mkdirs();

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            String[] headers = {"suiteName", "testName", "className", "testCaseID", "methodName", "paramName", "paramValue", "executeFlag"};

            addSheetRow(workbook, "CSP", headers, new String[][]{
                    {"CSP_Regression", "CSP QA", "tests.web.CSPScripts", "TC001", "TC001_CSP_Successful_login", "browser,env", "chrome,CSP_QA", "Y"}
            });

            addSheetRow(workbook, "ECS", headers, new String[][]{
                    {"ECS_Android", "Android QA", "tests.mobile.ECSScriptsAndroid", "TC01", "TC01_Android_logInToECSWithValidID", "browser,env", "android_app,ECS_QA", "Y"},
                    {"ECS_IOS", "iOS QA", "tests.mobile.ECSScriptsIOS", "TC01", "TC01_IOS_LoginToECSWithValidID", "browser,env", "ios_app,ECS_QA", "Y"}
            });

            addSheetRow(workbook, "API", headers, new String[][]{
                    {"API_Regression", "API QA", "tests.api.APIScripts", "TC01", "TC01_GETBearerToken", "browser,env", "api,API_TEST", "Y"}
            });

            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
            logger.info("Demo Excel test matrix created at {}", path);
        } catch (IOException e) {
            logger.error("Unable to generate demo Excel test matrix", e);
        }
    }

    private static void addSheetRow(XSSFWorkbook workbook, String sheetName, String[] headers, String[][] rows) {
        Sheet sheet = workbook.createSheet(sheetName);
        Row header = sheet.createRow(0);
        for (int c = 0; c < headers.length; c++) {
            header.createCell(c).setCellValue(headers[c]);
        }
        for (int r = 0; r < rows.length; r++) {
            Row row = sheet.createRow(r + 1);
            for (int c = 0; c < rows[r].length; c++) {
                row.createCell(c).setCellValue(rows[r][c]);
            }
        }
    }

    /*
     * Method Description : Reads test configuration from the Excel sheet named after the current appModule
     * Input Parameter(s) if any : path (String) - Excel file path
     * Output Parameter(s) if any : List<TestNGXMLGenerator>
     */
    static List<TestNGXMLGenerator> readExcel(String path) {
        List<TestNGXMLGenerator> data = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(path)) {
            Workbook workbook = new XSSFWorkbook(fis);
            Sheet sheet = workbook.getSheet(appModule);
            if (sheet == null) {
                logger.error("No sheet named '{}' found in {}", appModule, path);
                return data;
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                data.add(new TestNGXMLGenerator.Builder()
                        .setSuiteName(row.getCell(0).getStringCellValue())
                        .setTestName(row.getCell(1).getStringCellValue())
                        .setClassName(row.getCell(2).getStringCellValue())
                        .setTestCaseID(row.getCell(3).getStringCellValue())
                        .setMethodName(row.getCell(4).getStringCellValue())
                        .setParamName(row.getCell(5).getStringCellValue().split(","))
                        .setParamValue(row.getCell(6).getStringCellValue().split(","))
                        .setExecuteFlag(row.getCell(7).getStringCellValue()).build());
            }
            workbook.close();
        } catch (IOException e) {
            logger.error("Exception occurred while reading excel", e);
        }
        return data;
    }

    private static boolean isPipelineExecution() {
        return "true".equalsIgnoreCase(System.getenv("TF_BUILD"));
    }

    /*
     * Method Description : Builds XmlSuite based on test data
     * Input Parameter(s) if any : data (List<TestNGXMLGenerator>)
     * Output Parameter(s) if any : XmlSuite
     */
    static XmlSuite buildSuite(List<TestNGXMLGenerator> data) {
        XmlSuite suite = new XmlSuite();
        suite.setName(appModule + " " + suiteType + " Suite");
        suite.addListener("framework.listeners.CustomTestListener");
        if (!isPipelineExecution()) {
            suite.addListener("io.qameta.allure.testng.AllureTestNg");
        }

        Map<String, Map<String, List<String>>> testMap = new LinkedHashMap<>();
        Map<String, Map<String, String>> testParams = new LinkedHashMap<>();

        for (TestNGXMLGenerator row : data) {
            if (!"Y".equalsIgnoreCase(row.executeFlag) || !row.suiteName.contains(suiteType)) continue;

            testMap.computeIfAbsent(row.testName, k -> new LinkedHashMap<>())
                    .computeIfAbsent(row.className, k -> new ArrayList<>())
                    .add(row.methodName);

            Map<String, String> params = testParams.getOrDefault(row.testName, new LinkedHashMap<>());
            for (int i = 0; i < row.paramName.length; i++) {
                params.put(row.paramName[i], row.paramValue[i]);
            }
            params.put("testCaseID", row.getTestCaseID());
            testParams.put(row.testName, params);
        }

        setSuiteParameters(suite, testParams);
        configureParallelism(suite, testMap.size());

        for (String testName : testMap.keySet()) {
            XmlTest xmlTest = new XmlTest(suite);
            xmlTest.setName(testName);

            Map<String, String> parameters = testParams.get(testName);
            xmlTest.setParameters(parameters);

            List<XmlClass> xmlClasses = new ArrayList<>();
            for (Map.Entry<String, List<String>> classEntry : testMap.get(testName).entrySet()) {
                XmlClass xmlClass = new XmlClass(classEntry.getKey());
                List<XmlInclude> includes = new ArrayList<>();
                for (String method : classEntry.getValue()) {
                    includes.add(new XmlInclude(method));
                }
                xmlClass.setIncludedMethods(includes);
                xmlClasses.add(xmlClass);
            }
            xmlTest.setXmlClasses(xmlClasses);
        }
        return suite;
    }

    private static void setSuiteParameters(XmlSuite suite, Map<String, Map<String, String>> testParams) {
        String env = getRunTimeVariables("env");
        if (env == null || env.isEmpty()) {
            logger.error("Suite-level parameter 'env' is missing. Please provide it in runtime variables.");
        }
        suite.setParameters(Collections.singletonMap("env", env));
    }

    private static void configureParallelism(XmlSuite suite, int testCount) {
        String flag = getRunTimeVariables("parallelFlag");
        int threadCount = Integer.parseInt(getRunTimeVariables("threadCount"));

        if ("true".equalsIgnoreCase(flag)) {
            suite.setParallel(XmlSuite.ParallelMode.TESTS);
            suite.setThreadCount(Math.min(testCount, threadCount));
            suite.setDataProviderThreadCount(threadCount);
            logger.info("Parallel execution enabled. Mode=TESTS, ThreadCount={}", suite.getThreadCount());
        } else {
            suite.setThreadCount(1);
            suite.setDataProviderThreadCount(1);
            logger.info("Sequential execution. ThreadCount=1");
        }
    }

    /*
     * Method Description : Writes XmlSuite content to a file
     * Input Parameter(s) if any : suite (XmlSuite), filePath (String)
     * Output Parameter(s) if any : void
     */
    public static void writeSuiteToXmlFile(XmlSuite suite, String filePath) {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(suite.toXml());
        } catch (IOException e) {
            logger.error("Failed to write XML file: {}", e.getMessage());
        }
    }
}
