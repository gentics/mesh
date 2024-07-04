package com.gentics.mesh.plugin.favorite;

import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.plugin.favorite.client.AdminClient;
import com.gentics.mesh.plugin.favorite.client.UserClient;
import com.gentics.mesh.plugin.favorite.config.FavoritePluginConfig;
import com.gentics.mesh.plugin.favorite.error.FavoriteNotAllowedError;
import com.gentics.mesh.plugin.favorite.error.TypeNotFoundError;
import com.gentics.mesh.plugin.favorite.model.FavoriteResponse;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.MeshRestClientMessageException;
import com.gentics.mesh.rest.client.MeshWebrootResponse;
import com.gentics.mesh.rest.client.impl.EmptyResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

@Singleton
public class FavoriteService {
	private static final Logger log = LoggerFactory.getLogger(FavoriteService.class);

	private final MeshRestClient adminClient;

	private final MeshRestClient userClient;

	private final FavoritePluginConfig configuration;

	public static final String PLUGIN_SCHEMA_NAME = "favorite_folder";

	public static final String SECTION_SCHEMA_NAME = "favorite_section";

	public static final String FAVORITE_SCHEMA_NAME = "favorite";

	private final String FAVORITE_FIELD = "favorites";
	public static final String FAVORITE_PLUGIN_NAME = "favorite_plugin";

	protected static <T> T ifNotFound(Throwable t, Supplier<T> notFoundHandler) {
		if (t instanceof MeshRestClientMessageException) {
			MeshRestClientMessageException meshException = ((MeshRestClientMessageException) t);
			if (meshException.getStatusCode() == HttpResponseStatus.NOT_FOUND.code()) {
				return notFoundHandler.get();
			}
		}
		throw new RuntimeException(t);

	}

	@Inject
	public FavoriteService(@AdminClient MeshRestClient adminClient, @UserClient @Nullable MeshRestClient userClient, FavoritePluginConfig configuration) {
		this.adminClient = adminClient;
		this.userClient = userClient;
		this.configuration = configuration;
	}

	/**
	 * Post a Favorite for a type and this user
	 *
	 * @param projectName project name
	 * @param type        favorite type
	 * @param favoriteID  request favoriteID
	 * @return single node response of the created/updated favorite node
	 */
	public Single<NodeResponse> postFavorite(String projectName, String type, String favoriteID) {
		if (!configuration.getTypeOptions().containsKey(type)) {
			throw new TypeNotFoundError(type);
		}
		JsonArray value = new JsonArray();
		value.add(favoriteID);
		return ifLoggedIn(user -> {
			return getUserFavorite(projectName, type)
					.flatMap(favorite -> updateUserFavorite(projectName, favorite, favoriteID, type))
					.onErrorResumeNext(t -> ifNotFound(t, () -> createUserFavorites(projectName, type, value)));
		}, user -> Single.error(new FavoriteNotAllowedError()));
	}

	/**
	 * Override current Favorite for a type and current user
	 *
	 * @param projectName project name
	 * @param type        favorite type
	 * @param body        request body
	 * @return single node response of the created/updated favorite node
	 */
	public Single<NodeResponse> overrideFavorite(String projectName, String type, JsonObject body) {
		if (!configuration.getTypeOptions().containsKey(type)) {
			throw new TypeNotFoundError(type);
		}
		return ifLoggedIn(user -> {
			JsonArray favorites = body.getJsonArray(FAVORITE_FIELD);
			return getUserFavorite(projectName, type)
					.flatMap(favorite -> overrideUserFavorite(projectName, favorite, body, type))
					.onErrorResumeNext(t -> ifNotFound(t, () -> createUserFavorites(projectName, type, favorites)));
		}, user -> Single.error(new FavoriteNotAllowedError()));
	}

	/**
	 * Get favorite list of the user and given type
	 *
	 * @param projectName project name
	 * @param type        favorite type
	 * @return single favoriteResponse
	 */
	public Single<FavoriteResponse> getFavorite(String projectName, String type) {
		if (!configuration.getTypeOptions().containsKey(type)) {
			throw new TypeNotFoundError(type);
		}
		return ifLoggedIn(user -> {
			FavoriteResponse response = new FavoriteResponse();
			StringFieldListImpl emptyFavorites = new StringFieldListImpl();
			return getUserFavorite(projectName, type).flatMap(favorite -> {
				StringFieldListImpl valueField = favorite.getFields().getStringFieldList(FAVORITE_FIELD);
				response.setFavorites(valueField.getItems());
				return Single.just(response);
			}).onErrorResumeNext(t -> ifNotFound(t, () -> Single.just(response.setFavorites(emptyFavorites.getItems()))));
		}, user -> {
			StringFieldListImpl favorites = new StringFieldListImpl();
			FavoriteResponse response = new FavoriteResponse().setFavorites(favorites.getItems());
			return Single.just(response);
		});
	}

