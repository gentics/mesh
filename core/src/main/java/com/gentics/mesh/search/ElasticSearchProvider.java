package com.gentics.mesh.search;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import com.gentics.mesh.etc.ElasticSearchOptions;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class ElasticSearchProvider {

	private static final Logger log = LoggerFactory.getLogger(ElasticSearchProvider.class);

	private Node node;

	private ElasticSearchOptions options;

	public void start() {

		if (log.isDebugEnabled()) {
			log.debug("Creating elasticsearch node");
		}
		long start = System.currentTimeMillis();
		String dataDirectory = options.getDirectory();
		ImmutableSettings.Builder elasticsearchSettings = ImmutableSettings.settingsBuilder().put("http.enabled", "false").put("path.data",
				dataDirectory);
		node = NodeBuilder.nodeBuilder().local(true).settings(elasticsearchSettings.build()).node();
		if (log.isDebugEnabled()) {
			log.debug("Waited for elasticsearch shard: " + (System.currentTimeMillis() - start) + "[ms]");
		}
	}

	public ElasticSearchProvider init(ElasticSearchOptions options) {
		this.options = options;
		start();
		return this;
	}

	public Node getNode() {
		return node;
	}

	public void reset() {
		if (log.isDebugEnabled()) {
			log.debug("Resetting elastic search");
		}
		stop();
		try {
			if (options.getDirectory() != null) {
				File storageDirectory = new File(options.getDirectory());
				if (storageDirectory.exists()) {
					FileUtils.deleteDirectory(storageDirectory);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		start();
	}

	public void stop() {
		node.close();
	}
}
