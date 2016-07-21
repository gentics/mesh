package com.gentics.mesh.example;

import static com.gentics.mesh.util.UUIDUtil.randomUUID;

import java.util.HashMap;
import java.util.Map;

import com.gentics.mesh.core.rest.node.PublishStatusModel;
import com.gentics.mesh.core.rest.node.PublishStatusResponse;
import com.gentics.mesh.core.rest.node.VersionReference;
import com.gentics.mesh.core.rest.release.ReleaseCreateRequest;
import com.gentics.mesh.core.rest.release.ReleaseListResponse;
import com.gentics.mesh.core.rest.release.ReleaseResponse;
import com.gentics.mesh.core.rest.release.ReleaseUpdateRequest;
import com.gentics.mesh.core.rest.schema.SchemaReferenceList;
import com.gentics.mesh.core.rest.user.UserReference;

public class VersioningExamples extends AbstractExamples {

	public PublishStatusModel getPublishStatusModel(boolean published, UserReference publisher, Long publishTime, VersionReference version) {
		return new PublishStatusModel().setPublished(published).setPublisher(publisher).setPublishTime(publishTime).setVersion(version);
	}

	public VersionReference getVersionReference(String number) {
		return new VersionReference(randomUUID(), number);
	}

	public PublishStatusResponse getPublishStatusResponse() {
		PublishStatusResponse response = new PublishStatusResponse();
		Map<String, PublishStatusModel> languages = new HashMap<>();
		languages.put("en", getPublishStatusModel(true, getUserReference(), getTimestamp(), getVersionReference("3.0")));
		languages.put("de", getPublishStatusModel(false, null, null, getVersionReference("0.4")));
		languages.put("fr", getPublishStatusModel(false, null, null, getVersionReference("5.2")));
		response.setAvailableLanguages(languages);
		return response;
	}

	public ReleaseListResponse getReleaseListResponse() {
		ReleaseListResponse releaseList = new ReleaseListResponse();
		releaseList.getData().add(getReleaseResponse("summer2016"));
		releaseList.getData().add(getReleaseResponse("autumn2016"));
		setPaging(releaseList, 1, 10, 2, 20);
		return releaseList;
	}

	public ReleaseCreateRequest getReleaseCreateRequest(String name) {
		ReleaseCreateRequest create = new ReleaseCreateRequest();
		create.setName(name);
		return create;
	}

	public ReleaseUpdateRequest getReleaseUpdateRequest(String name) {
		ReleaseUpdateRequest update = new ReleaseUpdateRequest();
		update.setName(name);
		update.setActive(false);
		return update;
	}


	public PublishStatusModel getPublishStatusModel() {
		return getPublishStatusModel(true, getUserReference(), getTimestamp(), getVersionReference("3.0"));
	}

	public ReleaseResponse getReleaseResponse(String name) {
		ReleaseResponse response = new ReleaseResponse();
		response.setName(name);
		response.setUuid(randomUUID());
		response.setActive(true);
		response.setCreated(getTimestamp());
		response.setCreator(getUserReference());
		response.setEdited(getTimestamp());
		response.setEditor(getUserReference());
		response.setMigrated(true);
		response.setPermissions("READ", "UPDATE", "DELETE", "CREATE");
		response.setRolePerms("READ", "UPDATE", "DELETE", "CREATE");
		return response;
	}
}
