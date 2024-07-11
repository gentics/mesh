package com.gentics.mesh.contentoperation;

import static com.gentics.mesh.test.TestSize.EMPTY;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.db.TxAction1;
import org.apache.commons.lang3.tuple.Triple;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.gentics.mesh.contentoperation.CommonContentColumn;
import com.gentics.mesh.contentoperation.ContentCachedStorage;
import com.gentics.mesh.contentoperation.ContentNoCacheStorage;
import com.gentics.mesh.contentoperation.ContentStorageImpl;
import com.gentics.mesh.contentoperation.ContentTableNotFoundException;
import com.gentics.mesh.contentoperation.DynamicContentColumn;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.S3BinaryFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.etc.config.hibernate.HibernateStorageOptions;
import com.gentics.mesh.hibernate.data.domain.HibBinaryImpl;
import com.gentics.mesh.hibernate.data.domain.HibBranchImpl;
import com.gentics.mesh.hibernate.data.domain.HibMicronodeContainerImpl;
import com.gentics.mesh.hibernate.data.domain.HibMicroschemaImpl;
import com.gentics.mesh.hibernate.data.domain.HibMicroschemaVersionImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeFieldContainerEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeFieldContainerImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeImpl;
import com.gentics.mesh.hibernate.data.domain.HibS3BinaryImpl;
import com.gentics.mesh.hibernate.data.domain.HibSchemaImpl;
import com.gentics.mesh.hibernate.data.domain.HibSchemaVersionImpl;
import com.gentics.mesh.hibernate.data.domain.HibUnmanagedFieldContainer;
import com.gentics.mesh.hibernate.util.UuidGenerator;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.NoConsistencyCheck;
import com.gentics.mesh.util.CoreTestUtils;
import com.gentics.mesh.util.UUIDUtil;

@MeshTestSetting(testSize = EMPTY)
public class ContentStorageImplTest extends AbstractMeshTest {

	SchemaVersion version;
	SchemaVersion mockVersion;
	UuidGenerator generator;
	ContentStorageImpl contentStorage;
	UUID nodeUuid;

	@Before
	public void setup() {
		generator = Mockito.mock(UuidGenerator.class);
		Node node = tx(this::createHibernateNode);
		nodeUuid = UUIDUtil.toJavaUuid(node.getUuid());

		doAnswer(invocationOnMock -> UUID.fromString(invocationOnMock.getArgument(0))).when(generator).toJavaUuidOrGenerate(anyString());
		version = tx((TxAction1<HibSchemaVersionImpl>) this::createSchemaVersion);

		HibernateMeshOptions meshOptions = new HibernateMeshOptions();
		HibernateStorageOptions storageOptions = new HibernateStorageOptions();
		storageOptions.setSecondLevelCacheEnabled(false);
		meshOptions.setStorageOptions(storageOptions);

		mockVersion = Mockito.mock(SchemaVersion.class, Mockito.RETURNS_DEEP_STUBS);

		UUID schemaVersionId = UUID.randomUUID();
		UUID schemaId = UUID.randomUUID();
		when(mockVersion.getId()).thenReturn(schemaVersionId);
		when(mockVersion.getSchemaContainer().getUuid()).thenReturn(UUIDUtil.toShortUuid(schemaVersionId));
		when(mockVersion.getSchemaContainer().getId()).thenReturn(schemaId);
		when(mockVersion.getUuid()).thenReturn(UUIDUtil.toShortUuid(schemaVersionId));

		ContentNoCacheStorage contentNoCacheStorage = new ContentNoCacheStorage(meshOptions, tx(tx -> { return tx.<HibernateTx>unwrap().data().getDatabaseConnector(); }));
		contentStorage = new ContentStorageImpl(contentNoCacheStorage, Mockito.mock(ContentCachedStorage.class));
	}

	@Test(expected = ContentTableNotFoundException.class)
	public void testFindColumnWhenTableDoesNotExist() {
		tx(() -> {
			contentStorage.findColumn(mockVersion, UUID.randomUUID(), CommonContentColumn.DB_UUID);
		});
	}

