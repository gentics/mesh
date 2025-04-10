package com.gentics.mesh.database;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.hibernate.Session;
import org.hibernate.proxy.HibernateProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.gentics.mesh.cache.CacheCollection;
import com.gentics.mesh.cache.PermissionCache;
import com.gentics.mesh.contentoperation.ContentStorage;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.action.BranchDAOActions;
import com.gentics.mesh.core.action.GroupDAOActions;
import com.gentics.mesh.core.action.MicroschemaDAOActions;
import com.gentics.mesh.core.action.ProjectDAOActions;
import com.gentics.mesh.core.action.RoleDAOActions;
import com.gentics.mesh.core.action.SchemaDAOActions;
import com.gentics.mesh.core.action.TagDAOActions;
import com.gentics.mesh.core.action.TagFamilyDAOActions;
import com.gentics.mesh.core.action.UserDAOActions;
import com.gentics.mesh.core.context.ContextDataRegistry;
import com.gentics.mesh.core.data.HibElement;
import com.gentics.mesh.core.data.binary.Binaries;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.s3binary.S3Binaries;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.dagger.tx.TransactionScope;
import com.gentics.mesh.hibernate.data.dao.BinaryDaoImpl;
import com.gentics.mesh.hibernate.data.dao.BranchDaoImpl;
import com.gentics.mesh.hibernate.data.dao.ContentDaoImpl;
import com.gentics.mesh.hibernate.data.dao.GroupDaoImpl;
import com.gentics.mesh.hibernate.data.dao.HibDaoCollectionImpl;
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
import com.gentics.mesh.hibernate.data.domain.HibDatabaseElement;
import com.gentics.mesh.hibernate.util.HibernateUtil;
import com.gentics.mesh.hibernate.util.JpaUtil;
import com.gentics.mesh.hibernate.util.UuidGenerator;
import com.gentics.mesh.security.SecurityUtils;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.criteria.CriteriaQuery;

/**
 * Hibernate transaction implementation. 
 * 
 * @author plyhun
 *
 */
@TransactionScope
public class HibernateTxImpl implements HibernateTx {

	private static final Logger log = LoggerFactory.getLogger(HibernateTxImpl.class);

	private final EntityManager em;
	private final EntityTransaction tx;
	private final ContextDataRegistry contextDataRegistry;
	private final UuidGenerator uuidGenerator;
	private final HibTxData txData;
	private boolean isSuccess = false;
	private final HibernateTx parentTx;
	private final HibDaoCollectionImpl daoCollection;
	private final CacheCollection caches;
	private final SecurityUtils security;
	private final Binaries binaries;
	private final S3Binaries s3Binaries;
	private final ContentStorage contentStorage;

	private final List<HibernateTxAction0> deferredActions = new ArrayList<>();

	@Inject
	public HibernateTxImpl(HibernateDatabase db, HibTxData txData, HibDaoCollectionImpl daoCollection,
			Binaries binaries, CacheCollection caches, SecurityUtils security, UuidGenerator uuidGenerator,
			ContextDataRegistry contextDataRegistry, S3Binaries s3Binaries, ContentStorage contentStorage) {
		this.parentTx = HibernateTx.get();
		this.txData = txData;
		this.contextDataRegistry = contextDataRegistry;
		this.uuidGenerator = uuidGenerator;
		this.daoCollection = daoCollection;
		this.caches = caches;
		this.security = security;
		this.binaries = binaries;
		this.s3Binaries = s3Binaries;
		this.contentStorage = contentStorage;

		if (isNested()) {
			this.em = parentTx.entityManager();
			this.tx = parentTx.entityTransaction();
		} else {
			this.em = db.getEntityManagerFactory().createEntityManager();
			this.tx = em.getTransaction();
			tx.begin();
		}
		if (log.isDebugEnabled()) {
			log.debug((this.parentTx == null ? "Root" : this.parentTx.txId()) + " Tx " + txId() + " started on " + Thread.currentThread().getName() + " at " + System.currentTimeMillis());
		}
		Tx.setActive(this);
	}

	@Override
	public void commit() {
		if (!isNested()) {
			executeDeferred();
			tx.commit();
			tx.begin();
		}
	}

	@Override
	public void rollback() {
		if (isNested()) {
			failure();
		} else {
			tx.rollback();
			tx.begin();
		}
	}

	@Override
	public void success() {
		isSuccess = true;
	}

	@Override
	public void failure() {
		isSuccess = false;
	}

