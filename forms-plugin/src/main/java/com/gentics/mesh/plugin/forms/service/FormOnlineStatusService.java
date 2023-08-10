package com.gentics.mesh.plugin.forms.service;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.VersioningParameters;
import com.gentics.mesh.parameter.client.GenericParametersImpl;
import com.gentics.mesh.parameter.client.NodeParametersImpl;
import com.gentics.mesh.parameter.client.VersioningParametersImpl;
import com.gentics.mesh.plugin.common.dagger.UserClient;
import com.gentics.mesh.plugin.forms.FormsError;
import com.gentics.mesh.plugin.forms.model.response.FormOnlineResponse;
import com.gentics.mesh.rest.client.MeshRestClient;

import io.reactivex.Single;

/**
 * Service for handling the online status of a form
 */
@Singleton
public class FormOnlineStatusService {
	private final MeshRestClient userClient;

	/**
	 * Name of the field holding the online status
	 */
	public final static String FIELD_NAME = "online";

	private final static VersioningParameters PUBLISHED_VERSION = new VersioningParametersImpl().setVersion("published");

	private final static VersioningParameters DRAFT_VERSION = new VersioningParametersImpl().setVersion("draft");

	private final static GenericParameters GET_FIELDS = new GenericParametersImpl().setETag(false).setFields("uuid", "fields");

	/**
	 * Check whether the given node is an online form
	 * @param node form node
	 * @return true for online form
	 */
	public static boolean isOnline(NodeResponse node) {
		if (node.getFields() == null) {
			return false;
		} else if (node.getFields().getBooleanField(FIELD_NAME) == null) {
			return false;
		} else {
			return node.getFields().getBooleanField(FIELD_NAME).getValue();
		}
	}

	@Inject
	public FormOnlineStatusService(@UserClient @Nullable MeshRestClient userClient) {
		this.userClient = userClient;
	}

	/**
	 * Get the online status of a form
	 * @param projectName project name
	 * @param uuid form uuid
	 * @param language language
	 * @return single response
	 */
	public Single<FormOnlineResponse> get(String projectName, String uuid, String language) {
		return userClient
				.findNodeByUuid(projectName, uuid, PUBLISHED_VERSION, GET_FIELDS,
						new NodeParametersImpl().setLanguages(language))
				.toSingle()
				.flatMap(node -> {
					// if the form node was returned without fields, that means that the language does not exist
					if (node.getFields() == null) {
						String message = String.format("Form with uuid \"%s\" does not have language \"%s\".", uuid, language);
						return Single.error(new FormsError(message, 404));
					} else {
						return Single.just(new FormOnlineResponse().setOnline(isOnline(node)));
					}
				});
	}

	/**
	 * Set the form online
	 * @param projectName project name
	 * @param uuid form uuid
	 * @param language language
	 * @return single response
	 */
	public Single<FormOnlineResponse> setOnline(String projectName, String uuid, String language) {
		// first check the current status
		return get(projectName, uuid, language).flatMap(status -> {
			if (status.isOnline()) {
				// if the form is already online, no need to change anything
				return Single.just(status);
			} else {
				// update the "online" flag, republish the node and return with the new status
				NodeUpdateRequest request = new NodeUpdateRequest();
				request.getFields().put("online", new BooleanFieldImpl().setValue(true));
				request.setLanguage(language);
				request.setVersion("draft");

				return userClient.updateNode(projectName, uuid, request, DRAFT_VERSION, GET_FIELDS).toCompletable()
						.andThen(userClient.publishNode(projectName, uuid).toCompletable())
						.andThen(get(projectName, uuid, language));
			}
		});
	}

	/**
	 * Set the form offline
	 * @param projectName project name
	 * @param uuid form uuid
	 * @param language language
	 * @return single response
	 */
	public Single<FormOnlineResponse> setOffline(String projectName, String uuid, String language) {
		// first check the current status
		return get(projectName, uuid, language).flatMap(status -> {
			if (!status.isOnline()) {
				// if the form is already offline, no need to change anything
				return Single.just(status);
			} else {
				// update the "online" flag, republish the node and return with the new status
				NodeUpdateRequest request = new NodeUpdateRequest();
				request.getFields().put("online", new BooleanFieldImpl().setValue(false));
				request.setLanguage(language);
				request.setVersion("draft");

				return userClient.updateNode(projectName, uuid, request, DRAFT_VERSION, GET_FIELDS).toCompletable()
						.andThen(userClient.publishNode(projectName, uuid).toCompletable())
						.andThen(get(projectName, uuid, language));
			}
		});
	}
}
