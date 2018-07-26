package com.gentics.mesh.changelog.highlevel;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.graphdb.spi.Database;

import dagger.Lazy;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Singleton
public class RestructureWebrootIndex extends AbstractHighLevelChange {

	private static final Logger log = LoggerFactory.getLogger(RestructureWebrootIndex.class);

	private final Lazy<BootstrapInitializer> boot;

	private final Database db;

	@Inject
	public RestructureWebrootIndex(Database db , Lazy<BootstrapInitializer> boot) {
		this.db = db;
		this.boot = boot;
	}

	@Override
	public String getUuid() {
		return "7E94C51E763C46D394C51E763C86D3F5";
	}

	@Override
	public String getName() {
		return "Restructure Webroot Index";
	}

	@Override
	public String getDescription() {
		return "Restructures the webroot index by iterating over all publish and draft edges.";
	}

	@Override
	public void apply() {
		// TODO load all HAS_FIELD_CONTAINER edges
	}

}
