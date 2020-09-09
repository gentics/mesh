package com.gentics.mesh.search;

import javax.inject.Inject;

import com.gentics.mesh.auth.MeshAuthChainImpl;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.router.route.AbstractProjectEndpoint;
import com.gentics.mesh.search.index.node.NodeSearchHandler;
import com.gentics.mesh.search.index.tag.TagSearchHandler;
import com.gentics.mesh.search.index.tagfamily.TagFamilySearchHandler;

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
	public ProjectRawSearchEndpointImpl(MeshAuthChainImpl chain, BootstrapInitializer boot) {
		super("rawSearch", chain, boot);
	}

	@Override
	public String getDescription() {
		return "Provides endpoints which allow project wide search which return the unmodified Elasticsearch response.";
	}

	@Override
	public void registerEndPoints() {
		secureAll();

		registerRawSearchHandler("nodes", nodeSearchHandler);
		registerRawSearchHandler("tags", tagSearchHandler);
		registerRawSearchHandler("tagFamilies", tagFamilySearchHandler);
	}
}
