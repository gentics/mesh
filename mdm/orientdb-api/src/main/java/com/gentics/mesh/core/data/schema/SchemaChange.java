package com.gentics.mesh.core.data.schema;

import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;

/**
 * A schema change represents a single manipulation of a field container (e.g.: {@link SchemaModel}, {@link MicroschemaModel}).
 * 
 * <pre>
 * {@code
 *  (s:SchemaVersion)-[:HAS_CHANGE]->(c1:SchemaChange)-[:HAS_CHANGE]->(c2:SchemaChange)-(s2:SchemaVersion)
 * }
 * </pre>
 * 
 * The schema change stores {@link SchemaChangeModel} data. Since the {@link SchemaChangeModel} class is generic we will also store the model specific
 * properties in a generic way. The {@link #setRestProperty(String, Object)} method can be used to set such properties.
 */
public interface SchemaChange<T extends FieldSchemaContainer> extends MeshVertex, HibSchemaChange<T> {

}
