package com.gentics.mesh.etc.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.doc.GenerateDocumentation;
import com.gentics.mesh.etc.config.env.EnvironmentVariable;
import com.gentics.mesh.etc.config.env.Option;

@GenerateDocumentation
public class DebugInfoOptions implements Option {
	private static final String DEFAULT_LOG_FOLDER = "debuginfo";
	private static final String DEFAULT_LOG_FILE_SIZE = "5MB";
	private static final String DEFAULT_LOG_PATTERN = "%d{HH:mm:ss.SSS} [%meshName] %-5level [%thread] [%file:%line] - %msg%n";

	@JsonProperty(required = false)
	@JsonPropertyDescription("The path of the folder where the debug info log is stored.")
	@EnvironmentVariable(name = "MESH_DEBUGINFO_LOG_FOLDER", description = "Override the path to the debug info log folder")
	private String logFolder = DEFAULT_LOG_FOLDER;

	@JsonProperty(required = false)
	@JsonPropertyDescription("The maximum file size of a single log file. Default is '5MB'")
	@EnvironmentVariable(name = "MESH_DEBUGINFO_LOG_FILE_SIZE", description = "Override the log file size")
	private String logFileSize = DEFAULT_LOG_FILE_SIZE;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Enables the debug info log")
	@EnvironmentVariable(name = "MESH_DEBUGINFO_LOG_ENABLED", description = "Enables the debug info log")
	private boolean logEnabled = true;

	@JsonProperty(required = false)
	@JsonPropertyDescription("The pattern used for each log line")
	@EnvironmentVariable(name = "MESH_DEBUGINFO_LOG_PATTERN", description = "Override the log pattern")
	private String logPattern = DEFAULT_LOG_PATTERN;

	public String getLogFolder() {
		return logFolder;
	}

	public DebugInfoOptions setLogFolder(String logFolder) {
		this.logFolder = logFolder;
		return this;
	}

	public String getLogFileSize() {
		return logFileSize;
	}

	public void setLogFileSize(String logFileSize) {
		this.logFileSize = logFileSize;
	}

	public boolean isLogEnabled() {
		return logEnabled;
	}

	public DebugInfoOptions setLogEnabled(boolean logEnabled) {
		this.logEnabled = logEnabled;
		return this;
	}

	public String getLogPattern() {
		return logPattern;
	}

	public DebugInfoOptions setLogPattern(String logPattern) {
		this.logPattern = logPattern;
		return this;
	}
}