	@Override
	public void close() {
		if (log.isDebugEnabled()) {
			log.debug((this.parentTx == null ? "Root" : this.parentTx.txId()) + " Tx " + txId() + " closing on " + Thread.currentThread().getName() + " at " + System.currentTimeMillis());
		}
		if (!isNested()) {
			try {
				if (isSuccess) {
					executeDeferred();
					tx.commit();
				} else {
					tx.rollback();
				}
			} catch (Throwable t) {
				try {
					// if commit failed, we try to roll back
					if (isSuccess) {
						tx.rollback();
					}
				} catch (Throwable ignored) {
					log.debug("Rolling back transaction after error in commit failed", ignored);
				}
				if (t instanceof RuntimeException) {
					throw (RuntimeException)t;
				} else {
					throw new RuntimeException(t);
				}
			} finally {
				Tx.setActive(parentTx);
				deferredActions.clear();
				if (em.isOpen()) {
					em.close();
				}
			}
		} else {
			deferredActions.stream().forEach(txa -> parentTx.defer(txa));
			Tx.setActive(parentTx);
		}
	}

	@Override
	public HibTxData data() {
		return txData;
	}

	@Override
	public HibBranch getBranch(InternalActionContext ac, HibProject project) {
		// if no project was given, we load the project from the context. This will make sure, that the
		// project will be associated with the session
		if (project == null) {
			project = getProject(ac);
		}
		HibBranch branch = contextDataRegistry.getBranch(ac, project);
		if (branch == null) {
			return null;
		}
		// the entity might no longer be associated with a session, therefore we need to fetch it again
		return branchDao().findByUuid(branch.getProject(), branch.getUuid());
	}

	@Override
	public HibProject getProject(InternalActionContext ac) {
		HibProject project = contextDataRegistry.getProject(ac);
		if (project == null) {
			return null;
		}
		// the entity is no longer associated with a session, therefore we need to fetch it again
		return projectDao().findByUuid(project.getUuid());
	}

	@Override
	public Binaries binaries() {
		return binaries;
	}

	@Override
	public S3Binaries s3binaries() {
		return s3Binaries;
	}

	@Override
	public int txId() {
		return hashCode();
	}

	public EntityManager getEntityManager() {
		return em;
	}

	@Override
	public UserDaoImpl userDao() {
		return daoCollection.userDao();
	}

	@Override
	public UserDAOActions userActions() {
		return daoCollection.userActions();
	}

	@Override
	public GroupDaoImpl groupDao() {
		return daoCollection.groupDao();
	}

	@Override
	public GroupDAOActions groupActions() {
		return daoCollection.groupActions();
	}

	@Override
	public RoleDaoImpl roleDao() {
		return daoCollection.roleDao();
	}

	@Override
	public RoleDAOActions roleActions() {
		return daoCollection.roleActions();
	}

	@Override
	public ProjectDaoImpl projectDao() {
		return daoCollection.projectDao();
	}

	@Override
	public ProjectDAOActions projectActions() {
		return daoCollection.projectActions();
	}

	@Override
	public LanguageDaoImpl languageDao() {
		return daoCollection.languageDao();
	}

	@Override
	public JobDaoImpl jobDao() {
		return daoCollection.jobDao();
	}

	@Override
	public TagFamilyDaoImpl tagFamilyDao() {
		return daoCollection.tagFamilyDao();
	}

	@Override
	public TagFamilyDAOActions tagFamilyActions() {
		return daoCollection.tagFamilyActions();
	}

	@Override
	public TagDaoImpl tagDao() {
		return daoCollection.tagDao();
	}

	@Override
	public TagDAOActions tagActions() {
		return daoCollection.tagActions();
	}

	@Override
	public BranchDaoImpl branchDao() {
		return daoCollection.branchDao();
	}

	@Override
	public BranchDAOActions branchActions() {
		return daoCollection.branchActions();
	}

	@Override
	public MicroschemaDaoImpl microschemaDao() {
		return daoCollection.microschemaDao();
	}

	@Override
	public MicroschemaDAOActions microschemaActions() {
		return daoCollection.microschemaActions();
	}

	@Override
	public SchemaDaoImpl schemaDao() {
		return daoCollection.schemaDao();
	}

	@Override
	public SchemaDAOActions schemaActions() {
		return daoCollection.schemaActions();
	}

	@Override
	public BinaryDaoImpl binaryDao() {
		return daoCollection.binaryDao();
	}

	@Override
	public S3BinaryDaoImpl s3binaryDao() {
		return daoCollection.s3binaryDao();
	}

	@Override
	public NodeDaoImpl nodeDao() {
		return daoCollection.nodeDao();
	}

	@Override
	public ContentDaoImpl contentDao() {
		return daoCollection.contentDao();
	}

	@Override
	public ImageVariantDaoImpl imageVariantDao() {
		return daoCollection.imageVariantDao();
	}

	@Override
	public PermissionCache permissionCache() {
		return caches.permissionCache();
	}

