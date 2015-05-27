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
import com.gentics.mesh.core.data.service.RoutingContextService;
import com.gentics.mesh.core.data.service.TagService;
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
		String projectName = rcs.getProjectName(rc);
		TagListResponse listResponse = new TagListResponse();
		List<String> languageTags = rcs.getSelectedLanguageTags(rc);

		rcs.loadObject(rc, "uuid", PermissionType.READ, (AsyncResult<MeshNode> rh) -> {
			MeshNode node = rh.result();

			PagingInfo pagingInfo = rcs.getPagingInfo(rc);

			Page<Tag> tagPage = tlc.findTags(projectName, node, languageTags, pagingInfo);
			for (Tag tag : tagPage) {
				listResponse.getData().add(tagService.transformToRest(rc, tag));
			}
			RestModelPagingHelper.setPaging(listResponse, tagPage, pagingInfo);

		}, trh -> {
			rc.response().setStatusCode(200).end(JsonUtils.toJson(listResponse));
		});
	}

}
