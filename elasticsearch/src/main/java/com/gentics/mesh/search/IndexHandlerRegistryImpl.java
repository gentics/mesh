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

	protected final NodeIndexHandlerImpl nodeIndexHandler;

	protected final UserIndexHandlerImpl userIndexHandler;

	protected final GroupIndexHandlerImpl groupIndexHandler;

	protected final RoleIndexHandlerImpl roleIndexHandler;

	protected final ProjectIndexHandlerImpl projectIndexHandler;

	protected final TagFamilyIndexHandlerImpl tagFamilyIndexHandler;

	protected final TagIndexHandlerImpl tagIndexHandler;

	protected final SchemaContainerIndexHandlerImpl schemaContainerIndexHandler;

	protected final MicroschemaContainerIndexHandlerImpl microschemaContainerIndexHandler;

	@Inject
	public IndexHandlerRegistryImpl(NodeIndexHandlerImpl nodeIndexHandler, UserIndexHandlerImpl userIndexHandler,
			GroupIndexHandlerImpl groupIndexHandler, RoleIndexHandlerImpl roleIndexHandler,
			ProjectIndexHandlerImpl projectIndexHandler, TagFamilyIndexHandlerImpl tagFamilyIndexHandler,
			TagIndexHandlerImpl tagIndexHandler, SchemaContainerIndexHandlerImpl schemaContainerIndexHandler,
			MicroschemaContainerIndexHandlerImpl microschemaContainerIndexHandler) {
		this.nodeIndexHandler = nodeIndexHandler;
		this.userIndexHandler = userIndexHandler;
		this.groupIndexHandler = groupIndexHandler;
		this.roleIndexHandler = roleIndexHandler;
		this.projectIndexHandler = projectIndexHandler;
		this.tagFamilyIndexHandler = tagFamilyIndexHandler;
		this.tagIndexHandler = tagIndexHandler;
		this.schemaContainerIndexHandler = schemaContainerIndexHandler;
		this.microschemaContainerIndexHandler = microschemaContainerIndexHandler;
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