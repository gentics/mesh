package com.gentics.mesh.hibernate.data.dao;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.tuple.Pair;

import com.gentics.mesh.contentoperation.ContentStorage;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.PersistingMicroschemaDao;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.user.HibUser;
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
import com.gentics.mesh.database.connector.DatabaseConnector;
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
import com.gentics.mesh.util.VersionUtil;

import dagger.Lazy;
import io.vertx.core.Vertx;

/**
 * Microschema DAO implementation for Gentics Mesh.
 * 
 * @author plyhun
 *
 */
@Singleton
public class MicroschemaDaoImpl 
		extends AbstractHibContainerDao<MicroschemaResponse, MicroschemaVersionModel, 
		MicroschemaReference, HibMicroschema, 
		HibMicroschemaVersion, MicroschemaModel, HibMicroschemaImpl, HibMicroschemaVersionImpl> 
		implements PersistingMicroschemaDao {

	private final ContentStorage contentStorage;
	private final DatabaseConnector databaseConnector;

	@Inject
	public MicroschemaDaoImpl(RootDaoHelper<HibMicroschema, HibMicroschemaImpl, HibProject, HibProjectImpl> rootDaoHelper,
							  DaoHelper<HibMicroschemaVersion, HibMicroschemaVersionImpl> daoHelperVersion,
							  HibPermissionRoots permissionRoots, CommonDaoHelper commonDaoHelper,
							  CurrentTransaction currentTransaction, EventFactory eventFactory,
							  Lazy<Vertx> vertx, ContentStorage contentStorage, DatabaseConnector databaseConnector) {
		super(rootDaoHelper, permissionRoots, commonDaoHelper, currentTransaction, eventFactory, daoHelperVersion, vertx);
		this.contentStorage = contentStorage;
		this.databaseConnector = databaseConnector;
	}

	@Override
	public HibMicroschema create(MicroschemaVersionModel microschema, HibUser creator, String uuid, EventQueueBatch batch) {
		HibMicroschema container = PersistingMicroschemaDao.super.create(microschema, creator, uuid, batch);
		em().persist(container.getLatestVersion());
		em().persist(container);
		return container;
	}

	@Override
	public Result<? extends HibMicroschema> findAll(HibProject root) {
		return new TraversalResult<>(((HibProjectImpl) root).getMicroschemas());
	}

	@Override
	public void addItem(HibProject root, HibMicroschema item) {
		((HibMicroschemaImpl) item).getProjects().add(root);
		((HibProjectImpl) root).getHibMicroschemas().add(item);
		em().merge(root);
	}

	@Override
	public void removeItem(HibProject root, HibMicroschema item) {
		((HibProjectImpl) root).getHibMicroschemas().remove(item);
		((HibMicroschemaImpl) item).getProjects().remove(root);
		em().merge(root);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Result<? extends HibNodeFieldContainer> findDraftFieldContainers(HibMicroschemaVersion version,
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
	public Result<HibProject> findLinkedProjects(HibMicroschema schema) {
		return new TraversalResult<>(((HibMicroschemaImpl) schema).getLinkedProjects());
	}

	@Override
	public Class<? extends HibMicroschemaVersion> getVersionPersistenceClass() {
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
	public void deleteVersion(HibMicroschemaVersion version) {
		HibernateTx tx = HibernateTx.get();

		// Delete referenced jobs
		for (HibJob job : version.referencedJobsViaFrom()) {
			tx.jobDao().delete(job);
		}
		for (HibJob job : version.referencedJobsViaTo()) {
			tx.jobDao().delete(job);
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
			version.getPreviousVersion().setNextVersion(version.getNextVersion());
		}
		if (version.getNextVersion() != null) {
			version.getNextVersion().setPreviousVersion(version.getPreviousVersion());
		}

		// Drop the whole content table
		tx.contentDao().deleteContentTable(version);

		// Make the events, drop the version itself
		super.deleteVersion(version);
	}

	@Override
	public HibMicroschema beforeDeletedFromDatabase(HibMicroschema element) {
		HibMicroschemaImpl microschema = (HibMicroschemaImpl) element;
		microschema.getVersions().forEach(v -> v.setSchemaContainer(null));
		microschema.getVersions().clear();
		microschema.setLatestVersion(null);
		microschema.getProjects().forEach(p -> ((HibProjectImpl) p).getHibMicroschemas().remove(microschema));
		microschema.getProjects().clear();
		return microschema;
	}

	@Override
	public HibMicroschemaVersion createPersistedVersion(HibMicroschema container, Consumer<HibMicroschemaVersion> inflater) {
		HibMicroschemaVersion version = super.createPersistedVersion(container, inflater);
		HibernateTx.get().contentDao().createContentTable(version);
		return version;
	}

	@Override
	public void beforeVersionDeletedFromDatabase(HibMicroschemaVersion version) {
		HibernateTx tx = HibernateTx.get();

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
			version.getPreviousVersion().setNextVersion(version.getNextVersion());
		}
		if (version.getNextVersion() != null) {
			version.getNextVersion().setPreviousVersion(version.getPreviousVersion());
		}

		// Drop the whole content table
		tx.contentDao().deleteContentTable(version);

		em().createQuery("delete from branch_microschema_version_edge bmve where bmve.version = :version")
				.setParameter("version", version)
				.executeUpdate();
	}

	@SuppressWarnings("unchecked")
	@Override
    public void deleteVersions(Set<Pair<String, String>> versionSchemas) {
		SplittingUtils.splitAndConsume(versionSchemas, HibernateUtil.inQueriesLimitForSplitting(0), versions -> {
			Set<UUID> versionUuids = versions.stream().map(pair -> UUIDUtil.toJavaUuid(pair.getKey())).collect(Collectors.toSet());
			// Delete properties of changes
			{
				String cte = """
						 AS (    
					    select change_.dbuuid as change_dbuuid
					    from mesh_microschemaversion version_ 
					    left join mesh_schemachange change_ on change_.dbuuid = version_.nextchange_dbuuid
					    where version_.dbuuid in :versionUuids
					    UNION ALL
					    select change_.dbuuid as change_dbuuid
					    from mesh_schemachange change_
					    inner join allChanges prev_ on change_.previouschange_dbuuid = prev_.change_dbuuid    
					) select change_dbuuid from allChanges
				""";
				List<Object> changes = em().createNativeQuery("WITH " + databaseConnector.getCteFunctionDefinition("allChanges", List.of("change_dbuuid"), true) + cte)
						.setParameter("versionUuids", versionUuids).getResultList();
				log.info("Change properties dropped: {}", em().createNativeQuery("""
					delete from mesh_schemachange_properties WHERE schemachange_dbuuid in :changes
				""").setParameter("changes", changes).executeUpdate());
				// Delete references to the changes
				log.info("Change references dropped: {}", em().createQuery("""
					update microschemaversion set nextChange = NULL where nextChange.dbUuid in :changes
				""").setParameter("changes", changes).executeUpdate());
				// Delete changes
				log.info("Changes cleaned: {}", em().createQuery("""
					update schemachange set nextChange = NULL, previousChange = NULL where dbUuid in :changes
				""").setParameter("changes", changes).executeUpdate());
				log.info("Changes dropped: {}", em().createQuery("""
					delete from schemachange where dbUuid in :changes
				""").setParameter("changes", changes).executeUpdate());
			}
			// Delete jobs
			SplittingUtils.splitAndConsume(versionUuids, versionUuids.size() / 2, split -> {
				log.info("Referencing jobs dropped: {}", em().createQuery("""
						delete from job where toMicroschemaVersion.dbUuid in :versionUuids or fromMicroschemaVersion.dbUuid in :versionUuids
					""").setParameter("versionUuids", split).executeUpdate());
			});
			{
				// Construct target version chains
				List<UUID[]> list = em().createQuery("select pv.dbUuid, v.dbUuid, nv.dbUuid from microschemaversion v left join v.previousVersion pv left join v.nextVersion nv where v.dbUuid in :versionUuids", UUID[].class)
						.setParameter("versionUuids", versionUuids).getResultList();
				List<List<UUID>> chains = buildChains(list);
				long nextFixed = 0;
				long prevFixed = 0;
				long latestFixed = 0;
				for (List<UUID> chain : chains) {
					UUID prevVersion = chain.get(0);
					UUID lastVersion = chain.get(chain.size()-1);
					// Set next version chain
					if (!versionUuids.contains(prevVersion)) {
						// Set previous latest version, if applicable
						latestFixed += em().createNativeQuery("""
								update mesh_microschema set latestversion_dbuuid = :previousVersion where latestversion_dbuuid in :versionUuids
							""").setParameter("previousVersion", prevVersion).setParameter("versionUuids", chain).executeUpdate();
						nextFixed += em().createNativeQuery("""
								update mesh_microschemaversion set nextversion_dbuuid = :nextVersion where dbuuid = :previousVersion
							""").setParameter("previousVersion", prevVersion).setParameter("nextVersion", versionUuids.contains(lastVersion) ? null : lastVersion).executeUpdate();
					}
					// Set previous version chain
					if (!versionUuids.contains(lastVersion)) {
						prevFixed += em().createNativeQuery("""
								update mesh_microschemaversion set previousversion_dbuuid = :previousVersion where dbuuid = :nextVersion
							""").setParameter("previousVersion", versionUuids.contains(prevVersion) ? null : prevVersion).setParameter("nextVersion", lastVersion).executeUpdate();
					}
				}
				log.info("Version chains fixed: previous {}, next {}, latest {}", prevFixed, nextFixed, latestFixed);
			}
			// Drop the branch link
			log.info("Branch version edged dropped: {}", em().createNativeQuery("""
				delete from mesh_branch_microschema_version_edge where version_dbuuid in :versionUuids
			""").setParameter("versionUuids", versionUuids).executeUpdate());
			// Drop the content tables
			HibernateTx.get().defer(tx -> {
				for (UUID versionUuid : versionUuids) {
					String dropContentTable = databaseConnector.getHibernateDialect().getDropTableString(databaseConnector.getPhysicalTableName(versionUuid));
					tx.entityManager().createNativeQuery(dropContentTable).executeUpdate();
				}
				log.info("Content tables dropped: {}", versionUuids.size());
			});
			// Wipe out the versions to purge
			log.info("Droppable previous versions cleaned: {}", em().createQuery("""
				update microschemaversion set nextVersion = NULL, previousVersion = NULL where dbUuid in :versionUuids
			""").setParameter("versionUuids", versionUuids).executeUpdate());
			// Drop the versions
			log.info("Versions dropped: {}", em().createQuery("""
				delete from microschemaversion where dbUuid in :versionUuids
			""").setParameter("versionUuids", versionUuids).executeUpdate());
		});
    }

	/**
	 * Given a version, returns all the possible microschemas versions that might be related
	 * @param version
	 * @return
	 */
	public List<HibMicroschemaVersion> findMicroschemasRelatedToVersion(HibSchemaVersion version) {
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

	@Override
	public HibMicroschemaVersion findLatestVersion(HibBranch branch, HibMicroschema microschema) {
		List<HibMicroschemaVersionImpl> versions = em().createNamedQuery("microschemaversion.findInBranchForMicroschema", HibMicroschemaVersionImpl.class)
				.setParameter("branch", branch)
				.setParameter("microschema", microschema)
				.getResultList();

		return versions.stream().sorted((v1, v2) -> VersionUtil.compareVersions(v2.getVersion(), v1.getVersion()))
				.findFirst().orElse(null);
	}

	@Override
    public long countVersionEdges(HibMicroschemaVersion version) {
		return em().createNamedQuery("micronodefieldref.countByVersion", Long.class).setParameter("version", version).getSingleResult();
    }

	@Override
	public Map<String, Long> countVersionEdges() {
		@SuppressWarnings("unchecked")
		List<Object[]> resultList = em().createNamedQuery("micronodefieldref.countContent").getResultList();

		return resultList.stream().map(row -> {
			String schemaVersionUuid = UUIDUtil.toShortUuid((UUID) row[0]);
			Long count = (Long) row[1];
			return Pair.of(schemaVersionUuid, count);
		}).collect(Collectors.toMap(Pair::getKey, Pair::getValue));
	}
}
