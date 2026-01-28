package com.gentics.mesh.search.verticle.eventhandler;

import java.util.function.Function;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.search.Compliance;
import com.gentics.mesh.search.verticle.entity.MeshEntities;
import com.gentics.mesh.search.verticle.entity.MeshEntity;

/**
 * A factory to create simple event handlers.
 */
@Singleton
public class EventHandlerFactory {

	private final MeshEntities entities;
	private final MeshHelper helper;
	private final Compliance compliance;

	@Inject
	public EventHandlerFactory(MeshEntities entities, MeshHelper helper, Compliance compliance) {
		this.entities = entities;
		this.helper = helper;
		this.compliance = compliance;
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
		return new SimpleEventHandler<>(helper, entity.apply(entities), indexName, compliance);
	}
}
