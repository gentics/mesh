package com.gentics.mesh.search;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.AbstractProjectEndpoint;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.search.index.node.NodeSearchHandler;
import com.gentics.mesh.search.index.tag.TagSearchHandler;
import com.gentics.mesh.search.index.tagfamily.TagFamilySearchHandler;

@Singleton
public class ProjectRawSearchEndpointImpl extends AbstractProjectEndpoint implements SearchEndpoint {

	@Inject
	public NodeSearchHandler nodeSearchHandler;

	@Inject
	public TagSearchHandler tagSearchHandler;

	@Inject
	public TagFamilySearchHandler tagFamilySearchHandler;

	public ProjectRawSearchEndpointImpl() {
		super("rawSearch", null, null);
	}

	@Inject
	public ProjectRawSearchEndpointImpl(BootstrapInitializer boot, RouterStorage routerStorage) {
		super("rawSearch", boot, routerStorage);
	}

	@Override
	public String getDescription() {
		return "Provides endpoints which allow project wide search which return the raw search result.";
	}

	@Override
	public void registerEndPoints() {
		registerRawSearchHandler("nodes", nodeSearchHandler);
		registerRawSearchHandler("tags", tagSearchHandler);
		registerRawSearchHandler("tagFamilies", tagFamilySearchHandler);
	}
}
