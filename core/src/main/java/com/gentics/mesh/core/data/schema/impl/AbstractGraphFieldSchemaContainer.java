package com.gentics.mesh.core.data.schema.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_LATEST_VERSION;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_PARENT_CONTAINER;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.DELETE_ACTION;

import java.util.List;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerImpl;
import com.gentics.mesh.core.data.generic.AbstractMeshCoreVertex;
import com.gentics.mesh.core.data.schema.GraphFieldSchemaContainer;
import com.gentics.mesh.core.data.schema.GraphFieldSchemaContainerVersion;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.common.NameUuidReference;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import rx.Observable;

/**
 * The {@link AbstractGraphFieldSchemaContainer} contains the abstract graph element implementation for {@link GraphFieldSchemaContainer} implementations (e.g.:
 * {@link SchemaContainerImpl}, {@link MicroschemaContainerImpl}).
 * 
 * @param <R>
 *            Field container rest model type
 * @param <RE>
 *            Field container rest model reference type
 * @param <SC>
 *            Graph vertex type
 * @param <SCV>
 *            Graph vertex version type
 */
public abstract class AbstractGraphFieldSchemaContainer<R extends FieldSchemaContainer, RE extends NameUuidReference<RE>, SC extends GraphFieldSchemaContainer<R, RE, SC, SCV>, SCV extends GraphFieldSchemaContainerVersion<R, RE, SCV, SC>>
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
		setSingleLinkOutTo(version.getImpl(), HAS_LATEST_VERSION);
	}

	@Override
	public List<? extends SCV> findAll() {
		return out(HAS_PARENT_CONTAINER).toListExplicit(getContainerVersionClass());
	}

	@Override
	public SCV findVersionByUuid(String uuid) {
		return out(HAS_PARENT_CONTAINER).has("uuid", uuid).nextOrDefaultExplicit(getContainerVersionClass(), null);
	}

	@Override
	public SCV findVersionByRev(Integer version) {
		return out(HAS_PARENT_CONTAINER).has(AbstractGraphFieldSchemaContainerVersion.VERSION_PROPERTY_KEY, version)
				.nextOrDefaultExplicit(getContainerVersionClass(), null);
	}

	@Override
	public Observable<? extends SC> update(InternalActionContext ac) {
		throw new NotImplementedException("Updating is not directly supported for schemas. Please start a schema migration");
	}

	@Override
	public void addRelatedEntries(SearchQueueBatch batch, SearchQueueEntryAction action) {

	}

	@Override
	public Observable<R> transformToRest(InternalActionContext ac, int level, String... languageTags) {
		// Delegate transform call to latest version 
		return getLatestVersion().transformToRest(ac, level, languageTags);
	}

	@Override
	public Observable<R> transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		// Delegate transform call to latest version
		return getLatestVersion().transformToRestSync(ac, level, languageTags);
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
	public void delete() {
		// TODO should all references be updated to a new fallback schema?
		createIndexBatch(DELETE_ACTION);
		getElement().remove();
		//TODO delete versions and nodes as well
	}

}
