package com.gentics.mesh.core.language;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.ElasticsearchTestMode.TRACKING;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.rest.SortOrder;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.lang.LanguageListResponse;
import com.gentics.mesh.core.rest.lang.LanguageResponse;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.parameter.client.GenericParametersImpl;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.ProjectLoadParametersImpl;
import com.gentics.mesh.parameter.impl.SortingParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.definition.BasicRestTestcases;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Observable;

@MeshTestSetting(elasticsearch = TRACKING, testSize = TestSize.FULL, startServer = true)
public class LanguageEndpointTest extends AbstractMeshTest implements BasicRestTestcases {

	@Before
	public void setup() {
		tx(tx -> {
			tx.languageDao().assign(tx.languageDao().findByLanguageTag(italian()), project(), null, false);
			tx.success();
		});
	}

	@Override
	@Ignore("No Language creation allowed")
	@Test
	public void testCreate() throws Exception {
	}

	@Override
	@Ignore("No Language manipulation allowed")
	@Test
	public void testCreateReadDelete() throws Exception {
	}

	@Override
	@Ignore("No Language creation allowed")
	@Test
	public void testCreateWithNoPerm() throws Exception {
	}

	@Override
	@Ignore("No Language creation allowed")
	@Test
	public void testCreateWithUuid() throws Exception {
	}

	@Override
	@Ignore("No Language creation allowed")
	@Test
	public void testCreateWithDuplicateUuid() throws Exception {
	}

	@Override
	@Test
	public void testReadByUUID() throws Exception {
		String italianUuid = tx(tx -> { 
			return tx.languageDao().findByLanguageTag(italian()).getUuid();
		});
		LanguageResponse italian = call(() -> client().findLanguageByUuid(italianUuid));
		assertEquals(italianUuid, italian.getUuid());
		assertEquals(italian(), italian.getLanguageTag());
	}

	@Override
	@Ignore("No Language permissions supposrted")
	@Test
	public void testReadByUuidWithRolePerms() {
	}

	@Override
	@Ignore("No Language permissions supposrted")
	@Test
	public void testReadByUUIDWithMissingPermission() throws Exception {
	}

	@Override
	@Test
	public void testReadMultiple() throws Exception {
		LanguageListResponse languages = call(() -> client().findLanguages());
		assertEquals(11, languages.getData().size());
	}

	@Override
	@Ignore("No Language permissions supposrted")
	@Test
	public void testPermissionResponse() {
	}

	@Override
	@Ignore("No Language update allowed")
	@Test
	public void testUpdate() throws Exception {
	}

	@Override
	@Ignore
	@Test
	public void testUpdateByUUIDWithoutPerm() throws Exception {
	}

	@Override
	@Ignore("No Language creation allowed")
	@Test
	public void testUpdateWithBogusUuid() throws GenericRestException, Exception {
	}

	@Override
	@Ignore("No Language removal allowed")
	@Test
	public void testDeleteByUUID() throws Exception {
	}

	@Override
	@Ignore("No Language removal allowed")
	@Test
	public void testDeleteByUUIDWithNoPermission() throws Exception {
	}

	@Override
	@Ignore("No Language update allowed")
	@Test
	public void testUpdateMultithreaded() throws Exception {
	}

	@Override
	@Test
	public void testReadByUuidMultithreaded() throws Exception {
		int nJobs = 10;
		LanguageListResponse languages = call(() -> client().findLanguages());
		Random rnd = new Random();

		Observable.range(0, nJobs)
			.flatMapCompletable(i -> client().findLanguageByUuid(languages.getData().get(rnd.nextInt(languages.getData().size())).getUuid()).toCompletable())
			.blockingAwait();
	}

	@Override
	@Ignore("No Language removal allowed")
	@Test
	public void testDeleteByUUIDMultithreaded() throws Exception {
	}

	@Override
	@Ignore("No Language creation allowed")
	@Test
	public void testCreateMultithreaded() throws Exception {
	}

	@Override
	@Test
	public void testReadByUuidMultithreadedNonBlocking() throws Exception {
		int nJobs = 200;
		LanguageListResponse languages = call(() -> client().findLanguages());
		Random rnd = new Random();

		Observable.range(0, nJobs)
			.flatMapCompletable(i -> client().findLanguageByUuid(languages.getData().get(rnd.nextInt(languages.getData().size())).getUuid()).toCompletable())
			.blockingAwait();
	}