	/**
	 * Delete the favorites for current user and type.
	 *
	 * @param projectName project name
	 * @param type        favorite type
	 * @return single void
	 */
	public Single<EmptyResponse> deleteFavorite(String projectName, String type) {
		return ifLoggedIn(
				user -> getUserFavorite(projectName, type).flatMap(favorite -> delete(projectName, favorite)),
				user -> Single.error(new FavoriteNotAllowedError()));
	}


	/**
	 * Get the section node for this favorite type Creates the section node, if it does not exist
	 *
	 * @param projectName project name
	 * @param type        favorite type
	 * @return single section node
	 */
	protected Single<NodeResponse> getSection(String projectName, String type) {
		return adminClient.webroot(projectName, new String[]{FAVORITE_PLUGIN_NAME, type})
				.toSingle()
				.doAfterSuccess(MeshWebrootResponse::close)
				.map(MeshWebrootResponse::getNodeResponse)
				.onErrorResumeNext(t -> ifNotFound(t, () -> createSection(projectName, type)));
	}


	/**
	 * Create the favorite type-section node
	 *
	 * @param projectName project name
	 * @param type        favorite type
	 * @return single node response
	 */
	protected Single<NodeResponse> createSection(String projectName, String type) {
		return adminClient.webroot(projectName, "/" + FAVORITE_PLUGIN_NAME).toSingle().doAfterSuccess(MeshWebrootResponse::close).flatMap(it -> {
			NodeCreateRequest create = new NodeCreateRequest();
			create.setSchemaName(SECTION_SCHEMA_NAME);
			create.setLanguage("en");
			create.setParentNodeUuid(it.getNodeUuid());
			create.getFields().put("type", new StringFieldImpl().setString(type));
			return adminClient.createNode(projectName, create).toSingle();
		});

	}
	/**
	 * Get the favorites for this type and the current user
	 *
	 * @param projectName project name
	 * @param type        favorite type
	 * @return single favorite node
	 */
	protected Single<NodeResponse> getUserFavorite(String projectName, String type) {
		return getUser().flatMap(user ->
				adminClient.webroot(projectName, new String[]{FAVORITE_PLUGIN_NAME, type, user.getUuid()})
						.toSingle()
						.doAfterSuccess(MeshWebrootResponse::close)
						.doOnSubscribe(disp -> log.debug(String.format("Loading favorite section for type %s, user %s in project %s", user.getUuid(), type, projectName)))
						.map(MeshWebrootResponse::getNodeResponse));
	}

	/**
	 * Create the favorite for this type for current user
	 *
	 * @param projectName project name
	 * @param type        favorite type
	 * @param value       favorite value
	 * @return single node response of the favorite node
	 */
	protected Single<NodeResponse> createUserFavorites(String projectName, String type, JsonArray value) {
		return Single.zip(getSection(projectName, type), getUser(), (section, user) -> {
			NodeCreateRequest create = new NodeCreateRequest();
			create.setSchemaName(FAVORITE_SCHEMA_NAME);
			create.setLanguage("en");
			create.setParentNodeUuid(section.getUuid());
			StringFieldListImpl list = new StringFieldListImpl();

			create.getFields().put("userUuid", new StringFieldImpl().setString(user.getUuid()));
			for (Object o : value) {
				this.addFavorite(list, o.toString(), type);
			}
			create.getFields().put(FAVORITE_FIELD, list);
			return adminClient.createNode(projectName, create).toSingle();
		}).flatMap(x -> x);
	}

	/**
	 * Update favorite for current user and type
	 *
	 * @param projectName project name
	 * @param favorite    previous loaded favorite
	 * @param value       favorite value
	 * @return single node response of the (updated) favorite node
	 */
	protected Single<NodeResponse> updateUserFavorite(String projectName, NodeResponse favorite, String value, String type) {
		StringFieldListImpl list = favorite.getFields().getStringFieldList(FAVORITE_FIELD);
		this.addFavorite(list, value, type);
		favorite.getFields().put(FAVORITE_FIELD, list);

		return updateUserFavoriteNode(projectName, favorite);
	}

