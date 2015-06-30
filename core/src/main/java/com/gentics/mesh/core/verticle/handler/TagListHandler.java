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
import com.gentics.mesh.core.data.service.RoutingContextService;
import com.gentics.mesh.core.data.service.TagService;
import com.gentics.mesh.core.data.service.transformation.TransformationInfo;
import com.gentics.mesh.core.rest.tag.response.TagListResponse;
import com.gentics.mesh.paging.PagingInfo;
import com.gentics.mesh.util.JsonUtils;
import com.gentics.mesh.util.RestModelPagingHelper;

@Component
public class TagListHandler {

	@Autowired
	private TagService tagService;

	@Autowired
	private RoutingContextService rcs;

	public void handle(RoutingContext rc, TagListCallable tlc) {
		MeshAuthUser requestUser = getUser(rc);
		String projectName = rcs.getProjectName(rc);
		TagListResponse listResponse = new TagListResponse();
		List<String> languageTags = getSelectedLanguageTags(rc);

		rcs.loadObject(rc, "uuid", READ_PERM, MeshNode.class, (AsyncResult<MeshNode> rh) -> {
			MeshNode node = rh.result();

			PagingInfo pagingInfo = getPagingInfo(rc);

			Page<Tag> tagPage = tlc.findTags(projectName, node, languageTags, pagingInfo);
			for (Tag tag : tagPage) {
				TransformationInfo info = new TransformationInfo(requestUser, languageTags, rc);
				listResponse.getData().add(tag.transformToRest(info));
			}
			RestModelPagingHelper.setPaging(listResponse, tagPage, pagingInfo);

		}, trh -> {
			rc.response().setStatusCode(200).end(JsonUtils.toJson(listResponse));
		});
	}

}