	@Test
	public void testFindColumnWhenTableExistsAndContentDoesNot() {
		tx(tx -> {
			Assertions.assertThat((UUID) contentStorage.findColumn(version, UUID.randomUUID(), CommonContentColumn.DB_UUID)).isNull();
			Assertions.assertThat((String) contentStorage.findColumn(version, UUID.randomUUID(), CommonContentColumn.LANGUAGE_TAG)).isNull();
			Assertions.assertThat((Long) contentStorage.findColumn(version, UUID.randomUUID(), CommonContentColumn.DB_VERSION)).isNull();
		});
	}

	@Test
	public void testFindProperty() {
		tx(tx -> {
			UUID contentUUID = UUID.randomUUID();
			HibNodeFieldContainerImpl content = new HibNodeFieldContainerImpl();
			content.setDbUuid(contentUUID);
			content.setLanguageTag("en");
			content.setSchemaContainerVersion(version);
			content.setDbVersion(1L);
			content.put(CommonContentColumn.NODE, nodeUuid);

			contentStorage.insert(content, version);

			Assertions.assertThat((UUID) contentStorage.findColumn(version, contentUUID, CommonContentColumn.DB_UUID)).isEqualTo(contentUUID);
			Assertions.assertThat((String) contentStorage.findColumn(version, contentUUID, CommonContentColumn.LANGUAGE_TAG)).isEqualTo("en");
			Assertions.assertThat((Long) contentStorage.findColumn(version, contentUUID, CommonContentColumn.DB_VERSION)).isEqualTo(1L);

			tx.rollback();
		});
	}

	@Test
	public void testFindOne() {
		tx(tx -> {
			HibSchemaVersionImpl schemaVersion = createSchemaVersion(new BinaryFieldSchemaImpl().setName("binary1").setLabel("binary1"), new BinaryFieldSchemaImpl().setName("binary2").setLabel("binary2"), new S3BinaryFieldSchemaImpl().setName("s3binary1").setLabel("s3binary1"), new S3BinaryFieldSchemaImpl().setName("s3binary2").setLabel("s3binary2"));
			Timestamp editedTimestamp = new Timestamp(System.currentTimeMillis());
			UUID contentUUID = UUID.randomUUID();
			UUID editorUuid = UUID.randomUUID();

			HibNodeFieldContainerImpl content = new HibNodeFieldContainerImpl();
			content.setDbUuid(contentUUID);
			content.setDbVersion(1L);
			content.put(CommonContentColumn.EDITOR_DB_UUID, editorUuid);
			content.put(CommonContentColumn.EDITED, editedTimestamp);
			content.setBucketId(20);
			content.setSchemaContainerVersion(schemaVersion);
			content.setLanguageTag("en");
			content.put(CommonContentColumn.NODE, nodeUuid);
			content.put(CommonContentColumn.CURRENT_VERSION_NUMBER, "1.2");
			content.createBinary("binary1", createBinary()).setFileName("binary1");
			content.createBinary("binary2", createBinary()).setFileName("binary2");
			content.createS3Binary("s3binary1", createS3Binary()).setFileName("s3binary1");
			content.createS3Binary("s3binary2", createS3Binary()).setFileName("s3binary2");
			contentStorage.insert(content, schemaVersion);

			HibNodeFieldContainerImpl entity = contentStorage.findOne(schemaVersion, contentUUID);

			Assertions.assertThat(entity.getDbUuid()).isEqualTo(contentUUID);
			Assertions.assertThat(entity.getDbVersion()).isEqualTo(1L);
			Assertions.assertThat(entity.<UUID>get(CommonContentColumn.EDITOR_DB_UUID, () -> null)).isEqualTo(editorUuid);
			Assertions.assertThat(entity.getLastEditedTimestamp()).isEqualTo(editedTimestamp.getTime());
			Assertions.assertThat(entity.getBucketId()).isEqualTo(20);
			Assertions.assertThat(entity.<UUID>get(CommonContentColumn.SCHEMA_DB_UUID, () -> null)).isEqualTo(schemaVersion.getSchemaContainer().getId());
			Assertions.assertThat(entity.getSchemaContainerVersion()).isEqualTo(schemaVersion);
			Assertions.assertThat(entity.getLanguageTag()).isEqualTo("en");
			Assertions.assertThat(entity.<UUID>get(CommonContentColumn.NODE, () -> null)).isEqualTo(nodeUuid);
			Assertions.assertThat(entity.getVersion().toString()).isEqualTo("1.2");
			Assertions.assertThat(entity.getBinaryFileName("binary1")).isEqualTo("binary1");
			Assertions.assertThat(entity.getBinaryFileName("binary2")).isEqualTo("binary2");
			Assertions.assertThat(entity.getS3BinaryFileName("s3binary1")).isEqualTo("s3binary1");
			Assertions.assertThat(entity.getS3BinaryFileName("s3binary2")).isEqualTo("s3binary2");

			tx.rollback();
		});
	}