	@Test
	public void testReadByTag() {
		String italianUuid = tx(tx -> { 
			return tx.languageDao().findByLanguageTag(italian()).getUuid();
		});
		LanguageResponse italian = call(() -> client().findLanguageByTag(italian()));
		assertEquals(italianUuid, italian.getUuid());
		assertEquals(italian(), italian.getLanguageTag());
	}

	@Test
	public void testReadFromProjectByTag() {
		String italianUuid = tx(tx -> { 
			return tx.languageDao().findByLanguageTag(italian()).getUuid();
		});
		LanguageResponse italian = call(() -> client().findLanguageByTag(PROJECT_NAME, italian()));
		assertEquals(italianUuid, italian.getUuid());
		assertEquals(italian(), italian.getLanguageTag());
		assertTrue("The project should contain the language", tx(tx -> { 
			return project().getLanguages().stream().anyMatch(lang -> lang.getUuid().equals(italianUuid) && lang.getLanguageTag().equals(italian())); 
		}));
	}

	@Test
	public void testReadFromProjectByUUID() throws Exception {
		String italianUuid = tx(tx -> { 
			return tx.languageDao().findByLanguageTag(italian()).getUuid();
		});
		LanguageResponse italian = call(() -> client().findLanguageByUuid(PROJECT_NAME, italianUuid));
		assertEquals(italianUuid, italian.getUuid());
		assertEquals(italian(), italian.getLanguageTag());
		assertTrue("The project should contain the language", tx(tx -> { 
			return project().getLanguages().stream().anyMatch(lang -> lang.getUuid().equals(italianUuid) && lang.getLanguageTag().equals(italian())); 
		}));
	}

	@Test
	public void testAssignByUuid() {
		String frenchUuid = tx(tx -> { 
			return tx.languageDao().findByLanguageTag(french()).getUuid();
		});
		ProjectResponse projectResponse = call(() -> client().assignLanguageToProjectByUuid(PROJECT_NAME, frenchUuid, new ProjectLoadParametersImpl().setLangs(true)));
		assertNotNull(projectResponse.getLanguages());
		assertThat(projectResponse.getLanguages().stream().map(LanguageResponse::getUuid)).contains(frenchUuid);
	}

	@Test
	public void testAssignByTag() {
		ProjectResponse projectResponse = call(() -> client().assignLanguageToProjectByTag(PROJECT_NAME, french(), new ProjectLoadParametersImpl().setLangs(true)));
		assertNotNull(projectResponse.getLanguages());
		assertThat(projectResponse.getLanguages().stream().map(LanguageResponse::getLanguageTag)).contains(french());
	}

	@Test
	public void testUnassignByUuid() {
		String italianUuid = tx(tx -> { 
			return tx.languageDao().findByLanguageTag(italian()).getUuid();
		});
		ProjectResponse projectResponse = call(() -> client().unassignLanguageFromProjectByUuid(PROJECT_NAME, italianUuid, new ProjectLoadParametersImpl().setLangs(true)));
		assertNotNull(projectResponse.getLanguages());
		assertThat(projectResponse.getLanguages().stream().map(LanguageResponse::getUuid)).doesNotContain(italianUuid);
	}

	@Test
	public void testUnassignByTag() {
		ProjectResponse projectResponse = call(() -> client().unassignLanguageFromProjectByTag(PROJECT_NAME, italian(), new ProjectLoadParametersImpl().setLangs(true)));
		assertNotNull(projectResponse.getLanguages());
		assertThat(projectResponse.getLanguages().stream().map(LanguageResponse::getUuid)).doesNotContain(italian());
	}

	@Test
	public void testReadFromProjectByFalseTag() {
		call(() -> client().findLanguageByTag(PROJECT_NAME, "bogus"), HttpResponseStatus.NOT_FOUND, "error_language_not_found", "bogus");

		assertTrue("The project should not contain the false language", tx(tx -> { 
			return project().getLanguages().stream().noneMatch(lang -> lang.getUuid().equals("bogus") && lang.getLanguageTag().equals(italian())); 
		}));
	}

