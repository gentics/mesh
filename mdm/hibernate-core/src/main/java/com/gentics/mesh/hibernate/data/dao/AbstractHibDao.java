package com.gentics.mesh.hibernate.data.dao;

import org.hibernate.query.criteria.HibernateCriteriaBuilder;

import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.dao.Dao;
import com.gentics.mesh.data.dao.util.CommonDaoHelper;
import com.gentics.mesh.database.CurrentTransaction;
import com.gentics.mesh.hibernate.data.HibQueryFieldMapper;
import com.gentics.mesh.hibernate.data.permission.HibPermissionRoots;
import com.gentics.mesh.hibernate.event.EventFactory;

import dagger.Lazy;
import io.vertx.core.Vertx;
import jakarta.persistence.EntityManager;

/**
 * Common implementation of entity DAO.
 * 
 * @author plyhun
 *
 * @param <T>
 */
public abstract class AbstractHibDao<T extends HibBaseElement> implements Dao<T>, HibQueryFieldMapper {

	protected final Lazy<Vertx> vertx;
	protected final HibPermissionRoots permissionRoots;
	protected final CommonDaoHelper commonDaoHelper;
	protected final CurrentTransaction currentTransaction;
	protected final EventFactory eventFactory;
	
	public AbstractHibDao(HibPermissionRoots permissionRoots, CommonDaoHelper commonDaoHelper, CurrentTransaction currentTransaction, 
			EventFactory eventFactory, Lazy<Vertx> vertx) {
		this.permissionRoots = permissionRoots;
		this.commonDaoHelper = commonDaoHelper;
		this.currentTransaction = currentTransaction;
		this.eventFactory = eventFactory;
		this.vertx = vertx;
	}

	/**
	 * Get Hibernate JPA entity manager.
	 * 
	 * @return
	 */
	public EntityManager em() {
		return currentTransaction.getEntityManager();
	}

	/**
	 * Get Hibernate criteria query builder.
	 * 
	 * @return
	 */
	public HibernateCriteriaBuilder cb() {
		return (HibernateCriteriaBuilder) em().getCriteriaBuilder();
	}

	@Override
	public String mapGraphQlFilterFieldName(String gqlName) {
		switch (gqlName) {
		case "size": return "fileSize";
		case "width": return "imageWidth";
		case "height": return "imageHeight";
		}
		return HibQueryFieldMapper.super.mapGraphQlFilterFieldName(gqlName);
	}
}
