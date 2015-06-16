package com.gentics.mesh.core.verticle.handler;

import static com.gentics.mesh.core.data.model.relationship.Permission.READ_PERM;
import io.vertx.core.AsyncResult;
import io.vertx.ext.web.RoutingContext;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.model.tinkerpop.MeshNode;
import com.gentics.mesh.core.data.model.tinkerpop.Tag;
import com.gentics.mesh.core.data.service.MeshNodeService;
import com.gentics.mesh.core.data.service.RoutingContextService;
import com.gentics.mesh.core.data.service.TagService;
import com.gentics.mesh.core.rest.node.response.NodeListResponse;
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

	public void handleListByTag(RoutingContext rc, MeshNodeListTagCallable clc) {
		String projectName = rcs.getProjectName(rc);
		NodeListResponse listResponse = new NodeListResponse();
		List<String> languageTags = rcs.getSelectedLanguageTags(rc);

		rcs.loadObject(rc, "uuid", READ_PERM, (AsyncResult<Tag> rh) -> {
			Tag tag = rh.result();

			PagingInfo pagingInfo = rcs.getPagingInfo(rc);

			Page<MeshNode> contentPage = clc.findNodes(projectName, tag, languageTags, pagingInfo);
			for (MeshNode content : contentPage) {
				listResponse.getData().add(contentService.transformToRest(rc, content));
			}
			RestModelPagingHelper.setPaging(listResponse, contentPage, pagingInfo);

		}, trh -> {
			rc.response().setStatusCode(200).end(JsonUtils.toJson(listResponse));
		});
	}

	public void handleNodeList(RoutingContext rc, MeshNodeListNodeCallable clc) {
		String projectName = rcs.getProjectName(rc);
		NodeListResponse listResponse = new NodeListResponse();
		List<String> languageTags = rcs.getSelectedLanguageTags(rc);

		rcs.loadObject(rc, "uuid", READ_PERM, (AsyncResult<MeshNode> rh) -> {
			MeshNode parentNode = rh.result();

			PagingInfo pagingInfo = rcs.getPagingInfo(rc);

			Page<MeshNode> contentPage = clc.findNodes(projectName, parentNode, languageTags, pagingInfo);
			for (MeshNode content : contentPage) {
				listResponse.getData().add(contentService.transformToRest(rc, content));
			}
			RestModelPagingHelper.setPaging(listResponse, contentPage, pagingInfo);

		}, trh -> {
			rc.response().setStatusCode(200).end(JsonUtils.toJson(listResponse));
		});
	}
}
