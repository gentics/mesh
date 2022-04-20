package com.gentics.mesh.etc.config.search;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.annotation.Setter;
import com.gentics.mesh.doc.GenerateDocumentation;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.env.EnvironmentVariable;
import com.gentics.mesh.etc.config.env.Option;

/**
 * Search engine options POJO.
 */
@GenerateDocumentation
public class ElasticSearchOptions implements Option {

	/**
	 * Default ES connection details.
	 */
	public static final String DEFAULT_URL = "http://localhost:9200";
	public static final long DEFAULT_TIMEOUT = 60_000L;

	public static final int DEFAULT_BULK_LIMIT = 100;
	public static final int DEFAULT_BULK_LENGTH_LIMIT = 5_000_000;
	public static final int DEFAULT_SYNC_BATCH_SIZE = 50_000;

	public static final int DEFAULT_EVENT_BUFFER_SIZE = 1000;
	public static final int DEFAULT_BULK_DEBOUNCE_TIME = 2000;
	public static final int DEFAULT_IDLE_DEBOUNCE_TIME = 100;
	public static final int DEFAULT_RETRY_INTERVAL = 5000;
	public static final int DEFAULT_RETRY_LIMIT = 3;
	public static final boolean DEFAULT_WAIT_FOR_IDLE = true;
	public static final boolean DEFAULT_INCLUDE_BINARY_FIELDS = true;
	public static final MappingMode DEFAULT_MAPPING_MODE = MappingMode.DYNAMIC;
	public static final ComplianceMode DEFAULT_COMPLIANCE_MODE = ComplianceMode.ES_6;

	public static final String DEFAULT_PREFIX = "mesh-";

	public static final boolean DEFAULT_HOSTNAME_VERIFICATION = true;

	public static final long DEFAULT_INDEX_CHECK_INTERVAL = 60 * 1000;
	public static final long DEFAULT_INDEX_MAPPING_CACHE_TIMEOUT = 60 * 60 * 1000;

	public static final String MESH_ELASTICSEARCH_URL_ENV = "MESH_ELASTICSEARCH_URL";
	public static final String MESH_ELASTICSEARCH_USERNAME_ENV = "MESH_ELASTICSEARCH_USERNAME";
	public static final String MESH_ELASTICSEARCH_PASSWORD_ENV = "MESH_ELASTICSEARCH_PASSWORD";
	public static final String MESH_ELASTICSEARCH_CERT_PATH_ENV = "MESH_ELASTICSEARCH_CERT_PATH";
	public static final String MESH_ELASTICSEARCH_CA_PATH_ENV = "MESH_ELASTICSEARCH_CA_PATH";

	public static final String MESH_ELASTICSEARCH_TIMEOUT_ENV = "MESH_ELASTICSEARCH_TIMEOUT";
	public static final String MESH_ELASTICSEARCH_PREFIX_ENV = "MESH_ELASTICSEARCH_PREFIX";
	public static final String MESH_ELASTICSEARCH_BULK_LIMIT_ENV = "MESH_ELASTICSEARCH_BULK_LIMIT";
	public static final String MESH_ELASTICSEARCH_BULK_LENGTH_LIMIT_ENV = "MESH_ELASTICSEARCH_BULK_LENGTH_LIMIT";
	public static final String MESH_ELASTICSEARCH_EVENT_BUFFER_SIZE_ENV = "MESH_ELASTICSEARCH_EVENT_BUFFER_SIZE";
	public static final String MESH_ELASTICSEARCH_BULK_DEBOUNCE_TIME_ENV = "MESH_ELASTICSEARCH_BULK_DEBOUNCE_TIME";
	public static final String MESH_ELASTICSEARCH_IDLE_DEBOUNCE_TIME_ENV = "MESH_ELASTICSEARCH_IDLE_DEBOUNCE_TIME";
	public static final String MESH_ELASTICSEARCH_RETRY_INTERVAL_ENV = "MESH_ELASTICSEARCH_RETRY_INTERVAL";
	public static final String MESH_ELASTICSEARCH_RETRY_LIMIT_ENV = "MESH_ELASTICSEARCH_RETRY_LIMIT";
	public static final String MESH_ELASTICSEARCH_WAIT_FOR_IDLE_ENV = "MESH_ELASTICSEARCH_WAIT_FOR_IDLE";
	public static final String MESH_ELASTICSEARCH_MAPPING_MODE_ENV = "MESH_ELASTICSEARCH_MAPPING_MODE";
	public static final String MESH_ELASTICSEARCH_COMPLIANCE_MODE_ENV = "MESH_ELASTICSEARCH_COMPLIANCE_MODE";
	public static final String MESH_ELASTICSEARCH_SYNC_BATCH_SIZE_ENV = "MESH_ELASTICSEARCH_SYNC_BATCH_SIZE";
	public static final String MESH_ELASTICSEARCH_HOSTNAME_VERIFICATION_ENV = "MESH_ELASTICSEARCH_HOSTNAME_VERIFICATION";
	public static final String MESH_ELASTICSEARCH_INCLUDE_BINARY_FIELDS_ENV = "MESH_ELASTICSEARCH_INCLUDE_BINARY_FIELDS";

