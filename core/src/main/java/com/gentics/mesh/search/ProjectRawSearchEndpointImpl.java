package com.gentics.mesh.search;

import javax.inject.Inject;

import com.gentics.mesh.auth.MeshAuthChainImpl;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.admin.LocalConfigApi;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.router.route.AbstractProjectEndpoint;
import com.gentics.mesh.search.index.node.NodeSearchHandler;
import com.gentics.mesh.search.index.tag.TagSearchHandler;
import com.gentics.mesh.search.index.tagfamily.TagFamilySearchHandler;

/**
 * @see SearchEndpoint 
 */
public class ProjectRawSearchEndpointImpl extends AbstractProjectEndpoint implements SearchEndpoint {

	protected final NodeSearchHandler nodeSearchHandler;

	protected final TagSearchHandler tagSearchHandler;

	protected final TagFamilySearchHandler tagFamilySearchHandler;

	public ProjectRawSearchEndpointImpl() {
		super("rawSearch", null, null, null, null, null);
		this.nodeSearchHandler = null;
		this.tagSearchHandler = null;
		this.tagFamilySearchHandler = null;
	}

	@Inject
	public ProjectRawSearchEndpointImpl(MeshAuthChainImpl chain, BootstrapInitializer boot,
			NodeSearchHandler nodeSearchHandler, TagSearchHandler tagSearchHandler,
			TagFamilySearchHandler tagFamilySearchHandler, LocalConfigApi localConfigApi, Database db, MeshOptions options) {
		super("rawSearch", chain, boot, localConfigApi, db, options);
		this.nodeSearchHandler = nodeSearchHandler;
		this.tagSearchHandler = tagSearchHandler;
		this.tagFamilySearchHandler = tagFamilySearchHandler;
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