	@Test
	public void testFindOneInInterceptor() {
		tx(tx -> {
			HibSchemaVersionImpl schemaVersion = createSchemaVersion(new BinaryFieldSchemaImpl().setName("binary1").setLabel("binary1"), new BinaryFieldSchemaImpl().setName("binary2").setLabel("binary2"), new S3BinaryFieldSchemaImpl().setName("s3binary1").setLabel("s3binary1"), new S3BinaryFieldSchemaImpl().setName("s3binary2").setLabel("s3binary2"));
			UUID contentUUID = UUID.randomUUID();

			persistInContentInterceptor(contentUUID, schemaVersion, 20);

			HibNodeFieldContainerImpl entity = contentStorage.findOne(schemaVersion, contentUUID);

			Assertions.assertThat(entity.getDbUuid()).isEqualTo(contentUUID);
			Assertions.assertThat(entity.getDbVersion()).isEqualTo(1L);
			Assertions.assertThat(entity.getBucketId()).isEqualTo(20);
			Assertions.assertThat(entity.<UUID>get(CommonContentColumn.SCHEMA_DB_UUID, () -> null)).isEqualTo(schemaVersion.getSchemaContainer().getId());
			Assertions.assertThat(entity.getSchemaContainerVersion()).isEqualTo(schemaVersion);

			tx.rollback();
		});
	}

	@Test
	public void testFindOneMicronode() {
		tx(tx -> {
			HibMicroschemaVersionImpl microVersion = createMicroschemaVersion();
			createMicronodeTable(microVersion);
			UUID contentUUID = UUID.randomUUID();

			HibMicronodeContainerImpl microNode = new HibMicronodeContainerImpl();
			microNode.setDbUuid(contentUUID);
			microNode.setSchemaContainerVersion(microVersion);
			microNode.setDbVersion(1L);

			contentStorage.insert(microNode, microVersion);

			HibMicronodeContainerImpl entity = contentStorage.findOneMicronode(microVersion, contentUUID);

			Assertions.assertThat(entity.getDbUuid()).isEqualTo(contentUUID);
			Assertions.assertThat(entity.getDbVersion()).isEqualTo(1L);
			Assertions.assertThat(entity.<UUID>get(CommonContentColumn.SCHEMA_DB_UUID, () -> null)).isEqualTo(microVersion.getSchemaContainer().getId());
			Assertions.assertThat(entity.getSchemaContainerVersion()).isEqualTo(microVersion);

			tx.rollback();
		});
	}

	@Test
	public void addColumnTablesWhenColumnExists() {
		tx((tx) -> {
			DynamicContentColumn existingColumn = new DynamicContentColumn(new StringFieldSchemaImpl().setName(CommonContentColumn.DB_UUID.getLabel()));
			contentStorage.addColumnIfNotExists(version, existingColumn);
		});
	}

	@Test(expected = ContentTableNotFoundException.class)
	public void deleteWhenTableDoesNotExist() {
		tx(() -> {
			contentStorage.delete(UUID.randomUUID(), mockVersion);
		});
	}

	@Test
	public void addColumnWhenColumnDoesNotExist() {
		tx((tx) -> {
			DynamicContentColumn column = new DynamicContentColumn(new StringFieldSchemaImpl().setName("someLabel"));
			contentStorage.addColumnIfNotExists(version, column);
			UUID contentUuid = UUID.randomUUID();

			HibNodeFieldContainerImpl content = new HibNodeFieldContainerImpl();
			content.put(column, "someValue");
			content.setSchemaContainerVersion(version);
			content.setDbVersion(1L);
			content.setDbUuid(contentUuid);
			content.put(CommonContentColumn.NODE, nodeUuid);

			contentStorage.insert(content, version);
			String result = contentStorage.findColumn(version, contentUuid, column);

			Assertions.assertThat(result).isEqualTo("someValue");

			tx.rollback();
		});
	}

