package com.gentics.mesh.core.verticle.handler;

import static com.gentics.mesh.core.data.relationship.Permission.READ_PERM;
import static com.gentics.mesh.util.RoutingContextHelper.getPagingInfo;
import static com.gentics.mesh.util.RoutingContextHelper.getSelectedLanguageTags;
import static com.gentics.mesh.util.RoutingContextHelper.getUser;
import io.vertx.core.AsyncResult;
import io.vertx.ext.web.RoutingContext;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.node.MeshNode;
import com.gentics.mesh.core.data.service.MeshNodeService;
import com.gentics.mesh.core.data.service.RoutingContextService;
import com.gentics.mesh.core.data.service.TagService;
import com.gentics.mesh.core.data.service.transformation.TransformationInfo;
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
		MeshAuthUser requestUser = getUser(rc);

		NodeListResponse listResponse = new NodeListResponse();
		List<String> languageTags = getSelectedLanguageTags(rc);

		rcs.loadObject(rc, "uuid", READ_PERM, Tag.class, (AsyncResult<Tag> rh) -> {
			Tag tag = rh.result();

			PagingInfo pagingInfo = getPagingInfo(rc);
			TransformationInfo info = new TransformationInfo(requestUser, languageTags, rc);

			Page<MeshNode> nodePage = clc.findNodes(projectName, tag, languageTags, pagingInfo);
			for (MeshNode node : nodePage) {
				listResponse.getData().add(node.transformToRest(info));
			}
			RestModelPagingHelper.setPaging(listResponse, nodePage, pagingInfo);

		}, trh -> {
			rc.response().setStatusCode(200).end(JsonUtils.toJson(listResponse));
		});
	}

	public void handleNodeList(RoutingContext rc, MeshNodeListNodeCallable clc) {
		String projectName = rcs.getProjectName(rc);
		MeshAuthUser requestUser = getUser(rc);

		NodeListResponse listResponse = new NodeListResponse();
		List<String> languageTags = getSelectedLanguageTags(rc);

		rcs.loadObject(rc, "uuid", READ_PERM, MeshNode.class, (AsyncResult<MeshNode> rh) -> {
			MeshNode parentNode = rh.result();

			PagingInfo pagingInfo = getPagingInfo(rc);

			TransformationInfo info = new TransformationInfo(requestUser, languageTags, rc);
			Page<MeshNode> nodePage = clc.findNodes(projectName, parentNode, languageTags, pagingInfo);
			for (MeshNode node : nodePage) {
				listResponse.getData().add(node.transformToRest(info));
			}
			RestModelPagingHelper.setPaging(listResponse, nodePage, pagingInfo);

		}, trh -> {
			rc.response().setStatusCode(200).end(JsonUtils.toJson(listResponse));
		});
	}
}
