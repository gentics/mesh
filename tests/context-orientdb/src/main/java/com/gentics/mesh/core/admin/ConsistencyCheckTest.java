package com.gentics.mesh.core.admin;

import static com.gentics.mesh.core.rest.MeshEvent.REPAIR_FINISHED;
import static com.gentics.mesh.core.rest.MeshEvent.REPAIR_START;
import static com.gentics.mesh.core.rest.admin.consistency.ConsistencyRating.CONSISTENT;
import static com.gentics.mesh.core.rest.admin.consistency.ConsistencyRating.INCONSISTENT;
import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import com.gentics.mesh.core.data.GraphFieldContainerEdge;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.data.impl.UserImpl;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.relationship.GraphRelationships;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyRating;
import com.gentics.mesh.core.rest.admin.consistency.InconsistencyInfo;
import com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity;
import com.gentics.mesh.core.rest.admin.consistency.RepairAction;
import com.gentics.mesh.core.rest.branch.BranchCreateRequest;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.parameter.client.NodeParametersImpl;
import com.gentics.mesh.parameter.client.VersioningParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.syncleus.ferma.TEdge;

@MeshTestSetting(testSize = FULL, startServer = true, inMemoryDB = true)
public class ConsistencyCheckTest extends AbstractMeshTest {

	@Test
	public void testConsistencyCheck() {
		client().setLogin("admin", "admin");
		client().login().blockingGet();

		ConsistencyCheckResponse response = call(() -> client().checkConsistency());
		assertThat(response.getInconsistencies()).isEmpty();
		assertEquals(CONSISTENT, response.getResult());

		tx(() -> {
			((User)user()).getVertex().property(UserImpl.USERNAME_PROPERTY_KEY).remove();
		});
		response = call(() -> client().checkConsistency());
		assertThat(response.getInconsistencies()).hasSize(1);
		assertEquals(userUuid(), response.getInconsistencies().get(0).getElementUuid());
		assertEquals(INCONSISTENT, response.getResult());

		// Now fix the inconsistency. Otherwise the asserter of the test (within @After) would fail.
		tx(() -> {
			((User)user()).getVertex().property(UserImpl.USERNAME_PROPERTY_KEY, "blub");
		});
	}

	@Test
	public void testConsistencyRepair() {
		grantAdmin();

		ConsistencyCheckResponse response = call(() -> client().checkConsistency());
		assertThat(response.getInconsistencies()).isEmpty();
		assertEquals(ConsistencyRating.CONSISTENT, response.getResult());

		tx(() -> {
			((Node) content()).removeElement();
		});
		response = call(() -> client().checkConsistency());
		assertEquals(INCONSISTENT, response.getResult());
		assertThat(response.getInconsistencies()).hasSize(4);
		InconsistencyInfo info = response.getInconsistencies().get(0);
		assertFalse("The check should not repair the inconsistency", info.isRepaired());
		assertEquals(RepairAction.DELETE, info.getRepairAction());

		expect(REPAIR_START).one();
		expect(REPAIR_FINISHED).one();
		response = call(() -> client().repairConsistency());
		awaitEvents();

		assertEquals(INCONSISTENT, response.getResult());
		// We only see two inconsistencies because the other two were additional versions of the node and were also deleted.
		assertThat(response.getInconsistencies()).hasSize(2);
		info = response.getInconsistencies().get(0);
		assertTrue("The repair should not repair the inconsistency", info.isRepaired());
		assertEquals(RepairAction.DELETE, info.getRepairAction());

		response = call(() -> client().checkConsistency());
		assertThat(response.getInconsistencies()).isEmpty();
		assertEquals(ConsistencyRating.CONSISTENT, response.getResult());

	}

