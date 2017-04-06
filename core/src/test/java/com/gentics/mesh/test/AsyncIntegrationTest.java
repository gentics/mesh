package com.gentics.mesh.test;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.rest.group.GroupCreateRequest;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.util.UUIDUtil;

import io.vertx.core.Vertx;
import rx.Observable;

@Ignore
public class AsyncIntegrationTest {

	@Test
	public void testMultipleAsyncCreation() {
		int count = 10;
		String prefix = "testk" + UUIDUtil.randomUUID();

		MeshRestClient client = localEmptyClient();

		Observable.range(1, count).map(i -> prefix + i).flatMap(name -> createGroupRx(client, name)).toCompletable().await();
	}

	private MeshRestClient localEmptyClient() {
		MeshRestClient client = MeshRestClient.create("localhost", 8080, Vertx.vertx());
		client.setLogin("admin", "admin");
		client.login().toCompletable().await();
		return client;
	}

	private Observable<GroupResponse> createGroupRx(MeshRestClient client, String name) {
		GroupCreateRequest req = new GroupCreateRequest();
		req.setName(name);
		return client.createGroup(req).toObservable();
	}

}
