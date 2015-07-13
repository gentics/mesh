//package com.gentics.mesh.core.verticle.handler;
//
//import static com.gentics.mesh.core.data.relationship.Permission.READ_PERM;
//import static com.gentics.mesh.util.RoutingContextHelper.getPagingInfo;
//import static com.gentics.mesh.util.RoutingContextHelper.getUser;
//import io.vertx.core.AsyncResult;
//import io.vertx.ext.web.RoutingContext;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//import com.gentics.mesh.api.common.PagingInfo;
//import com.gentics.mesh.cli.BootstrapInitializer;
//import com.gentics.mesh.core.Page;
//import com.gentics.mesh.core.data.MeshAuthUser;
//import com.gentics.mesh.core.data.Tag;
//import com.gentics.mesh.core.data.node.Node;
//import com.gentics.mesh.core.data.node.impl.NodeImpl;
//import com.gentics.mesh.core.data.service.RoutingContextService;
//import com.gentics.mesh.core.rest.tag.TagListResponse;
//import com.gentics.mesh.json.JsonUtil;
//import com.gentics.mesh.util.RestModelPagingHelper;
//
//@Component
//public class TagListHandler {
//
//	@Autowired
//	private BootstrapInitializer boot;
//
//	@Autowired
//	private RoutingContextService rcs;
//
//	public void handle(String projectName, RoutingContext rc, TagListCallable tlc) {
//		MeshAuthUser requestUser = getUser(rc);
//		TagListResponse listResponse = new TagListResponse();
//		rcs.loadObject(rc, "uuid", READ_PERM, NodeImpl.class, (AsyncResult<Node> rh) -> {
//			Node node = rh.result();
//
//			PagingInfo pagingInfo = getPagingInfo(rc);
//
//			Page<? extends Tag> tagPage = tlc.findTags(projectName, node, pagingInfo);
//			for (Tag tag : tagPage) {
//				tag.transformToRest(requestUser, th -> {
//					if (th.failed()) {
//						rc.fail(rh.cause());
//					}
//					listResponse.getData().add(th.result());
//				});
//			}
//			RestModelPagingHelper.setPaging(listResponse, tagPage);
//
//		}, trh -> {
//			rc.response().setStatusCode(200).end(JsonUtil.toJson(listResponse));
//		});
//	}
//
//}
