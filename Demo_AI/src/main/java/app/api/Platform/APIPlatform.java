package app.api.Platform;

import framework.base.TestBase;

import java.util.HashMap;
import java.util.Map;

/**
 * Platform-specific API helpers. Trimmed to the single helper the Platform authentication
 * demo test (TC01_GETBearerToken) needs - building a single query-parameter map from test data.
 */
public class APIPlatform extends TestBase {

    /*
     * Method Description : Builds a single-entry query parameter map from a value found in test data
     * Input Parameter(s) if any : Map<String, String> testData, String keyPath, String key
     * Output Parameter(s) if any : Map<String, String>
     */
    public static Map<String, String> getParamForApi(Map<String, String> testData, String keyPath, String key) {
        Map<String, String> params = new HashMap<>();
        if (keyPath != null && key != null) {
            String paramValue = testData.get(keyPath);
            params = Map.of(key, paramValue);
        }
        return params;
    }
}
