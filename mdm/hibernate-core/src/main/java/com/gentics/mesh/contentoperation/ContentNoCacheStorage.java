package com.gentics.mesh.contentoperation;

import static com.gentics.mesh.contentoperation.CommonContentColumn.BUCKET_ID;
import static com.gentics.mesh.contentoperation.CommonContentColumn.CURRENT_VERSION_NUMBER;
import static com.gentics.mesh.contentoperation.CommonContentColumn.DB_UUID;
import static com.gentics.mesh.contentoperation.CommonContentColumn.DB_VERSION;
import static com.gentics.mesh.contentoperation.CommonContentColumn.EDITED;
import static com.gentics.mesh.contentoperation.CommonContentColumn.EDITOR_DB_UUID;
import static com.gentics.mesh.contentoperation.CommonContentColumn.LANGUAGE_TAG;
import static com.gentics.mesh.contentoperation.CommonContentColumn.NODE;
import static com.gentics.mesh.contentoperation.CommonContentColumn.SCHEMA_DB_UUID;
import static com.gentics.mesh.contentoperation.CommonContentColumn.SCHEMA_VERSION_DB_UUID;
import static com.gentics.mesh.hibernate.util.HibernateUtil.collectVersionColumns;
import static com.gentics.mesh.hibernate.util.HibernateUtil.collectVersionColumnsByAlias;
import static com.gentics.mesh.hibernate.util.HibernateUtil.streamContentSelectClause;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.hibernate.Session;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.sql.Delete;
import org.hibernate.sql.Insert;
import org.hibernate.sql.SimpleSelect;
import org.hibernate.type.spi.TypeConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibFieldSchemaVersionElement;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.common.ReferenceType;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.database.connector.DatabaseConnector;
import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.etc.config.hibernate.HibernateStorageOptions;
import com.gentics.mesh.unhibernate.ANSIJoinFragment;
import com.gentics.mesh.unhibernate.Select;
import com.gentics.mesh.hibernate.data.domain.HibMicronodeContainerImpl;
import com.gentics.mesh.hibernate.data.domain.HibMicroschemaVersionImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeFieldContainerImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeImpl;
import com.gentics.mesh.hibernate.data.domain.HibSchemaVersionImpl;
import com.gentics.mesh.hibernate.data.domain.HibUnmanagedFieldContainer;
import com.gentics.mesh.hibernate.util.HibernateUtil;
import com.gentics.mesh.hibernate.util.SplittingUtils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.JoinType;

/**
 * Implements database storage access logic
 */
@Singleton
public class ContentNoCacheStorage {
	private static final Logger log = LoggerFactory.getLogger(ContentStorageImpl.class);

	public static final String DEFAULT_ALIAS = "content";

	private final HibernateStorageOptions storageOptions;
	private final DatabaseConnector databaseConnector;

	@Inject
	public ContentNoCacheStorage(HibernateMeshOptions options, DatabaseConnector databaseConnector) {
		this.storageOptions = options.getStorageOptions();
		this.databaseConnector = databaseConnector;
	}

	private SimpleSelect buildSelect(HibFieldSchemaVersionElement<?, ?, ?, ?, ?> version, List<ContentColumn> columns, String where) {
		Select select = new Select(databaseConnector.getSessionMetadataIntegrator().getSessionFactoryImplementor());
		streamContentSelectClause(HibernateTx.get().data().getDatabaseConnector(), columns, Optional.empty(), false).forEach(select::addColumn);;
		select.setTableName(from(version));
		ANSIJoinFragment ansiJoinFragment = new ANSIJoinFragment();
		columns.stream().filter(c -> c instanceof JoinedContentColumn)
				.map(c -> Pair.of(((JoinedContentColumn) c).getTableName(), ((JoinedContentColumn) c).getTableAlias()))
				.distinct()
				.forEach(p -> {
					ansiJoinFragment.addJoin(p.getKey(), p.getValue(), new String[]{DEFAULT_ALIAS + "." + databaseConnector.renderColumn(DB_UUID)}, new String[]{"containeruuid"}, JoinType.LEFT);
				});
		if (!ansiJoinFragment.isEmpty()) {
			select.setJoin(ansiJoinFragment.toFromFragmentString());
		}
		if (StringUtils.isNotEmpty(where)) {
			select.addRestriction(where);
		}

		return select;
	}

