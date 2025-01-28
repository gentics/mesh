package com.gentics.mesh.hibernate.data.dao;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.list.HibBooleanFieldList;
import com.gentics.mesh.core.data.node.field.list.HibDateFieldList;
import com.gentics.mesh.core.data.node.field.list.HibHtmlFieldList;
import com.gentics.mesh.core.data.node.field.list.HibMicronodeFieldList;
import com.gentics.mesh.core.data.node.field.list.HibNodeFieldList;
import com.gentics.mesh.core.data.node.field.list.HibNumberFieldList;
import com.gentics.mesh.core.data.node.field.list.HibStringFieldList;
import com.gentics.mesh.core.data.node.field.nesting.HibMicronodeField;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.data.dao.NodeDaoImpl;
import com.gentics.mesh.hibernate.data.domain.HibBinaryImpl;
import com.gentics.mesh.hibernate.data.domain.HibMicronodeContainerImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeFieldContainerEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeFieldContainerImpl;
import com.gentics.mesh.hibernate.data.domain.HibS3BinaryImpl;
import com.gentics.mesh.hibernate.data.domain.HibUnmanagedFieldContainer;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.DummyEventQueueBatch;
import com.gentics.mesh.util.CoreTestUtils;

@MeshTestSetting(testSize = TestSize.PROJECT)
public class NodeDaoImplTest extends AbstractMeshTest {

	private final FieldSchema nodeField = FieldUtil.createNodeFieldSchema("nodeField");
	private final FieldSchema microField = FieldUtil.createMicronodeFieldSchema("microField").setAllowedMicroSchemas("testMicroschema");
	private final FieldSchema binaryField = FieldUtil.createBinaryFieldSchema("binaryField");
	private final FieldSchema s3BinaryField = FieldUtil.createS3BinaryFieldSchema("s3BinaryField");
	private final FieldSchema stringList = FieldUtil.createListFieldSchema("stringListField", "string");
	private final FieldSchema htmlList = FieldUtil.createListFieldSchema("htmlListField", "html");
	private final FieldSchema numberList = FieldUtil.createListFieldSchema("numberListField", "number");
	private final FieldSchema dateList = FieldUtil.createListFieldSchema("dateListField", "date");
	private final FieldSchema boolList = FieldUtil.createListFieldSchema("boolListField", "boolean");
	private final FieldSchema nodeList = FieldUtil.createListFieldSchema("nodeListField", "node");
	private final FieldSchema micronodeList = FieldUtil.createListFieldSchema("microListField", "micronode").setAllowedSchemas("testMicroschema");

	private final FieldSchema[] fieldSchemas = new FieldSchema[] {
			nodeField,
			microField,
			binaryField,
			s3BinaryField,
			stringList,
			htmlList,
			numberList,
			dateList,
			boolList,
			nodeList,
			micronodeList
	};

	private final FieldSchema[] microFieldSchemas = new FieldSchema[] {
			nodeField,
			stringList,
			htmlList,
			numberList,
			dateList,
			boolList,
			nodeList,
	};

	@Test
	public void testRecursiveNodeDeletion() {
		HibSchema fatSchema = createFatSchema();
		HibMicroschema fatMicroschema = createFatMicroschema();
		HibProject projectToTest = tx(() -> createProject("projectToTest", fatSchema.getName()));
		HibNode sourceNode = tx(() -> {
			NodeDao nodeDao = Tx.get().nodeDao();
			Tx.get().microschemaDao().assign(fatMicroschema, projectToTest, user(), new DummyEventQueueBatch());
			ContentDao contentDao = Tx.get().contentDao();

			HibNode refNode = nodeDao.create(projectToTest, user(), fatSchema.getLatestVersion());
			HibNode source = nodeDao.create(projectToTest, user(), fatSchema.getLatestVersion());
			HibNodeFieldContainerImpl sourceContainer = (HibNodeFieldContainerImpl) contentDao.createFieldContainer(source, "en", projectToTest.getLatestBranch(), user());
			HibNode parentNode = source;

			// create 10 nodes with containers
			for (int i = 0; i < 10; i++) {
				parentNode = nodeDao.create(parentNode, user(), fatSchema.getLatestVersion(), projectToTest);
				HibNodeFieldContainerImpl container = (HibNodeFieldContainerImpl) contentDao.createFieldContainer(parentNode, "en", projectToTest.getLatestBranch(), user());
				fillContainer(container, refNode, fatMicroschema.getLatestVersion());
			}

			return source;
		});

		// delete from base node
		try (Tx tx = tx()) {
			// count before deletion
			for (String tableName : Arrays.asList("nodefieldref", "micronodefieldref", "binaryfieldref", "binary",
					"s3binaryfieldref", "s3binary", "stringlistitem", "htmllistitem", "numberlistitem", "datelistitem",
					"boollistitem", "micronodelistitem")) {
				assertThat(tableCount(tableName)).as("Record count in table " + tableName).isGreaterThan(0);
			}

			tx.success();
		}

		tx(() -> {
			NodeDaoImpl nodeDaoImpl = HibernateTx.get().nodeDao();
			nodeDaoImpl.delete(nodeDaoImpl.findByUuid(projectToTest, sourceNode.getUuid()), true, true);
		});

		try (Tx tx = tx()) {
			for (String tableName : Arrays.asList("nodefieldref", "micronodefieldref", "binaryfieldref", "binary",
					"s3binaryfieldref", "s3binary", "stringlistitem", "htmllistitem", "numberlistitem", "datelistitem",
					"boollistitem", "micronodelistitem")) {
				assertThat(tableCount(tableName)).as("Record count in table " + tableName).isEqualTo(0);
			}

			tx.success();
		}
	}

