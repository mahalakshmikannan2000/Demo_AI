# CSP Login Demo — UTAF_CFC Framework Showcase

A trimmed-down extract of the full `UTAF_CFC` automation framework, built to demo the
**framework capabilities** to another client team without exposing the full application
suite. It contains one working test case per platform, but every core framework feature is
fully wired and functional:

| Feature                         | Where |
|----------------------------------|-------|
| Browser / device configuration   | `framework.base.DriverFactory` (Chrome, Edge, BrowserStack Android/iOS) |
| Database connectivity            | `framework.database.DBUtils` + `src/main/resources/properties/DB.properties` |
| ExtentReports (HTML report)      | `framework.utils.ExtentReport`, `ScreenShotUtil` |
| Allure Report                    | `framework.utils.AllureReportGenerator` + `generate-allure-*.bat` |
| Dynamic TestNG XML generation    | `framework.utils.TestNGXMLGenerator` (reads `src/main/resources/TestNGXML.xlsx`) |
| JSON-driven test data            | `framework.utils.DataProvider` + `src/test/java/testData/**/*.json` |
| Retry / listeners                | `framework.listeners.RetryAnalyzer`, `CustomTestListener` |
| CI/CD                            | `azure-pipelines-*.yml` (CSP web, ECS Android, ECS iOS) |

## Included test cases

| # | Test | Class | Platform |
|---|------|-------|----------|
| 1 | `TC001_CSP_Successful_login` | `tests.web.CSPScripts` | Web (Chrome/Edge) |
| 2 | `TC01_Android_logInToECSWithValidID` | `tests.mobile.ECSScriptsAndroid` | Android (BrowserStack App Automate) |
| 3 | `TC01_IOS_LoginToECSWithValidID` | `tests.mobile.ECSScriptsIOS` | iOS (BrowserStack App Automate) |
| 4 | `TC01_GETBearerToken` | `tests.api.APIScripts` | REST API (Rest Assured) |

## Project layout

```
src/main/java/framework/   core framework (config, base, database, listeners, utils, api)
src/main/java/app/         page objects (app.web.csp, app.mobile.ECS, app.api.Platform)
src/main/resources/        properties, logback.xml, TestNGXML.xlsx (auto-generated), payload
src/test/java/tests/       TestNG test classes (web, mobile, api)
src/test/java/testData/    JSON test data consumed by DataProvider
azure-pipelines-*.yml      Azure DevOps pipelines (CSP web, ECS Android, ECS iOS)
generate-allure-*.bat      Allure report generation scripts
```

## Prerequisites

- Java 17+
- Maven 3.8+
- Chrome or Edge installed locally (for the CSP web test)
- BrowserStack account (for the ECS Android/iOS mobile tests) — credentials are already
  configured in `config.properties` (`bs_user` / `bs_key`)
- (Optional) Allure CLI on `PATH`, or copy an Allure CLI distribution into `tools/allure/`,
  to enable one-click HTML report generation via the `.bat` scripts

## How to run

All suites are driven by **`framework.utils.TestNGXMLGenerator`**, which reads the Excel
test matrix at `src/main/resources/TestNGXML.xlsx` (auto-created with demo data on first
run if missing), builds a TestNG `XmlSuite` in memory, writes it out to `generated.xml`,
and executes it — this is the "dynamic TestNG XML generation" feature.

Run any of the 4 demo suites with Maven, switching `appName`/`env` per platform:

```bash
# CSP web login (Chrome)
mvn test-compile exec:java -DappName=CSP_Regression -Denv=CSP_QA

# ECS Android login (BrowserStack)
mvn test-compile exec:java -DappName=ECS_Android -Denv=ECS_QA

# ECS iOS login (BrowserStack)
mvn test-compile exec:java -DappName=ECS_IOS -Denv=ECS_QA

# Platform API - bearer token auth
mvn test-compile exec:java -DappName=API_Regression -Denv=API_TEST
```

Any property in `config.properties` can be overridden at the command line with `-Dkey=value`
(e.g. `-Dheadless=true`, `-DparallelFlag=true`, `-DthreadCount=3`).

## Reports

- **ExtentReports**: written to `reports/<APP>/<timestamp>/` after every run
  (`<APP>_Main_Report_*.html` = detailed, `<APP>_Emailable_Report.html` = summary), with
  screenshots captured on pass/fail per the `screenShotOnFailure` flag.
- **Allure**: raw results are written to `allure-results/` (via the `allure-testng`
  listener); at the end of the suite `AllureReportGenerator` shells out to
  `generate-allure-single-file.bat` (single shareable HTML) or
  `generate-allure-history-report.bat` (trend/history view), depending on the
  `singleAllure` flag in `config.properties`.

## CI/CD

Three Azure DevOps pipelines are included, all invoking the same
`exec:java -Dexec.mainClass=framework.utils.TestNGXMLGenerator` entry point used locally:

- `azure-pipelines-smoke-CSP.yml` — CSP web login, with Allure history/single-file handling
  and Extent/Allure artifact publishing.
- `azure-pipelines-android-regression-ECS.yml` — ECS Android login on BrowserStack.
- `azure-pipelines-ios-regression-ECS.yml` — ECS iOS login on BrowserStack.

## Notes on trimming

This project is intentionally a **subset** of the full `UTAF_CFC` framework:

- Only the page objects/methods needed by the 4 demo test cases are included (e.g.
  `CSPLoginPage` instead of ~35 CSP pages, `APIClient` trimmed to the ~6 methods the token
  test needs).
- Applicant/DB-result-collection, Excel/email dashboards, and Azure Test Plan result
  sync were removed from `CustomTestListener`/`TestBase` — they were specific to the
  original application's reporting pipeline, not to the framework capability being
  demonstrated.
- All configuration values (URLs, encrypted credentials, DB/BrowserStack/Azure DevOps
  keys) are the same real values used in the source project, since this demo is for a
  different department of the same client.
