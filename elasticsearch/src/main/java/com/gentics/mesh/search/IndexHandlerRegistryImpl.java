package com.gentics.mesh.search;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.HibElement;
import com.gentics.mesh.core.data.search.IndexHandler;
import com.gentics.mesh.core.search.index.node.NodeIndexHandler;
import com.gentics.mesh.search.index.group.GroupIndexHandlerImpl;
import com.gentics.mesh.search.index.microschema.MicroschemaContainerIndexHandlerImpl;
import com.gentics.mesh.search.index.node.NodeIndexHandlerImpl;
import com.gentics.mesh.search.index.project.ProjectIndexHandlerImpl;
import com.gentics.mesh.search.index.role.RoleIndexHandlerImpl;
import com.gentics.mesh.search.index.schema.SchemaContainerIndexHandlerImpl;
import com.gentics.mesh.search.index.tag.TagIndexHandlerImpl;
import com.gentics.mesh.search.index.tagfamily.TagFamilyIndexHandlerImpl;
import com.gentics.mesh.search.index.user.UserIndexHandlerImpl;

/**
 * Central location for all search index handlers.
 */
@Singleton
public class IndexHandlerRegistryImpl implements IndexHandlerRegistry {

	@Inject
	NodeIndexHandlerImpl nodeIndexHandler;

	@Inject
	UserIndexHandlerImpl userIndexHandler;

	@Inject
	GroupIndexHandlerImpl groupIndexHandler;

	@Inject
	RoleIndexHandlerImpl roleIndexHandler;

	@Inject
	ProjectIndexHandlerImpl projectIndexHandler;

	@Inject
	TagFamilyIndexHandlerImpl tagFamilyIndexHandler;

	@Inject
	TagIndexHandlerImpl tagIndexHandler;

	@Inject
	SchemaContainerIndexHandlerImpl schemaContainerIndexHandler;

	@Inject
	MicroschemaContainerIndexHandlerImpl microschemaContainerIndexHandler;

	@Inject
	public IndexHandlerRegistryImpl() {

	}

	@Override
	public List<IndexHandler<?>> getHandlers() {
		return Arrays.asList(
			nodeIndexHandler,
			userIndexHandler,
			groupIndexHandler,
			roleIndexHandler,
			projectIndexHandler,
			tagFamilyIndexHandler,
			tagIndexHandler,
			schemaContainerIndexHandler,
			microschemaContainerIndexHandler);
	}

	/**
	 * Identify the handler and return the matching one.
	 * 
	 * @param element
	 * @return
	 */
	public IndexHandler<?> getForClass(HibElement element) {
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

	@Override
	public NodeIndexHandler getNodeIndexHandler() {
		return nodeIndexHandler;
	}
}