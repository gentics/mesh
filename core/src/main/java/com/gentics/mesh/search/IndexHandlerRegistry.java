package com.gentics.mesh.search;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang.NotImplementedException;

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
 * Central location to register search index handlers.
 */
@Singleton
public class IndexHandlerRegistry {

	private Map<String, IndexHandler> handlers = Collections.synchronizedMap(new HashMap<>());

	private static IndexHandlerRegistry instance;

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
		instance = this;

	}

	public void init() {
		registerHandler(nodeIndexHandler);
		registerHandler(userIndexHandler);
		registerHandler(groupIndexHandler);
		registerHandler(roleIndexHandler);
		registerHandler(projectIndexHandler);
		registerHandler(tagFamilyIndexHandler);
		registerHandler(tagIndexHandler);
		registerHandler(schemaContainerIndexHandler);
		registerHandler(microschemaContainerIndexHandler);
	}

	/**
	 * Get the instance
	 * 
	 * @return instance
	 */
	public static IndexHandlerRegistry getInstance() {
		return instance;
	}

	/**
	 * Register the given handler.
	 * 
	 * @param handler
	 */
	public void registerHandler(IndexHandler handler) {
		handlers.put(handler.getKey(), handler);
	}

	/**
	 * Unregister the given handler.
	 * 
	 * @param handler
	 */
	public void unregisterHandler(IndexHandler handler) {
		handlers.remove(handler.getKey());
	}

	/**
	 * Return a collection which contains all registered handlers.
	 * 
	 * @return
	 */
	public Collection<IndexHandler> getHandlers() {
		return handlers.values();
	}

	/**
	 * Get the index handler with given key
	 * 
	 * @param key
	 *            index handler key
	 * @return index handler or null if not registered
	 */
	public IndexHandler get(String key) {
		if (!handlers.containsKey(key)) {
			throw new NotImplementedException("Index type {" + key + "} was not registered.");
		}
		return handlers.get(key);
	}
}
