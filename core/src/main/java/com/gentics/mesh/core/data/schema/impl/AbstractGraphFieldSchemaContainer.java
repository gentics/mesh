package com.gentics.mesh.core.data.schema.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_LATEST_VERSION;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_PARENT_CONTAINER;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerImpl;
import com.gentics.mesh.core.data.generic.AbstractMeshCoreVertex;
import com.gentics.mesh.core.data.schema.GraphFieldSchemaContainer;
import com.gentics.mesh.core.data.schema.GraphFieldSchemaContainerVersion;
import com.gentics.mesh.core.rest.common.NameUuidReference;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.util.ETag;

/**
 * The {@link AbstractGraphFieldSchemaContainer} contains the abstract graph element implementation for {@link GraphFieldSchemaContainer} implementations (e.g.:
 * {@link SchemaContainerImpl}, {@link MicroschemaContainerImpl}).
 * 
 * @param <R>
 *            Field container rest model response type
 * @param <RM>
 *            Field container rest model type
 * @param <RE>
 *            Field container rest model reference type
 * @param <SC>
 *            Graph vertex type
 * @param <SCV>
 *            Graph vertex version type
 */
public abstract class AbstractGraphFieldSchemaContainer<R extends FieldSchemaContainer, RM extends FieldSchemaContainer, RE extends NameUuidReference<RE>, SC extends GraphFieldSchemaContainer<R, RE, SC, SCV>, SCV extends GraphFieldSchemaContainerVersion<R, RM, RE, SCV, SC>>
		extends AbstractMeshCoreVertex<R, SC> implements GraphFieldSchemaContainer<R, RE, SC, SCV> {

	/**
	 * Return the class that is used to construct new containers.
	 * 
	 * @return
	 */
	protected abstract Class<? extends SC> getContainerClass();

	/**
	 * Return the class that is used to construct new container versions.
	 * 
	 * @return
	 */
	protected abstract Class<? extends SCV> getContainerVersionClass();

	@Override
	public SCV getLatestVersion() {
		return out(HAS_LATEST_VERSION).nextOrDefaultExplicit(getContainerVersionClass(), null);
	}

	@Override
	public void setLatestVersion(SCV version) {
		setSingleLinkOutTo(version, HAS_LATEST_VERSION);
	}

	@Override
	public Iterable<? extends SCV> findAll() {
		return out(HAS_PARENT_CONTAINER).frameExplicit(getContainerVersionClass());
	}

	@Override
	public SCV findVersionByUuid(String uuid) {
		return out(HAS_PARENT_CONTAINER).has("uuid", uuid).nextOrDefaultExplicit(getContainerVersionClass(), null);
	}

	@Override
	public SCV findVersionByRev(String version) {
		return out(HAS_PARENT_CONTAINER).has(AbstractGraphFieldSchemaContainerVersion.VERSION_PROPERTY_KEY, version)
				.nextOrDefaultExplicit(getContainerVersionClass(), null);
	}

	@Override
	public boolean update(InternalActionContext ac, EventQueueBatch batch) {
		throw new NotImplementedException("Updating is not directly supported for schemas. Please start a schema migration");
	}

	@Override
	public R transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		String version = ac.getVersioningParameters().getVersion();
		// Delegate transform call to latest version
		if (version == null || version.equals("draft")) {
			return getLatestVersion().transformToRestSync(ac, level, languageTags);
		} else {
			SCV foundVersion = findVersionByRev(version);
			if (foundVersion == null) {
				throw error(NOT_FOUND, "object_not_found_for_uuid_version", getUuid(), version);
			}
			return foundVersion.transformToRestSync(ac, level, languageTags);
		}
	}

	@Override
	public String getName() {
		return getProperty("name");
	}

	@Override
	public void setName(String name) {
		setProperty("name", name);
	}

	@Override
	public void delete(BulkActionContext bac) {
		// TODO should all references be updated to a new fallback schema?
		bac.add(onDeleted());
		getElement().remove();
		// TODO delete versions and nodes as well
	}

	@Override
	public String getSubETag(InternalActionContext ac) {
		return ETag.hash(getLatestVersion().getETag(ac));
	}

	@Override
	public Map<Branch, SCV> findReferencedBranches() {
		Map<Branch, SCV> references = new HashMap<>();
		for (SCV version : findAll()) {
			version.getBranches().forEach(branch -> references.put(branch, version));
		}
		return references;
	}

}