	@Test
	public void testNodeDeletionByProject() {
		HibSchema fatSchema = createFatSchema();
		HibMicroschema fatMicroschema = createFatMicroschema();
		HibProject projectToBeDeleted = tx(() -> createProject("projectToBeDeleted", fatSchema.getName()));
		HibProject projectToStay = tx(() -> createProject("projectToStay", fatSchema.getName()));

		tx(() -> {
			NodeDao nodeDao = Tx.get().nodeDao();
			Tx.get().microschemaDao().assign(fatMicroschema, projectToBeDeleted, user(), new DummyEventQueueBatch());
			ContentDao contentDao = Tx.get().contentDao();
			HibNode node = nodeDao.create(projectToBeDeleted, user(), fatSchema.getLatestVersion());
			HibNode refNode = nodeDao.create(projectToBeDeleted, user(), fatSchema.getLatestVersion());

			// create 10 containers
			for (int i = 0; i < 10; i++) {
				HibNodeFieldContainerImpl container = (HibNodeFieldContainerImpl) contentDao.createFieldContainer(node, "en", projectToBeDeleted.getLatestBranch(), user());
				HibNodeFieldContainerEdgeImpl edge = (HibNodeFieldContainerEdgeImpl) contentDao.getContainerEdges(container).findFirst().get();
				Set<String> wuf = edge.getWebrootUrlFields();
				if (wuf == null) {
					wuf = new HashSet<>();
					edge.setWebrootUrlFields(wuf);
				}
				wuf.add("value" + i);
				fillContainer(container, refNode, fatMicroschema.getLatestVersion());
			}
		});

		tx(() -> {
			NodeDao nodeDao = Tx.get().nodeDao();
			Tx.get().microschemaDao().assign(fatMicroschema, projectToStay, user(), new DummyEventQueueBatch());
			ContentDao contentDao = Tx.get().contentDao();
			HibNode node = nodeDao.create(projectToStay, user(), fatSchema.getLatestVersion());
			HibNode refNode = nodeDao.create(projectToStay, user(), fatSchema.getLatestVersion());

			// create 10 containers
			for (int i = 0; i < 10; i++) {
				HibNodeFieldContainerImpl container = (HibNodeFieldContainerImpl) contentDao.createFieldContainer(node, "en", projectToBeDeleted.getLatestBranch(), user());
				fillContainer(container, refNode, fatMicroschema.getLatestVersion());
			}

			return node;
		});

		// delete from project
		tx(() -> {
			// count before deletion
			long edgeCount = tableCount("nodefieldcontainer");
			long versionCount = tableCount("nodefieldcontainer_versions_edge");
			long fieldRefCount = tableCount("nodefieldref");
			long microRefCount = tableCount("micronodefieldref");
			long binaryRefCount = tableCount("binaryfieldref");
			long binaryCount = tableCount("binary");
			long s3BinaryCount = tableCount("s3binary");
			long s3BinaryRefCount = tableCount("s3binaryfieldref");
			long stringListCount = tableCount("stringlistitem");
			long htmlListCount = tableCount("htmllistitem");
			long numberListCount = tableCount("numberlistitem");
			long dateListCount = tableCount("datelistitem");
			long boolListCount = tableCount("boollistitem");
			long microListCount = tableCount("micronodelistitem");

			Assertions.assertThat(edgeCount).isGreaterThan(0);
			Assertions.assertThat(versionCount).isGreaterThan(0);
			Assertions.assertThat(fieldRefCount).isGreaterThan(0);
			Assertions.assertThat(microRefCount).isGreaterThan(0);
			Assertions.assertThat(binaryRefCount).isGreaterThan(0);
			Assertions.assertThat(binaryCount).isGreaterThan(0);
			Assertions.assertThat(s3BinaryRefCount).isGreaterThan(0);
			Assertions.assertThat(s3BinaryCount).isGreaterThan(0);
			Assertions.assertThat(stringListCount).isGreaterThan(0);
			Assertions.assertThat(htmlListCount).isGreaterThan(0);
			Assertions.assertThat(numberListCount).isGreaterThan(0);
			Assertions.assertThat(dateListCount).isGreaterThan(0);
			Assertions.assertThat(boolListCount).isGreaterThan(0);
			Assertions.assertThat(microListCount).isGreaterThan(0);

			NodeDaoImpl nodeDaoImpl = HibernateTx.get().nodeDao();
			nodeDaoImpl.deleteAllFromProject(projectToBeDeleted);

			// assert that half of the fields were deleted
			Assertions.assertThat(tableCount("nodefieldcontainer")).isEqualTo(edgeCount - 4);
			Assertions.assertThat(tableCount("nodefieldcontainer_versions_edge")).isEqualTo(versionCount / 2 + 2); // 2 of the version edges are folder containers
			Assertions.assertThat(tableCount("nodefieldref")).isEqualTo(fieldRefCount / 2);
			Assertions.assertThat(tableCount("micronodefieldref")).isEqualTo(microRefCount / 2);
			Assertions.assertThat(tableCount("binaryfieldref")).isEqualTo(binaryRefCount / 2);
			Assertions.assertThat(tableCount("binary")).isEqualTo(binaryCount / 2);
			Assertions.assertThat(tableCount("s3binaryfieldref")).isEqualTo(s3BinaryRefCount / 2);
			Assertions.assertThat(tableCount("s3binary")).isEqualTo(s3BinaryCount / 2);
			Assertions.assertThat(tableCount("stringlistitem")).isEqualTo(stringListCount / 2);
			Assertions.assertThat(tableCount("htmllistitem")).isEqualTo(htmlListCount / 2);
			Assertions.assertThat(tableCount("numberlistitem")).isEqualTo(numberListCount / 2);
			Assertions.assertThat(tableCount("datelistitem")).isEqualTo(dateListCount / 2);
			Assertions.assertThat(tableCount("boollistitem")).isEqualTo(boolListCount / 2);
			Assertions.assertThat(tableCount("micronodelistitem")).isEqualTo(microListCount / 2);
		});
	}

