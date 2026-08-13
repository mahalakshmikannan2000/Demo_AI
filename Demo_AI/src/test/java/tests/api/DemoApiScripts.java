package tests.api;

import com.google.gson.JsonObject;
import framework.api.APIClient;
import framework.base.PageObjectInitiatorDemo;
import framework.base.TestBase;
import framework.config.PropertyReader;
import framework.utils.DataProvider;
import framework.utils.PayloadBuilder;
import io.restassured.response.Response;
import org.testng.ITestContext;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Map;

/**
 * Demo API suite — targets reqres.in (free public API, no real credentials)
 * Mirrors APIScripts structure exactly:
 *   extends TestBase, uses APIClient.buildEndPoint(), PayloadBuilder, verifyResponseCode()
 */
public class DemoApiScripts extends TestBase {

    static String urlPath = PropertyReader.getProperty("DEMO_API", "baseurlPath");

    @Test(description = "GET - Fetch list of users",
          dataProvider = "commonDataProvider",
          dataProviderClass = DataProvider.class)
    public void TC001_GET_Users(Map<String, String> testData, ITestContext context) {
        String endpoint = APIClient.buildEndPoint(context, testData, urlPath);

        Response response = PageObjectInitiatorDemo.getApiClient()
                .sendGetRequest(endpoint, null, null, null);

        PageObjectInitiatorDemo.getApiClient()
                .verifyResponseCode(
                        Integer.parseInt(testData.get("expectedStatus")), response);
    }

    @Test(description = "GET - Fetch single user by ID",
          dataProvider = "commonDataProvider",
          dataProviderClass = DataProvider.class)
    public void TC002_GET_SingleUser(Map<String, String> testData, ITestContext context) {
        String endpoint = APIClient.buildEndPoint(context, testData, urlPath);

        Response response = PageObjectInitiatorDemo.getApiClient()
                .sendGetRequest(endpoint, null, null, null);

        PageObjectInitiatorDemo.getApiClient()
                .verifyResponseCode(
                        Integer.parseInt(testData.get("expectedStatus")), response);
    }

//    @Test(description = "POST - Create a new user",
//          dataProvider = "commonDataProvider",
//          dataProviderClass = DataProvider.class)
//    public void TC003_POST_CreateUser(Map<String, String> testData, ITestContext context)
//            throws IOException {
//        String endpoint = APIClient.buildEndPoint(context, testData, urlPath);
//
//        JsonObject payload = PayloadBuilder.loadJsonFile(
//                "src/main/resources/payload/DemoCreateUser.json");
//
//        Response response = PageObjectInitiatorDemo.getApiClient()
//                .sendPostRequestWithRawPayload(endpoint, payload, null);
//
//        PageObjectInitiatorDemo.getApiClient()
//                .verifyResponseCode(
//                        Integer.parseInt(testData.get("expectedStatus")), response);
//    }
}
