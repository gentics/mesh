package com.gentics.mesh.plugin.favorite;

import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.plugin.favorite.client.AdminClient;
import com.gentics.mesh.plugin.favorite.client.UserClient;
import com.gentics.mesh.plugin.favorite.config.FavoritePluginConfig;
import com.gentics.mesh.plugin.favorite.error.FavoriteNotAllowedError;
import com.gentics.mesh.plugin.favorite.model.FavoriteResponse;
import com.gentics.mesh.plugin.favorite.model.PostedFavorite;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.MeshRestClientMessageException;
import com.gentics.mesh.rest.client.MeshWebrootResponse;
import com.gentics.mesh.rest.client.impl.EmptyResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

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

	private final SchemaService schemaService;

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
	public FavoriteService(@AdminClient MeshRestClient adminClient, @UserClient @Nullable MeshRestClient userClient, FavoritePluginConfig configuration, SchemaService schemaService) {
		this.adminClient = adminClient;
		this.userClient = userClient;
		this.configuration = configuration;
		this.schemaService = schemaService;
	}

	/**
	 * Post a Favorite for a type and this user
	 *
	 * @param projectName project name
	 * @param type favorite type
	 * @param body request body
	 * @return single node response of the created/updated favorite node
	 */
	public Single<NodeResponse> postFavorite(String projectName, String type, String body) {
		return ifLoggedIn(user -> {
				PostedFavorite posted = JsonUtil.readValue(body, PostedFavorite.class);
				String value = posted.getValue();
				return getUserFavorite(projectName, type)
						.flatMap(favorite -> updateUserFavorite(projectName, favorite, value, false))
						.onErrorResumeNext(t -> ifNotFound(t, () -> createUserFavorite(projectName, type, value)));
		}, user -> Single.error(new FavoriteNotAllowedError()));
	}

	/**
	 * Override current Favorite for a type and current user
	 *
	 * @param projectName project name
	 * @param type favorite type
	 * @param body request body
	 * @return single node response of the created/updated favorite node
	 */
	public Single<NodeResponse> overrideFavorite(String projectName, String type, String body) {
		return ifLoggedIn(user -> {
				PostedFavorite posted = JsonUtil.readValue(body, PostedFavorite.class);
				String value = posted.getValue();
				return getUserFavorite(projectName, type)
						.flatMap(favorite -> updateUserFavorite(projectName, favorite, value, true))
						.onErrorResumeNext(t -> ifNotFound(t, () -> createUserFavorite(projectName, type, value)));
		}, user -> Single.error(new FavoriteNotAllowedError()));
	}

	/**
	 * Get favorite list of the user and given type
	 *
	 * @param projectName project name
	 * @param type favorite type
	 *
	 * @return single favoriteResponse
	 */
	public Single<FavoriteResponse> getFavorite(String projectName, String type) {
		return ifLoggedIn(user -> {
				FavoriteResponse response = new FavoriteResponse();
					return getUserFavorite(projectName, type).flatMap(favorite -> {
						StringFieldListImpl valueField = favorite.getFields().getStringFieldList("value");

						response.setFavorites(valueField.getItems());
						return Single.just(response);
					}).onErrorResumeNext(t -> ifNotFound(t, () -> Single.just(response)));
		}, user -> {
			FavoriteResponse response = new FavoriteResponse();
			return Single.just(response);
		});
	}

	/**
	 * Delete the favorites for current user and type.
	 *
	 * @param projectName project name
	 * @param type favorite type
	 *
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
	 * @param type favorite type
	 *
	 * @return single section node
	 */
	protected Single<NodeResponse> getSection(String projectName, String type) {
		return adminClient.webroot(projectName, new String[] {FavoritePlugin.NAME, getSectionSegment(type)})
				.toSingle()
				.map(MeshWebrootResponse::getNodeResponse)
				.onErrorResumeNext(t -> ifNotFound(t, () -> createSection(projectName, type)));
	}


	/**
	 * Create the favorite type-section node
	 *
	 * @param projectName project name
	 * @param type favorite type
	 *
	 * @return single node response
	 */
	protected Single<NodeResponse> createSection(String projectName, String type) {
		NodeCreateRequest create = new NodeCreateRequest();
		create.setSchemaName(SchemaService.SECTION_SCHEMA_NAME);
		create.setLanguage("en");
		create.setParentNodeUuid(schemaService.getRootNodeUuid(projectName));
		create.getFields().put("type", new StringFieldImpl().setString(getSectionSegment(type)));
		return adminClient.createNode(projectName, create).toSingle();
	}


	/**
	 * Get the favorites for this type and the current user
	 *
	 * @param projectName project name
	 * @param type favorite type
	 *
	 * @return single favorite node
	 */
	protected Single<NodeResponse> getUserFavorite(String projectName, String type) {
		return getUser().flatMap(user ->
			adminClient.webroot(projectName, new String[] {FavoritePlugin.NAME, getSectionSegment(type), user.getUuid()})
				.toSingle()
				.doOnSubscribe(disp -> log.debug(String.format("Loading favorite section for type %s, user %s in project %s", user.getUuid(), type, projectName)))
				.map(MeshWebrootResponse::getNodeResponse));
	}

	/**
	 * Create the favorite for this type for current user
	 *
	 * @param projectName project name
	 * @param type favorite type
	 * @param value favorite value
	 *
	 * @return single node response of the favorite node
	 */
	protected Single<NodeResponse> createUserFavorite(String projectName, String type, String value) {
		return Single.zip(getSection(projectName, type), getUser(), (section, user) -> {
			NodeCreateRequest create = new NodeCreateRequest();
			create.setSchemaName(SchemaService.FAVORITE_SCHEMA_NAME);
			create.setLanguage("en");
			create.setParentNodeUuid(section.getUuid());

			StringFieldListImpl list = new StringFieldListImpl();
			list.add(value);

			create.getFields().put("userUuid", new StringFieldImpl().setString(user.getUuid()));
			create.getFields().put("value", list);
			return adminClient.createNode(projectName, create).toSingle();
		}).flatMap(x -> x);
	}

	/**
	 * Update favorite for current user and type
	 *
	 * @param projectName project name
	 * @param favorite previous loaded favorite
	 * @param value favorite value
	 * @param override overrides current favorite entries
	 *
	 * @return single node response of the (updated) favorite node
	 */
	protected Single<NodeResponse> updateUserFavorite(String projectName, NodeResponse favorite, String value, boolean override) {
		StringFieldListImpl list;

		if(override) {
			list = new StringFieldListImpl();
		}
		else{
			list = favorite.getFields().getStringFieldList("value");
		}


		this.addFavorite(list, value);

		favorite.getFields().put("value", list);

		return updateUserFavoriteNode(projectName, favorite);
	}

	/**
	 * Update mesh node with given node
	 *
	 * @param projectName projectName
	 * @param favorite mesh node
	 *
	 * @return single node response of the (updated) favorite node
	 */
	private Single<NodeResponse> updateUserFavoriteNode(String projectName, NodeResponse favorite){

		NodeUpdateRequest update = new NodeUpdateRequest();

		update.setLanguage(favorite.getLanguage());
		update.setVersion(favorite.getVersion());
		update.setFields(favorite.getFields());

		return adminClient.updateNode(projectName, favorite.getUuid(), update).toSingle();
	}

	/**
	 * Add favorite entry to current list
	 *
	 * when uniqueEntries from config is true no duplicate values will be saved
	 * when maxEntries from config is configured and there are too many entries - the oldest entry
	 *
	 * @param list list with current favorite entries
	 * @param favorite new favorite entry
	 */
	protected void addFavorite(StringFieldListImpl list, String favorite) {
		List<String> items = list.getItems();
		boolean found = false;
		if(this.configuration.hasUniqueEntries()) {
			for(String item : items) {
				if(item.equals(favorite)) {
					found = true;
					break;
				}
			}
		}
		if(!found) {
			//if max Entries are reached - delete oldest (last) entry
			if(this.configuration.getMaxEntriesPerType() > 0 && items.size() >= this.configuration.getMaxEntriesPerType()) {
				items.remove(items.size()-1);
			}
			items.add(0,favorite);
		}

		list.setItems(items);

	}


	/**
	 * Delete the favorite node.
	 *
	 * @param projectName project name
	 * @param favorite favorite node
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
	 * Get the type section segment field value
	 *
	 * @param type node UUID
	 * @return segment field value
	 */
	protected String getSectionSegment(String type) {
		return type;
	}

	/**
	 * Delete the favorite of current user and given type+value
	 *
	 * @param projectName project name
	 * @param type type
	 * @param value  the value to delete
	 *
	 * @return single void
	 */
	public Single<Object> deleteFavorite(String projectName, String type, String value) {
		return ifLoggedIn(user -> getUserFavorite(projectName, type)
			.flatMap(favorite -> {
				StringFieldListImpl list = favorite.getFields().getStringFieldList("value");
				list.getItems().removeAll(Collections.singleton(value));
				favorite.getFields().put("value", list);

				return updateUserFavoriteNode(projectName, favorite);
			}),
			user -> Single.error(new FavoriteNotAllowedError()))	;
	}
}