	private HibSchema createFatSchema() {
		return tx(() -> {
			HibSchema schema = createSchema(Tx.get());
			schema.setName("testSchema");
			HibSchemaVersion schemaVersion = createSchemaVersion(Tx.get(), schema, version -> {
				CoreTestUtils.fillSchemaVersion(version, schema, "testSchema", "1.0", fieldSchemas);
			});
			schema.setLatestVersion(schemaVersion);

			return schema;
		});
	}

	private HibMicroschema createFatMicroschema() {
		return tx(() -> {
			final String microschemaName = "testMicroschema";
			HibMicroschema microschema = createMicroschema(Tx.get());
			microschema.setName(microschemaName);
			HibMicroschemaVersion microschemaVersion = createMicroschemaVersion(Tx.get(), microschema, version -> {
				CoreTestUtils.fillMicroschemaVersion(version, microschema, microschemaName, "1.0", microFieldSchemas);
			});
			microschema.setLatestVersion(microschemaVersion);

			return microschema;
		});
	}

	private void fillContainer(HibNodeFieldContainerImpl container, HibNode refNode, HibMicroschemaVersion version) {
		container.createNode("nodeField", refNode);
		fillCommon(container, refNode);

		HibMicronodeField microField = container.createMicronode("microField", version);
		HibMicronodeContainerImpl micronode = (HibMicronodeContainerImpl) microField.getMicronode();
		fillCommon(micronode, refNode);

		HibMicronodeFieldList microListField = container.createMicronodeList("microListField");
		HibMicronodeContainerImpl micronodeListItem = (HibMicronodeContainerImpl) microListField.createMicronode(version);
		fillCommon(micronodeListItem, refNode);

		container.createBinary("binaryField", createBinary());
		container.createS3Binary("s3BinaryField", createS3Binary());
	}

	private void fillCommon(HibUnmanagedFieldContainer<?, ?, ?, ?, ?> container, HibNode refNode) {
		container.createNode("nodeField", refNode);

		HibStringFieldList stringListField = container.createStringList("stringListField");
		stringListField.createString("1");
		stringListField.createString("2");
		stringListField.createString("3");

		HibHtmlFieldList htmlListField = container.createHTMLList("htmlListField");
		htmlListField.createHTML("1");
		htmlListField.createHTML("2");
		htmlListField.createHTML("3");

		HibNumberFieldList numberListField = container.createNumberList("numberListField");
		numberListField.createNumber(1);
		numberListField.createNumber(2);
		numberListField.createNumber(3);

		HibDateFieldList dateListField = container.createDateList("dateListField");
		dateListField.createDate(System.currentTimeMillis() - 3000);
		dateListField.createDate(System.currentTimeMillis() - 2000);
		dateListField.createDate(System.currentTimeMillis() - 1000);

		HibBooleanFieldList boolListField = container.createBooleanList("boolListField");
		boolListField.createBoolean(true);
		boolListField.createBoolean(false);
		boolListField.createBoolean(true);

		HibNodeFieldList nodeListField = container.createNodeList("nodeListField");
		nodeListField.createNode(refNode);
	}

	private long tableCount(String fieldName) {
		EntityManager em = HibernateTx.get().entityManager();
		return ((Number) em.createQuery(String.format("select count(e) from %s e", fieldName))
				.getSingleResult()).longValue();
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
}