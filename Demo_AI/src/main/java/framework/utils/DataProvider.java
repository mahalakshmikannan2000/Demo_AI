package framework.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;

import java.io.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static framework.utils.WebActions.getRunTimeVariables;

/**
 * Reads JSON test data files (src/test/java/testData/{web|mobile|api}/{TestClass}.json) and
 * feeds the matching automationId's row(s) into the TestNG @Test method - the JSON-driven
 * input capability highlighted in this demo.
 */
public class DataProvider {

    public static final Logger logger = LoggerFactory.getLogger(DataProvider.class);

    /*
     * Method Description : Returns the JSON file path based on browser type and test class
     * Input Parameter(s) if any : String browserName, Class<?> clazz
     * Output Parameter(s) if any : String - Full path of the JSON file for test data
     */
    public static String getJsonPath(String browserName, Class<?> clazz) {
        String className = clazz.getSimpleName();
        String browser = browserName.toLowerCase();
        if (browser.contains("chrome") || browser.contains("safari") || browser.equalsIgnoreCase("Edge")) {
            browser = "web";
        } else if (browser.contains("ios") || browser.contains("android")) {
            browser = "mobile";
        } else {
            browser = "api";
        }

        return System.getProperty("user.dir") + "/src/test/java/testData/" + browser + "/" + className + ".json";
    }

    /*
     * Method Description : Provides test data to TestNG tests from JSON based on environment and automation ID
     * Input Parameter(s) if any : Method method, ITestContext context
     * Output Parameter(s) if any : Object[][] - 2D array of maps representing test data for the test method
     */
    @org.testng.annotations.DataProvider(name = "commonDataProvider", parallel = true)
    public Object[][] getTestData(Method method, ITestContext context) throws NullPointerException {
        String groupName = context.getCurrentXmlTest().getParameter("env");
        String browser = context.getCurrentXmlTest().getParameter("browser");
        String scriptId = method.getName();
        Class<?> testClass = method.getDeclaringClass();

        String path = getJsonPath(browser, testClass);

        ObjectMapper mapper = new ObjectMapper();
        InputStream is = null;
        try {
            is = new FileInputStream(new File(path));
        } catch (FileNotFoundException e) {
            logger.error("Exception Occurred while Getting Testdata {}", String.valueOf(e));
        }

        Map<String, List<Map<String, Object>>> fullData = null;
        try {
            fullData = mapper.readValue(is, new TypeReference<Map<String, List<Map<String, Object>>>>() {
            });
        } catch (IOException e) {
            logger.error("Exception while Getting Testdata Input {}", String.valueOf(e));
        }

        String resolvedGroupName = resolveDataGroupName(fullData, groupName);

        List<Map<String, Object>> rawList = fullData.getOrDefault(resolvedGroupName, Collections.emptyList());

        List<Map<String, Object>> matchedList = rawList.stream()
                .filter(item -> scriptId.equals(item.get("automationId")))
                .toList();

        List<Map<String, String>> finalList = new ArrayList<>();
        for (Map<String, Object> item : matchedList) {
            Map<String, String> converted = item.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> e.getValue() == null ? null : e.getValue().toString()
                    ));
            finalList.add(converted);
        }
        if (finalList.isEmpty()) {
            logger.error("No test data found for method: " + scriptId
                    + " in env: " + groupName
                    + " using data group: " + resolvedGroupName);
        }

        Object[][] result = new Object[finalList.size()][1];
        for (int i = 0; i < finalList.size(); i++) {
            result[i][0] = finalList.get(i);
        }

        return result;
    }

    private String resolveDataGroupName(Map<String, List<Map<String, Object>>> fullData, String groupName) {
        String brand = getRunTimeVariables("brand");

        if (brand == null || brand.trim().isEmpty()) {
            brand = System.getProperty("brand");
        }

        if (brand == null || brand.trim().isEmpty()) {
            brand = System.getenv("brand");
        }

        if (brand != null && !brand.trim().isEmpty()) {
            String brandGroupName = groupName + "_" + brand.trim().toUpperCase();

            if (fullData.containsKey(brandGroupName)) {
                logger.info("Using brand-specific test data group: {}", brandGroupName);
                return brandGroupName;
            }

            logger.info("Brand-specific test data group not found: {}. Falling back to: {}",
                    brandGroupName, groupName);
        }

        return groupName;
    }
}