	@Test
	public void deleteWhenTableExistsAndContentDoesNot() {
		tx((tx) -> {
			contentStorage.delete(UUID.randomUUID(), version);

			tx.rollback();
		});
	}

	@Test(expected = ContentTableNotFoundException.class)
	public void insertWhenTableDoesNotExist() {
		tx(() -> {
			HibNodeFieldContainerImpl content = new HibNodeFieldContainerImpl();
			content.put(CommonContentColumn.SCHEMA_VERSION_DB_UUID, "schema_version_UUID");
			contentStorage.insert(content, mockVersion);
		});
	}

	@Test
	public void deleteWhenContentExists() {
		tx((tx) -> {
			UUID contentUuid = UUID.randomUUID();
			createContent(contentUuid, version, 1);

			contentStorage.delete(contentUuid, version);
			HibNodeFieldContainerImpl result = contentStorage.findOne(version, contentUuid);

			Assertions.assertThat(result).isNull();
			tx.rollback();
		});
	}

	@Test
	public void tableDropWhenTableExists() {
		tx((tx) -> {
			contentStorage.createTable(mockVersion);
			contentStorage.dropTable(mockVersion);

			Assertions.assertThatThrownBy(() -> contentStorage.findColumn(mockVersion, UUID.randomUUID(), CommonContentColumn.DB_VERSION)).isInstanceOf(ContentTableNotFoundException.class);

			tx.rollback();
		});
	}

	@Test
	@NoConsistencyCheck
	public void tableDropWhenTableDoesNotExist() {
		tx(() -> contentStorage.dropTable(version));
	}

	@Test
	public void testFindMany() {
		tx((tx) -> {
			version = createSchemaVersion();
			UUID contentUuid = UUID.randomUUID();
			createContent(contentUuid, version, 1);
			UUID contentUuid2 = UUID.randomUUID();
			createContent(contentUuid2, version, 2);
			UUID contentUuid3 = UUID.randomUUID();
			createContent(contentUuid3, version, 3);
			UUID contentUuid4 = UUID.randomUUID();
			createContent(contentUuid4, version, 4);

			List<HibNodeFieldContainerImpl> containers = contentStorage.findMany(version);

			Map<UUID, HibNodeFieldContainerImpl> result = containers.stream()
					.collect(Collectors.toMap(HibUnmanagedFieldContainer::getDbUuid, Function.identity()));

			Assertions.assertThat(result).hasSize(4);
			Assertions.assertThat(result.get(contentUuid).getBucketId()).isEqualTo(1);
			Assertions.assertThat(result.get(contentUuid2).getBucketId()).isEqualTo(2);
			Assertions.assertThat(result.get(contentUuid3).getBucketId()).isEqualTo(3);
			Assertions.assertThat(result.get(contentUuid4).getBucketId()).isEqualTo(4);

			tx.rollback();
		});
	}

	@Test
	public void testFindManyInterceptor() {
		tx((tx) -> {
			version = createSchemaVersion();
			UUID contentUuid = UUID.randomUUID();
			persistInContentInterceptor(contentUuid, version, 1);
			UUID contentUuid2 = UUID.randomUUID();
			createContent(contentUuid2, version, 2);
			UUID contentUuid3 = UUID.randomUUID();
			persistInContentInterceptor(contentUuid3, version, 3);
			UUID contentUuid4 = UUID.randomUUID();
			createContent(contentUuid4, version, 4);

			List<HibNodeFieldContainerImpl> containers = contentStorage.findMany(version);

			Map<UUID, HibNodeFieldContainerImpl> result = containers.stream()
					.collect(Collectors.toMap(HibUnmanagedFieldContainer::getDbUuid, Function.identity()));

			Assertions.assertThat(result).hasSize(4);
			Assertions.assertThat(result.get(contentUuid).getBucketId()).isEqualTo(1);
			Assertions.assertThat(result.get(contentUuid2).getBucketId()).isEqualTo(2);
			Assertions.assertThat(result.get(contentUuid3).getBucketId()).isEqualTo(3);
			Assertions.assertThat(result.get(contentUuid4).getBucketId()).isEqualTo(4);

			tx.rollback();
		});
	}

