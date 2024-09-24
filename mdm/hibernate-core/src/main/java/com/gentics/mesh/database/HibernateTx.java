package com.gentics.mesh.database;

import java.util.function.Function;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import org.hibernate.internal.SessionImpl;

import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.hibernate.ContentInterceptor;
import com.gentics.mesh.hibernate.data.dao.BinaryDaoImpl;
import com.gentics.mesh.hibernate.data.dao.BranchDaoImpl;
import com.gentics.mesh.hibernate.data.dao.ContentDaoImpl;
import com.gentics.mesh.hibernate.data.dao.GroupDaoImpl;
import com.gentics.mesh.hibernate.data.dao.ImageVariantDaoImpl;
import com.gentics.mesh.hibernate.data.dao.JobDaoImpl;
import com.gentics.mesh.hibernate.data.dao.LanguageDaoImpl;
import com.gentics.mesh.hibernate.data.dao.MicroschemaDaoImpl;
import com.gentics.mesh.hibernate.data.dao.NodeDaoImpl;
import com.gentics.mesh.hibernate.data.dao.ProjectDaoImpl;
import com.gentics.mesh.hibernate.data.dao.RoleDaoImpl;
import com.gentics.mesh.hibernate.data.dao.S3BinaryDaoImpl;
import com.gentics.mesh.hibernate.data.dao.SchemaDaoImpl;
import com.gentics.mesh.hibernate.data.dao.TagDaoImpl;
import com.gentics.mesh.hibernate.data.dao.TagFamilyDaoImpl;
import com.gentics.mesh.hibernate.data.dao.UserDaoImpl;
import com.gentics.mesh.hibernate.util.UuidGenerator;

/**
 * Hibernate-based transaction.
 * 
 * @author plyhun
 *
 */
public interface HibernateTx extends CommonTx {
	
	/**
	 * Return the transaction with Hibernate context.
	 * 
	 * @return Currently active transaction
	 */
	static HibernateTx get() {
		return (HibernateTx) Tx.get();
	}

	/**
	 * Get Hibernate Entity manager.
	 * 
	 * @return
	 */
	public EntityManager entityManager();

	/**
	 * Get the inner transaction of an entity manager.
	 * 
	 * @return
	 */
	public EntityTransaction entityTransaction();

	/**
	 * Get the database UUID generator.
	 * 
	 * @return
	 */
	public UuidGenerator uuidGenerator();

	/**
	 * Instantly delete the element.
	 * 
	 * @param <T> element type
	 * @param element element to delete
	 * @param uuidFieldName UUID field name of an element
	 * @param idGetter UUID getter
	 */
	<T> void forceDelete(T element, String uuidFieldName, Function<T, Object> idGetter);

	/**
	 * Get the session form of an entity manager.
	 * 
	 * @return
	 */
	default SessionImpl getSessionImpl() {
		EntityManager em = entityManager();
		return em.unwrap(SessionImpl.class);		
	}

	/**
	 * Get the content interceptor
	 * @return content interceptor instance
	 */
	default ContentInterceptor getContentInterceptor() {
		return ContentInterceptor.class.cast(getSessionImpl().getInterceptor());
	}

	@Override
	ImageVariantDaoImpl imageVariantDao();
	
	@Override
	NodeDaoImpl nodeDao();

	@Override
	UserDaoImpl userDao();

	@Override
	GroupDaoImpl groupDao();

	@Override
	RoleDaoImpl roleDao();

	@Override
	ProjectDaoImpl projectDao();

	@Override
	LanguageDaoImpl languageDao();

	@Override
	JobDaoImpl jobDao();

	@Override
	TagFamilyDaoImpl tagFamilyDao();

	@Override
	TagDaoImpl tagDao();

	@Override
	BranchDaoImpl branchDao();

	@Override
	MicroschemaDaoImpl microschemaDao();

	@Override
	SchemaDaoImpl schemaDao();

	@Override
	ContentDaoImpl contentDao();

	@Override
	BinaryDaoImpl binaryDao();

	@Override
	S3BinaryDaoImpl s3binaryDao();

	/**
	 * Add the given action to be executed before the transaction is closed.
	 * @param action action
	 */
	void defer(HibernateTxAction0 action);

	/**
	 * Refresh the persisted entity, if possible
	 * 
	 * @param entity
	 */
	default void refresh(Object entity) {
		EntityManager em = entityManager();
		if (em.contains(entity)) {
			em.refresh(entity);
		}
	}

	@Override
	HibTxData data();
}
