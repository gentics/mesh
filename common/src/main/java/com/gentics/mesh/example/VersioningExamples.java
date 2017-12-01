package com.gentics.mesh.example;

import static com.gentics.mesh.core.rest.common.Permission.CREATE;
import static com.gentics.mesh.core.rest.common.Permission.DELETE;
import static com.gentics.mesh.core.rest.common.Permission.READ;
import static com.gentics.mesh.core.rest.common.Permission.UPDATE;
import static com.gentics.mesh.util.UUIDUtil.randomUUID;

import java.util.HashMap;
import java.util.Map;

import com.gentics.mesh.core.rest.node.PublishStatusModel;
import com.gentics.mesh.core.rest.node.PublishStatusResponse;
import com.gentics.mesh.core.rest.release.ReleaseCreateRequest;
import com.gentics.mesh.core.rest.release.ReleaseListResponse;
import com.gentics.mesh.core.rest.release.ReleaseResponse;
import com.gentics.mesh.core.rest.release.ReleaseUpdateRequest;
import com.gentics.mesh.core.rest.user.UserReference;

public class VersioningExamples extends AbstractExamples {

	/**
	 * Create a publish status model with the given information.
	 * 
	 * @param published
	 * @param publisher
	 * @param publishDate
	 * @param version
	 * @return
	 */
	public PublishStatusModel createPublishStatusModel(boolean published, UserReference publisher, String publishDate, String version) {
		return new PublishStatusModel().setPublished(published).setPublisher(publisher).setPublishDate(publishDate).setVersion(version);
	}

	public PublishStatusResponse createPublishStatusResponse() {
		PublishStatusResponse response = new PublishStatusResponse();
		Map<String, PublishStatusModel> languages = new HashMap<>();
		languages.put("en", createPublishStatusModel(true, createUserReference(), createTimestamp(), "3.0"));
		languages.put("de", createPublishStatusModel(false, null, null, "0.4"));
		languages.put("fr", createPublishStatusModel(false, null, null, "5.2"));
		response.setAvailableLanguages(languages);
		return response;
	}

	public ReleaseListResponse createReleaseListResponse() {
		ReleaseListResponse releaseList = new ReleaseListResponse();
		releaseList.getData().add(createReleaseResponse("summer2016"));
		releaseList.getData().add(createReleaseResponse("autumn2016"));
		setPaging(releaseList, 1, 10, 2, 20);
		return releaseList;
	}

	public ReleaseCreateRequest createReleaseCreateRequest(String name) {
		ReleaseCreateRequest create = new ReleaseCreateRequest();
		create.setName(name);
		create.setHostname("getmesh.io");
		create.setSsl(true);
		return create;
	}

	public ReleaseUpdateRequest createReleaseUpdateRequest(String name) {
		ReleaseUpdateRequest update = new ReleaseUpdateRequest();
		update.setName(name);
		update.setHostname("getmesh.io");
		update.setSsl(true);
		// update.setActive(false);
		return update;
	}

	public PublishStatusModel createPublishStatusModel() {
		return createPublishStatusModel(true, createUserReference(), createTimestamp(), "3.0");
	}

	/**
	 * Create a dummy release response with the given release name.
	 * 
	 * @param name
	 *            Name of the release
	 * @return Constructed response
	 */
	public ReleaseResponse createReleaseResponse(String name) {
		ReleaseResponse response = new ReleaseResponse();
		response.setName(name);
		response.setUuid(randomUUID());
		// response.setActive(true);
		response.setCreated(createTimestamp());
		response.setCreator(createUserReference());
		response.setEdited(createTimestamp());
		response.setEditor(createUserReference());
		response.setHostname("getmesh.io");
		response.setSsl(true);
		response.setMigrated(true);
		response.setPermissions(READ, UPDATE,  DELETE, CREATE);
		response.setRolePerms(READ, UPDATE,  DELETE, CREATE);
		return response;
	}
}