	@Test
	public void testAssignByFalseTag() {
		call(() -> client().assignLanguageToProjectByTag(PROJECT_NAME, "bogus", new ProjectLoadParametersImpl().setLangs(true)), 
				HttpResponseStatus.NOT_FOUND, "error_language_not_found", "bogus");
		ProjectResponse projectResponse = call(() -> client().findProjectByName(PROJECT_NAME, new ProjectLoadParametersImpl().setLangs(true)));
		assertNotNull(projectResponse.getLanguages());
		assertThat(projectResponse.getLanguages().stream().map(LanguageResponse::getLanguageTag)).doesNotContain("bogus");
	}

	@Test
	public void testUnassignByFalseTag() {
		call(() -> client().unassignLanguageFromProjectByTag(PROJECT_NAME, "bogus", new ProjectLoadParametersImpl().setLangs(true)), 
				HttpResponseStatus.NOT_FOUND, "error_language_not_found", "bogus");
		ProjectResponse projectResponse = call(() -> client().findProjectByName(PROJECT_NAME, new ProjectLoadParametersImpl().setLangs(true)));
		assertNotNull(projectResponse.getLanguages());
		assertThat(projectResponse.getLanguages().stream().map(LanguageResponse::getUuid)).doesNotContain("bogus");
	}

