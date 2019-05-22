package com.vaadin.starter.skeleton;

import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import com.vaadin.testbench.TestBench;
import com.vaadin.testbench.parallel.ParallelTest;

/**
 * Base class for ITs.
 */
public abstract class AbstractViewTest extends ParallelTest {
    private static final int SERVER_PORT = 8080;

    private final String route;
    private final By rootSelector;

    public AbstractViewTest() {
        this("", By.tagName("body"));
    }

    protected AbstractViewTest(String route, By rootSelector) {
        this.route = route;
        this.rootSelector = rootSelector;
    }

    @Before
    public void setup() throws Exception {
        if (isUsingHub()) {
            super.setup();
        } else {
            setDriver(TestBench.createDriver(new ChromeDriver()));
        }
        getDriver().get(getURL(route));
    }

    @After
    public void finalCheck() {
        getDriver().quit();
    }

    /**
     * Convenience method for getting the root element of the view based on
     * the selector passed to the constructor.
     *
     * @return the root element
     */
    protected WebElement getRootElement() {
        return findElement(rootSelector);
    }

    /**
     * Returns deployment host name concatenated with route.
     *
     * @return URL to route
     */
    private static String getURL(String route) {
        return String.format("http://%s:%d/%s", getDeploymentHostname(),
                SERVER_PORT, route);
    }

    /**
     * Finds elements by selector under shadow root of given web component.
     *
     * @param webComponent - component root
     * @return result of selection
     */
    protected List<WebElement> findInShadowRoot(WebElement webComponent,
                                                By by) {

        waitUntil(driver -> getCommandExecutor().executeScript(
                "return arguments[0].shadowRoot", webComponent) != null);
        WebElement shadowRoot = (WebElement) getCommandExecutor()
                .executeScript("return arguments[0].shadowRoot",
                        webComponent);
        Assert.assertNotNull("Could not locate shadowRoot in the element",
                shadowRoot);
        return shadowRoot.findElements(by);
    }

    /**
     * Property set to true when running on a test hub.
     */
    private static final String USE_HUB_PROPERTY = "test.use.hub";

    /**
     * Returns whether we are using a test hub
     *
     * @return whether we are using a test hub
     */
    private static boolean isUsingHub() {
        return Boolean.TRUE.toString().equals(
                System.getProperty(USE_HUB_PROPERTY));
    }

    /**
     * If running on CI, get the host name from environment variable HOSTNAME
     *
     * @return the host name
     */
    private static String getDeploymentHostname() {
        return isUsingHub() ? System.getenv("HOSTNAME") : "localhost";
    }
}
