package com.gentics.mesh.search.migration;

import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.search.AbstractNodeSearchEndpointTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.TestSize.FULL;

@MeshTestSetting(testSize = FULL, startServer = true)
public class NodeMigrationSearchTrackingTest extends AbstractNodeSearchEndpointTest {
	@Test
	public void testMigrationRequests() {
		String schema = "folder";
		long nodeCount = findNodesBySchema(schema).count().blockingGet();

		invokeSchemaMigration(schema).blockingAwait();
		waitForSearchIdleEvent();

		// It should delete and create documents during the migration.
		assertThat(trackingSearchProvider()).hasSymmetricNodeRequests();
	}

	private Completable invokeSchemaMigration(String schemaName) {
		return findSchemaByName(schemaName)
			.flatMapCompletable(schema -> client().updateSchema(schema.getUuid(), addRandomField(schema)).toCompletable());
	}

	private SchemaUpdateRequest addRandomField(SchemaResponse schemaResponse) {
		SchemaUpdateRequest request = schemaResponse.toUpdateRequest();
		request.getFields().add(new StringFieldSchemaImpl().setName(RandomStringUtils.randomAlphabetic(10)));
		return request;
	}

	private Single<SchemaResponse> findSchemaByName(String schemaName) {
		return fetchList(client().findSchemas())
			.filter(schema -> schema.getName().equals(schemaName))
			.singleOrError();
	}

	private <T> Observable<T> fetchList(MeshRequest<? extends ListResponse<T>> request) {
		return request.toObservable().flatMap(response -> Observable.fromIterable(response.getData()));
	}

}
