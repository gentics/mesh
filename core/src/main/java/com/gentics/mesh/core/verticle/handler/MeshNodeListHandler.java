package com.gentics.mesh.core.verticle.handler;

import io.vertx.core.AsyncResult;
import io.vertx.ext.apex.RoutingContext;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.model.MeshNode;
import com.gentics.mesh.core.data.model.Tag;
import com.gentics.mesh.core.data.model.auth.PermissionType;
import com.gentics.mesh.core.data.service.MeshNodeService;
import com.gentics.mesh.core.data.service.RoutingContextService;
import com.gentics.mesh.core.data.service.TagService;
import com.gentics.mesh.core.rest.meshnode.response.NodeListResponse;
import com.gentics.mesh.paging.PagingInfo;
import com.gentics.mesh.util.JsonUtils;
import com.gentics.mesh.util.RestModelPagingHelper;

@Component
public class MeshNodeListHandler {

	@Autowired
	private TagService tagService;

	@Autowired
	private MeshNodeService contentService;

	@Autowired
	private RoutingContextService rcs;

	public void handle(RoutingContext rc, MeshNodeListCallable clc) {
		String projectName = rcs.getProjectName(rc);
		NodeListResponse listResponse = new NodeListResponse();
		List<String> languageTags = rcs.getSelectedLanguageTags(rc);

		rcs.loadObject(rc, "uuid", PermissionType.READ, (AsyncResult<Tag> rh) -> {
			Tag rootTag = rh.result();

			PagingInfo pagingInfo = rcs.getPagingInfo(rc);

			Page<MeshNode> contentPage = clc.findContents(projectName, rootTag, languageTags, pagingInfo);
			for (MeshNode content : contentPage) {
				listResponse.getData().add(contentService.transformToRest(rc, content));
			}
			RestModelPagingHelper.setPaging(listResponse, contentPage, pagingInfo);

		}, trh -> {
			rc.response().setStatusCode(200).end(JsonUtils.toJson(listResponse));
		});
	}
}
