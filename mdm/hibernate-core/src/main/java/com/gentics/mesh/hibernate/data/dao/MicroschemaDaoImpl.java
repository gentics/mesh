package com.gentics.mesh.hibernate.data.dao;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.contentoperation.ContentStorage;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.dao.PersistingMicroschemaDao;
import com.gentics.mesh.core.data.job.Job;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.project.Project;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.data.user.User;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.data.dao.util.CommonDaoHelper;
import com.gentics.mesh.database.CurrentTransaction;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.hibernate.data.domain.HibBranchImpl;
import com.gentics.mesh.hibernate.data.domain.HibMicroschemaImpl;
import com.gentics.mesh.hibernate.data.domain.HibMicroschemaVersionImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeFieldContainerEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeFieldContainerImpl;
import com.gentics.mesh.hibernate.data.domain.HibProjectImpl;
import com.gentics.mesh.hibernate.data.permission.HibPermissionRoots;
import com.gentics.mesh.hibernate.event.EventFactory;
import com.gentics.mesh.hibernate.util.HibernateUtil;
import com.gentics.mesh.hibernate.util.SplittingUtils;
import com.gentics.mesh.util.UUIDUtil;
import dagger.Lazy;
import io.vertx.core.Vertx;

/**
 * Microschema DAO implementation for Enterprise Mesh.
 * 
 * @author plyhun
 *
 */
