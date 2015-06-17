package com.gentics.mesh.core.verticle.handler;

import static com.gentics.mesh.core.data.model.relationship.Permission.READ_PERM;
import static com.gentics.mesh.util.RoutingContextHelper.getUser;
import io.vertx.core.AsyncResult;
import io.vertx.ext.web.RoutingContext;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.model.tinkerpop.MeshNode;
import com.gentics.mesh.core.data.model.tinkerpop.MeshShiroUser;
import com.gentics.mesh.core.data.model.tinkerpop.Tag;
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
		MeshShiroUser requestUser = getUser(rc);
		String projectName = rcs.getProjectName(rc);
		TagListResponse listResponse = new TagListResponse();
		List<String> languageTags = rcs.getSelectedLanguageTags(rc);

		rcs.loadObject(rc, "uuid", READ_PERM, (AsyncResult<MeshNode> rh) -> {
			MeshNode node = rh.result();

			PagingInfo pagingInfo = rcs.getPagingInfo(rc);

			Page<Tag> tagPage = tlc.findTags(projectName, node, languageTags, pagingInfo);
			for (Tag tag : tagPage) {
				listResponse.getData().add(tag.transformToRest(requestUser));
			}
			RestModelPagingHelper.setPaging(listResponse, tagPage, pagingInfo);

		}, trh -> {
			rc.response().setStatusCode(200).end(JsonUtils.toJson(listResponse));
		});
	}

}
