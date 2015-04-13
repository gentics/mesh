package com.gentics.cailun.core.verticle;

import static com.gentics.cailun.util.JsonUtils.fromJson;
import static com.gentics.cailun.util.JsonUtils.toJson;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import io.vertx.ext.apex.Route;

import org.apache.commons.lang3.StringUtils;
import org.jacpfx.vertx.spring.SpringVerticle;
import org.neo4j.graphdb.Transaction;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.AbstractCoreApiVerticle;
import com.gentics.cailun.core.data.model.CaiLunRoot;
import com.gentics.cailun.core.data.model.auth.CaiLunPermission;
import com.gentics.cailun.core.data.model.auth.Group;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.model.auth.Role;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.rest.common.response.GenericMessageResponse;
import com.gentics.cailun.core.rest.group.request.GroupCreateRequest;
import com.gentics.cailun.core.rest.group.request.GroupUpdateRequest;
import com.gentics.cailun.core.rest.group.response.GroupListResponse;
import com.gentics.cailun.error.HttpStatusCodeErrorException;
import com.gentics.cailun.path.PagingInfo;
import com.gentics.cailun.util.RestModelPagingHelper;

@Component
@Scope("singleton")
@SpringVerticle
public class GroupVerticle extends AbstractCoreApiVerticle {

	public GroupVerticle() {
		super("groups");
	}

	@Override
	public void registerEndPoints() throws Exception {
		// addGroupChildGroupHandlers();
		addGroupUserHandlers();
		addGroupRoleHandlers();

		addCreateHandler();
		addReadHandler();
		addUpdateHandler();
		addDeleteHandler();
	}

	private void addGroupRoleHandlers() {
		route("/:groupUuid/roles/:roleUuid").method(POST).handler(rc -> {
			Group group = null;
			try (Transaction tx = graphDb.beginTx()) {
				group = getObject(rc, "groupUuid", PermissionType.UPDATE);
				Role role = getObject(rc, "roleUuid", PermissionType.READ);

				if (group.addRole(role)) {
					group = groupService.save(group);
				} else {
					// TODO 200?
			}
			tx.success();
		}
		rc.response().setStatusCode(200);
		rc.response().end(toJson(groupService.transformToRest(group)));
	}	);

		route("/:groupUuid/roles/:roleUuid").method(DELETE).handler(rc -> {
			Group group = null;
			try (Transaction tx = graphDb.beginTx()) {
				group = getObject(rc, "groupUuid", PermissionType.UPDATE);
				Role role = getObject(rc, "roleUuid", PermissionType.READ);

				if (group.removeRole(role)) {
					group = groupService.save(group);

				} else {
					// TODO 200?
			}
			tx.success();
		}
		rc.response().setStatusCode(200);
		rc.response().end(toJson(groupService.transformToRest(group)));
	}	);
	}

	private void addGroupUserHandlers() {
		Route route = route("/:groupUuid/users/:userUuid").method(POST);
		route.handler(rc -> {
			Group group = null;
			try (Transaction tx = graphDb.beginTx()) {
				group = getObject(rc, "groupUuid", PermissionType.UPDATE);
				User user = getObject(rc, "userUuid", PermissionType.READ);

				if (group.addUser(user)) {
					group = groupService.save(group);
				} else {
					// TODO 200?
				}
				tx.success();
			}
			rc.response().setStatusCode(200);
			rc.response().end(toJson(groupService.transformToRest(group)));
		});

		route = route("/:groupUuid/users/:userUuid").method(DELETE);
		route.handler(rc -> {
			Group group = null;
			try (Transaction tx = graphDb.beginTx()) {
				group = getObject(rc, "groupUuid", PermissionType.UPDATE);
				User user = getObject(rc, "userUuid", PermissionType.READ);

				if (group.removeUser(user)) {
					groupService.save(group);
				} else {
					// TODO 200?
				}
				tx.success();
			}
			rc.response().setStatusCode(200);
			rc.response().end(toJson(groupService.transformToRest(group)));
		});
	}

