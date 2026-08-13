package framework.listeners;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

import static framework.utils.WebActions.getRunTimeVariables;

/**
 * Retries a failed test up to the "retryCount" configured in config.properties - intended
 * for environment/network flakiness only.
 */
public class RetryAnalyzer implements IRetryAnalyzer {
    private int count = 0;
    private static final int RETRY = Integer.parseInt(getRunTimeVariables("retryCount"));

    /*
     * Method Description : Determines if a test should be retried based on the retry count
     * Input Parameter(s) if any : ITestResult result
     * Output Parameter(s) if any : boolean
     */
    @Override
    public boolean retry(ITestResult result) {
        if (count < RETRY) {
            count++;
            return true;
        }
        return false;
    }

    public boolean isLastAttempt() {
        return count >= RETRY;
    }
}
