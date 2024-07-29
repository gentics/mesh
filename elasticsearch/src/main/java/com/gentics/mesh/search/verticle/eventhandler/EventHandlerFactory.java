package com.gentics.mesh.search.verticle.eventhandler;

import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.search.ComplianceMode;
import com.gentics.mesh.search.verticle.entity.MeshEntities;
import com.gentics.mesh.search.verticle.entity.MeshEntity;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.function.Function;

/**
 * A factory to create simple event handlers.
 */
@Singleton
public class EventHandlerFactory {

	private final MeshEntities entities;
	private final MeshHelper helper;
	private final ComplianceMode complianceMode;

	@Inject
	public EventHandlerFactory(MeshEntities entities, MeshHelper helper, MeshOptions options) {
		this.entities = entities;
		this.helper = helper;
		this.complianceMode = options.getSearchOptions().getComplianceMode();
	}

	/**
	 * Creates a simple event handler for an entity.
	 * 
	 * @param entity
	 *            A function to get an entity.
	 * @param indexName
	 *            The static index name for the entity
	 * @param <T>
	 * @return
	 */
	public <T extends HibBaseElement> EventHandler createSimpleEventHandler(Function<MeshEntities, MeshEntity<T>> entity,
		String indexName) {
		return new SimpleEventHandler<>(helper, entity.apply(entities), indexName, complianceMode);
	}
}
