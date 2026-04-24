package com.gentics.mesh.core.field.micronode;

import static com.gentics.mesh.test.TestSize.FULL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.MicroschemaDao;
import com.gentics.mesh.core.data.dao.PersistingMicroschemaDao;
import com.gentics.mesh.core.data.node.HibMicronode;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.list.HibMicronodeFieldList;
import com.gentics.mesh.core.data.node.field.list.HibNodeFieldList;
import com.gentics.mesh.core.data.node.field.nesting.HibMicronodeField;
import com.gentics.mesh.core.data.node.field.nesting.HibNodeField;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaUpdateRequest;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.schema.impl.BooleanFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicronodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NodeFieldSchemaImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.util.StreamUtil;

@MeshTestSetting(testSize = FULL, startServer = true)
public class ReferencedByMicronodeTest extends AbstractMeshTest {

	@Test
	public void testReferencedByMicronode() throws IOException {
		Calendar date = Calendar.getInstance();
		date.set(2016, 1, 6, 3, 44);
		String microschemaUuid = tx(tx -> {
			HibNode newOverview = content("news overview");

			ContentDao contentDao = tx.contentDao();
			MicroschemaDao microschemaDao = tx.microschemaDao();

			MicroschemaVersionModel fullMicroschema = new MicroschemaModelImpl();
			fullMicroschema.setName("full");

			fullMicroschema.addField(new ListFieldSchemaImpl().setListType("node").setName("listfield-node").setLabel("Node List Field"));
			fullMicroschema.addField(new NodeFieldSchemaImpl().setName("nodefield").setLabel("Node Field"));

			HibMicroschema microschema = microschemaDao.create(fullMicroschema, getRequestUser(), createBatch());
			waitForJobs(() -> {
				tx.branchDao().assignMicroschemaVersion(latestBranch(), user(), microschema.getLatestVersion(), createBatch());
			}, JobStatus.COMPLETED, 1);
			{
				HibNode node = folder("2015");
				HibNode referenced = folder("2014");
				prepareTypedSchema(node, new MicronodeFieldSchemaImpl().setName("micronodefield").setLabel("Micronode Field"), false);
				prepareTypedSchema(node, new ListFieldSchemaImpl().setListType("micronode").setName("listfield-micronode").setLabel("Micronode List Field"), false);
				tx.commit();
	
				HibNodeFieldContainer container = contentDao.createFieldContainer(node, english(),
						node.getProject().getLatestBranch(), user(),
						contentDao.getLatestDraftFieldContainer(node, english()), true);
				HibMicroschemaVersion latestVersion = microschema.getLatestVersion();
				System.err.println(latestVersion.getVersion());
				HibMicronodeField micronodeField = container.createMicronode("micronodefield", latestVersion);
				HibMicronode micronode = micronodeField.getMicronode();
				assertNotNull("Micronode must not be null", micronode);
	
				HibNodeFieldList nodeList = micronode.createNodeList("listfield-node");
				nodeList.createNode(0, referenced);
				nodeList.createNode(1, newOverview);
	
				micronode.createNode("nodefield", newOverview);

				HibMicronodeFieldList micronodeListField = container.createMicronodeList("listfield-micronode");
				HibMicronode listItem = micronodeListField.createMicronode(microschema.getLatestVersion());
				listItem.createNode("nodefield", newOverview);
				micronodeListField.addItem(micronodeField);
			}
			{
				HibNode node = folder("2014");
				HibNode referenced = folder("2015");
				prepareTypedSchema(node, new MicronodeFieldSchemaImpl().setName("micronodefield").setLabel("Micronode Field"), false);
				prepareTypedSchema(node, new ListFieldSchemaImpl().setListType("micronode").setName("listfield-micronode").setLabel("Micronode List Field"), false);
				tx.commit();
	
				HibNodeFieldContainer container = contentDao.createFieldContainer(node, english(),
						node.getProject().getLatestBranch(), user(),
						contentDao.getLatestDraftFieldContainer(node, english()), true);
				HibMicronodeField micronodeField = container.createMicronode("micronodefield", microschema.getLatestVersion());
				HibMicronode micronode = micronodeField.getMicronode();
				assertNotNull("Micronode must not be null", micronode);
	
				HibNodeFieldList nodeList = micronode.createNodeList("listfield-node");
				nodeList.createNode(0, referenced);
				nodeList.createNode(1, newOverview);
	
				micronode.createNode("nodefield", newOverview);

				HibMicronodeFieldList micronodeListField = container.createMicronodeList("listfield-micronode");
				HibMicronode listItem = micronodeListField.createMicronode(microschema.getLatestVersion());
				listItem.createNode("nodefield", newOverview);
				micronodeListField.addItem(micronodeField);
			}
			return microschema.getUuid();
		});

		tx(tx -> {
			// Test one micronode
			HibNode node2015 = folder("2015");
			HibNodeFieldContainer content2015 = tx.contentDao().getFieldContainer(node2015, english());
			HibMicronodeField micronodeField2015 = content2015.getMicronode("micronodefield");
			HibMicronode micronode2015 = micronodeField2015.getMicronode();
			assertNotNull("Micronode must not be null", micronode2015);
			HibNodeFieldList nodeList2015 = micronode2015.getNodeList("listfield-node");
			assertNotNull("Node list must not be null", nodeList2015);
			HibNodeField nodeRef2015 = micronode2015.getNode("nodefield");
			assertNotNull("Node reference must not be null", nodeRef2015);

			Collection<HibNodeField> fields2015 = new HashSet<>(nodeList2015.getList());
			fields2015.add(nodeRef2015);
			Map<HibNodeField, Collection<HibNodeFieldContainer>> referencingContents2015 = tx.contentDao().getReferencingContents(fields2015).collect(Collectors.toMap(Pair::getKey, Pair::getValue));
			assertThat(referencingContents2015).as("Micronode referencing node 2015").hasSize(3);

			// Test another micronode
			HibNode node2014 = folder("2014");
			HibNodeFieldContainer content2014 = tx.contentDao().getFieldContainer(node2014, english());
			HibMicronodeField micronodeField2014 = content2014.getMicronode("micronodefield");
			HibMicronode micronode2014 = micronodeField2014.getMicronode();
			assertNotNull("Micronode must not be null", micronode2014);
			HibNodeFieldList nodeList2014 = micronode2014.getNodeList("listfield-node");
			assertNotNull("Node list must not be null", nodeList2014);
			HibNodeField nodeRef2014 = micronode2014.getNode("nodefield");
			assertNotNull("Node reference must not be null", nodeRef2014);

			Collection<HibNodeField> fields2014 = new HashSet<>(nodeList2014.getList());
			fields2014.add(nodeRef2014);
			Map<HibNodeField, Collection<HibNodeFieldContainer>> referencingContents2014 = tx.contentDao().getReferencingContents(fields2014).collect(Collectors.toMap(Pair::getKey, Pair::getValue));
			assertThat(referencingContents2014).as("Micronode referencing node 2014").hasSize(3);

			// Test both micronodes
			Collection<HibNodeField> fieldsAll = new HashSet<>(nodeList2014.getList());
			fieldsAll.add(nodeRef2014);
			fieldsAll.add(nodeRef2015);
			fieldsAll.addAll(nodeList2015.getList());
			Map<HibNodeField, Collection<HibNodeFieldContainer>> referencingContentsAll = tx.contentDao().getReferencingContents(fieldsAll).collect(Collectors.toMap(Pair::getKey, Pair::getValue));
			assertThat(referencingContentsAll).as("Micronode referencing node all").hasSize(6);
		});

		MicroschemaUpdateRequest microschemaUpdate = adminCall(() -> client().findMicroschemaByUuid(microschemaUuid)).toRequest();
		microschemaUpdate.addField(new BooleanFieldSchemaImpl().setName("bool"));
		waitForJobs(() -> {
			adminCall(() -> client().updateMicroschema(microschemaUuid, microschemaUpdate));
		}, JobStatus.COMPLETED, 1);

		tx(tx -> {
			HibNode newOverview = content("news overview");

			ContentDao contentDao = tx.contentDao();
			MicroschemaDao microschemaDao = tx.microschemaDao();

			HibMicroschema microschema = microschemaDao.findByName("full");
			waitForJobs(() -> {
				tx.branchDao().assignMicroschemaVersion(latestBranch(), user(), microschema.getLatestVersion(), createBatch());
			}, JobStatus.COMPLETED, 1);
			waitForJobs(() -> {
				adminCall(() -> client().migrateBranchMicroschemas(projectName(), latestBranch().getUuid()));
			}, JobStatus.COMPLETED, 1);
			{
				HibNode node = folder("2015");
				HibNode referenced = folder("2014");
				prepareTypedSchema(node, new MicronodeFieldSchemaImpl().setName("micronodefield").setLabel("Micronode Field"), false);
				tx.commit();
	
				HibNodeFieldContainer container = contentDao.createFieldContainer(node, english(),
						node.getProject().getLatestBranch(), user(),
				contentDao.getLatestDraftFieldContainer(node, english()), true);

				HibMicroschemaVersion latestVersion = microschema.getLatestVersion();
				System.err.println(latestVersion.getVersion());
				HibMicronodeField micronodeField = container.createMicronode("micronodefield", latestVersion);
				HibMicronode micronode = micronodeField.getMicronode();
				assertNotNull("Micronode must not be null", micronode);
	
				HibNodeFieldList nodeList = micronode.createNodeList("listfield-node");
				nodeList.createNode(0, referenced);
				nodeList.createNode(1, newOverview);
				nodeList.createNode(2, newOverview);
				nodeList.createNode(3, referenced);
	
				micronode.createNode("nodefield", newOverview);

				HibMicronodeFieldList micronodeListField = container.createMicronodeList("listfield-micronode");
				HibMicronode listItem = micronodeListField.createMicronode(microschema.getLatestVersion());
				listItem.createNode("nodefield", newOverview);
				micronodeListField.addItem(micronodeField);
			}
			{
				HibNode node = folder("2014");
				HibNode referenced = folder("2015");
				prepareTypedSchema(node, new MicronodeFieldSchemaImpl().setName("micronodefield").setLabel("Micronode Field"), false);
				tx.commit();
	
				HibNodeFieldContainer container = contentDao.createFieldContainer(node, english(),
						node.getProject().getLatestBranch(), user(),
						contentDao.getLatestDraftFieldContainer(node, english()), true);
				HibMicronodeField micronodeField = container.createMicronode("micronodefield", microschema.getLatestVersion());
				HibMicronode micronode = micronodeField.getMicronode();
				assertNotNull("Micronode must not be null", micronode);
	
				HibNodeFieldList nodeList = micronode.createNodeList("listfield-node");
				nodeList.createNode(0, referenced);
				nodeList.createNode(1, newOverview);
				nodeList.createNode(2, newOverview);
				nodeList.createNode(3, referenced);
	
				micronode.createNode("nodefield", newOverview);

				HibMicronodeFieldList micronodeListField = container.createMicronodeList("listfield-micronode");
				HibMicronode listItem = micronodeListField.createMicronode(microschema.getLatestVersion());
				listItem.createNode("nodefield", newOverview);
				micronodeListField.addItem(micronodeField);
			}
		});

		NodeUpdateRequest updateRequest = nonAdminCall(() -> client().findNodeByUuid(projectName(), tx(() -> folder("2014").getUuid()))).toRequest();
		updateRequest.getFields().getStringField("slug").setString("whatever");
		nonAdminCall(() -> client().updateNode(projectName(), tx(() -> folder("2014").getUuid()), updateRequest));

		NodeUpdateRequest updateRequest1 = nonAdminCall(() -> client().findNodeByUuid(projectName(), tx(() -> folder("2015").getUuid()))).toRequest();
		updateRequest1.getFields().getStringField("slug").setString("wherever");
		nonAdminCall(() -> client().updateNode(projectName(), tx(() -> folder("2015").getUuid()), updateRequest1));
		
		tx(tx -> {
			Collection<HibNodeField> fieldsAll = new HashSet<>();
			HibMicroschema microschema = tx.microschemaDao().findByUuid(microschemaUuid);
			StreamUtil.toStream(tx.microschemaDao().findAllVersions(microschema))
				.flatMap(version -> ((PersistingMicroschemaDao) tx.microschemaDao()).findMicronodes(version).stream())
				.forEach(micronode -> {
					HibNodeFieldList nodeList2015 = micronode.getNodeList("listfield-node");
					HibNodeField nodeRef2015 = micronode.getNode("nodefield");
					fieldsAll.add(nodeRef2015);
					if (nodeList2015 != null) {
						fieldsAll.addAll(nodeList2015.getList());
					}
				});

			Map<HibNodeField, Collection<HibNodeFieldContainer>> referencingContentsAll = tx.contentDao().getReferencingContents(fieldsAll).collect(Collectors.toMap(Pair::getKey, Pair::getValue));
			assertThat(referencingContentsAll).as("Micronode referencing node all").hasSize(26);
		});
	}
}
