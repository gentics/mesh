package com.gentics.mesh.search;

import javax.inject.Inject;

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

public class RawSearchEndpointImpl extends AbstractInternalEndpoint implements SearchEndpoint {

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
	public RawSearchEndpointImpl(NodeSearchHandler searchHandler) {
		super("rawSearch");
	}

	public RawSearchEndpointImpl() {
		super("rawSearch");
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
