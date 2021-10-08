package com.gentics.mesh.core.data.schema;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import java.util.HashMap;
import java.util.Map;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.ReferenceableElement;
import com.gentics.mesh.core.data.UserTrackingVertex;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.common.NameUuidReference;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainerVersion;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;

/**
 * Common graph model interface for schema field containers.
 * 
 * @param <R>
 *            Response model class of the container (e.g.: {@link SchemaModel})
 * @param <V>
 *            Container type
 * @param <RE>
 *            Response reference model class of the container (e.g.: {@link SchemaReferenceImpl})
 * @param <VV>
 *            Container version type
 */
public interface GraphFieldSchemaContainer<R extends FieldSchemaContainer, RM extends FieldSchemaContainerVersion, RE extends NameUuidReference<RE>, V extends HibFieldSchemaElement<R, RM, V, VV>, VV extends HibFieldSchemaVersionElement<R, RM, V, VV>>
	extends MeshCoreVertex<R>, ReferenceableElement<RE>, UserTrackingVertex, HibFieldSchemaElement<R, RM, V, VV> {

	/**
	 * Return the global root element for this type of schema container.
	 * 
	 * @return
	 */
	RootVertex<? extends V> getRoot();

	/**
	 * Return the version of the container using the version UUID as a reference.
	 * 
	 * @param uuid
	 * @return
	 */
	VV findVersionByUuid(String uuid);

	/**
	 * Return the version of the container using the version revision as a reference.
	 * 
	 * @param version
	 * @return
	 */
	VV findVersionByRev(String version);

	/**
	 * Return an iterable with all found schema versions.
	 * 
	 * @return
	 */
	Iterable<? extends VV> findAll();
}
