package com.gentics.mesh.plugin.like.service;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.core.rest.common.Permission;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NodeFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.client.NodeParametersImpl;
import com.gentics.mesh.parameter.client.PagingParametersImpl;
import com.gentics.mesh.plugin.like.LikePlugin;
import com.gentics.mesh.plugin.like.config.LikePluginConfig;
import com.gentics.mesh.plugin.like.dagger.AdminClient;
import com.gentics.mesh.plugin.like.dagger.UserClient;
import com.gentics.mesh.plugin.like.error.InvalidLikeError;
import com.gentics.mesh.plugin.like.error.LikesNotActivatedError;
import com.gentics.mesh.plugin.like.error.LikesNotAllowedError;
import com.gentics.mesh.plugin.like.model.LikeConfig;
import com.gentics.mesh.plugin.like.model.LikeResponse;
import com.gentics.mesh.plugin.like.model.PlusMinus;
import com.gentics.mesh.plugin.like.model.PlusMinusType;
import com.gentics.mesh.plugin.like.model.PostedLike;
import com.gentics.mesh.plugin.like.model.Rating;
import com.gentics.mesh.plugin.like.model.StatisticsResponse;
import com.gentics.mesh.plugin.like.model.Type;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.MeshRestClientMessageException;
import com.gentics.mesh.rest.client.MeshWebrootResponse;
import com.gentics.mesh.rest.client.impl.EmptyResponse;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Singleton
public class LikeService {
	private static final Logger log = LoggerFactory.getLogger(LikeService.class);

	private final MeshRestClient adminClient;

	private final MeshRestClient userClient;

	private final LikePluginConfig configuration;

	private final ObjectMapper objectMapper;

	private final SchemaService schemaService;

	private Single<UserResponse> user;

	private final CachingService cachingService;

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
	public LikeService(@AdminClient MeshRestClient adminClient, @UserClient @Nullable MeshRestClient userClient, LikePluginConfig configuration,
			ObjectMapper objectMapper, SchemaService schemaService, CachingService cachingService) {
		this.adminClient = adminClient;
		this.userClient = userClient;
		this.configuration = configuration;
		this.objectMapper = objectMapper;
		this.schemaService = schemaService;
		this.cachingService = cachingService;
	}

	/**
	 * Post a like for a node
	 * @param projectName project name
	 * @param nodeUuid node UUID
	 * @param lang language
	 * @param body request body
	 * @return single node response of the created/updated like node
	 */
	public Single<NodeResponse> postLike(String projectName, String nodeUuid, String lang, String body) {
		return ifLoggedIn(user -> getConfig(projectName, nodeUuid, lang).flatMap(config -> {
			if (config.isActive()) {
				PlusMinus plusMinus = validatePlusMinus(config, body);
				Rating rating = validateRating(config, body);
				return getUserLike(projectName, nodeUuid, lang)
						.flatMap(like -> updateUserLike(projectName, like, plusMinus, rating))
						.onErrorResumeNext(t -> ifNotFound(t, () -> createUserLike(projectName, nodeUuid, lang, plusMinus, rating)));
			} else {
				return Single.error(new LikesNotActivatedError());
			}
		}), user -> Single.error(new LikesNotAllowedError()));
	}