	private String from(HibFieldSchemaVersionElement<?, ?, ?, ?, ?> version) {
		return databaseConnector.getPhysicalTableName(version) + " " + DEFAULT_ALIAS;
	}

	private String whereDbUuid() {
		return DEFAULT_ALIAS + "." + databaseConnector.renderColumn(DB_UUID) + " = ?";
	}

	/**
	 * Needed to make sure that hibernate will return the actual java representation of the column type
	 * @param columns
	 * @param query
	 */
	private void addScalarForDeserialization(List<ContentColumn> columns, NativeQuery<?> query) {
		columns.forEach(c -> {
			query.addScalar(databaseConnector.renderColumn(c), c.getJavaClass());
		});
	}

	@SuppressWarnings("unchecked")
	<T> T findColumn(HibFieldSchemaVersionElement<?, ?, ?, ?, ?> version, UUID contentUuid, ContentColumn contentColumn) {
		SimpleSelect select = buildSelect(version, Collections.singletonList(contentColumn), whereDbUuid());

		EntityManager em = HibernateTx.get().entityManager();
		NativeQuery<?> query = em.createNativeQuery(select.toStatementString())
				.setParameter(1, contentUuid)
				.unwrap(NativeQuery.class)
				.addScalar(databaseConnector.renderColumn(contentColumn), contentColumn.getJavaClass());

		try {
			return (T) query.uniqueResultOptional().orElse(null);
		} catch (Throwable t) {
			throw processThrowable(t, em, (UUID) version.getId());
		}
	}

	<T> List<T> findColumnValues(HibFieldSchemaVersionElement<?, ?, ?, ?, ?> version, ContentColumn contentColumn) {
		List<ContentColumn> columns = Collections.singletonList(contentColumn);
		SimpleSelect select = buildSelect(version, columns, "");
		select.setOrderBy("order by " + databaseConnector.renderColumn(contentColumn));

		return doList(select.toStatementString(), version, (query) -> {
			addScalarForDeserialization(columns, query);
		});
	}

	/**
	 * Insert the container into the mesh_content_ table
	 * @param container container
	 * @param version schema version
	 */
	void insert(HibUnmanagedFieldContainer<?, ?, ?, ?, ?> container, HibFieldSchemaVersionElement<?, ?, ?, ?, ?> version) {
		Insert insert = new Insert(databaseConnector.getSessionMetadataIntegrator().getSessionFactoryImplementor());
		insert.setTableName(databaseConnector.getPhysicalTableName(version));

		List<Pair<ContentColumn, Object>> insertValues = container.getAll();

		insertValues.forEach(kv -> {
			insert.addColumn(databaseConnector.renderColumn(kv.getKey()));
		});

		long affectedRows = doUpdate(insert.toStatementString(), (UUID) version.getId(), query -> {
			setValues(query, insertValues, 1);
		});

		if (affectedRows != 1) {
			throw new IllegalStateException("JDBC insert did not return expected result");
		}
	}

	void delete(UUID dbUuid, HibFieldSchemaVersionElement<?, ?, ?, ?, ?> version) {
		Delete delete = new Delete(databaseConnector.getSessionMetadataIntegrator().getSessionFactoryImplementor());
		delete.setTableName(databaseConnector.getPhysicalTableName(version));
		delete.addColumnRestriction(databaseConnector.renderColumn(DB_UUID));

		doUpdate(delete.toStatementString(), (UUID) version.getId(), (query) -> query.unwrap(NativeQuery.class).setParameter(1, dbUuid));
	}

	/**
	 * Delete the containers referenced by the provided keys
	 * @param contentKeys
	 */
	public long delete(Collection<ContentKey> contentKeys) {
		Map<UUID, List<ContentKey>> containersByVersionUuid = contentKeys.stream()
				.collect(Collectors.groupingBy(ContentKey::getSchemaVersionUuid));

		long deletedCount = 0;
		for (Map.Entry<UUID, List<ContentKey>> entry : containersByVersionUuid.entrySet()) {
			UUID key = entry.getKey();
			List<UUID> containersUuids = entry.getValue().stream().map(ContentKey::getContentUuid).collect(Collectors.toList());

			String delete = "delete from " + databaseConnector.getPhysicalTableName(key) + " where " + databaseConnector.renderColumn(DB_UUID) + " in :dbuuids";

			deletedCount += SplittingUtils.splitAndCount(containersUuids, HibernateUtil.inQueriesLimitForSplitting(1), 
					slice -> doUpdate(delete, key, (query) -> query.unwrap(NativeQuery.class).setParameterList("dbuuids", slice)));
		}
		log.info("Deleted " + deletedCount + " containers");
		return deletedCount;
	}