	private void addDeleteHandler() {
		route("/:uuid").method(DELETE).handler(rc -> {
			String uuid = rc.request().params().get("uuid");
			try (Transaction tx = graphDb.beginTx()) {
				Group group = getObject(rc, "uuid", PermissionType.DELETE);
				groupService.delete(group);
				tx.success();
			}
			rc.response().setStatusCode(200);
			rc.response().end(toJson(new GenericMessageResponse(i18n.get(rc, "group_deleted", uuid))));

		});
	}

	private void addUpdateHandler() {
		route("/:uuid").method(PUT).handler(rc -> {
			Group group = null;
			try (Transaction tx = graphDb.beginTx()) {
				group = getObject(rc, "uuid", PermissionType.UPDATE);
				GroupUpdateRequest requestModel = fromJson(rc, GroupUpdateRequest.class);

				if (StringUtils.isEmpty(requestModel.getName())) {
					throw new HttpStatusCodeErrorException(400, i18n.get(rc, "error_name_must_be_set"));
				}

				// TODO should we keep this? I don't think so since group save would fail anyway. Let the index handle this?
				if (!group.getName().equals(requestModel.getName())) {

					Group groupWithSameName = groupService.findByName(requestModel.getName());
					if (groupWithSameName != null && !groupWithSameName.getUuid().equals(group.getUuid())) {
						throw new HttpStatusCodeErrorException(400, "Group name {" + groupWithSameName.getName()
								+ "} is already taken. Choose a different one.");
					}
					group.setName(requestModel.getName());
				}

				// TODO update timestamps
				group = groupService.save(group);
				tx.success();
			}
			rc.response().setStatusCode(200);
			rc.response().end(toJson(groupService.transformToRest(group)));

		});

	}

	private void addReadHandler() {
		route("/:uuid").method(GET).handler(rc -> {
			Group group;
			try (Transaction tx = graphDb.beginTx()) {
				group = getObject(rc, "uuid", PermissionType.READ);
				tx.success();
			}
			rc.response().setStatusCode(200);
			rc.response().end(toJson(groupService.transformToRest(group)));
		});

		/*
		 * List all groups when no parameter was specified
		 */
		route("/").method(GET).handler(
				rc -> {
					GroupListResponse listResponse = new GroupListResponse();
					try (Transaction tx = graphDb.beginTx()) {
						PagingInfo pagingInfo = getPagingInfo(rc);
						User requestUser = springConfiguration.authService().getUser(rc);
						Page<Group> groupPage = groupService.findAllVisible(requestUser, pagingInfo);
						for (Group group : groupPage) {
							listResponse.getData().add(groupService.transformToRest(group));
						}
						RestModelPagingHelper.setPaging(listResponse, groupPage.getNumber(), groupPage.getTotalPages(), pagingInfo.getPerPage(),
								groupPage.getTotalElements());
						tx.success();
					}
					rc.response().setStatusCode(200);
					rc.response().end(toJson(listResponse));
				});
	}

	private void addCreateHandler() {
		route("/").method(POST).handler(rc -> {

			Group group = null;
			try (Transaction tx = graphDb.beginTx()) {
				GroupCreateRequest requestModel = fromJson(rc, GroupCreateRequest.class);

				if (StringUtils.isEmpty(requestModel.getName())) {
					throw new HttpStatusCodeErrorException(400, i18n.get(rc, "error_name_must_be_set"));
				}

				CaiLunRoot root = cailunRootService.findRoot();
				failOnMissingPermission(rc, root.getGroupRoot(), PermissionType.CREATE);

				group = new Group(requestModel.getName());
				group = groupService.save(group);
				roleService.addCRUDPermissionOnRole(rc, new CaiLunPermission(root.getGroupRoot(), PermissionType.CREATE), group);
				tx.success();
			}
			group = groupService.reload(group);
			rc.response().setStatusCode(200);
			rc.response().end(toJson(groupService.transformToRest(group)));

		});

	}
}