	/**
	 * Get the like of the user
	 * @param projectName project name
	 * @param nodeUuid node UUID
	 * @param lang language
	 * @return singe like response
	 */
	public Single<LikeResponse> getLike(String projectName, String nodeUuid, String lang) {
		return ifLoggedIn(user -> {
			return getConfig(projectName, nodeUuid, lang).flatMap(config -> {
				LikeResponse response = new LikeResponse();
				response.setConfiguration(config);
				if (config.isActive()) {
					return getUserLike(projectName, nodeUuid, lang).flatMap(like -> {
						switch (config.getType()) {
						case like:
							response.setPosted(new PostedLike().setLike(true));
							break;
						case plusminus:
							BooleanFieldImpl plusminusField = like.getFields().getBooleanField("plusminus");
							if (plusminusField != null) {
								response.setPosted(new PostedLike().setPlusMinus(plusminusField.getValue() ? PlusMinusType.plus : PlusMinusType.minus));
							}
							break;
						case rating:
							NumberFieldImpl ratingField = like.getFields().getNumberField("rating");
							if (ratingField != null) {
								response.setPosted(new PostedLike().setRating(ratingField.getNumber().intValue()));
							}
							break;
						}

						return Single.just(response);
					}).onErrorResumeNext(t -> ifNotFound(t, () -> Single.just(response)))
							.flatMap(likeResponse -> addStatistics(projectName, nodeUuid, lang, likeResponse));
				} else {
					return Single.just(response);
				}
			});
		}, user -> {
			LikeResponse response = new LikeResponse();
			response.setConfiguration(new LikeConfig());
			return Single.just(response);
		});
	}

	/**
	 * Delete the like of a user
	 * @param projectName project name
	 * @param nodeUuid node UUID
	 * @param lang language
	 * @return single void
	 */
	public Single<EmptyResponse> deleteLike(String projectName, String nodeUuid, String lang) {
		return ifLoggedIn(user -> getConfig(projectName, nodeUuid, lang).flatMap(config -> {
			if (config.isActive()) {
				return getUserLike(projectName, nodeUuid, lang)
					.flatMap(like -> delete(projectName, like));
			} else {
				return Single.error(new LikesNotActivatedError());
			}
		}), user -> Single.error(new LikesNotAllowedError()));
	}

	/**
	 * Get the statistics for a node
	 * @param projectName project name
	 * @param nodeUuid node UUID
	 * @param lang language
	 * @return single statistics
	 */
	public Single<StatisticsResponse> getStatistics(String projectName, String nodeUuid, String lang) {
		return getConfig(projectName, nodeUuid, lang).flatMap(config -> {
				if (config.getType() != null) {
					return getSection(projectName, nodeUuid, lang)
						.flatMap(section -> {
							return cachingService.get(getStatsCacheKey(section.getUuid()), key -> getSectionLikes(projectName, section)
									.flatMap(likes -> getStatsResponse(config, likes)));
						});
				} else {
					return Single.just(new StatisticsResponse());
				}
			});
	}

	/**
	 * Add the statistics to the like response
	 * @param projectName project name
	 * @param nodeUuid node UUID
	 * @param lang language
	 * @param likeResponse like response
	 * @return single like response (with statistics)
	 */
	protected Single<LikeResponse> addStatistics(String projectName, String nodeUuid, String lang, LikeResponse likeResponse) {
		return getStatistics(projectName, nodeUuid, lang).flatMap(stats -> {
			return Single.just(likeResponse.setStatistics(stats));
		});
	}

	/**
	 * Get the like configuration for the given node.
	 * If the node does not exist, or does not have the config field, or the config field is not filled with proper
	 * configuration, this returns a LikeConfig that will return false fir {@link LikeConfig#isActive()}.
	 * @param projectName project name
	 * @param nodeUuid node UUID
	 * @param lang language
	 * @return single like config
	 */
	protected Single<LikeConfig> getConfig(String projectName, String nodeUuid, String lang) {
		return adminClient.findNodeByUuid(projectName, nodeUuid, new NodeParametersImpl().setLanguages(lang)).toSingle()
				.doOnSubscribe(disp -> log.debug(String.format("Loading node %s in project %s", nodeUuid, projectName)))
				.doOnSuccess(node -> String.format("Found node %s in project %s", nodeUuid, projectName))
				.map(node -> {
					return getConfig(node);
				}).onErrorResumeNext(t -> {
					log.error(String.format("Did not find node %s in project %s", nodeUuid, projectName));
					return ifNotFound(t, () -> Single.just(new LikeConfig()));
				});
	}

