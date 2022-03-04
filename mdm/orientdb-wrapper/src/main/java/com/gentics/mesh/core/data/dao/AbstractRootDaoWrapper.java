package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.cli.OrientDBBootstrapInitializer;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.common.RestModel;

import dagger.Lazy;

/**
 * A generalized OrientDB implementation for {@link OrientDBRootDao}.
 * Due to the limitation of Java type system, we need to carry around two distinct generics for the same end type of an entity.
 * 
 * @author plyhun
 *
 * @param <RM> REST leaf entity model
 * @param <L> MDM API leaf entity counterpart
 * @param <D> OrientDB Vertex leaf entity counterpart
 * @param <R> MDM API root entity type 
 */
public abstract class AbstractRootDaoWrapper<RM extends RestModel, L extends HibCoreElement<RM>, D extends MeshCoreVertex<RM>, R extends HibCoreElement<? extends RestModel>> 
	extends AbstractDaoWrapper<L> implements DaoTransformable<L, RM>, PersistingRootDao<R, L> {

	public AbstractRootDaoWrapper(Lazy<OrientDBBootstrapInitializer> boot) {
		super(boot);
	}

	@SuppressWarnings("unchecked")
	@Override
	public L createPersisted(R root, String uuid) {
		D vertex = getRoot(root).create();
		L entity = (L) vertex;
		if (uuid != null) {
			entity.setUuid(uuid);
		}
		getRoot(root).addItem(vertex);
		return entity;
	}

	/**
	 * Since OrientDB does not tell apart POJOs and persistent entities, 
	 * processing the entity updates directly into the persistent state, 
	 * the merge implementation here is empty.
	 */
	@Override
	public L mergeIntoPersisted(R root, L entity) {
		return entity;
	}

	@Override
	public void deletePersisted(R root, L entity) {
		getRoot(root).findByUuid(entity.getUuid()).remove();
	}

	/**
	 * Get container vertex for the given root entity.  
	 * 
	 * @param root
	 * @return
	 */
	protected abstract RootVertex<D> getRoot(R root);
}
