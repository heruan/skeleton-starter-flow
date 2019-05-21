package com.vaadin.starter.skeleton.it;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.BeforeClass;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import com.vaadin.testbench.TestBench;
import com.vaadin.testbench.parallel.ParallelTest;

public abstract class VaadinIT extends ParallelTest {
    private static final String USE_HUB_PROPERTY = "test.use.hub";
    private static final String WEBDRIVER_CHROME_DRIVER = "webdriver.chrome.driver";
    private static final String CHROMEDRIVER_NAME_PART = "chromedriver";
    // examples: driver\windows\googlechrome\64bit\chromedriver.exe
    private static final int MAX_DRIVER_SEARCH_DEPTH = 4;

    public static final String SERVER_PORT_PROPERTY_KEY = "serverPort";
    private static final int SERVER_PORT = Integer.parseInt(
            System.getProperty(SERVER_PORT_PROPERTY_KEY, "8080"));

    @BeforeClass
    public static void setChromeDriverPath() {
        fillEnvironmentProperty();
    }

    @Before
    public void setup() throws Exception {
        if (VaadinIT.runningInCI()) {
            VaadinIT.setUsingHub();
            super.setup();
        } else {
            setDriver(VaadinIT.createHeadlessChromeDriver());
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

    public static WebDriver createHeadlessChromeDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless", "--disable-gpu");
        return TestBench.createDriver(new ChromeDriver(options));
    }

    private static void fillEnvironmentProperty() {
        if (System.getProperty(WEBDRIVER_CHROME_DRIVER) == null) {
            Optional.ofNullable(getDriverLocation())
                    .ifPresent(driverLocation -> System.setProperty(
                            WEBDRIVER_CHROME_DRIVER, driverLocation));
        }
    }

    private static String getDriverLocation() {
        Path driverDirectory = Paths.get("../../driver/");
        if (!driverDirectory.toFile().isDirectory()) {
            System.out.println(String.format(
                    "Could not find driver directory: %s", driverDirectory));
            return null;
        }

        List<Path> driverPaths = getDriverPaths(driverDirectory);

        if (driverPaths.isEmpty()) {
            System.out.println("No " + CHROMEDRIVER_NAME_PART + " found at \""
                    + driverDirectory.toAbsolutePath() + "\"\n"
                    + "  Verify that the path is correct and that driver-binary-downloader-maven-plugin has been run at least once.");
            return null;
        }

        if (driverPaths.size() > 1) {
            System.out.println(String.format(
                    "Have found multiple driver paths, using the first one from the list: %s",
                    driverPaths));
        }
        return driverPaths.get(0).toAbsolutePath().toString();

    }

    private static List<Path> getDriverPaths(Path driverDirectory) {
        List<Path> driverPaths;
        try {
            driverPaths = Files
                    .find(driverDirectory, MAX_DRIVER_SEARCH_DEPTH,
                            VaadinIT::isChromeDriver)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException("Error trying to locate "
                    + CHROMEDRIVER_NAME_PART + " binary", e);
        }
        return driverPaths;
    }

    private static boolean isChromeDriver(Path path,
                                          BasicFileAttributes attributes) {
        return attributes.isRegularFile()
                && path.toString().toLowerCase(Locale.getDefault())
                .contains(CHROMEDRIVER_NAME_PART);
    }

    private static boolean runningInCI() {
        return System.getenv("TEAMCITY_SERVER") != null;
    }

    private static void setUsingHub() {
        System.setProperty("test.use.hub", Boolean.TRUE.toString());
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