	/**
	 * Update favorite for current user and type
	 *
	 * @param projectName project name
	 * @param favorite    previous loaded favorite
	 * @param value       favorite value
	 * @return single node response of the (updated) favorite node
	 */
	protected Single<NodeResponse> overrideUserFavorite(String projectName, NodeResponse favorite, JsonObject value, String type) {
		StringFieldListImpl list = new StringFieldListImpl();
		JsonArray favorites = value.getJsonArray(FAVORITE_FIELD);
		for (Object singleFavorite : favorites) {
			this.addFavorite(list, singleFavorite.toString(), type);

		}
		favorite.getFields().put(FAVORITE_FIELD, list);
		return updateUserFavoriteNode(projectName, favorite);
	}

	/**
	 * Update mesh node with given node
	 *
	 * @param projectName projectName
	 * @param favorite    mesh node
	 * @return single node response of the (updated) favorite node
	 */
	private Single<NodeResponse> updateUserFavoriteNode(String projectName, NodeResponse favorite) {

		NodeUpdateRequest update = new NodeUpdateRequest();

		update.setLanguage(favorite.getLanguage());
		update.setVersion(favorite.getVersion());
		update.setFields(favorite.getFields());

		return adminClient.updateNode(projectName, favorite.getUuid(), update).toSingle();
	}

	/**
	 * Add favorite entry to current list
	 * <p>
	 * when uniqueEntries from config is true no duplicate values will be saved
	 * when maxEntries from config is configured and there are too many entries - the oldest entry
	 *
	 * @param list     list with current favorite entries
	 * @param favorite new favorite entry
	 */
	protected void addFavorite(StringFieldListImpl list, String favorite, String type) {
		String listOrdering = this.configuration.getTypeOptions().get(type).getAddToList();
		int maxEntries = this.configuration.getTypeOptions().get(type).getmaxEntries();
		boolean allowDuplicates = this.configuration.getTypeOptions().get(type).hasAllowDuplicates();
		boolean reorderDuplicates = this.configuration.getTypeOptions().get(type).hasReorderDuplicates();

		List<String> items = list.getItems();
		//if the favorite is already in the list, allow duplicates is set to false, and reorder duplicates is set to false then do nothing with the favorite.
		if (!allowDuplicates && !reorderDuplicates && items.contains(favorite)) {
			return;
		}

		//check if the reorder duplicates is set to true and allow duplicates is set to false. Then delete all the currencies of the favorite so we can add the favorite again according to the policy: prepend or append.
		if (!allowDuplicates && reorderDuplicates) {
			list.getItems().removeAll(Collections.singleton(favorite));
		}
		//if duplicates are allowed, or the element is not found at all, or the element was removed because the policy is set o reorder duplicates, then add the favorite according to the policy: prepend or append.
		//if max Entries are reached - delete oldest (last) entry
		if ("prepend".equalsIgnoreCase(listOrdering)) {
			if (maxEntries > 0 && items.size() >= maxEntries) {
				items.remove(items.size() - 1);
			}
			items.add(0, favorite);

		} else {
			//if max Entries are reached - delete newest (first) entry
			if (maxEntries > 0 && items.size() >= maxEntries) {
				items.remove(0);
			}
			items.add(favorite);
		}

		list.setItems(items);
	}


	/**
	 * Delete the favorite node.
	 *
	 * @param projectName project name
	 * @param favorite    favorite node
	 * @return single void
	 */
	protected Single<EmptyResponse> delete(String projectName, NodeResponse favorite) {
		return adminClient.deleteNode(projectName, favorite.getUuid()).toSingle();
	}

	/**
	 * Get the user.
	 *
	 * @return single user
	 */
	protected Single<UserResponse> getUser() {
		return userClient.me().toSingle();
	}

	protected <T> Single<T> ifLoggedIn(Function<UserResponse, Single<T>> loggedInHandler, Function<UserResponse, Single<T>> anonymousHandler) {
		return userClient.me().toSingle().flatMap(user -> {
			if (user.getUsername().equals("anonymous")) {
				return anonymousHandler.apply(user);
			} else {
				return loggedInHandler.apply(user);
			}
		});
	}


	/**
	 * Delete the favorite of current user and given type+value
	 *
	 * @param projectName project name
	 * @param type        type
	 * @param value       the value to delete
	 * @return single void
	 */
	public Single<Object> deleteFavorite(String projectName, String type, String value) {
		if (!configuration.getTypeOptions().containsKey(type)) {
			throw new TypeNotFoundError(type);
		}
		return ifLoggedIn(user -> getUserFavorite(projectName, type)
						.flatMap(favorite -> {
							StringFieldListImpl list = favorite.getFields().getStringFieldList(FAVORITE_FIELD);
							list.getItems().removeAll(Collections.singleton(value));
							favorite.getFields().put(FAVORITE_FIELD, list);

							return updateUserFavoriteNode(projectName, favorite);
						}),
				user -> Single.error(new FavoriteNotAllowedError()));
	}
}
