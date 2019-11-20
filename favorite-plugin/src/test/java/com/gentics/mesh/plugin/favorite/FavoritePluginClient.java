package com.gentics.mesh.plugin.favorite;

import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.parameter.client.DeleteParametersImpl;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.impl.EmptyResponse;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;
import java.util.List;

public class FavoritePluginClient {

	private final MeshRestClient client;
	private final String projectName;

	public FavoritePluginClient(MeshRestClient client, String projectName) {
		this.client = client;
		this.projectName = projectName;
	}
	// TODO Please change that to post(String, EmptyResponse.class when in MESH Available)

	public Completable addFavorite(String type, String value) {
		return Completable.fromAction(() -> client.post(String.format("/%s/plugins/favorite/%s/%s", projectName, type, value)).blockingAwait());
	}

	public Completable overrideFavorite(String type, String... values) {
		return overrideFavorite(type, Arrays.asList(values));
	}

	public Completable overrideFavorite(String type, List<String> values) {
		JsonObject value = new JsonObject().put("favorites", new JsonArray(values));
		return Completable.fromAction(() -> client.post(String.format("/%s/plugins/favorite/%s", projectName, type), value).blockingAwait());
	}
	public Completable deleteFavorite(String type, String value) {
		return Completable.fromAction(() -> client.delete(String.format("/%s/plugins/favorite/%s/%s", projectName, type, value)).blockingAwait());
	}

	public Single<List<String>> getFavorites(String type) {
		return client.get(String.format("/%s/plugins/favorite/%s", projectName, type)).toSingle().map(it -> it.getJsonArray("favorites").getList());
	}

	public Completable deleteAllFavorites() {
		return client.webroot(projectName, "/favorite_plugin").toSingle()
				.flatMap(it -> client.findNodeChildren(projectName, it.getNodeUuid()).toSingle())
				.flatMapObservable(res -> Observable.fromIterable(res.getData()))
				.flatMapSingle(favorite -> delete(projectName, favorite)).ignoreElements();
	}
	protected Single<EmptyResponse> delete(String projectName, NodeResponse favorite) {
		return client.deleteNode(projectName, favorite.getUuid(), new DeleteParametersImpl().setRecursive(true)).toSingle();
	}

}
