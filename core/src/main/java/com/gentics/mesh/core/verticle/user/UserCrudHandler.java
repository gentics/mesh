package com.gentics.mesh.core.verticle.user;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.rest.Messages.message;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.auth.MeshAuthProvider;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.user.UserAPITokenResponse;
import com.gentics.mesh.core.rest.user.UserPermissionResponse;
import com.gentics.mesh.core.rest.user.UserResetTokenResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.util.DateUtils;
import com.gentics.mesh.util.TokenUtil;

import io.reactivex.Single;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Handler which contains methods for user related requests.
 */
@Singleton
public class UserCrudHandler extends AbstractCrudHandler<User, UserResponse> {

	private static final Logger log = LoggerFactory.getLogger(UserCrudHandler.class);

	private BootstrapInitializer boot;

	private MeshAuthProvider authProvider;

	@Inject
	public UserCrudHandler(Database db, BootstrapInitializer boot, HandlerUtilities utils, MeshAuthProvider authProvider) {
		super(db, utils);
		this.boot = boot;
		this.authProvider = authProvider;
	}

	@Override
	public RootVertex<User> getRootVertex(InternalActionContext ac) {
		return boot.userRoot();
	}

	/**
	 * Handle a permission read request.
	 * 
	 * @param ac
	 * @param userUuid
	 * @param pathToElement
	 */
	public void handlePermissionRead(InternalActionContext ac, String userUuid, String pathToElement) {
		validateParameter(userUuid, "error_uuid_must_be_specified");
		validateParameter(pathToElement, "user_permission_path_missing");

		if (log.isDebugEnabled()) {
			log.debug("Handling permission request for element on path {" + pathToElement + "}");
		}
		db.operateTx(() -> {
			// 1. Load the user that should be used - read perm implies that the
			// user is able to read the attached permissions
			User user = boot.userRoot().loadObjectByUuid(ac, userUuid, READ_PERM);

			// 2. Resolve the path to element that is targeted
			MeshVertex targetElement = MeshInternal.get().boot().meshRoot().resolvePathToElement(pathToElement);
			if (targetElement == null) {
				throw error(NOT_FOUND, "error_element_for_path_not_found", pathToElement);
			}
			UserPermissionResponse response = new UserPermissionResponse();

			// 1. Add granted permissions
			for (GraphPermission perm : user.getPermissions(targetElement)) {
				response.set(perm.getRestPerm(), true);
			}

			// 2. Add not granted permissions
			response.setOthers(false);
			return Single.just(response);
		}).subscribe(model -> ac.send(model, OK), ac::fail);

	}

	/**
	 * Handle the fetch token action for the user with the given uuid.
	 * 
	 * @param ac
	 * @param userUuid
	 *            User uuid
	 */
	public void handleFetchToken(InternalActionContext ac, String userUuid) {
		validateParameter(userUuid, "The userUuid must not be empty");

		db.operateTx(() -> {
			// 1. Load the user that should be used
			User user = boot.userRoot().loadObjectByUuid(ac, userUuid, CREATE_PERM);

			// 2. Generate a new token and store it for the user
			UserResetTokenResponse tokenResponse = db.tx(() -> {
				String token = TokenUtil.randomToken();
				Long tokenTimestamp = System.currentTimeMillis();
				user.setResetToken(token);
				user.setResetTokenIssueTimestamp(tokenTimestamp);
				UserResetTokenResponse response = new UserResetTokenResponse();
				String created = DateUtils.toISO8601(tokenTimestamp, 0);
				response.setCreated(created);
				response.setToken(token);
				return response;
			});
			return Single.just(tokenResponse);
		}).subscribe(model -> ac.send(model, CREATED), ac::fail);
	}

	/**
	 * Handle the api key generation action for the user.
	 * 
	 * @param ac
	 * @param userUuid
	 */
	public void handleIssueAPIToken(InternalActionContext ac, String userUuid) {
		validateParameter(userUuid, "The userUuid must not be empty");

		db.operateTx(() -> {
			// 1. Load the user that should be used
			User user = boot.userRoot().loadObjectByUuid(ac, userUuid, UPDATE_PERM);

			// 2. Generate the api key for the user
			UserAPITokenResponse apiKeyRespose = db.tx(() -> {
				String tokenId = TokenUtil.randomToken(); 
				String apiToken = authProvider.generateAPIToken(user, tokenId);
				UserAPITokenResponse response = new UserAPITokenResponse();
				response.setPreviousIssueDate(user.getAPITokenIssueDate());

				// 3. Issue a new token and update the issue timestamp
				user.setAPITokenId(tokenId);
				user.setAPITokenIssueTimestamp();
				response.setToken(apiToken);
				return response;
			});
			return Single.just(apiKeyRespose);
		}).subscribe(model -> ac.send(model, CREATED), ac::fail);
	}

	/**
	 * Delete the stored api key token code in order to invalidate the API key.
	 * 
	 * @param ac
	 * @param userUuid
	 */
	public void handleDeleteAPIToken(InternalActionContext ac, String userUuid) {
		validateParameter(userUuid, "The userUuid must not be empty");

		db.operateTx(() -> {
			// 1. Load the user that should be used
			User user = boot.userRoot().loadObjectByUuid(ac, userUuid, UPDATE_PERM);

			// 2. Generate the api key for the user
			GenericMessageResponse message = db.tx(() -> {
				user.resetAPIToken();
				return message(ac, "api_key_invalidated");
			});
			return Single.just(message);
		}).subscribe(model -> ac.send(model, CREATED), ac::fail);
	}

}
