package com.gentics.mesh.core.data.schema.handler;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.schema.GraphFieldSchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaChange;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.Schema;

/**
 * The field container mutator utilizes {@link SchemaChange} objects in order to modify/mutate a given field container implementaton (eg. {@link Schema} or
 * {@link Microschema}. This way {@link SchemaChange} operations can be applied on a given schema.
 */
@Component
public class FieldSchemaContainerMutator {

	private static FieldSchemaContainerMutator instance;

	public static FieldSchemaContainerMutator getInstance() {
		return instance;
	}

	@PostConstruct
	public void setup() {
		FieldSchemaContainerMutator.instance = this;
	}

	/**
	 * Applies all changes that are connected to the container to the version of the container and returns the mutated version of the field container that was
	 * initially loaded from the graph field container element.
	 * 
	 * @param container
	 *            Graph element that provides the chain of changes and the field container that should be mutated
	 * @return
	 */
	public <R extends FieldSchemaContainer> R apply(GraphFieldSchemaContainer<R, ?, ?> container) {

		R oldSchema = container.getSchema();
		ServerSchemaStorage.getSchemaStorage().remove(oldSchema);
		SchemaChange<?> change = container.getNextChange();
		while (change != null) {
			oldSchema = change.apply(oldSchema);
			change = change.getNextChange();
		}
		return oldSchema;
	}

}