	/**
	 * Get the section node for likes attached to the given node. Creates the section node, if it does not exist
	 * @param projectName project name
	 * @param nodeUuid node UUID
	 * @param lang language
	 * @return single section node
	 */
	protected Single<NodeResponse> getSection(String projectName, String nodeUuid, String lang) {
		return adminClient.webroot(projectName, new String[] {LikePlugin.NAME, getSectionSegment(nodeUuid, lang)})
			.toSingle()
			.doAfterSuccess(MeshWebrootResponse::close)
			.map(MeshWebrootResponse::getNodeResponse)
			.onErrorResumeNext(t -> {
				return ifNotFound(t, () -> createSection(projectName, nodeUuid, lang));
			});
	}

	/**
	 * Check whether the user has the permission on the node
	 * @param projectName project name
	 * @param nodeUuid node UUID
	 * @param perm required permission
	 * @return single boolean
	 */
	protected Single<Boolean> checkPermission(String projectName, String nodeUuid, Permission perm) {
		return getUser()
			.flatMap(user -> adminClient.readUserPermissions(user.getUuid(), String.format("%s/nodes/%s", projectName, nodeUuid)).toSingle())
			.flatMap(permResponse -> Single.just(permResponse.get(perm)));
	}

	/**
	 * Check whether the user has the permission on the node
	 * @param projectName project name
	 * @param nodeUuid node UUID
	 * @param perm required permission
	 * @param grantedHandler handle granted permission
	 * @param deniedHandler handle denied permission
	 * @return single response
	 */
	protected <T> Single<T> ifPermission(String projectName, String nodeUuid, Permission perm, Function<Boolean, Single<T>> grantedHandler,
			Function<Boolean, Single<T>> deniedHandler) {
		return checkPermission(projectName, nodeUuid, perm).flatMap(granted -> {
			if (granted) {
				return grantedHandler.apply(granted);
			} else {
				return deniedHandler.apply(granted);
			}
		});
	}

	/**
	 * Create the like section node
	 * @param projectName project name
	 * @param nodeUuid node UUID
	 * @param lang language
	 * @return single node response
	 */
	protected Single<NodeResponse> createSection(String projectName, String nodeUuid, String lang) {
		NodeCreateRequest create = new NodeCreateRequest();
		create.setSchemaName(SchemaService.SECTION_SCHEMA_NAME);
		create.setLanguage("en");
		create.setParentNodeUuid(schemaService.getRootNodeUuid(projectName));
		create.getFields().put("nodeUuid", new StringFieldImpl().setString(getSectionSegment(nodeUuid, lang)));
		create.getFields().put("node", new NodeFieldImpl().setUuid(nodeUuid));
		return adminClient.createNode(projectName, create).toSingle();
	}

	/**
	 * Get the like configuration for the node
	 * @param node node
	 * @return like configuration
	 */
	protected LikeConfig getConfig(NodeResponse node) {
		StringFieldImpl configField = node.getFields().getStringField(configuration.getConfigField());
		if (configField == null) {
			log.debug(String.format("Node %s does not have field %s", node.getUuid(), configuration.getConfigField()));
			return new LikeConfig();
		}
		try {
			return objectMapper.readValue(configField.getString(), LikeConfig.class);
		} catch (IOException e) {
			log.error(String.format("Node %s has invalid configuration", node.getUuid()), e);
			return new LikeConfig();
		}
	}

	/**
	 * Get the like of the user for the node
	 * @param projectName project name
	 * @param nodeUuid node UUID
	 * @param lang language
	 * @return single like node
	 */
	protected Single<NodeResponse> getUserLike(String projectName, String nodeUuid, String lang) {
		return getUser().flatMap(user -> {
			return adminClient.webroot(projectName, new String[] {LikePlugin.NAME, getSectionSegment(nodeUuid, lang), user.getUuid()})
					.toSingle()
					.doAfterSuccess(MeshWebrootResponse::close)
					.doOnSubscribe(
							disp -> log.debug(String.format("Loading likes section for node %s, user %s in project %s", nodeUuid, user.getUuid(), projectName)))
					.map(MeshWebrootResponse::getNodeResponse);
		});
	}

