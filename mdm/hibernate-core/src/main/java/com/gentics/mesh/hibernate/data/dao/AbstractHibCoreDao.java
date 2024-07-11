package com.gentics.mesh.hibernate.data.dao;

import com.gentics.mesh.core.data.CoreElement;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.event.MeshElementEventModel;
import com.gentics.mesh.data.dao.util.CommonDaoHelper;
import com.gentics.mesh.database.CurrentTransaction;
import com.gentics.mesh.hibernate.data.permission.HibPermissionRoots;
import com.gentics.mesh.hibernate.event.EventFactory;

import dagger.Lazy;
import io.vertx.core.Vertx;

/**
 * Common implementation of database managed entity DAO.
 * 
 * @author plyhun
 *
 * @param <T>
 * @param <R>
 * @param <D>
 */
public abstract class AbstractHibCoreDao<T extends CoreElement<R>, R extends RestModel, D extends T> extends AbstractHibDao<T> {

	protected final DaoHelper<T,D> daoHelper;

	public AbstractHibCoreDao(DaoHelper<T,D> daoHelper,
			HibPermissionRoots permissionRoots, CommonDaoHelper commonDaoHelper,
			CurrentTransaction currentTransaction, EventFactory eventFactory,
			Lazy<Vertx> vertx) {
		super(permissionRoots, commonDaoHelper, currentTransaction, eventFactory, vertx);
		this.daoHelper = daoHelper;
	}

	/**
	 * Method which is being invoked once the element has been created.
	 * 
	 * @param element
	 * @return Created event
	 */
	public MeshElementEventModel onCreated(T element) {
		return eventFactory.onCreated(element);
	}

	/**
	 * Method which is being invoked once the element has been updated.
	 * 
	 * @param element
	 * @return Created event
	 */
	public MeshElementEventModel onUpdated(T element) {
		return eventFactory.onUpdated(element);
	}

	/**
	 * Method which is being invoked once the element has been deleted.
	 * 
	 * @param element
	 * @return Created event
	 */
	public MeshElementEventModel onDeleted(T element) {
		return eventFactory.onDeleted(element);
	}

	/**
	 * Gets called after the element is actually created.
	 * 
	 * @param element
	 * @return argument, itself or modified
	 */
	public T afterCreatedInDatabase(T element) {
		return element;
	}

	/**
	 * Gets called before the element is physically removed.
	 * 
	 * @param element
	 * @return argument, itself or modified
	 */
	public T beforeDeletedFromDatabase(T element) {
		return element;
	}

	/**
	 * Gets called after the element is physically removed.
	 * 
	 * @param element
	 * @return argument, itself or modified
	 */
	public void afterDeletedFromDatabase(T element) {
	}

	/**
	 * Gets called before the element changes are physically stored.
	 * 
	 * @param element
	 * @return argument, itself or modified
	 */
	public T beforeChangedInDatabase(T element) {
		return element;
	}

	/**
	 * Gets called after the element changes are physically stored.
	 * 
	 * @param element
	 * @return argument, itself or modified
	 */
	public T afterChangedInDatabase(T element) {
		return element;
	}

	@Override
	public String[] getHibernateEntityName(Object... unused) {
		return new String[] {currentTransaction.getTx().data().getDatabaseConnector().maybeGetDatabaseEntityName(getPersistenceClass()).get()};
	}

	public Class<D> getPersistenceClass() {
		return daoHelper.getDomainClass();
	}

	@Override
	public String mapGraphQlFilterFieldName(String gqlName) {
		switch (gqlName) {
		case "creator": return "creator_dbUuid";
		case "editor": return "editor_dbUuid";
		}
		return super.mapGraphQlFilterFieldName(gqlName);
	}

	@Override
	public String mapGraphQlSortingFieldName(String gqlName) {
		switch (gqlName) {
		case "creator": return "USER.creator_dbUuid";
		case "editor": return "USER.editor_dbUuid";
		case "USER.editor_dbUuid": return mapGraphQlSortingFieldName("editor");
		case "USER.creator_dbUuid": return mapGraphQlSortingFieldName("creator");
		}
		return super.mapGraphQlSortingFieldName(gqlName);
	}
}
