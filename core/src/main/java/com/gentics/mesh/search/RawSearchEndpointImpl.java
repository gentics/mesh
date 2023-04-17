package com.gentics.mesh.search;

import javax.inject.Inject;

import com.gentics.mesh.auth.MeshAuthChainImpl;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.admin.LocalConfigApi;
import com.gentics.mesh.router.route.AbstractInternalEndpoint;
import com.gentics.mesh.search.index.AdminIndexHandler;
import com.gentics.mesh.search.index.group.GroupSearchHandler;
import com.gentics.mesh.search.index.microschema.MicroschemaSearchHandler;
import com.gentics.mesh.search.index.node.NodeSearchHandler;
import com.gentics.mesh.search.index.project.ProjectSearchHandler;
import com.gentics.mesh.search.index.role.RoleSearchHandler;
import com.gentics.mesh.search.index.schema.SchemaSearchHandler;
import com.gentics.mesh.search.index.tag.TagSearchHandler;
import com.gentics.mesh.search.index.tagfamily.TagFamilySearchHandler;
import com.gentics.mesh.search.index.user.UserSearchHandler;

/**
 * 
 * Endpoint defintion for the /api/v1/rawSearch/:field routes.
 * 
 * @see SearchProvider
 */
public class RawSearchEndpointImpl extends AbstractInternalEndpoint implements SearchEndpoint {

	protected final AdminIndexHandler adminHandler;

	protected final  UserSearchHandler userSearchHandler;

	protected final  GroupSearchHandler groupSearchHandler;

	protected final  RoleSearchHandler roleSearchHandler;

	protected final  NodeSearchHandler nodeSearchHandler;

	protected final  TagSearchHandler tagSearchHandler;

	protected final  TagFamilySearchHandler tagFamilySearchHandler;

	protected final ProjectSearchHandler projectSearchHandler;

	protected final  SchemaSearchHandler schemaContainerSearchHandler;

	protected final  MicroschemaSearchHandler microschemaContainerSearchHandler;

	@Inject
	public RawSearchEndpointImpl(MeshAuthChainImpl chain, NodeSearchHandler searchHandler,
			AdminIndexHandler adminHandler, UserSearchHandler userSearchHandler, GroupSearchHandler groupSearchHandler,
			RoleSearchHandler roleSearchHandler,
			NodeSearchHandler nodeSearchHandler,
			TagSearchHandler tagSearchHandler,
			TagFamilySearchHandler tagFamilySearchHandler,
			ProjectSearchHandler projectSearchHandler,
			SchemaSearchHandler schemaContainerSearchHandler,
			MicroschemaSearchHandler microschemaContainerSearchHandler, LocalConfigApi localConfigApi, Database db) {
		super("rawSearch", chain, localConfigApi, db);
		this.adminHandler = adminHandler;
		this.userSearchHandler = userSearchHandler;
		this.groupSearchHandler = groupSearchHandler;
		this.roleSearchHandler = roleSearchHandler;
		this.nodeSearchHandler = nodeSearchHandler;
		this.tagSearchHandler = tagSearchHandler;
		this.tagFamilySearchHandler = tagFamilySearchHandler;
		this.projectSearchHandler = projectSearchHandler;
		this.schemaContainerSearchHandler = schemaContainerSearchHandler;
		this.microschemaContainerSearchHandler = microschemaContainerSearchHandler;
	}

	public RawSearchEndpointImpl() {
		super("rawSearch", null, null, null);
		this.adminHandler = null;
		this.userSearchHandler = null;
		this.groupSearchHandler = null;
		this.roleSearchHandler = null;
		this.nodeSearchHandler = null;
		this.tagSearchHandler = null;
		this.tagFamilySearchHandler = null;
		this.projectSearchHandler = null;
		this.schemaContainerSearchHandler = null;
		this.microschemaContainerSearchHandler = null;
	}

	@Override
	public String getDescription() {
		return "Provides search endpoints which can be used to invoke global searches which return the unmodified Elasticsearch response.";
	}

	@Override
	public void registerEndPoints() {
		secureAll();

		addSearchEndpoints();
	}

	private void addSearchEndpoints() {
		registerRawSearchHandler("users", userSearchHandler);
		registerRawSearchHandler("groups", groupSearchHandler);
		registerRawSearchHandler("roles", roleSearchHandler);

		registerRawSearchHandler("nodes", nodeSearchHandler);
		registerRawSearchHandler("tags", tagSearchHandler);
		registerRawSearchHandler("tagFamilies", tagFamilySearchHandler);

		registerRawSearchHandler("projects", projectSearchHandler);
		registerRawSearchHandler("schemas", schemaContainerSearchHandler);
		registerRawSearchHandler("microschemas", microschemaContainerSearchHandler);
	}

}