	/**
	 * Validate plusminus body
	 * @param config config
	 * @param body posted body
	 * @return plusminus data or null
	 */
	protected PlusMinus validatePlusMinus(LikeConfig config, String body) {
		if (config.getType() == Type.plusminus) {
			PlusMinus plusminus = JsonUtil.readValue(body, PlusMinus.class);
			if (plusminus.getRating() == null) {
				throw new InvalidLikeError("Plusminus must either be true or false");
			}
			return plusminus;
		} else {
			return null;
		}
	}

	/**
	 * Validate rating body
	 * @param config config
	 * @param body posted body
	 * @return rating data or null
	 */
	protected Rating validateRating(LikeConfig config, String body) {
		if (config.getType() == Type.rating) {
			Rating rating = JsonUtil.readValue(body, Rating.class);
			if (rating.getRating() < 1 || rating.getRating() > config.getMaxRating()) {
				throw new InvalidLikeError(String.format("Rating must be between %d and %d", 1, config.getMaxRating()));
			}
			return rating;
		} else {
			return null;
		}
	}

	/**
	 * Create the user like
	 * @param projectName project name
	 * @param nodeUuid node UUID
	 * @param lang language
	 * @param plusMinus optional plusminus data
	 * @param rating optional rating data
	 * @return single node response of the like node
	 */
	protected Single<NodeResponse> createUserLike(String projectName, String nodeUuid, String lang, PlusMinus plusMinus, Rating rating) {
		return Single.zip(getSection(projectName, nodeUuid, lang), getUser(), (section, user) -> {
			NodeCreateRequest create = new NodeCreateRequest();
			create.setSchemaName(SchemaService.LIKE_SCHEMA_NAME);
			create.setLanguage("en");
			create.setParentNodeUuid(section.getUuid());
			create.getFields().put("userUuid", new StringFieldImpl().setString(user.getUuid()));
			if (plusMinus != null) {
				create.getFields().put("plusminus", new BooleanFieldImpl().setValue(plusMinus.getRating() == PlusMinusType.plus));
			}
			if (rating != null) {
				create.getFields().put("rating", new NumberFieldImpl().setNumber(rating.getRating()));
			}
			return adminClient.createNode(projectName, create).toSingle().doOnSuccess(this::invalidateStatsCache);
		}).flatMap(x -> x);
	}

	/**
	 * Update user like
	 * @param projectName project name
	 * @param like old like
	 * @param plusMinus optional plusminus data
	 * @param rating optional rating data
	 * @return single node response of the (updated) like node
	 */
	protected Single<NodeResponse> updateUserLike(String projectName, NodeResponse like, PlusMinus plusMinus, Rating rating) {
		if (plusMinus == null && rating == null) {
			// normal "like" posts need no updates
			return Single.just(like);
		}
		NodeUpdateRequest update = new NodeUpdateRequest();
		update.setLanguage(like.getLanguage());
		update.setVersion(like.getVersion());
		if (plusMinus != null) {
			update.getFields().put("plusminus", new BooleanFieldImpl().setValue(plusMinus.getRating() == PlusMinusType.plus));
		}
		if (rating != null) {
			update.getFields().put("rating", new NumberFieldImpl().setNumber(rating.getRating()));
		}
		return adminClient.updateNode(projectName, like.getUuid(), update).toSingle().doOnSuccess(this::invalidateStatsCache);
	}

	/**
	 * Delete the like
	 * @param projectName project name
	 * @param like like node
	 * @return single void
	 */
	protected Single<EmptyResponse> delete(String projectName, NodeResponse like) {
		return adminClient.deleteNode(projectName, like.getUuid()).toSingle().doOnSuccess(v -> invalidateStatsCache(like));
	}

	/**
	 * Get the user
	 * @return single user
	 */
	protected Single<UserResponse> getUser() {
		if (user == null) {
			user = userClient.me().toSingle().cache();
		}
		return user;
	}

