package com.gentics.mesh.core.data.schema;

import java.util.List;
import java.util.Map;

import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaReference;

/**
 * A schema container is a graph element which stores the JSON schema data.
 */
public interface SchemaContainer extends GraphFieldSchemaContainer<Schema, SchemaReference, SchemaContainer, SchemaContainerVersion> {

	/**
	 * Type Value: {@value #TYPE}
	 */
	public static final String TYPE = "schemaContainer";

	/**
	 * Return the list of nodes which are referencing the schema container.
	 * 
	 * @return
	 */
	List<? extends NodeImpl> getNodes();

	/**
	 * Return a map of all releases which reference the schema container via an assigned schema container version. The found schema container version will be
	 * added as key to the map.
	 * 
	 * @return
	 */
	Map<Release, SchemaContainerVersion> findReferencedReleases();

}
