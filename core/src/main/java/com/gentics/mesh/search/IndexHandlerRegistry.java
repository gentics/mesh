package com.gentics.mesh.search;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.search.index.IndexHandler;
import com.gentics.mesh.search.index.group.GroupIndexHandler;
import com.gentics.mesh.search.index.microschema.MicroschemaContainerIndexHandler;
import com.gentics.mesh.search.index.node.NodeIndexHandler;
import com.gentics.mesh.search.index.project.ProjectIndexHandler;
import com.gentics.mesh.search.index.role.RoleIndexHandler;
import com.gentics.mesh.search.index.schema.SchemaContainerIndexHandler;
import com.gentics.mesh.search.index.tag.TagIndexHandler;
import com.gentics.mesh.search.index.tagfamily.TagFamilyIndexHandler;
import com.gentics.mesh.search.index.user.UserIndexHandler;

/**
 * Central location for all search index handlers.
 */
@Singleton
public class IndexHandlerRegistry {

	@Inject
	NodeIndexHandler nodeIndexHandler;

	@Inject
	UserIndexHandler userIndexHandler;

	@Inject
	GroupIndexHandler groupIndexHandler;

	@Inject
	RoleIndexHandler roleIndexHandler;

	@Inject
	ProjectIndexHandler projectIndexHandler;

	@Inject
	TagFamilyIndexHandler tagFamilyIndexHandler;

	@Inject
	TagIndexHandler tagIndexHandler;

	@Inject
	SchemaContainerIndexHandler schemaContainerIndexHandler;

	@Inject
	MicroschemaContainerIndexHandler microschemaContainerIndexHandler;

	@Inject
	public IndexHandlerRegistry() {

	}

	/**
	 * Return a collection which contains all registered handlers.
	 * 
	 * @return
	 */
	public Set<IndexHandler> getHandlers() {
		Set<IndexHandler> allIndexHandlers = new HashSet<>();
		allIndexHandlers.add(nodeIndexHandler);
		allIndexHandlers.add(userIndexHandler);
		allIndexHandlers.add(groupIndexHandler);
		allIndexHandlers.add(roleIndexHandler);
		allIndexHandlers.add(projectIndexHandler);
		allIndexHandlers.add(tagFamilyIndexHandler);
		allIndexHandlers.add(tagIndexHandler);
		allIndexHandlers.add(schemaContainerIndexHandler);
		allIndexHandlers.add(microschemaContainerIndexHandler);
		return allIndexHandlers;
	}

	/**
	 * Return the handler for the given type.
	 * 
	 * @param type
	 * @return
	 */
	public IndexHandler getHandlerWithKey(String type) {
		return getHandlers().stream().filter(handler -> handler.getKey().equals(type)).findFirst().orElseGet(null);
	}

}