	public static final String MESH_ELASTICSEARCH_INDEX_CHECK_INTERVAL_ENV = "MESH_ELASTICSEARCH_INDEX_CHECK_INTERVAL";
	public static final String MESH_ELASTICSEARCH_INDEX_MAPPING_CACHE_TIMEOUT_ENV = "MESH_ELASTICSEARCH_INDEX_MAPPING_CACHE_TIMEOUT";

	@JsonProperty(required = false)
	@JsonPropertyDescription("Elasticsearch connection url to be used. Set this setting to null will disable the Elasticsearch support.")
	@EnvironmentVariable(name = MESH_ELASTICSEARCH_URL_ENV, description = "Override the configured elasticsearch server url. The value can be set to null in order to disable the Elasticsearch support.")
	private String url = DEFAULT_URL;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Username to be used for Elasticsearch authentication.")
	@EnvironmentVariable(name = MESH_ELASTICSEARCH_USERNAME_ENV, description = "Override the configured Elasticsearch connection username.")
	private String username;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Password to be used for Elasticsearch authentication.")
	@EnvironmentVariable(name = MESH_ELASTICSEARCH_PASSWORD_ENV, description = "Override the configured Elasticsearch connection password.")
	private String password;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Path to the trusted server certificate (PEM format). This setting can be used when the Elasticsearch server is using a self-signed certificate which would otherwise not be trusted.")
	@EnvironmentVariable(name = MESH_ELASTICSEARCH_CERT_PATH_ENV, description = "Override the configured trusted server certificate.")
	private String certPath;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Path to the trusted common authority certificate (PEM format)")
	@EnvironmentVariable(name = MESH_ELASTICSEARCH_CA_PATH_ENV, description = "Override the configured common authority certificate path.")
	private String caPath;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Flag which controls whether hostname verification should be enabled. Default: " + DEFAULT_HOSTNAME_VERIFICATION)
	@EnvironmentVariable(name = MESH_ELASTICSEARCH_HOSTNAME_VERIFICATION_ENV, description = "Override the configured hostname verification flag.")
	private boolean hostnameVerification = DEFAULT_HOSTNAME_VERIFICATION;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Timeout for Elasticsearch operations. Default: " + DEFAULT_TIMEOUT + "ms")
	@EnvironmentVariable(name = MESH_ELASTICSEARCH_TIMEOUT_ENV, description = "Override the configured elasticsearch server timeout.")
	private Long timeout = DEFAULT_TIMEOUT;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Search server prefix for this installation. Choosing different prefixes for each Gentics Mesh instance will allow you to use a single Elasticsearch cluster for multiple Gentics Mesh instances. Default: "
		+ DEFAULT_PREFIX)
	@EnvironmentVariable(name = MESH_ELASTICSEARCH_PREFIX_ENV, description = "Override the configured elasticsearch prefix.")
	private String prefix = DEFAULT_PREFIX;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Upper limit for the size of bulk requests. Default: " + DEFAULT_BULK_LIMIT)
	@EnvironmentVariable(name = MESH_ELASTICSEARCH_BULK_LIMIT_ENV, description = "Override the batch bulk limit. Default: " + DEFAULT_BULK_LIMIT)
	private int bulkLimit = DEFAULT_BULK_LIMIT;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Upper limit for the total encoded string length of the bulk requests. Default: " + DEFAULT_BULK_LENGTH_LIMIT)
	@EnvironmentVariable(name = MESH_ELASTICSEARCH_BULK_LENGTH_LIMIT_ENV, description = "Override the batch bulk length limit. Default: "
		+ DEFAULT_BULK_LENGTH_LIMIT)
	private long bulkLengthLimit = DEFAULT_BULK_LENGTH_LIMIT;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Upper limit for mesh events that are to be mapped to elastic search requests. Default: "
		+ DEFAULT_EVENT_BUFFER_SIZE)
	@EnvironmentVariable(name = MESH_ELASTICSEARCH_EVENT_BUFFER_SIZE_ENV, description = "Override the configured event buffer size.")
	private int eventBufferSize = DEFAULT_EVENT_BUFFER_SIZE;

	@JsonProperty(required = false)
	@JsonPropertyDescription("The maximum amount of time in milliseconds between two bulkable requests before they are sent. Default: "
		+ DEFAULT_BULK_DEBOUNCE_TIME)
	@EnvironmentVariable(name = MESH_ELASTICSEARCH_BULK_DEBOUNCE_TIME_ENV, description = "Override the bulk debounce time.")
	private int bulkDebounceTime = DEFAULT_BULK_DEBOUNCE_TIME;

	@JsonProperty(required = false)
	@JsonPropertyDescription("The maximum amount of time in milliseconds between two successful requests before the idle event is emitted. Default: "
		+ DEFAULT_IDLE_DEBOUNCE_TIME)
	@EnvironmentVariable(name = MESH_ELASTICSEARCH_IDLE_DEBOUNCE_TIME_ENV, description = "Override the idle debounce time.")
	private int idleDebounceTime = DEFAULT_IDLE_DEBOUNCE_TIME;

	@JsonProperty(required = false)
	@JsonPropertyDescription("The time in milliseconds between retries of elastic search requests in case of a failure. Default: "
		+ DEFAULT_RETRY_INTERVAL)
	@EnvironmentVariable(name = MESH_ELASTICSEARCH_RETRY_INTERVAL_ENV, description = "Override the retry interval.")
	private int retryInterval = DEFAULT_RETRY_INTERVAL;

	@JsonProperty(required = false)
	@JsonPropertyDescription("The amount of retries on a single request before the request is discarded. Default: "
		+ DEFAULT_RETRY_LIMIT)
	@EnvironmentVariable(name = MESH_ELASTICSEARCH_RETRY_LIMIT_ENV, description = "Override the retry limit.")
	private int retryLimit = DEFAULT_RETRY_LIMIT;

	@JsonProperty(required = false)
	@JsonPropertyDescription("If true, search endpoints wait for elasticsearch to be idle before sending a response. Default: "
		+ DEFAULT_WAIT_FOR_IDLE)
	@EnvironmentVariable(name = MESH_ELASTICSEARCH_WAIT_FOR_IDLE_ENV, description = "Override the search idle wait flag.")
	private boolean waitForIdle = DEFAULT_WAIT_FOR_IDLE;

	@JsonProperty(required = false)
	@JsonPropertyDescription("If true, the content and metadata of binary fields will be included in the search index. Default: "
		+ DEFAULT_INCLUDE_BINARY_FIELDS)
	@EnvironmentVariable(name = MESH_ELASTICSEARCH_INCLUDE_BINARY_FIELDS_ENV, description = "Override the search include binary fields flag.")
	private boolean includeBinaryFields = DEFAULT_INCLUDE_BINARY_FIELDS;

	@JsonProperty(required = false)
	@JsonPropertyDescription("This setting controls the mapping mode of fields for Elasticsearch. When set to STRICT only fields which have a custom mapping will be added to Elasticsearch. Mode DYNAMIC will automatically use the Gentics Mesh default mappings which can be supplemented with custom mappings. Default: DYNAMIC")
	@EnvironmentVariable(name = MESH_ELASTICSEARCH_MAPPING_MODE_ENV, description = "Override the search mapping mode. ")
	private MappingMode mappingMode = DEFAULT_MAPPING_MODE;

	@JsonProperty(required = false)
	@JsonPropertyDescription("This setting controls the compliance mode for Elasticsearch. When set to ES_7 it will support Elasticsearch 7.x - In PRE_ES_7 mode it will support Elasticsearch 6.x - Default: PRE_ES_7")
	@EnvironmentVariable(name = MESH_ELASTICSEARCH_COMPLIANCE_MODE_ENV, description = "Override the search compliance mode.")
	private ComplianceMode complianceMode = DEFAULT_COMPLIANCE_MODE;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Configure the index sync batch size. Default: " + DEFAULT_SYNC_BATCH_SIZE)
	@EnvironmentVariable(name = MESH_ELASTICSEARCH_SYNC_BATCH_SIZE_ENV, description = "Override the search sync batch size")
	private int syncBatchSize = DEFAULT_SYNC_BATCH_SIZE;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Set the interval of index checks in ms. Default: " + DEFAULT_INDEX_CHECK_INTERVAL)
	@EnvironmentVariable(name = MESH_ELASTICSEARCH_INDEX_CHECK_INTERVAL_ENV, description = "Override the interval for index checks")
	private long indexCheckInterval = DEFAULT_INDEX_CHECK_INTERVAL;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Set the timeout for the cache of index mappings in ms. Default: " + DEFAULT_INDEX_MAPPING_CACHE_TIMEOUT)
	@EnvironmentVariable(name = MESH_ELASTICSEARCH_INDEX_MAPPING_CACHE_TIMEOUT_ENV, description = "Override the timeout for the cache if index mappings")
	private long indexMappingCacheTimeout = DEFAULT_INDEX_MAPPING_CACHE_TIMEOUT;

	public ElasticSearchOptions() {

	}

	/**
	 * Return the operation timeout in milliseconds.
	 * 
	 * @return
	 */
	public Long getTimeout() {
		return timeout;
	}

	/**
	 * Set the operation timeout.
	 * 
	 * @param timeout
	 * @return Fluent API
	 */
	public ElasticSearchOptions setTimeout(Long timeout) {
		this.timeout = timeout;
		return this;
	}

	/**
	 * Return the elasticsearch connection url.
	 * 
	 * @return
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Set the connection url.
	 * 
	 * @param url
	 * @return Fluent API
	 */
	public ElasticSearchOptions setUrl(String url) {
		this.url = url;
		return this;
	}

	public String getUsername() {
		return username;
	}

	/**
	 * Set the username to be used for basic authentication.
	 * 
	 * @param username
	 * @return Fluent API
	 */
	public ElasticSearchOptions setUsername(String username) {
		this.username = username;
		return this;
	}

	/**
	 * Return the password to be used for basic authentication.
	 * 
	 * @return
	 */
	public String getPassword() {
		return password;
	}

	@Setter
	public ElasticSearchOptions setPassword(String password) {
		this.password = password;
		return this;
	}

	public String getCertPath() {
		return certPath;
	}

	@Setter
	public ElasticSearchOptions setCertPath(String certPath) {
		this.certPath = certPath;
		return this;
	}

	public String getCaPath() {
		return caPath;
	}

	@Setter
	public ElasticSearchOptions setCaPath(String caPath) {
		this.caPath = caPath;
		return this;
	}

	public boolean isHostnameVerification() {
		return hostnameVerification;
	}

	@Setter
	public ElasticSearchOptions setHostnameVerification(boolean hostnameVerification) {
		this.hostnameVerification = hostnameVerification;
		return this;
	}

	public int getBulkLimit() {
		return bulkLimit;
	}

	@Setter
	public ElasticSearchOptions setBulkLimit(int bulkLimit) {
		this.bulkLimit = bulkLimit;
		return this;
	}

	public long getBulkLengthLimit() {
		return bulkLengthLimit;
	}

	@Setter
	public ElasticSearchOptions setBulkLengthLimit(long bulkLengthLimit) {
		this.bulkLengthLimit = bulkLengthLimit;
		return this;
	}

	public String getPrefix() {
		return prefix;
	}

	@Setter
	public ElasticSearchOptions setPrefix(String prefix) {
		this.prefix = prefix;
		return this;
	}

	public int getEventBufferSize() {
		return eventBufferSize;
	}

	@Setter
	public ElasticSearchOptions setEventBufferSize(int eventBufferSize) {
		this.eventBufferSize = eventBufferSize;
		return this;
	}

	public int getBulkDebounceTime() {
		return bulkDebounceTime;
	}

	@Setter
	public ElasticSearchOptions setBulkDebounceTime(int bulkDebounceTime) {
		this.bulkDebounceTime = bulkDebounceTime;
		return this;
	}

	public int getIdleDebounceTime() {
		return idleDebounceTime;
	}

	@Setter
	public ElasticSearchOptions setIdleDebounceTime(int idleDebounceTime) {
		this.idleDebounceTime = idleDebounceTime;
		return this;
	}

	public int getRetryInterval() {
		return retryInterval;
	}

	@Setter
	public ElasticSearchOptions setRetryInterval(int retryInterval) {
		this.retryInterval = retryInterval;
		return this;
	}

	public boolean isWaitForIdle() {
		return waitForIdle;
	}

	@Setter
	public ElasticSearchOptions setWaitForIdle(boolean waitForIdle) {
		this.waitForIdle = waitForIdle;
		return this;
	}

	public boolean isIncludeBinaryFields() {
		return includeBinaryFields;
	}

	@Setter
	public ElasticSearchOptions setIncludeBinaryFields(boolean includeBinaryFields) {
		this.includeBinaryFields = includeBinaryFields;
		return this;
	}

	public MappingMode getMappingMode() {
		return mappingMode;
	}

	@Setter
	public ElasticSearchOptions setMappingMode(MappingMode mode) {
		this.mappingMode = mode;
		return this;
	}

	public ComplianceMode getComplianceMode() {
		return complianceMode;
	}

	@Setter
	public ElasticSearchOptions setComplianceMode(ComplianceMode complianceMode) {
		this.complianceMode = complianceMode;
		return this;
	}

	public int getRetryLimit() {
		return retryLimit;
	}

	@Setter
	public ElasticSearchOptions setRetryLimit(int retryLimit) {
		this.retryLimit = retryLimit;
		return this;
	}

	/**
	 * Validate the options.
	 */
	public void validate(MeshOptions meshOptions) {

	}

	/**
	 * Disable the elasticsearch integration by setting a null URL.
	 * 
	 * @return
	 */
	@JsonIgnore
	public ElasticSearchOptions disable() {
		setUrl(null);
		return this;
	}

	public int getSyncBatchSize() {
		return syncBatchSize;
	}

	public void setSyncBatchSize(int batchSize) {
		this.syncBatchSize = batchSize;
	}

	/**
	 * Index check interval in ms
	 * @return interval
	 */
	public long getIndexCheckInterval() {
		return indexCheckInterval;
	}

	/**
	 * Set the index check interval in ms
	 * @param indexCheckInterval interval
	 */
	public void setIndexCheckInterval(long indexCheckInterval) {
		this.indexCheckInterval = indexCheckInterval;
	}

	/**
	 * Timeout for the cache of index mappings in ms
	 * @return timeout
	 */
	public long getIndexMappingCacheTimeout() {
		return indexMappingCacheTimeout;
	}

	/**
	 * Set the timeout for the cache of index mappings
	 * @param indexMappingCacheTimeout timeout
	 */
	public void setIndexMappingCacheTimeout(long indexMappingCacheTimeout) {
		this.indexMappingCacheTimeout = indexMappingCacheTimeout;
	}
}