	/**
	 * Get the user and check, whether the user is logged in.
	 * @param loggedInHandler handler for logged in users
	 * @param anonymousHandler handler for the anonymous user
	 * @return single response
	 */
	protected <T> Single<T> ifLoggedIn(Function<UserResponse, Single<T>> loggedInHandler, Function<UserResponse, Single<T>> anonymousHandler) {
		return getUser().flatMap(user -> {
			if (user.getUsername().equals("anonymous")) {
				return anonymousHandler.apply(user);
			} else {
				return loggedInHandler.apply(user);
			}
		});
	}

	/**
	 * Get all likes of a section
	 * @param projectName project name
	 * @param section section
	 * @return single node list response
	 */
	protected Single<NodeListResponse> getSectionLikes(String projectName, NodeResponse section) {
		return adminClient.findNodeChildren(projectName, section.getUuid(), new PagingParametersImpl().setPerPage(Long.MAX_VALUE)).toSingle();
	}

	/**
	 * Get the statistics for the section likes
	 * @param config configuration
	 * @param sectionLikes list of section likes
	 * @return statistics response
	 */
	protected Single<StatisticsResponse> getStatsResponse(LikeConfig config, NodeListResponse sectionLikes) {
		StatisticsResponse response = new StatisticsResponse();

		AtomicInteger likes = new AtomicInteger();
		Map<Integer, AtomicInteger> ratings = null;
		Map<PlusMinusType, AtomicInteger> plusMinus = null;
		switch (config.getType()) {
		case plusminus:
			plusMinus = new TreeMap<>();
			plusMinus.put(PlusMinusType.plus, new AtomicInteger());
			plusMinus.put(PlusMinusType.minus, new AtomicInteger());
			break;
		case rating:
			ratings = new TreeMap<>();
			for (int i = 1; i <= config.getMaxRating(); i++) {
				ratings.put(i, new AtomicInteger());
			}
			break;
		default:
			break;
		}

		for (NodeResponse like : sectionLikes.getData()) {
			likes.incrementAndGet();
			BooleanFieldImpl plusMinusField = like.getFields().getBooleanField("plusminus");
			NumberFieldImpl ratingField = like.getFields().getNumberField("rating");
			if (plusMinusField != null && plusMinus != null) {
				plusMinus.get(plusMinusField.getValue() ? PlusMinusType.plus : PlusMinusType.minus).incrementAndGet();
			}
			if (ratingField != null && ratings != null) {
				int rating = ratingField.getNumber().intValue();
				ratings.computeIfAbsent(rating, AtomicInteger::new).incrementAndGet();
			}
		}

		response.setLikes(likes.get());
		if (plusMinus != null) {
			response.setPlusMinus(toTreeMap(plusMinus));
		}
		if (ratings != null) {
			response.setRatings(toTreeMap(ratings));
		}

		return Single.just(response);
	}

	protected <T> Map<T, Integer> toTreeMap(Map<T, AtomicInteger> map) {
		Map<T, Integer> treeMap = new TreeMap<>();
		for (Map.Entry<T, AtomicInteger> entry : map.entrySet()) {
			treeMap.put(entry.getKey(), entry.getValue().get());
		}
		return treeMap;
	}

	/**
	 * Get the like section segment field value
	 * @param nodeUuid node UUID
	 * @param lang language
	 * @return segment field value
	 */
	protected String getSectionSegment(String nodeUuid, String lang) {
		return nodeUuid + "_" + lang;
	}

	/**
	 * Invalidate the cache for the statistics after the like has been saved
	 * @param like like
	 */
	protected void invalidateStatsCache(NodeResponse like) {
		cachingService.invalidate(getStatsCacheKey(like.getParentNode().getUuid()));
	}

	/**
	 * Create the cache key for the statistics
	 * @param sectionUuid section Node UUID
	 * @return cache key
	 */
	protected String getStatsCacheKey(String sectionUuid) {
		return String.format("stats_%s", sectionUuid);
	}
}
