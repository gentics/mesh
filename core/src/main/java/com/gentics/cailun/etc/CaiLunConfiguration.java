package com.gentics.cailun.etc;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import lombok.Data;
import lombok.NoArgsConstructor;

import org.apache.commons.lang3.StringUtils;

@NoArgsConstructor
@Data
public class CaiLunConfiguration {

	public static final String HTTP_PORT_KEY = "httpPort";
	public static final int DEFAULT_HTTP_PORT = 8080;
	private static final String DEFAULT_DIRECTORY_NAME = "graphdb";
	private int httpPort = DEFAULT_HTTP_PORT;

	private String storageDirectory;
	private Map<String, CaiLunVerticleConfiguration> verticles = new HashMap<>();


	public String getStorageDirectory() {
		if (StringUtils.isEmpty(storageDirectory)) {
			// Check for target directory and use it as a subdirectory if possible
			File targetDir = new File("target");
			if (targetDir.exists()) {
				this.storageDirectory = new File("target" + File.separator + DEFAULT_DIRECTORY_NAME).getAbsolutePath();
			} else {
				this.storageDirectory = new File(DEFAULT_DIRECTORY_NAME).getAbsolutePath();
			}
		}
		return storageDirectory;

	}

}