	void dropTable(HibFieldSchemaVersionElement<?, ?, ?, ?, ?> version) {
		String dropContentTable = databaseConnector.getHibernateDialect().getDropTableString(databaseConnector.getPhysicalTableName(version));
		HibernateTx.get().defer(tx -> {
			tx.entityManager().createNativeQuery(dropContentTable).executeUpdate();
		});
	}

	long getGlobalCount() {
		HibernateTx tx = HibernateTx.get();
		EntityManager em = tx.entityManager();
		Set<String> contentTables = tx.loadAll(HibSchemaVersionImpl.class).map(databaseConnector::getPhysicalTableName).collect(Collectors.toSet());
		return tx.contentDao().getTotalsCache().get("select count(1) from ", key -> contentTables.stream()
				// HQL has no UNION ALL, so we have to count manually. Fortunately there is no lot of entries.
				.map(table -> em.createNativeQuery(key + table).getSingleResult())
				.map(Number.class::cast)
				.reduce(0, (a, b) -> a.longValue() + b.longValue())
				.longValue());
	}

	void addColumnIfNotExists(HibFieldSchemaVersionElement<?, ?, ?, ?, ?> version, DynamicContentColumn column) {
		EntityManager em = HibernateTx.get().entityManager();
		Session session = (Session) em.getDelegate();
		session.doWork(connection -> {
			DatabaseMetaData metaData = connection.getMetaData();
			Identifier physicalTableNameIdentifier = databaseConnector.getPhysicalTableNameIdentifier((UUID) version.getId());
			String metadataTable = databaseConnector.getSessionMetadataIntegrator().getJdbcEnvironment().getIdentifierHelper().toMetaDataObjectName(physicalTableNameIdentifier);
			Identifier columnId = databaseConnector.identify(column.getLabel());
			String metadataColumn = databaseConnector.getSessionMetadataIntegrator().getJdbcEnvironment().getIdentifierHelper().toMetaDataObjectName(columnId);

			ResultSet rs = metaData.getColumns(null, null, metadataTable, metadataColumn);

			if (!rs.next()) {
				try {
					String alterTableStatementColumn = createAlterTableAddColumnStatement(databaseConnector.getPhysicalTableName(version), column);
					em.createNativeQuery(alterTableStatementColumn).executeUpdate();
				} catch (Throwable e) {
					log.error("Exception when trying to add a column", e);
					if (!databaseConnector.tableExists(connection, (UUID) version.getId())) {
						throw new ContentTableNotFoundException(databaseConnector.getPhysicalTableName(version));
					}
				}
			}
		});
	}

	private String createAlterTableAddColumnStatement(String tableName, DynamicContentColumn column) {
		Dialect dialect = databaseConnector.getHibernateDialect();
		StringBuilder root = (new StringBuilder(dialect.getAlterTableString(tableName))).append(' ').append(dialect.getAddColumnString());
		root.append(databaseConnector.renderColumn(column))
			.append(' ')
			.append(databaseConnector.getSqlTypeName(column.getFieldType(), databaseConnector.getUUIDTypeName()));

		return root.toString();
	}

	/**
	 * Get the SQL statement for creating the content table for the given schema version
	 * @param version schema version
	 * @param uuidTypeName the name of the sql type used for uuids
	 * @return SQL statement
	 */
	public String getCreateTableSql(HibSchemaVersion version, String uuidTypeName) {
		TypeConfiguration typeConfig = databaseConnector.getSessionMetadataIntegrator().getSessionFactoryImplementor().getTypeConfiguration();
		StringBuffer tableString = commonTablePrefix(version, uuidTypeName).append(",\n");
		Dialect dialect = databaseConnector.getHibernateDialect();

		// bucket id
		tableString.append(databaseConnector.renderColumn(BUCKET_ID)).append(' ');
		tableString.append(typeConfig.getDdlTypeRegistry().getTypeName(Types.INTEGER, dialect)).append(",\n");

		// timestamp
		tableString.append(databaseConnector.renderColumn(EDITED)).append(' ');
		tableString.append(typeConfig.getDdlTypeRegistry().getTypeName(Types.TIMESTAMP, dialect)).append(",\n");

		// editor
		tableString.append(databaseConnector.renderColumn(EDITOR_DB_UUID)).append(' ');
		tableString.append(uuidTypeName).append(",\n");

		// node
		tableString.append(databaseConnector.renderColumn(NODE)).append(' ');
		tableString.append(uuidTypeName).append(" not null,\n");

		// version number
		tableString.append(databaseConnector.renderColumn(CURRENT_VERSION_NUMBER)).append(' ');
		tableString.append(typeConfig.getDdlTypeRegistry().getTypeName(Types.VARCHAR, new Size(0,0,128L), typeConfig.getBasicTypeForJavaType(String.class))).append("\n");

		return tableString.append(")").toString();
	}

