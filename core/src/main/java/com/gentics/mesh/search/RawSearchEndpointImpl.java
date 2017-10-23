package com.gentics.mesh.search;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.AbstractEndpoint;
import com.gentics.mesh.etc.RouterStorage;
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

@Singleton
public class RawSearchEndpointImpl extends AbstractEndpoint implements SearchEndpoint {

	@Inject
	AdminIndexHandler adminHandler;

	@Inject
	UserSearchHandler userSearchHandler;

	@Inject
	GroupSearchHandler groupSearchHandler;

	@Inject
	RoleSearchHandler roleSearchHandler;

	@Inject
	NodeSearchHandler nodeSearchHandler;

	@Inject
	TagSearchHandler tagSearchHandler;

	@Inject
	TagFamilySearchHandler tagFamilySearchHandler;

	@Inject
	ProjectSearchHandler projectSearchHandler;

	@Inject
	SchemaSearchHandler schemaContainerSearchHandler;

	@Inject
	MicroschemaSearchHandler microschemaContainerSearchHandler;

	@Inject
	public RawSearchEndpointImpl(RouterStorage routerStorage, NodeSearchHandler searchHandler) {
		super("rawSearch", routerStorage);
	}

	public RawSearchEndpointImpl() {
		super("rawSearch", null);
	}

	@Override
	public String getDescription() {
		return "Provides search endpoints which can be used to invoke global searches which return the raw search result.";
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
