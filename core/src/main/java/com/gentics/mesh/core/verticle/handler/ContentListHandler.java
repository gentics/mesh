package com.gentics.mesh.core.verticle.handler;

import io.vertx.core.AsyncResult;
import io.vertx.ext.apex.RoutingContext;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.model.Content;
import com.gentics.mesh.core.data.model.Tag;
import com.gentics.mesh.core.data.model.auth.PermissionType;
import com.gentics.mesh.core.data.service.ContentService;
import com.gentics.mesh.core.data.service.RoutingContextService;
import com.gentics.mesh.core.data.service.TagService;
import com.gentics.mesh.core.rest.content.response.ContentListResponse;
import com.gentics.mesh.paging.PagingInfo;
import com.gentics.mesh.util.JsonUtils;
import com.gentics.mesh.util.RestModelPagingHelper;

@Component
public class ContentListHandler {

	@Autowired
	private TagService tagService;

	@Autowired
	private ContentService contentService;

	@Autowired
	private RoutingContextService rcs;

	public void handle(RoutingContext rc, ContentListCallable clc) {
		String projectName = rcs.getProjectName(rc);
		ContentListResponse listResponse = new ContentListResponse();
		List<String> languageTags = rcs.getSelectedLanguageTags(rc);

		rcs.loadObject(rc, "uuid", PermissionType.READ, (AsyncResult<Tag> rh) -> {
			Tag rootTag = rh.result();

			PagingInfo pagingInfo = rcs.getPagingInfo(rc);

			Page<Content> contentPage = clc.findContents(projectName, rootTag, languageTags, pagingInfo);
			for (Content content : contentPage) {
				listResponse.getData().add(contentService.transformToRest(rc, content));
			}
			RestModelPagingHelper.setPaging(listResponse, contentPage, pagingInfo);

		}, trh -> {
			rc.response().setStatusCode(200).end(JsonUtils.toJson(listResponse));
		});
	}
}