	void createTable(HibSchemaVersion version) {
		HibernateTx.get().defer(tx -> {
			String uuidTypeName = databaseConnector.getUUIDTypeName();
			tx.entityManager().createNativeQuery(getCreateTableSql(version, uuidTypeName)).executeUpdate();
		});
	}

	void createIndex(HibSchemaVersion version, CommonContentColumn column, boolean unique) {
		HibernateTx.get().defer(tx -> {
			String createIndexSql = getCreateIndexSql(version, column, unique);
			tx.entityManager().createNativeQuery(createIndexSql).executeUpdate();
		});
	}

	/**
	 * Return the sql string for creating an index
	 * @param version the version
	 * @param column the column for which the index will be created
	 * @param unique whether the index is unique
	 * @return
	 */
	public String getCreateIndexSql(HibSchemaVersion version, CommonContentColumn column, boolean unique) {
		String tableName = databaseConnector.getPhysicalTableName(version);
		Dialect dialect = databaseConnector.getHibernateDialect();
		String index_name = "idx_" + tableName + "_" + column.getLabel();

		return new StringBuilder( "create" )
				.append( unique ? " unique" : "" )
				.append( " index " )
				.append( dialect.qualifyIndexName() ? index_name : StringHelper.unqualify( index_name ) )
				.append( " on " )
				.append( tableName )
				.append( " (" )
				.append(databaseConnector.renderColumn(column))
				.append(")")
				.toString();
	}

	/**
	 * Get the SQL statement for creating the content table for the given microschema version
	 * @param microVersion microschema version
	 * @return SQL statement
	 */
	public String getCreateMicronodeTableSql(HibMicroschemaVersion microVersion, String uuidTypeName) {
		return commonTablePrefix(microVersion, uuidTypeName).append(")").toString();
	}

	void createMicronodeTable(HibMicroschemaVersion microVersion) {
		HibernateTx.get().defer(tx -> {
			tx.entityManager().createNativeQuery(getCreateMicronodeTableSql(microVersion, databaseConnector.getUUIDTypeName())).executeUpdate();
		});
	}

