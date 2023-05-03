package com.gentics.mesh.plugin.search.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.etc.config.env.EnvironmentVariable;
import com.gentics.mesh.plugin.common.license.LicenseConfiguration;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SearchConfiguration extends LicenseConfiguration {

	public static final String SEARCH_PAGE_SIZE = "SEARCH_PAGE_SIZE";
	public static final String SEARCH_PROJECTS = "SEARCH_PROJECTS";
	public static final String SEARCH_RESULT_QUERY = "SEARCH_RESULT_QUERY";
	public static final String SEARCH_DID_YOU_MEAN_THRESHOLD = "SEARCH_DID_YOU_MEAN_THRESHOLD";
	public static final String SEARCH_CACHE_ENABLED = "SEARCH_CACHE_ENABLED";
	public static final String SEARCH_CACHE_EXPIRE_TIME = "SEARCH_CACHE_EXPIRE_TIME";
	public static final String SEARCH_WAIT_FOR_IDLE = "SEARCH_WAIT_FOR_IDLE";

	public static final int DEFAULT_DID_YOU_MEAN_THRESHOLD = 10;
	public static final int DEFAULT_PAGE_SIZE = 8;
	public static final int DEFAULT_EXPIRE_TIME = 60;
	public static final Boolean DEFAULT_WAIT_FOR_IDLE = null;

	private final Logger log = LoggerFactory.getLogger(SearchConfiguration.class);

	@JsonProperty(defaultValue = "8")
	@JsonPropertyDescription("How many search hits should be returned per page.")
	@EnvironmentVariable(name = SEARCH_PAGE_SIZE, description = "Override the page size")
	private int pageSize = DEFAULT_PAGE_SIZE;

	@JsonProperty
	@JsonPropertyDescription("List of project to be searched by this portal instance. Use '*' to search all projects.")
	@EnvironmentVariable(name = SEARCH_PROJECTS, description = "Override the searchable projects.")
	private Map<String, Set<String>> projects = new HashMap<>();

	@JsonProperty
	@JsonPropertyDescription("Path to the search result query to get custom search result fields. When empty the result of the elastic search query itself will be the final result.")
	@EnvironmentVariable(name = SEARCH_RESULT_QUERY, description = "Override the search result query.")
	private String resultQuery;

	@JsonProperty(defaultValue = "10")
	@JsonPropertyDescription("The threshold how many search hits are required to skip the did-you-mean suggestions.")
	@EnvironmentVariable(name = SEARCH_DID_YOU_MEAN_THRESHOLD, description = "Override the did-you-mean threshold.")
	private int didYouMeanThreshold = DEFAULT_DID_YOU_MEAN_THRESHOLD;

	@JsonProperty(defaultValue = "true")
	@JsonPropertyDescription("Whether Elasticsearch queries should be cached.")
	@EnvironmentVariable(name = SEARCH_CACHE_ENABLED, description = "Override whether caching is enabled.")
	private boolean caching = true;

	@JsonProperty(defaultValue = "60s")
	@JsonPropertyDescription("The expiretime in seconds for cached information about schemas")
	@EnvironmentVariable(name = SEARCH_CACHE_EXPIRE_TIME, description = "Override the expire time of cached search queries.")
	private int expireTime = DEFAULT_EXPIRE_TIME;

	@JsonProperty(defaultValue = "null (parameter is not set)")
	@JsonPropertyDescription("The value for the waitForIdle search parameter for the request to Mesh.")
	@EnvironmentVariable(name = SEARCH_WAIT_FOR_IDLE, description = "Override the value for the waitForIdle parameter.")
	private Boolean waitForIdle = DEFAULT_WAIT_FOR_IDLE;

	@JsonProperty
	@JsonPropertyDescription("Set of custom queries. The values are used as paths and query filenames.")
	private Set<CustomQueryOptions> customQueries = new HashSet<>();

	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public Map<String, Set<String>> getProjects() {
		return projects;
	}

	public SearchConfiguration setProjects(Map<String, Set<String>> projects) {
		this.projects = projects;

		return this;
	}

	public String getResultQuery() {
		return resultQuery;
	}

	public SearchConfiguration setResultQuery(String resultQuery) {
		this.resultQuery = resultQuery;

		return this;
	}

	public int getDidYouMeanThreshold() {
		return didYouMeanThreshold;
	}

	public SearchConfiguration setDidYouMeanThreshold(int didYouMeanThreshold) {
		this.didYouMeanThreshold = didYouMeanThreshold;

		return this;
	}

	public boolean isCaching() {
		return caching;
	}

	public SearchConfiguration setCaching(boolean caching) {
		this.caching = caching;

		return this;
	}

	public int getExpireTime() {
		return expireTime;
	}

	public SearchConfiguration setExpireTime(int expireTime) {
		this.expireTime = expireTime;

		return this;
	}

	public Boolean getWaitForIdle() {
		return waitForIdle;
	}

	public SearchConfiguration setWaitForIdle(Boolean waitForIdle) {
		this.waitForIdle = waitForIdle;

		return this;
	}

	public Set<CustomQueryOptions> getCustomQueries() {
		return customQueries;
	}

	public SearchConfiguration setCustomQueries(Set<CustomQueryOptions> customQueries) {
		this.customQueries = customQueries;

		return this;
	}

	@JsonIgnore
	public Set<String> getSearchedProjects(String project) {
		return getProjects().computeIfAbsent(project, p -> Collections.singleton(p));
	}

	@JsonIgnore
	public boolean isMultiProjectSearch(String project) {
		Set<String> searched = getSearchedProjects(project);

		return searched.size() > 1 || searched.contains("*");
	}

	public void validate(File storageDir) throws FileNotFoundException {
		if (!StringUtils.isBlank(resultQuery)) {
			File query = new File(storageDir, "queries/" + resultQuery + ".graphql");

			if (!query.canRead()) {
				throw new FileNotFoundException("Could not open file \"" + query + "\"");
			}
		}

		// Make sure the project itself is searched for every project.
		projects.entrySet().forEach(e -> {
			Set<String> searched = e.getValue();

			if (searched.contains("*")) {
				searched.removeIf(p -> !"*".equals(p));
			} else {
				searched.add(e.getKey());
			}
		});
	}
}