	@Override
	public PasswordEncoder passwordEncoder() {
		return security.passwordEncoder();
	}

	@Override 
	public UuidGenerator uuidGenerator() {
		return uuidGenerator;
	}

	@Override
	public <T extends HibElement> T create(String uuid, Class<? extends T> classOfT, Consumer<T> inflater) {
		HibTxData.assertUnused(uuid, data().permissionRoots());
		try {
			T instance = classOfT.getConstructor().newInstance();
			// We have to cast here because it is not possible for a generic type argument
			// to both extend another type argument and interfaces.
			// See
			// https://stackoverflow.com/questions/13101991/java-generics-make-generic-to-extends-2-interfaces
			HibDatabaseElement dbElement = ((HibDatabaseElement) instance);
			dbElement.setDbUuid(uuidGenerator.toJavaUuidOrGenerate(uuid));
			inflater.accept(instance);
			em.persist(instance);

			return instance;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public <T extends HibElement> T persist(T element) {
		if (em.contains(element)) {
			element = em.merge(element);
		} else {
			em.persist(element);
		}
		return element;
	}

	@Override
	public <T extends HibElement> void delete(T element) {
		if (em.contains(element)) {
			em.remove(element);
		} else {
			forceDelete(element, "dbUuid", e -> e.getId(), false);
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T extends HibElement, B extends T> Stream<T> loadAll(Class<B> classOfT) {
		return (Stream<T>) HibernateUtil.loadAll(em, classOfT);
	}

	@Override
	public EntityManager entityManager() {
		return em;
	}

	@Override
	public EntityTransaction entityTransaction() {
		return tx;
	}

	protected boolean isNested() {
		return parentTx != null;
	}

	@Override
	public <T extends HibElement> long count(Class<? extends T> classOfT) {
		CriteriaQuery<? extends T> query = em.getCriteriaBuilder().createQuery(classOfT);
		query.from(classOfT);
		return JpaUtil.count(em, query);
	}

	@Override
	public <T extends HibElement> T load(Object id, Class<? extends T> classOfT) {
		return em.find(classOfT, id);
	}

	@Override
	public void defer(HibernateTxAction0 action) {
		deferredActions.add(action);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> void forceDelete(T element, String uuidFieldName, Function<T, Object> idGetter, boolean throwIfFailed) {
		if (element instanceof HibernateProxy) {
			element = (T) ((HibernateProxy) element).getHibernateLazyInitializer().getImplementation();
		}
		Entity entity = element.getClass().getAnnotation(Entity.class);
		String entityName = entity != null ? entity.name() : element.getClass().getSimpleName();
		int deleted = em.createQuery("delete from " + entityName + " e where e." + uuidFieldName + " = :uuid").setParameter("uuid", idGetter.apply(element)).executeUpdate();
		if (1 != deleted) {
			if (throwIfFailed) {
				throw new IllegalStateException("Cannot delete " + entityName + " / " + idGetter.apply(element));
			} else {
				log.debug("Force deletion {}#{} result: {}", entityName, idGetter.apply(element), deleted);
			}
		}	
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T attach(T element, boolean pushIntoDb) {
		if (element == null || !(element instanceof HibDatabaseElement)) {
			return element;
		}
		if (pushIntoDb) {
			return em.merge(element);
		} else {
			Session session = em.unwrap(Session.class);
			if (!session.contains(element)) {
				HibDatabaseElement de;
				if (element instanceof HibernateProxy) {
					de = (HibDatabaseElement) ((HibernateProxy) element).getHibernateLazyInitializer().getImplementation();
				} else {
					de = (HibDatabaseElement) element;
				}
				element = (T) load(de.getId(), de.getClass());
			}
			return element;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> Class<? super T> entityClassOf(T element) {
		if (element instanceof HibernateProxy) {
			return (Class<? super T>) ((HibernateProxy) element).getHibernateLazyInitializer().getPersistentClass();
		} else {
			return (Class<? super T>) element.getClass();
		}
	}

	@Override
	public <T> T detach(T element) {
		if (element != null && element instanceof HibDatabaseElement) {
			em.detach(element);
		}
		return element;
	}

	/**
	 * Access the content storage.
	 * 
	 * @return
	 */
	public ContentStorage getContentStorage() {
		return contentStorage;
	}

	private void executeDeferred() {
		while (deferredActions.size() > 0) {
			deferredActions.removeIf(action -> {
				try {
					action.handle(this);
					return true;
				} catch (Throwable t) {
					failure();
					if (t instanceof RuntimeException) {
						throw (RuntimeException)t;
					} else {
						throw new RuntimeException(t);
					}
				}
			});
		}
	}
}
