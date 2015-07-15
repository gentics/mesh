package com.gentics.mesh.core.verticle.project;

import static com.gentics.mesh.core.data.relationship.Permission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.UPDATE_PERM;
import static com.gentics.mesh.json.JsonUtil.fromJson;
import static com.gentics.mesh.util.RoutingContextHelper.getPagingInfo;
import static com.gentics.mesh.util.RoutingContextHelper.getSelectedLanguageTags;
import static com.gentics.mesh.util.RoutingContextHelper.getUser;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Route;

import org.apache.commons.lang3.StringUtils;
import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractProjectRestVerticle;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.tag.TagCreateRequest;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.core.rest.tag.TagUpdateRequest;
import com.gentics.mesh.util.BlueprintTransaction;

/**
 * The tag verticle provides rest endpoints which allow manipulation and handling of tag related objects.
 * 
 * @author johannes2
 *
 */
@Component
@Scope("singleton")
@SpringVerticle
public class ProjectTagVerticle extends AbstractProjectRestVerticle {

	private static final Logger log = LoggerFactory.getLogger(ProjectTagVerticle.class);

	public ProjectTagVerticle() {
		super("tags");
	}

	@Override
	public void registerEndPoints() throws Exception {
		route("/*").handler(springConfiguration.authHandler());
		addCreateHandler();
		addReadHandler();
		addUpdateHandler();
		addDeleteHandler();

		addTaggedNodesHandler();
		if (log.isDebugEnabled()) {
			log.debug("Registered tag verticle endpoints");
		}
	}

	private void addTaggedNodesHandler() {
		Route getRoute = route("/:uuid/nodes").method(GET).produces(APPLICATION_JSON);
		getRoute.handler(rc -> {

			Project project = getProject(rc);
			loadObject(rc, "uuid", READ_PERM, project.getTagRoot(), rh -> {
				Tag tag = rh.result();
				Page<? extends Node> page = tag.findTaggedNodes(getUser(rc), getSelectedLanguageTags(rc), getPagingInfo(rc));
				transformAndResponde(rc, page, new NodeListResponse());
			});

		});
	}

	// TODO fetch project specific tag
	// TODO update other fields as well?
	// TODO Update user information
	// TODO use schema and only handle those i18n properties that were specified within the schema.
	private void addUpdateHandler() {
		Route route = route("/:uuid").method(PUT).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		route.handler(rc -> {
			Project project = getProject(rc);
			if (project == null) {
				rc.fail(new HttpStatusCodeErrorException(400, "Project not found"));
				// TODO i18n error
			} else {
				loadObject(rc, "uuid", UPDATE_PERM, project.getTagRoot(), rh -> {
					if (hasSucceeded(rc, rh)) {
						Tag tag = rh.result();

						TagUpdateRequest requestModel = fromJson(rc, TagUpdateRequest.class);

						if (StringUtils.isEmpty(requestModel.getFields().getName())) {
							rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "tag_name_not_set")));
						} else {
							try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
								tag.setName(requestModel.getFields().getName());
								tx.success();
							}
							transformAndResponde(rc, tag);
						}
					}
				});

			}

		});

	}

	// TODO load project specific root tag
	// TODO handle creator
	// TODO load schema and set the reference to the tag
	// newTag.setSchemaName(request.getSchemaName());
	// TODO maybe projects should not be a set?
	private void addCreateHandler() {
		Route route = route("/").method(POST).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		route.handler(rc -> {
			Project project = getProject(rc);
			Future<Tag> tagCreated = Future.future();
			TagCreateRequest requestModel = fromJson(rc, TagCreateRequest.class);

			if (StringUtils.isEmpty(requestModel.getFields().getName())) {
				rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "tag_name_not_set")));
			} else {

				// TODO check tag family reference for null

				loadObjectByUuid(rc, requestModel.getTagFamilyReference().getUuid(), CREATE_PERM, project.getTagFamilyRoot(), rh -> {
					if (hasSucceeded(rc, rh)) {
						try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
							TagFamily tagFamily = rh.result();
							Tag newTag = tagFamily.create(requestModel.getFields().getName(), project);
							project.getTagRoot().addTag(newTag);
							tagCreated.complete(newTag);
							transformAndResponde(rc, newTag);
						}
					}
				});
			}

		});
	}

	// TODO filtering, sorting
	private void addReadHandler() {
		Route route = route("/:uuid").method(GET).produces(APPLICATION_JSON);
		route.handler(rc -> {
			Project project = getProject(rc);
			loadTransformAndResponde(rc, "uuid", READ_PERM, project.getTagRoot());
		});

		Route readAllRoute = route().method(GET).produces(APPLICATION_JSON);
		readAllRoute.handler(rc -> {
			Project project = getProject(rc);
			loadTransformAndResponde(rc, project.getTagRoot(), new TagListResponse());
		});

	}

	// TODO filter by projectName
	private void addDeleteHandler() {
		Route route = route("/:uuid").method(DELETE).produces(APPLICATION_JSON);
		route.handler(rc -> {
			delete(rc, "uuid", "tag_deleted", getProject(rc).getTagRoot());
		});
	}

}