@Singleton
public class MicroschemaDaoImpl 
		extends AbstractHibContainerDao<MicroschemaResponse, MicroschemaVersionModel, 
		MicroschemaReference, Microschema, 
		MicroschemaVersion, MicroschemaModel, HibMicroschemaImpl, HibMicroschemaVersionImpl> 
		implements PersistingMicroschemaDao {

	private final ContentStorage contentStorage;

	@Inject
	public MicroschemaDaoImpl(RootDaoHelper<Microschema, HibMicroschemaImpl, Project, HibProjectImpl> rootDaoHelper,
							  DaoHelper<MicroschemaVersion, HibMicroschemaVersionImpl> daoHelperVersion,
							  HibPermissionRoots permissionRoots, CommonDaoHelper commonDaoHelper,
							  CurrentTransaction currentTransaction, EventFactory eventFactory,
							  Lazy<Vertx> vertx, ContentStorage contentStorage) {
		super(rootDaoHelper, permissionRoots, commonDaoHelper, currentTransaction, eventFactory, daoHelperVersion, vertx);
		this.contentStorage = contentStorage;
	}

	@Override
	public Microschema create(MicroschemaVersionModel microschema, User creator, String uuid, EventQueueBatch batch) {
		Microschema container = PersistingMicroschemaDao.super.create(microschema, creator, uuid, batch);
		em().persist(container.getLatestVersion());
		em().persist(container);
		return container;
	}

	@Override
	public Result<? extends Microschema> findAll(Project root) {
		return new TraversalResult<>(((HibProjectImpl) root).getMicroschemas());
	}

	@Override
	public void addItem(Project root, Microschema item) {
		((HibMicroschemaImpl) item).getProjects().add(root);
		((HibProjectImpl) root).getHibMicroschemas().add(item);
		em().merge(root);
	}

	@Override
	public void removeItem(Project root, Microschema item) {
		((HibProjectImpl) root).getHibMicroschemas().remove(item);
		((HibMicroschemaImpl) item).getProjects().remove(root);
		em().merge(root);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Result<? extends NodeFieldContainer> findDraftFieldContainers(MicroschemaVersion version,
			String branchUuid) {
		// Step 1: Fetch all container uuids which have a reference to a micronode with the provided version
		List<UUID> havingMicronodeField = em().createNamedQuery("micronodefieldref.findByVersion")
				.setParameter("version", version)
				.getResultList();
		List<UUID> havingMicronodeListField = em().createNamedQuery("micronodelistitem.findByVersion")
				.setParameter("version", version)
				.getResultList();

		Set<UUID> havingAnyMicroField = new HashSet<>(havingMicronodeField);
		havingAnyMicroField.addAll(havingMicronodeListField);

		if (havingAnyMicroField.isEmpty()) {
			return TraversalResult.empty();
		}

		// Step 2: Get the draft edges for those container uuids and the provided branch
		List<HibNodeFieldContainerEdgeImpl> edges = SplittingUtils.splitAndMergeInList(havingAnyMicroField, HibernateUtil.inQueriesLimitForSplitting(3), 
				slice -> em().createQuery("select edge from nodefieldcontainer edge where edge.contentUuid in :containerUuids and edge.type = :type and edge.branch.dbUuid = :branchUuid", HibNodeFieldContainerEdgeImpl.class)
					.setParameter("containerUuids", slice)
					.setParameter("type", ContainerType.DRAFT)
					.setParameter("branchUuid", UUIDUtil.toJavaUuid(branchUuid))
					.getResultList());

		// Step 3: Get the containers
		List<HibNodeFieldContainerImpl> containers = contentStorage.findMany(edges);

		return new TraversalResult<>(containers);
	}

	@Override
	public Result<Project> findLinkedProjects(Microschema schema) {
		return new TraversalResult<>(((HibMicroschemaImpl) schema).getLinkedProjects());
	}

	@Override
	public Class<? extends MicroschemaVersion> getVersionPersistenceClass() {
		return HibMicroschemaVersionImpl.class;
	}

	@Override
	protected String getVersionFieldLabel() {
		return "microschema";
	}

	@Override
	protected String getVersionTableLabel() {
		return HibMicroschemaVersionImpl.TABLE_NAME;
	}

	@Override
	public void deleteVersion(MicroschemaVersion version, BulkActionContext bac) {
		HibernateTx tx = HibernateTx.get();

		// Delete referenced jobs
		for (Job job : version.referencedJobsViaFrom()) {
			tx.jobDao().delete(job, bac);
		}
		for (Job job : version.referencedJobsViaTo()) {
			tx.jobDao().delete(job, bac);
		}

		// Drop references from the branches
		tx.projectDao().findAll().stream()
				.flatMap(project -> tx.branchDao().findAll(project).stream())
				.map(HibBranchImpl.class::cast)
				.forEach(branch -> branch.removeMicroschemaVersion(version));

		// Rearrange versioning
		HibMicroschemaImpl microschema = (HibMicroschemaImpl) version.getSchemaContainer();
		microschema.getVersions().removeIf(v -> v.getUuid().equals(version.getUuid()));
		if (microschema.getLatestVersion().getUuid().equals(version.getUuid())) {
			microschema.setLatestVersion(version.getPreviousVersion());
		}
		if (version.getPreviousVersion() != null) {
			version.getPreviousVersion().setNextVersion(null);
		}
		if (version.getNextVersion() != null) {
			version.getNextVersion().setPreviousVersion(version.getPreviousVersion());
		}

		// Drop the whole content table
		tx.contentDao().deleteContentTable(version);

		// Make the events, drop the version itself
		super.deleteVersion(version, bac);
	}

	@Override
	public Microschema beforeDeletedFromDatabase(Microschema element) {
		HibMicroschemaImpl microschema = (HibMicroschemaImpl) element;
		microschema.getVersions().forEach(v -> v.setSchemaContainer(null));
		microschema.getVersions().clear();
		microschema.setLatestVersion(null);
		microschema.getProjects().forEach(p -> ((HibProjectImpl) p).getHibMicroschemas().remove(microschema));
		microschema.getProjects().clear();
		return microschema;
	}

	@Override
	public MicroschemaVersion createPersistedVersion(Microschema container, Consumer<MicroschemaVersion> inflater) {
		MicroschemaVersion version = super.createPersistedVersion(container, inflater);
		HibernateTx.get().contentDao().createContentTable(version);
		return version;
	}

	@Override
	public Result<? extends Micronode> findMicronodes(MicroschemaVersion version) {
		return new TraversalResult<>(HibernateTx.get().contentDao().getFieldsContainers(version));
	}

	@Override
	public void beforeVersionDeletedFromDatabase(MicroschemaVersion version) {
		em().createQuery("delete from branch_microschema_version_edge bmve where bmve.version = :version")
				.setParameter("version", version)
				.executeUpdate();
		super.beforeVersionDeletedFromDatabase(version);
	}

	/**
	 * Given a version, returns all the possible microschemas versions that might be related
	 * @param version
	 * @return
	 */
	public List<MicroschemaVersion> findMicroschemasRelatedToVersion(SchemaVersion version) {
		Set<String> microschemaNames = new HashSet<>();
		for (FieldSchema field : version.getSchema().getFields()) {
			if (field instanceof MicronodeFieldSchema) {
				MicronodeFieldSchema microschema = (MicronodeFieldSchema) field;
				String[] allowedMicroSchemas = microschema.getAllowedMicroSchemas();
				for (String allowedMicroSchema : allowedMicroSchemas) {
					microschemaNames.add(allowedMicroSchema);
				}
			} else if (field instanceof ListFieldSchema) {
				ListFieldSchema listFieldSchema = (ListFieldSchema) field;
				if (FieldTypes.MICRONODE.equals(FieldTypes.valueByName(listFieldSchema.getListType()))) {
					String[] allowedMicroSchemas = listFieldSchema.getAllowedSchemas();
					for (String allowedMicroSchema : allowedMicroSchemas) {
						microschemaNames.add(allowedMicroSchema);
					}
				}
			}
		}

		return microschemaNames.stream()
				.map(this::findByName)
				.flatMap(microschema -> ((HibMicroschemaImpl) microschema).getVersions().stream())
				.collect(Collectors.toList());
	}
}
