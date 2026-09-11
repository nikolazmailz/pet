package ru.ntdev.srhr.ms.mock.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mock.wiremock")
public class WireMockProperties {

    /**
     * HTTP port used by embedded WireMock.
     * Use 0 in tests to allocate a random free port.
     */
    private int port = 8080;

    /**
     * Classpath directory containing mappings/ and __files/.
     */
    private String rootDirectory = "wiremock";

    /**
     * Enables verbose WireMock logging.
     */
    private boolean verbose = true;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getRootDirectory() {
        return rootDirectory;
    }

    public void setRootDirectory(String rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
}
