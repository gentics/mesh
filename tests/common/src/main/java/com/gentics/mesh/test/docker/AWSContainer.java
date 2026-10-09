package com.gentics.mesh.test.docker;

import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.utility.Base58;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

public class AWSContainer extends GenericContainer<AWSContainer> {

    private static final int DEFAULT_PORT = 9000;
    private static final String DEFAULT_IMAGE = System.getProperty("mesh.container.image.prefix", "") + "rustfs/rustfs";
    private static final String DEFAULT_TAG = "1.0.1";
    private static final String RUSTFS_ACCESS_KEY = "RUSTFS_ACCESS_KEY";
    private static final String RUSTFS_SECRET_KEY = "RUSTFS_SECRET_KEY";
    private static final String DEFAULT_STORAGE_DIRECTORY = "/data";
    private static final String HEALTH_ENDPOINT = "/health";
    CredentialsProvider credentials;
    public AWSContainer(CredentialsProvider credentials) {
        this(DEFAULT_IMAGE + ":" + DEFAULT_TAG, credentials);
    }


    public AWSContainer(String image, CredentialsProvider credentials) {
        super(DEFAULT_IMAGE + ":" + DEFAULT_TAG);
        this.credentials=credentials;

    }
    @Override
    protected void configure() {
        withNetworkAliases("rustfs-" + Base58.randomString(6));
        addExposedPort(DEFAULT_PORT);
        if (credentials != null) {
            withEnv(RUSTFS_ACCESS_KEY, credentials.getAccessKey());
            withEnv(RUSTFS_SECRET_KEY, credentials.getSecretKey());
        }
        withCommand(DEFAULT_STORAGE_DIRECTORY);
        setWaitStrategy(new HttpWaitStrategy()
                .forPort(DEFAULT_PORT)
                .forPath(HEALTH_ENDPOINT)
                .withStartupTimeout(Duration.ofMinutes(5)));
    }

    public String getHostAddress() {
        return getContainerIpAddress() + ":" + getMappedPort(DEFAULT_PORT);
    }

    /**
     * Return the ids of all currently running containers of the S3 image.
     *
     * @return container ids
     */
    public static Set<String> runningContainerIds() {
        String image = DEFAULT_IMAGE + ":" + DEFAULT_TAG;
        return DockerClientFactory.instance().client().listContainersCmd().exec().stream()
                .filter(container -> image.equals(container.getImage()))
                .map(container -> container.getId())
                .collect(Collectors.toSet());
    }


    public static class CredentialsProvider {
        private String accessKey;
        private String secretKey;

        public CredentialsProvider(String accessKey, String secretKey) {
            this.accessKey = accessKey;
            this.secretKey = secretKey;
        }

        public String getAccessKey() {
            return accessKey;
        }

        public String getSecretKey() {
            return secretKey;
        }
    }
}