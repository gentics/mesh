package com.gentics.mesh.plugin.favorite;

import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.parameter.client.DeleteParametersImpl;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.MeshWebrootResponse;
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

	/**
	 * Performs a request to add a favorite value of a specific type in the given project.
	 *
	 * @param type  type of the favorite
	 * @param value value of the favorite
	 * @return a Completable
	 */
	public Completable addFavorite(String type, String value) {
		return client.post(String.format("/%s/plugins/favorite/%s/%s", projectName, type, value), EmptyResponse.class).toCompletable();
	}

	/**
	 * Performs a request to override the favorites of a specific type in the given project.
	 *
	 * @param type   type of the favorite
	 * @param values list of favorite values
	 * @return a Completable
	 */
	public Completable overrideFavorite(String type, String... values) {
		return overrideFavorite(type, Arrays.asList(values));
	}

	//TODO next Mesh Release please change it to post(path, body, EmptyResponse.class)
	public Completable overrideFavorite(String type, List<String> values) {
		JsonObject value = new JsonObject().put("favorites", new JsonArray(values));
		return client.post(String.format("/%s/plugins/favorite/%s", projectName, type), toRestModel(value), EmptyResponse.class).toCompletable();
	}

	private RestModel toRestModel(JsonObject obj) {
		return new RestModel() {
			@Override
			public String toJson() {
				return obj.toString();
			}
		};
	}

	/**
	 * Performs a request to delete a favorite value of a specific type in the given project.
	 *
	 * @param type  type of the favorite
	 * @param value value of the favorite
	 * @return a Completable
	 */
	public Completable deleteFavorite(String type, String value) {
		return client.delete(String.format("/%s/plugins/favorite/%s/%s", projectName, type, value), EmptyResponse.class).toCompletable();
	}

	/**
	 * Performs a request to retrieve the favorites of a specific type in the given project.
	 *
	 * @param type type of the favorite
	 * @return List of Strings with all the favorites
	 */
	public Single<List<String>> getFavorites(String type) {
		return client.get(String.format("/%s/plugins/favorite/%s", projectName, type)).toSingle().map(it -> it.getJsonArray("favorites").getList());
	}

	/**
	 * Deletes all the favorites, regardless of the type of the given project.
	 *
	 * @return Completable
	 */
	public Completable deleteAllFavorites() {
		return client.webroot(projectName, "/favorite_plugin").toSingle()
				.doAfterSuccess(MeshWebrootResponse::close)
				.flatMap(it -> client.findNodeChildren(projectName, it.getNodeUuid()).toSingle())
				.flatMapObservable(res -> Observable.fromIterable(res.getData()))
				.flatMapSingle(favorite -> delete(projectName, favorite)).ignoreElements();
	}

	protected Single<EmptyResponse> delete(String projectName, NodeResponse favorite) {
		return client.deleteNode(projectName, favorite.getUuid(), new DeleteParametersImpl().setRecursive(true)).toSingle();
	}

}