	/**
	 * Test repairing multiple INITIAL edges
	 */
	@Test
	public void testRepairMultipleInitial() {
		grantAdmin();

		String branchName = "newbranch";
		String nodeUuid = tx(() -> content().getUuid());
		String projectName = tx(() -> project().getName());

		// modify the content in the initial branch (in all languages)
		NodeResponse nodeResponse = call(() -> client().findNodeByUuid(projectName, nodeUuid));
		Set<String> languages = nodeResponse.getAvailableLanguages().keySet();

		for (String language : languages) {
			NodeResponse draft = call(() -> client().findNodeByUuid(projectName, nodeUuid,
					new VersioningParametersImpl().setVersion("draft"),
					new NodeParametersImpl().setLanguages(language)));
			FieldMap fields = draft.getFields();
			StringFieldImpl titleField = fields.getStringField("title");
			titleField.setString(titleField.getString() + " modified");
			fields.put("title", titleField);
			call(() -> client().updateNode(projectName, nodeUuid,
					new NodeUpdateRequest().setLanguage(language).setFields(fields)));
		}

		AtomicReference<String> newBranchUuid = new AtomicReference<>();
		// create new latest branch
		waitForJobs(() -> {
			newBranchUuid.set(call(() -> client().createBranch(projectName, new BranchCreateRequest().setName(branchName).setLatest(true))).getUuid());
		}, COMPLETED, 1);

		// modify the content also in the new branch
		for (String language : languages) {
			NodeResponse draft = call(() -> client().findNodeByUuid(projectName, nodeUuid,
					new VersioningParametersImpl().setVersion("draft"),
					new NodeParametersImpl().setLanguages(language)));
			FieldMap fields = draft.getFields();
			StringFieldImpl titleField = fields.getStringField("title");
			titleField.setString(titleField.getString() + " modified");
			fields.put("title", titleField);
			call(() -> client().updateNode(projectName, nodeUuid,
					new NodeUpdateRequest().setLanguage(language).setFields(fields)));
		}

		// check consistency
		ConsistencyCheckResponse response = call(() -> client().checkConsistency());
		assertThat(response.getInconsistencies()).isEmpty();
		assertThat(response.getResult()).as("Result").isEqualTo(ConsistencyRating.CONSISTENT);

		// make an inconsistency by adding another INITIAL edge
		AtomicReference<String> contentUuid = new AtomicReference<>();
		tx(tx -> {
			NodeDao nodeDao = tx.nodeDao();
			ContentDao contentDao = tx.contentDao();
			HibNode node = nodeDao.findByUuidGlobal(nodeUuid);
			HibNodeFieldContainer enDraft = contentDao.getFieldContainer(node, "en", newBranchUuid.get(), ContainerType.DRAFT);
			contentUuid.set(enDraft.getUuid());

			GraphFieldContainerEdge edge = ((NodeImpl) node).addFramedEdge(GraphRelationships.HAS_FIELD_CONTAINER,
					(NodeGraphFieldContainerImpl) enDraft, GraphFieldContainerEdgeImpl.class);
			edge.setBranchUuid(newBranchUuid.get());
			edge.setLanguageTag("en");
			edge.setType(ContainerType.INITIAL);
		});

		// must be inconsistent now
		response = call(() -> client().checkConsistency());
		assertThat(response.getInconsistencies()).usingFieldByFieldElementComparator().containsOnly(
				new InconsistencyInfo()
					.setDescription(String.format("The node has more than one GFC of type %s, language %s for branch %s", ContainerType.INITIAL, "en", newBranchUuid.get()))
					.setElementUuid(nodeUuid)
					.setRepairAction(RepairAction.NONE)
					.setRepaired(false)
					.setSeverity(InconsistencySeverity.HIGH),
				new InconsistencyInfo()
					.setDescription(String.format("GraphFieldContainer of Node {%s} is INITIAL for branch %s and has another INITIAL GFC for the branch as a previous version", nodeUuid, newBranchUuid.get()))
					.setElementUuid(contentUuid.get())
					.setRepairAction(RepairAction.DELETE)
					.setRepaired(false)
					.setSeverity(InconsistencySeverity.MEDIUM)
				);
		assertThat(response.getResult()).as("Result").isEqualTo(ConsistencyRating.INCONSISTENT);

		// repair
		response = call(() -> client().repairConsistency());
		assertThat(response.getInconsistencies()).usingFieldByFieldElementComparator().containsOnly(
				new InconsistencyInfo()
					.setDescription(String.format("The node has more than one GFC of type %s, language %s for branch %s", ContainerType.INITIAL, "en", newBranchUuid.get()))
					.setElementUuid(nodeUuid)
					.setRepairAction(RepairAction.NONE)
					.setRepaired(false)
					.setSeverity(InconsistencySeverity.HIGH),
				new InconsistencyInfo()
					.setDescription(String.format("GraphFieldContainer of Node {%s} is INITIAL for branch %s and has another INITIAL GFC for the branch as a previous version", nodeUuid, newBranchUuid.get()))
					.setElementUuid(contentUuid.get())
					.setRepairAction(RepairAction.DELETE)
					.setRepaired(true)
					.setSeverity(InconsistencySeverity.MEDIUM)
				);
		// since we found the consistency twice (and had no clue how to repair the first one), we are still "inconsistent"
		assertThat(response.getResult()).as("Result").isEqualTo(ConsistencyRating.INCONSISTENT);

		// check again
		response = call(() -> client().checkConsistency());
		assertThat(response.getInconsistencies()).isEmpty();
		assertThat(response.getResult()).as("Result").isEqualTo(ConsistencyRating.CONSISTENT);
	}
}
