package com.gentics.mesh.example;

import static com.gentics.mesh.core.rest.common.Permission.CREATE;
import static com.gentics.mesh.core.rest.common.Permission.DELETE;
import static com.gentics.mesh.core.rest.common.Permission.READ;
import static com.gentics.mesh.core.rest.common.Permission.UPDATE;
import static com.gentics.mesh.example.ExampleUuids.BRANCH_UUID;
import static com.gentics.mesh.example.ExampleUuids.UUID_1;
import static com.gentics.mesh.example.ExampleUuids.UUID_2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gentics.mesh.core.rest.branch.BranchCreateRequest;
import com.gentics.mesh.core.rest.branch.BranchListResponse;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.branch.BranchUpdateRequest;
import com.gentics.mesh.core.rest.node.PublishStatusModel;
import com.gentics.mesh.core.rest.node.PublishStatusResponse;
import com.gentics.mesh.core.rest.tag.TagReference;
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
		languages.put("en", createPublishStatusModel(true, createUserReference(), createNewTimestamp(), "3.0"));
		languages.put("de", createPublishStatusModel(false, null, null, "0.4"));
		languages.put("fr", createPublishStatusModel(false, null, null, "5.2"));
		response.setAvailableLanguages(languages);
		return response;
	}

	public BranchListResponse createBranchListResponse() {
		BranchListResponse branchList = new BranchListResponse();
		branchList.getData().add(createBranchResponse("summer2016", false));
		branchList.getData().add(createBranchResponse("autumn2016", true));
		setPaging(branchList, 1, 10, 2, 20);
		return branchList;
	}

	public BranchCreateRequest createBranchCreateRequest(String name) {
		BranchCreateRequest create = new BranchCreateRequest();
		create.setName(name);
		create.setHostname("getmesh.io");
		create.setSsl(true);
		return create;
	}

	public BranchUpdateRequest createBranchUpdateRequest(String name) {
		BranchUpdateRequest update = new BranchUpdateRequest();
		update.setName(name);
		update.setHostname("getmesh.io");
		update.setSsl(true);
		// update.setActive(false);
		return update;
	}

	public PublishStatusModel createPublishStatusModel() {
		return createPublishStatusModel(true, createUserReference(), createNewTimestamp(), "3.0");
	}

	/**
	 * Create a dummy branch response with the given branch name.
	 * 
	 * @param name
	 *            Name of the branch
	 * @param latest 
	 * @return Constructed response
	 */
	public BranchResponse createBranchResponse(String name, boolean latest) {
		BranchResponse response = new BranchResponse();
		response.setName(name);
		response.setUuid(BRANCH_UUID);
		// response.setActive(true);
		response.setCreated(createOldTimestamp());
		response.setCreator(createUserReference());
		response.setEdited(createNewTimestamp());
		response.setEditor(createUserReference());
		response.setHostname("getmesh.io");
		response.setSsl(true);
		response.setPathPrefix("my/prefix");
		response.setLatest(latest);

		List<TagReference> tags = new ArrayList<>();
		tags.add(new TagReference().setName("dev").setUuid(UUID_1).setTagFamily("branchTags"));
		tags.add(new TagReference().setName("prod").setUuid(UUID_2).setTagFamily("branchTags"));
		response.setTags(tags);
		response.setMigrated(true);
		response.setPermissions(READ, UPDATE,  DELETE, CREATE);
		response.setRolePerms(READ, UPDATE,  DELETE, CREATE);
		return response;
	}
}
