package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainerVersion;

/**
 * A root storage definition for the versioned container entities.
 * 
 * @author plyhun
 *
 * @param <R> REST container entity
 * @param <RM> REST container entity version
 * @param <D> Graph container entity
 * @param <DV> Graph container entity version
 */
public interface ContainerRootVertex<
			R extends FieldSchemaContainer, 
			RM extends FieldSchemaContainerVersion, 
			D extends MeshCoreVertex<R>, 
			DV extends MeshCoreVertex<R>
		> extends RootVertex<D> {

	/**
	 * Create a new container version.
	 * 
	 * @return
	 */
	DV createVersion();	
}
