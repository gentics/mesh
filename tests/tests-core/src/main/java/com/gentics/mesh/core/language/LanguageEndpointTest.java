package com.gentics.mesh.core.language;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.ElasticsearchTestMode.TRACKING;
import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.lang.LanguageListResponse;
import com.gentics.mesh.core.rest.lang.LanguageResponse;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.definition.BasicRestTestcases;

import io.reactivex.Observable;

@MeshTestSetting(elasticsearch = TRACKING, testSize = TestSize.FULL, startServer = true)
public class LanguageEndpointTest extends AbstractMeshTest implements BasicRestTestcases {

	@Override
	@Ignore
	@Test
	public void testCreate() throws Exception {
	}

	@Override
	@Ignore
	@Test
	public void testCreateReadDelete() throws Exception {
	}

	@Override
	@Ignore
	@Test
	public void testCreateWithNoPerm() throws Exception {
	}

	@Override
	@Ignore
	@Test
	public void testCreateWithUuid() throws Exception {
	}

	@Override
	@Ignore
	@Test
	public void testCreateWithDuplicateUuid() throws Exception {
	}

	@Override
	@Test
	public void testReadByUUID() throws Exception {
		String englishUuid = tx(tx -> { 
			return tx.languageDao().findByLanguageTag(english()).getUuid();
		});
		LanguageResponse english = call(() -> client().findLanguageByUuid(englishUuid));
		assertEquals(englishUuid, english.getUuid());
		assertEquals(english(), english.getLanguageTag());
	}

	@Override
	@Ignore
	@Test
	public void testReadByUuidWithRolePerms() {
	}

	@Override
	@Ignore
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
	@Ignore
	@Test
	public void testPermissionResponse() {
	}

	@Override
	@Ignore
	@Test
	public void testUpdate() throws Exception {
	}

	@Override
	@Ignore
	@Test
	public void testUpdateByUUIDWithoutPerm() throws Exception {
	}

	@Override
	@Ignore
	@Test
	public void testUpdateWithBogusUuid() throws GenericRestException, Exception {
	}

	@Override
	@Ignore
	@Test
	public void testDeleteByUUID() throws Exception {
	}

	@Override
	@Ignore
	@Test
	public void testDeleteByUUIDWithNoPermission() throws Exception {
	}

	@Override
	@Ignore
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
	@Ignore
	@Test
	public void testDeleteByUUIDMultithreaded() throws Exception {
	}

	@Override
	@Ignore
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
		String englishUuid = tx(tx -> { 
			return tx.languageDao().findByLanguageTag(english()).getUuid();
		});
		LanguageResponse english = call(() -> client().findLanguageByTag(english()));
		assertEquals(englishUuid, english.getUuid());
		assertEquals(english(), english.getLanguageTag());
	}
}
