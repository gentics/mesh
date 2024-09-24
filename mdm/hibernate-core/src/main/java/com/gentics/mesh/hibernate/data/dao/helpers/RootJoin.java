package com.gentics.mesh.hibernate.data.dao.helpers;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.EntityType;

import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.query.NativeJoin;

/**
 * Entity-to-root connection definition.
 */
public interface RootJoin<CHILDIMPL extends HibBaseElement, ROOTIMPL extends HibBaseElement> {

	/**
	 * Make native SQL to the root entity,
	 * 
	 * @param myAlias entity alias
	 * @param root root entity
	 * @return
	 */
	NativeJoin makeJoin(String myAlias, ROOTIMPL root);

	/**
	 * Get JPA join definition.
	 * 
	 * @return
	 */
	Join<CHILDIMPL, ROOTIMPL> getJoin(Root<CHILDIMPL> dRoot, EntityType<CHILDIMPL> rootMetamodel);

	/**
	 * Get domain entity class.
	 * 
	 * @return
	 */
	Class<CHILDIMPL> getDomainClass();

	/**
	 * Get root entity class.
	 * 
	 * @return
	 */
	Class<ROOTIMPL> getRootClass();
}
