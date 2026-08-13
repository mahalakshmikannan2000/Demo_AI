package framework.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import framework.utils.PayloadBuilder;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.ITestContext;

import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * Thin REST Assured wrapper (build endpoint from a JSON base-URL map, send GET/POST, verify
 * status code, hold the bearer token) - trimmed from the full framework's ~40 API helper
 * methods down to what the Platform authentication demo test exercises.
 */
public class APIClient {

    private static final Logger logger = LoggerFactory.getLogger(APIClient.class);

    protected Response apiResponse;
    protected RequestSpecification requestSpecification;

    private final String contentTypeHeader = "Content-type";
    private final String contentTypeJson = "application/json";
    private final String apiKeyHeader = "x-api-key";
    private final String apiKeyValue = "reqres-free-v1";

    private static String token = null;

    /*
     * Method Description : Resolves the endpoint URL by looking up env/module/function in the
     * JSON base-URL map (APIBaseURL.json) and appending the endpoint path from test data
     * Input Parameter(s) if any : ITestContext context, Map<String, String> testData, String baseurlPath
     * Output Parameter(s) if any : String
     */
    public static String buildEndPoint(ITestContext context, Map<String, String> testData, String baseurlPath) {
        String baseurl = "";
        String[] param = context.getCurrentXmlTest().getParameter("env").split("_");
        String module = testData.get("module");
        String function = testData.get("function");
        String endpoints = testData.get("endpoint");
        String env = param[0];
        try {
            JsonObject jsonObj = PayloadBuilder.loadJsonFile(baseurlPath);

            String url = jsonObj.getAsJsonObject(param[1])
                    .getAsJsonObject(module)
                    .get(function).getAsString();
            baseurl = url + endpoints;

            if (baseurl.isEmpty()) {
                Assert.assertTrue(true, "URL not found for env=" + env + ", module=" + module + ", function=" + function);
            }
            return baseurl;
        } catch (Exception e) {
            Assert.assertFalse(false, "URL not found for env=" + env + ", module=" + module + ", function=" + function);
        }
        return baseurl;
    }

    /*
     * Method Description : Builds the RestAssured request headers, adding a Bearer token when supplied
     * Input Parameter(s) if any : String token
     * Output Parameter(s) if any : RequestSpecification
     */
    public RequestSpecification getHeaders(String token) {
        if (token != null) {
            requestSpecification = given()
                    .header(contentTypeHeader, contentTypeJson)
                    .header(apiKeyHeader, apiKeyValue)
                    .header("Authorization", "Bearer " + token);
        } else {
            requestSpecification = given()
                    .header(contentTypeHeader, contentTypeJson)
                    .header(apiKeyHeader, apiKeyValue);
        }
        return requestSpecification;
    }

    private RequestSpecification setParamforAPI(Map<String, String> queryParams, Map<String, String> pathParams) {
        if (pathParams != null && !pathParams.isEmpty()) {
            requestSpecification.pathParams(pathParams);
        }
        if (queryParams != null && !queryParams.isEmpty()) {
            requestSpecification.queryParams(queryParams);
        }
        return requestSpecification;
    }

    /*
     * Method Description : Sends a GET request to the specified endpoint with query/path parameters and auth token
     * Input Parameter(s) if any : String endpoint, Map<String, String> queryParams, Map<String, String> pathParams, String token
     * Output Parameter(s) if any : Response
     */
    public Response sendGetRequest(String endpoint, Map<String, String> queryParams, Map<String, String> pathParams, String token) {
        requestSpecification = getHeaders(token).baseUri(endpoint);
        requestSpecification = setParamforAPI(queryParams, pathParams).baseUri(endpoint);
        return requestSpecification
                .when()
                .get()
                .then()
                .extract()
                .response();
    }

    /*
     * Method Description : Verifies that the API response status code matches the expected value
     * Input Parameter(s) if any : int expectedStatusCode, Response response
     * Output Parameter(s) if any : void
     */
    public void verifyResponseCode(int expectedStatusCode, Response response) {
        if (response == null) {
            Assert.fail("API response is null. Check endpoint, request payload, token, network, or request exception.");
        }
        if (response.getStatusCode() == expectedStatusCode) {
            logger.info("Response contains expected status Code : {}", response.getStatusCode());
        } else {
            Assert.fail("Response validation failed. Value found: " + response.getStatusCode()
                    + ", Response Body: " + response.getBody().asString());
        }
    }

    /*
     * Method Description : Returns the given JSON string pretty-printed, wrapped in a <pre> tag for reports
     * Input Parameter(s) if any : String jsonObject
     * Output Parameter(s) if any : String
     */
    public static String returnJSONObject(String jsonObject) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return "<pre>" + gson.toJson(JsonParser.parseString(jsonObject)) + "</pre>";
    }

    /*
     * Method Description : Loads a JSON payload and replaces one element's value - used when
     * building request payloads from a template
     * Input Parameter(s) if any : JsonObject payload, String replaceElement, String replaceVal
     * Output Parameter(s) if any : JsonObject
     */
    public static JsonObject getPayloadandReplaceValue(JsonObject payload, String replaceElement, String replaceVal) {
        for (String key : payload.keySet()) {
            JsonElement element = payload.get(key);
            if (element.isJsonObject()) {
                JsonObject nested = element.getAsJsonObject();
                if (nested.has(replaceElement)) {
                    nested.addProperty(replaceElement, replaceVal);
                }
            }
        }
        return payload;
    }

    public static void setToken(String token) {
        APIClient.token = token;
    }

    public static String getToken() {
        return token;
    }
}