	@Test
	public void testFindManyMicronodes() {
		tx((tx) -> {
			MicroschemaVersion version = createMicroschemaVersion();
			createMicronodeTable(version);
			UUID contentUuid = UUID.randomUUID();
			createMicroContent(contentUuid, version, 1);
			UUID contentUuid2 = UUID.randomUUID();
			createMicroContent(contentUuid2, version, 2);
			UUID contentUuid3 = UUID.randomUUID();
			createMicroContent(contentUuid3, version, 3);
			UUID contentUuid4 = UUID.randomUUID();
			createMicroContent(contentUuid4, version, 4);

			List<HibMicronodeContainerImpl> containers = contentStorage.findManyMicronodes(version);

			Map<UUID, HibMicronodeContainerImpl> result = containers.stream()
					.collect(Collectors.toMap(HibUnmanagedFieldContainer::getDbUuid, Function.identity()));

			Assertions.assertThat(result).hasSize(4);
			Assertions.assertThat(result.get(contentUuid).getDbVersion()).isEqualTo(1);
			Assertions.assertThat(result.get(contentUuid2).getDbVersion()).isEqualTo(2);
			Assertions.assertThat(result.get(contentUuid3).getDbVersion()).isEqualTo(3);
			Assertions.assertThat(result.get(contentUuid4).getDbVersion()).isEqualTo(4);

			tx.rollback();
		});
	}

	@Test
	@NoConsistencyCheck
	public void testFindManyColumnBetween() {
		tx((tx) -> {
			version = createSchemaVersion();
			UUID contentUuid = UUID.randomUUID();
			createContent(contentUuid, version, 1);
			tx.commit();
			createContainerEdge(contentUuid, version, "en", ContainerType.INITIAL);
			UUID contentUuid2 = UUID.randomUUID();
			createContent(contentUuid2, version, 2);
			createContainerEdge(contentUuid2, version, "en", ContainerType.DRAFT);
			UUID contentUuid3 = UUID.randomUUID();
			createContent(contentUuid3, version, 3);
			createContainerEdge(contentUuid3, version, "en", ContainerType.DRAFT);
			UUID contentUuid4 = UUID.randomUUID();
			createContent(contentUuid4, version, 4);
			createContainerEdge(contentUuid4, version, "en", ContainerType.PUBLISHED);

			EntityManager entityManager = HibernateTx.get().entityManager();
			List<HibNodeFieldContainerEdgeImpl> edges = entityManager.createQuery("select e from nodefieldcontainer e where e.version.dbUuid = :versionUuid", HibNodeFieldContainerEdgeImpl.class)
					.setParameter("versionUuid", version.getId())
					.getResultList();
			Assertions.assertThat(edges).hasSize(4);
			List<HibNodeFieldContainerImpl> containers = contentStorage.findMany(edges, Triple.of(CommonContentColumn.BUCKET_ID, 2, 3)).collect(Collectors.toList());

			Map<UUID, HibNodeFieldContainerImpl> result = containers.stream()
					.collect(Collectors.toMap(HibUnmanagedFieldContainer::getDbUuid, Function.identity()));

			Assertions.assertThat(result).hasSize(2);
			Assertions.assertThat(result.get(contentUuid2).getBucketId()).isEqualTo(2);
			Assertions.assertThat(result.get(contentUuid3).getBucketId()).isEqualTo(3);
			tx.rollback();
		});
	}

	@Test
	@NoConsistencyCheck
	public void testGlobalCount() {
		tx((tx) -> {
			SchemaVersion version1 = createSchemaVersion();
			createContent(UUID.randomUUID(), version1, 1);

			SchemaVersion version2 = createSchemaVersion();
			createContent(UUID.randomUUID(), version2, 2);

			Assertions.assertThat(contentStorage.getGlobalCount()).isEqualTo(2);

			tx.rollback();
		});
	}

	private HibNodeFieldContainerImpl createContent(UUID contentUuid, SchemaVersion version, int bucketId) {
		HibNodeFieldContainerImpl content = new HibNodeFieldContainerImpl();
		content.setDbUuid(contentUuid);
		content.setSchemaContainerVersion(version);
		content.setBucketId(bucketId);
		content.setDbVersion(1L);
		content.put(CommonContentColumn.NODE, nodeUuid);

		contentStorage.insert(content, version);
		return content;
	}

