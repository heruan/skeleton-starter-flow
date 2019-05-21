package com.vaadin.starter.skeleton.it;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import com.vaadin.testbench.ScreenshotOnFailureRule;
import com.vaadin.testbench.TestBench;
import com.vaadin.testbench.parallel.ParallelTest;

public abstract class VaadinIT extends ParallelTest {
    private static final String USE_HUB_PROPERTY = "test.use.hub";

    public static final String SERVER_PORT_PROPERTY_KEY = "serverPort";
    private static final int SERVER_PORT = Integer.parseInt(
            System.getProperty(SERVER_PORT_PROPERTY_KEY, "8080"));

    @Rule
    public ScreenshotOnFailureRule rule = new ScreenshotOnFailureRule(this,
            true);

    @Before
    public void setup() throws Exception {
        if (isUsingHub()) {
            super.setup();
        } else {
            setDriver(createHeadlessChromeDriver());
        }

    }

    public void open() {
        open("");
    }

    public void open(String testPath) {
        String url = getTestURL(getRootURL(), testPath);
        System.out.println("Attempting to open test page @ " + url);
        getDriver().get(url);
    }

    private static WebDriver createHeadlessChromeDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless", "--disable-gpu");
        return TestBench.createDriver(new ChromeDriver(options));
    }

    private static boolean isUsingHub() {
        return Boolean.TRUE.toString().equals(System.getProperty(USE_HUB_PROPERTY));
    }

    private static String getRootURL() {
        return "http://" + getDeploymentHostname() + ":" + SERVER_PORT;
    }

    private static String getDeploymentHostname() {
        if (!isUsingHub()) {
            return "localhost";
        }
        return getCurrentHostAddress();
    }

    private static String getCurrentHostAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface nwInterface = interfaces.nextElement();
                if (!nwInterface.isUp() || nwInterface.isLoopback()
                        || nwInterface.isVirtual()) {
                    continue;
                }
                Optional<String> address = getHostAddress(nwInterface);
                if (address.isPresent()) {
                    return address.get();
                }
            }
        } catch (SocketException e) {
            throw new RuntimeException("Could not find the host name", e);
        }
        throw new RuntimeException(
                "No compatible (10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16) ip address found.");
    }

    private static Optional<String> getHostAddress(
            NetworkInterface nwInterface) {
        Enumeration<InetAddress> addresses = nwInterface.getInetAddresses();
        while (addresses.hasMoreElements()) {
            InetAddress address = addresses.nextElement();
            if (address.isLoopbackAddress()) {
                continue;
            }
            if (address.isSiteLocalAddress()) {
                return Optional.of(address.getHostAddress());
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the URL to be used for the test.
     *
     * @param parameters
     *            query string parameters to add to the url
     * @param rootUrl
     *            the root URL of the server (hostname + port)
     * @param testPath
     *            the path of the test
     *
     * @return the URL for the test
     */
    private static String getTestURL(String rootUrl, String testPath,
                                    String... parameters) {
        while (rootUrl.endsWith("/")) {
            rootUrl = rootUrl.substring(0, rootUrl.length() - 1);
        }
        rootUrl = rootUrl + testPath;

        if (parameters != null && parameters.length != 0) {
            if (!rootUrl.contains("?")) {
                rootUrl += "?";
            } else {
                rootUrl += "&";
            }

            rootUrl += String.join("&", parameters);
        }

        return rootUrl;
    }
}
