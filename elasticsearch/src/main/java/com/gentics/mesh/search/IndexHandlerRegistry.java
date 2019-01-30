package com.gentics.mesh.search;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.search.IndexHandler;
import com.gentics.mesh.graphdb.model.MeshElement;
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
	public List<IndexHandler<?>> getHandlers() {
		List<IndexHandler<?>> allIndexHandlers = new ArrayList<>();
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
	 * Identify the handler and return the matching one.
	 * 
	 * @param element
	 * @return
	 */
	public IndexHandler<?> getForClass(MeshElement element) {
		Class<?> clazzOfElement = element.getClass();
		return getForClass(clazzOfElement);
	}

	/**
	 * Return the matching handler for the given element class.
	 * 
	 * @param elementClass
	 * @return
	 */
	public IndexHandler<?> getForClass(Class<?> elementClass) {
		for (IndexHandler<?> handler : getHandlers()) {
			if (handler.accepts(elementClass)) {
				return (IndexHandler<?>) handler;
			}
		}
		return null;
	}

	public NodeIndexHandler getNodeIndexHandler() {
		return nodeIndexHandler;
	}
}