	private HibNodeFieldContainerImpl persistInContentInterceptor(UUID contentUuid, SchemaVersion version, int bucketId) {
		HibNodeFieldContainerImpl content = new HibNodeFieldContainerImpl();
		content.setDbUuid(contentUuid);
		content.setSchemaContainerVersion(version);
		content.setBucketId(bucketId);
		content.setDbVersion(1L);
		content.put(CommonContentColumn.NODE, nodeUuid);

		HibernateTx.get().getContentInterceptor().persist(content);
		return content;
	}

	private HibMicronodeContainerImpl createMicroContent(UUID contentUuid, MicroschemaVersion version, long dbVersion) {
		HibMicronodeContainerImpl microNode = new HibMicronodeContainerImpl();
		microNode.setDbUuid(contentUuid);
		microNode.setSchemaContainerVersion(version);
		microNode.setDbVersion(dbVersion);

		contentStorage.insert(microNode, version);
		return microNode;
	}

	private HibSchemaVersionImpl createSchemaVersion(FieldSchema ...fields) {
		EntityManager entityManager = HibernateTx.get().entityManager();

		HibSchemaImpl schema = new HibSchemaImpl();
		schema.setDbUuid(UUID.randomUUID());
		schema.setName(UUIDUtil.toShortUuid(schema.getDbUuid()));
		entityManager.persist(schema);
		return (HibSchemaVersionImpl) CoreTestUtils.createSchemaVersion(schema, "schema", "1.0", fields);
	}

	private Node createHibernateNode() {
		EntityManager entityManager = HibernateTx.get().entityManager();

		HibNodeImpl hibNode = new HibNodeImpl();
		hibNode.setDbUuid(UUID.randomUUID());
		entityManager.persist(hibNode);

		return hibNode;
	}

	private HibBinaryImpl createBinary() {
		EntityManager entityManager = HibernateTx.get().entityManager();

		HibBinaryImpl binary = new HibBinaryImpl();
		binary.setDbUuid(UUID.randomUUID());
		entityManager.persist(binary);
		return binary;
	}

	private HibS3BinaryImpl createS3Binary() {
		EntityManager entityManager = HibernateTx.get().entityManager();

		HibS3BinaryImpl binary = new HibS3BinaryImpl();
		binary.setDbUuid(UUID.randomUUID());
		entityManager.persist(binary);
		return binary;
	}

	private HibMicroschemaVersionImpl createMicroschemaVersion() {
		EntityManager entityManager = HibernateTx.get().entityManager();

		HibMicroschemaImpl schema = new HibMicroschemaImpl();
		schema.setDbUuid(UUID.randomUUID());
		schema.setName(UUIDUtil.toShortUuid(schema.getDbUuid()));
		entityManager.persist(schema);
		HibMicroschemaVersionImpl version = new HibMicroschemaVersionImpl();
		version.setDbUuid(UUID.randomUUID());
		version.setSchemaContainer(schema);
		entityManager.persist(version);

		return version;
	}

	private HibNodeFieldContainerEdgeImpl createContainerEdge(UUID containerUuid, SchemaVersion schemaVersion, String languageTag, ContainerType initial) {
		EntityManager em = HibernateTx.get().entityManager();
		HibNodeFieldContainerEdgeImpl edge = new HibNodeFieldContainerEdgeImpl();
		edge.setElement(UUID.randomUUID());
		edge.setContentUuid(containerUuid);
		edge.setLanguageTag(languageTag);
		edge.setType(initial);
		HibNodeImpl hibNode = new HibNodeImpl();
		hibNode.setDbUuid(UUID.randomUUID());
		edge.setNode(hibNode);
		em.persist(hibNode);
		HibBranchImpl branc = new HibBranchImpl();
		branc.setDbUuid(UUID.randomUUID());
		branc.setName(UUIDUtil.toShortUuid(branc.getDbUuid()));
		em.persist(branc);
		edge.setBranch(branc);
		edge.setVersion(schemaVersion);
		em.persist(edge);

		return edge;
	}

	private void createMicronodeTable(MicroschemaVersion version) {
		contentStorage.createMicronodeTable(version);
		Tx.get().commit(); // make sure table is created
	}
}