package com.gentics.mesh.core.verticle.tag;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import org.springframework.stereotype.Component;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.page.impl.PageImpl;
import com.gentics.mesh.core.verticle.handler.AbstractHandler;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Observable;

@Component
public class TagCrudHandler extends AbstractHandler {

	private static final Logger log = LoggerFactory.getLogger(TagCrudHandler.class);

	public TagFamily getTagFamily(InternalActionContext ac, String uuid) {
		return ac.getProject().getTagFamilyRoot().findByUuidSync(uuid);
	}

	/**
	 * Add the handler that returns a node list for a specified tag.
	 * 
	 * @param ac
	 * @param tagFamilyUuid
	 *            Uuid of the tag's parent tag family
	 * @param tagUuid
	 *            Uuid of the tag
	 */
	public void handleTaggedNodesList(InternalActionContext ac, String tagFamilyUuid, String tagUuid) {
		validateParameter(tagFamilyUuid, "tagFamilyUuid");
		validateParameter(tagUuid, "tagUuid");

		db.asyncNoTrxExperimental(() -> {
			return getTagFamily(ac, tagFamilyUuid).getTagRoot().loadObjectByUuid(ac, tagUuid, READ_PERM).flatMap(tag -> {
				try {
					PageImpl<? extends Node> page = tag.findTaggedNodes(ac.getUser(), ac.getSelectedLanguageTags(), ac.getPagingParameter());
					return page.transformToRest(ac);
				} catch (Exception e) {
					return Observable.error(e);
				}
			});
		}).subscribe(model -> ac.respond(model, OK), ac::fail);
	}

	/**
	 * Read paged list of tags.
	 * 
	 * @param ac
	 * @param tagFamilyUuid
	 */
	public void handleReadTagList(InternalActionContext ac, String tagFamilyUuid) {
		validateParameter(tagFamilyUuid, "tagFamilyUuid");

		//		db.asyncNoTrxExperimental(() -> {
		//			Project project = ac.getProject();
		//			MeshAuthUser requestUser = ac.getUser();
		//			PagingParameter pagingInfo = ac.getPagingParameter();
		//
		//			// TODO this is not checking for the project name and project relationship. We _need_ to fix this!
		//			return project.getTagFamilyRoot().loadObjectByUuid(ac, tagFamilyUuid, READ_PERM).flatMap(tagFamily -> {
		//				try {
		//					PageImpl<? extends Tag> tagPage = tagFamily.getTags(requestUser, pagingInfo);
		//					return tagPage.transformToRest(ac);
		//				} catch (Exception e) {
		//					return Observable.error(e);
		//				}
		//			});
		//		}).subscribe(model -> ac.respond(model, OK), ac::fail);

		HandlerUtilities.readElementList(ac, () -> {
			return getTagFamily(ac, tagFamilyUuid).getTagRoot();
		});
	}

	public void handleCreate(InternalActionContext ac, String tagFamilyUuid) {
		validateParameter(tagFamilyUuid, "tagFamilyUuid");
		db.asyncNoTrxExperimental(() -> {
			return getTagFamily(ac, tagFamilyUuid).create(ac).flatMap(tag -> {
				return db.noTrx(() -> {
					// created.reload();
					return tag.transformToRest(ac);
				});
			});
		}).subscribe(model -> ac.respond(model, CREATED), ac::fail);
	}

	public void handleUpdate(InternalActionContext ac, String tagFamilyUuid, String tagUuid) {
		validateParameter(tagFamilyUuid, "tagFamilyUuid");
		validateParameter(tagUuid, "tagUuid");

		HandlerUtilities.updateElement(ac, tagUuid, () -> {
			return getTagFamily(ac, tagFamilyUuid).getTagRoot();
		});

	}

	public void handleRead(InternalActionContext ac, String tagFamilyUuid, String tagUuid) {
		validateParameter(tagFamilyUuid, "tagFamilyUuid");
		validateParameter(tagUuid, "tagUuid");

		HandlerUtilities.readElement(ac, tagUuid, () -> {
			return getTagFamily(ac, tagFamilyUuid).getTagRoot();
		});

	}

	public void handleDelete(InternalActionContext ac, String tagFamilyUuid, String tagUuid) {
		validateParameter(tagFamilyUuid, "tagFamilyUuid");
		validateParameter(tagUuid, "tagUuid");

		HandlerUtilities.deleteElement(ac, () -> {
			return getTagFamily(ac, tagFamilyUuid).getTagRoot();
		} , tagUuid, "tag_deleted");

	}

}