	private StringBuffer commonTablePrefix(HibFieldSchemaVersionElement<?,?,?,?,?> version, String uuidTypeName) {
		Dialect dialect = databaseConnector.getHibernateDialect();
		TypeConfiguration typeConfig = databaseConnector.getSessionMetadataIntegrator().getSessionFactoryImplementor().getTypeConfiguration();
		StringBuffer tableString = new StringBuffer();
		tableString.append("create table ");
		tableString.append(databaseConnector.getPhysicalTableName(version));
		tableString.append(" (\n");

		// db uuid
		tableString.append(databaseConnector.renderColumn(DB_UUID)).append(' ');
		tableString.append(uuidTypeName).append(' ');
		tableString.append("primary key").append(" not null,\n");

		// db version
		tableString.append(databaseConnector.renderColumn(DB_VERSION)).append(' ');
		tableString.append(typeConfig.getDdlTypeRegistry().getTypeName(Types.BIGINT, dialect)).append(" not null,\n");

		// schema version
		tableString.append(databaseConnector.renderColumn(SCHEMA_VERSION_DB_UUID)).append(' ');
		tableString.append(uuidTypeName).append(" not null,\n");

		// schema
		tableString.append(databaseConnector.renderColumn(SCHEMA_DB_UUID)).append(' ');
		tableString.append(uuidTypeName).append(" not null,\n");

		// language
		tableString.append(databaseConnector.renderColumn(LANGUAGE_TAG)).append(' ');
		tableString.append(typeConfig.getDdlTypeRegistry().getTypeName(Types.VARCHAR, new Size(0, 0, 10L), typeConfig.getBasicTypeForJavaType(String.class)));
		List<FieldSchema> fields = version.getSchema().getFields();

		if (fields.size() > 0) {
			tableString.append(",\n");
		}

		for (int i = 0; i < fields.size(); i++) {
			FieldSchema f = fields.get(i);
			FieldTypes fieldType = FieldTypes.valueByName(f.getType());
			DynamicContentColumn dynamicColumn = new DynamicContentColumn(f);
			tableString.append(databaseConnector.renderColumn(dynamicColumn)).append(' ');
			tableString.append(databaseConnector.getSqlTypeName(fieldType, uuidTypeName));
			if (i != fields.size() - 1) {
				tableString.append(",\n");
			}
		}

		return tableString;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private int setValues(Query query, List<Pair<ContentColumn, Object>> keyValues, int initialIndex) {
		int i = initialIndex;
		NativeQuery<?> nQuery = query.unwrap(NativeQuery.class);
		for (Pair<ContentColumn, Object> kv : keyValues) {
			nQuery.setParameter(i++, kv.getKey().transformToPersistedValue(kv.getValue()), (Class) kv.getKey().getJavaClass());
		}
		return i;
	}

	@SuppressWarnings("unchecked")
	private <T> List<T> doList(String statementString, HibFieldSchemaVersionElement<?, ?, ?, ?, ?> version, Consumer<NativeQuery<?>> beforeQuery) {
		EntityManager em = HibernateTx.get().entityManager();
		NativeQuery<?> query = em.createNativeQuery(statementString).unwrap(NativeQuery.class);
		try {
			beforeQuery.accept(query);
			return (List<T>) query.getResultList();
		} catch (Exception e) {
			throw processThrowable(e, em, (UUID) version.getId());
		}
	}

	@SuppressWarnings("unchecked")
	private <T> Stream<T> doStream(String statementString, HibFieldSchemaVersionElement<?, ?, ?, ?, ?> version, Consumer<NativeQuery<?>> beforeQuery) {
		EntityManager em = HibernateTx.get().entityManager();
		NativeQuery<T> query = em.createNativeQuery(statementString).unwrap(NativeQuery.class);
		try {
			beforeQuery.accept(query);
			return (Stream<T>) query.getResultStream();
		} catch (Exception e) {
			throw processThrowable(e, em, (UUID) version.getId());
		}
	}

	private long doUpdate(String dmlString, UUID versionUuid, Consumer<NativeQuery<?>> beforeQuery) {
		EntityManager em = HibernateTx.get().entityManager();
		NativeQuery<?> query = em.createNativeQuery(dmlString).unwrap(NativeQuery.class);
		query.addSynchronizedQuerySpace(""); // prevent eviction of all hibernate second cache entities
		try {
			beforeQuery.accept(query);
			return query.executeUpdate();
		} catch (Exception e) {
			throw processThrowable(e, em, versionUuid);
		}
	}

	private RuntimeException processThrowable(Throwable e, EntityManager em, UUID versionUuid) {
		if (!databaseConnector.tableExists(HibernateUtil.getConnectionFromEntityManager(em), versionUuid)) {
			return new ContentTableNotFoundException(databaseConnector.getPhysicalTableName(versionUuid));
		} else {
			return new RuntimeException("Could not perform query", e);
		}
	}

	HibUnmanagedFieldContainer<?, ?, ?, ?, ?> findOne(ContentKey key) {
		HibernateTx tx = HibernateTx.get();
		EntityManager em = tx.entityManager();
		HibFieldSchemaVersionElement<?, ?, ?, ?, ?> version;
		if (ReferenceType.FIELD.equals(key.getType())) {
			version = em.find(HibSchemaVersionImpl.class, key.getSchemaVersionUuid());
		} else {
			version = em.find(HibMicroschemaVersionImpl.class, key.getSchemaVersionUuid());
		}

		List<ContentColumn> columns = collectVersionColumns(version);
		Map<String, ContentColumn> columnsByAlias = collectVersionColumnsByAlias(columns);
		SimpleSelect select = buildSelect(version, columns, whereDbUuid());

		Supplier<HibUnmanagedFieldContainer<?, ?, ?, ?, ?>> constructor = ReferenceType.FIELD.equals(key.getType()) ? HibNodeFieldContainerImpl::new : HibMicronodeContainerImpl::new;

		@SuppressWarnings({ "rawtypes", "unchecked" })
		List<HibUnmanagedFieldContainer<?, ?, ?, ?, ?>> resultList = doList(select.toStatementString(), version, (query) -> {
			addScalarForDeserialization(columns, query);
			ContentResultTransformer<HibUnmanagedFieldContainer<?, ?, ?, ?, ?>> transformer = new ContentResultTransformer<>((UUID) version.getId(), columnsByAlias, constructor);
			query.setResultListTransformer((ResultListTransformer) transformer);
			query.setTupleTransformer(transformer);
			query.setParameter(1, key.getContentUuid());
		});

		return resultList.size() == 1 ? resultList.get(0) : null;
	}

	<T extends HibUnmanagedFieldContainer<?, ?, ?, ?, ?>, R extends Comparable<R>> Map<ContentKey, T> findMany(Collection<ContentKey> keys, Triple<ContentColumn, R, R> columnBetween) {
		HibernateTx tx = HibernateTx.get();
		EntityManager em = tx.entityManager();
		if (keys.isEmpty()) {
			return Collections.emptyMap();
		}

		// group by schema uuid so that we are sure we are selecting from the same table
		Map<UUID, List<ContentKey>> bySchemaUUID = keys.stream().collect(Collectors.groupingBy(ContentKey::getSchemaVersionUuid));
		Map<ContentKey, T> result = new HashMap<>();
		for (Map.Entry<UUID, List<ContentKey>> entry : bySchemaUUID.entrySet()) {
			UUID schemaVersionUuid = entry.getKey();
			ReferenceType type = entry.getValue().get(0).getType(); // we are sure there is at least one element

			HibFieldSchemaVersionElement<?, ?, ?, ?, ?> version;
			if (ReferenceType.FIELD.equals(type)) {
				version = em.find(HibSchemaVersionImpl.class, schemaVersionUuid);
			} else {
				version = em.find(HibMicroschemaVersionImpl.class, schemaVersionUuid);
			}
			List<UUID> keysForVersion = keys.stream()
					.filter(key -> key.getSchemaVersionUuid().equals(schemaVersionUuid))
					.map(ContentKey::getContentUuid)
					.collect(Collectors.toList());

			executeQuery(result, columnBetween, version, keysForVersion);
		}

		return result;
	}

	private <T extends HibUnmanagedFieldContainer<?, ?, ?, ?, ?>, R extends Comparable<R>> void executeQuery(Map<ContentKey, T> result, Triple<ContentColumn, R, R> columnBetween, HibFieldSchemaVersionElement<?, ?, ?, ?, ?> version, List<UUID> keys) {
		SplittingUtils.splitAndConsume(keys, HibernateUtil.inQueriesLimitForSplitting(columnBetween != null ? 2 : 0), (keysParams) -> {
			List<ContentColumn> columns = collectVersionColumns(version);
			Map<String, ContentColumn> columnsByAlias = collectVersionColumnsByAlias(columns);
			String where = DEFAULT_ALIAS + "." + databaseConnector.renderColumn(DB_UUID) + " in (:list)";
			if (columnBetween != null) {
				where += " and " + databaseConnector.renderColumn(columnBetween.getLeft()) + " between :low and :high";
			}

			SimpleSelect select = buildSelect(version, columns, where);

			Supplier<HibUnmanagedFieldContainer<?, ?, ?, ?, ?>> constructor = version instanceof HibSchemaVersion ? HibNodeFieldContainerImpl::new : HibMicronodeContainerImpl::new;

			@SuppressWarnings({ "rawtypes", "unchecked" })
			Stream<T> resultStream = doStream(select.toStatementString(), version, (query) -> {
				addScalarForDeserialization(columns, query);
				ContentResultTransformer<HibUnmanagedFieldContainer<?, ?, ?, ?, ?>> transformer = new ContentResultTransformer<>((UUID) version.getId(), columnsByAlias, constructor);
				query.setResultListTransformer((ResultListTransformer) transformer);
				query.setTupleTransformer(transformer);
				query.setParameterList("list", keysParams, UUID.class);
				if (columnBetween != null) {
					query.setParameter("low", columnBetween.getMiddle());
					query.setParameter("high", columnBetween.getRight());
				}
			});
			result.putAll(resultStream.distinct().collect(Collectors.toMap(ContentKey::fromContent, Function.identity())));
		});
	}

	<T extends HibUnmanagedFieldContainer<?, ?, ?, ?, ?>> Map<ContentKey, T> findMany(Collection<ContentKey> keys) {
		return findMany(keys, null);
	}

	@SuppressWarnings("unchecked")
	Iterable<ContentKey> findAllKeys() {
		HibernateTx tx = HibernateTx.get();
		EntityManager em = tx.entityManager();
		Stream<Object[]> nodeContainerStream = em
				.createQuery("select edge.contentUuid, edge.version.dbUuid from nodefieldcontainer edge")
				.getResultStream();
		Stream<Object[]> micronodeContainerStream = em
				.createQuery("select edge.valueOrUuid, edge.microschemaVersion.dbUuid from micronodefieldref edge")
				.getResultStream();

		return Stream.concat(
				nodeContainerStream.map((Object[] l) -> new ContentKey((UUID) l[0], (UUID) l[1], ReferenceType.FIELD)),
				micronodeContainerStream.map((Object[] l) -> new ContentKey((UUID) l[0], (UUID) l[1], ReferenceType.MICRONODE))).collect(Collectors.toList());
	}

	/**
	 * Delete all field containers having the provided version linked to the project
	 * @param version
	 * @param project
	 */
	public long delete(HibFieldSchemaVersionElement<?, ?, ?, ?, ?> version, HibProject project) {
		String tableName = databaseConnector.getPhysicalTableName(version);
		String deleteStatement = String.format("delete from %1$s where exists (select 1 from mesh_node node where %1$s.%2$s = node.dbuuid and node.project_dbuuid = :projectUuid)", tableName, databaseConnector.renderColumn(NODE));

		long count = doUpdate(deleteStatement, (UUID) version.getId(), q -> q.setParameter("projectUuid", project.getId()));
		log.info("Deleted " + count + " containers");
		return count;
	}

	/**
	 * Delete all field containers having the provided version linked to one of the provided nodes.
	 * @param version
	 * @param nodes
	 * @return
	 */
	public long delete(HibSchemaVersion version, Set<HibNodeImpl> nodes) {
		String tableName = databaseConnector.getPhysicalTableName(version);
		String deleteStatement = String.format("delete from %s where %s in :nodesUuid", tableName, databaseConnector.renderColumn(NODE));

		long count = SplittingUtils.splitAndCount(nodes.stream().map(HibNodeImpl::getDbUuid).collect(Collectors.toList()), HibernateUtil.inQueriesLimitForSplitting(1), slice -> {
			return (long) doUpdate(deleteStatement, (UUID) version.getId(), q -> q.setParameter("nodesUuid", slice));
		});
		log.info("Deleted " + count + " containers");
		return count;
	}

	/**
	 * Delete all micronodes that are not referenced by any field.
	 * @param version
	 * @return
	 */
	public long deleteUnreferencedMicronodes(HibMicroschemaVersion version) {
		String tableName = databaseConnector.getPhysicalTableName(version);
		String deleteStatement = String.format("delete from %1$s where not exists (select 1 from mesh_micronodefieldref micronode where %1$s.%2$s = micronode.valueoruuid) " +
				" and not exists (select 1 from mesh_micronodelistitem item where %1$s.%2$s = item.valueoruuid)", tableName, databaseConnector.renderColumn(DB_UUID));

		long count = doUpdate(deleteStatement, (UUID) version.getId(), q -> {});
		log.info("Deleted " + count + " micronodes");
		return count;
	}

	@SuppressWarnings("unchecked")
	public List<ContentKey> findByNodes(HibSchemaVersion version, Set<HibNodeImpl> nodes) {
		EntityManager em = HibernateTx.get().entityManager();
		String tableName = databaseConnector.getPhysicalTableName(version);
		String selectStatement = String.format("select %s from %s where %s in :nodesUuid", databaseConnector.renderColumn(DB_UUID), tableName, databaseConnector.renderColumn(NODE));

		return SplittingUtils.splitAndMergeInList(nodes.stream().map(HibNodeImpl::getDbUuid).collect(Collectors.toList()), HibernateUtil.inQueriesLimitForSplitting(1), slice -> {
			Stream<UUID> stream = em.createNativeQuery(selectStatement)
					.setParameter("nodesUuid", slice)
				.unwrap(NativeQuery.class)
				.addScalar(databaseConnector.renderColumn(DB_UUID), UUID.class)
				.getResultStream();

			return stream.map(uuid -> ContentKey.fromContentUUIDAndVersion(uuid, version))
				.collect(Collectors.toList());
		});
	}
}