	@Test
	public void testCreateAssignedLanguage() {
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setAssignLanguage(false);
		nodeCreateRequest.setSchemaName("folder");
		nodeCreateRequest.setLanguage(italian());
		nodeCreateRequest.getFields().put("name", FieldUtil.createStringField("Buon Compleanno!"));
		nodeCreateRequest.setParentNodeUuid(tx(() -> project().getBaseNode().getUuid()));
		NodeResponse nodeResponse = call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest, new NodeParametersImpl().setLanguages(italian()), new GenericParametersImpl().setFields("fields", "language")));
		assertEquals(nodeResponse.getLanguage(), italian());
		assertEquals(nodeResponse.getFields().getStringField("name").getValue(), "Buon Compleanno!");
	}

	@Test
	public void testCreateUnassignedLanguage() {
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setAssignLanguage(false);
		nodeCreateRequest.setSchemaName("folder");
		nodeCreateRequest.setLanguage(french());
		nodeCreateRequest.getFields().put("name", FieldUtil.createStringField("Bonne Fête!"));
		nodeCreateRequest.setParentNodeUuid(tx(() -> project().getBaseNode().getUuid()));
		call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest, new NodeParametersImpl().setLanguages(french()), new GenericParametersImpl().setFields("fields", "language")), 
				HttpResponseStatus.BAD_REQUEST, "error_language_not_assigned", french(), PROJECT_NAME);
	}

	@Test
	public void testCreateUnssignedLanguageAutoAssign() {
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest().setAssignLanguage(true);
		nodeCreateRequest.setAssignLanguage(true);
		nodeCreateRequest.setSchemaName("folder");
		nodeCreateRequest.setLanguage(french());
		nodeCreateRequest.getFields().put("name", FieldUtil.createStringField("Bonne Fête!"));
		nodeCreateRequest.setParentNodeUuid(tx(() -> project().getBaseNode().getUuid()));
		NodeResponse nodeResponse = call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest, new NodeParametersImpl().setLanguages(french()), new GenericParametersImpl().setFields("fields", "language")));
		assertEquals(nodeResponse.getLanguage(), french());
		assertEquals(nodeResponse.getFields().getStringField("name").getValue(), "Bonne Fête!");
	}

	@Test
	public void testUnassignOccupiedLanguageByTag() {
		NodeListResponse nodes = call(() -> client().findNodes(PROJECT_NAME));
		assertThat(nodes.getData()).isNotEmpty();
		call(() -> client().unassignLanguageFromProjectByTag(PROJECT_NAME, english(), new ProjectLoadParametersImpl().setLangs(true)), HttpResponseStatus.CONFLICT, "error_language_still_in_use", english(), PROJECT_NAME);
		ProjectResponse projectResponse = call(() -> client().findProjectByName(PROJECT_NAME, new ProjectLoadParametersImpl().setLangs(true)));
		assertNotNull(projectResponse.getLanguages());
		assertThat(projectResponse.getLanguages().stream().map(LanguageResponse::getLanguageTag)).contains(english());
	}

	@Test
	public void testUnassignOccupiedLanguageByUuid() {
		NodeListResponse nodes = call(() -> client().findNodes(PROJECT_NAME));
		assertThat(nodes.getData()).isNotEmpty();
		String englishUuid = tx(tx -> { 
			return tx.languageDao().findByLanguageTag(english()).getUuid();
		});
		call(() -> client().unassignLanguageFromProjectByUuid(PROJECT_NAME, englishUuid, new ProjectLoadParametersImpl().setLangs(true)), HttpResponseStatus.CONFLICT, "error_language_still_in_use", english(), PROJECT_NAME);
		ProjectResponse projectResponse = call(() -> client().findProjectByName(PROJECT_NAME, new ProjectLoadParametersImpl().setLangs(true)));
		assertNotNull(projectResponse.getLanguages());
		assertThat(projectResponse.getLanguages().stream().map(LanguageResponse::getUuid)).contains(englishUuid);
	}

	@Test
	public void testNotPermittedProject() {
		String englishUuid = tx(tx -> {
			assertTrue(tx.roleDao().revokePermissions(role(), project(), InternalPermission.values()));
			return tx.languageDao().findByLanguageTag(english()).getUuid();
		});
		String projectUuid = tx(() -> {
			return project().getUuid();
		});
		call(() -> client().findLanguages(PROJECT_NAME), HttpResponseStatus.FORBIDDEN, "error_missing_perm", projectUuid, InternalPermission.READ_PERM.getRestPerm().getName());
		call(() -> client().findLanguageByUuid(PROJECT_NAME, englishUuid), HttpResponseStatus.FORBIDDEN, "error_missing_perm", projectUuid, InternalPermission.READ_PERM.getRestPerm().getName());
		call(() -> client().findLanguageByTag(PROJECT_NAME, english()), HttpResponseStatus.FORBIDDEN, "error_missing_perm", projectUuid, InternalPermission.READ_PERM.getRestPerm().getName());
		call(() -> client().findLanguageByTag(PROJECT_NAME, english()), HttpResponseStatus.FORBIDDEN, "error_missing_perm", projectUuid, InternalPermission.READ_PERM.getRestPerm().getName());
		call(() -> client().assignLanguageToProjectByUuid(PROJECT_NAME, englishUuid), HttpResponseStatus.FORBIDDEN, "error_missing_perm", projectUuid, InternalPermission.UPDATE_PERM.getRestPerm().getName());
		call(() -> client().assignLanguageToProjectByTag(PROJECT_NAME, english()), HttpResponseStatus.FORBIDDEN, "error_missing_perm", projectUuid, InternalPermission.UPDATE_PERM.getRestPerm().getName());
		call(() -> client().unassignLanguageFromProject(PROJECT_NAME, englishUuid), HttpResponseStatus.FORBIDDEN, "error_missing_perm", projectUuid, InternalPermission.UPDATE_PERM.getRestPerm().getName());
		call(() -> client().unassignLanguageFromProjectByTag(PROJECT_NAME, english()), HttpResponseStatus.FORBIDDEN, "error_missing_perm", projectUuid, InternalPermission.UPDATE_PERM.getRestPerm().getName());
		{
			NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
			nodeCreateRequest.setSchemaName("folder");
			nodeCreateRequest.setLanguage(italian());
			nodeCreateRequest.getFields().put("name", FieldUtil.createStringField("Buon Compleanno!"));
			nodeCreateRequest.setParentNodeUuid(tx(() -> project().getBaseNode().getUuid()));
			call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest, new NodeParametersImpl().setLanguages(italian()), new GenericParametersImpl().setFields("fields", "language")), 
					HttpResponseStatus.FORBIDDEN, "error_missing_perm", projectUuid, InternalPermission.READ_PERM.getRestPerm().getName());
		}
	}

	@Test
	@Override
	public void testReadPermittedSorted() throws Exception {
		LanguageListResponse list = call(() -> client().findLanguages(new SortingParametersImpl("name", SortOrder.DESCENDING)));
		assertEquals("Total data size should be 11", 11, list.getData().size());
		assertThat(list.getData()).isSortedAccordingTo((a, b) -> b.getName().compareTo(a.getName()));
	}
}
