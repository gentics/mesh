package com.gentics.mesh.core.verticle.tag;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.page.impl.PageImpl;
import com.gentics.mesh.core.verticle.handler.AbstractHandler;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.parameter.impl.NodeParameters;
import com.gentics.mesh.parameter.impl.PagingParameters;

import rx.Single;

public class TagCrudHandler extends AbstractHandler {

	public TagCrudHandler(Database db, MeshSpringConfiguration springConfiguration, BootstrapInitializer boot, RouterStorage routerStorage) {
		super(db, springConfiguration, boot, routerStorage);
	}

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

		db.asyncNoTx(() -> {
			PagingParameters pagingParams = ac.getPagingParameters();
			NodeParameters nodeParams = ac.getNodeParameters();
			return getTagFamily(ac, tagFamilyUuid).getTagRoot().loadObjectByUuid(ac, tagUuid, READ_PERM).flatMap(tag -> {
				try {
					PageImpl<? extends Node> page = tag.findTaggedNodes(ac.getUser(), ac.getRelease(null), nodeParams.getLanguageList(),
							ContainerType.forVersion(ac.getVersioningParameters().getVersion()), pagingParams);
					return page.transformToRest(ac, 0);
				} catch (Exception e) {
					return Single.error(e);
				}
			});
		}).subscribe(model -> ac.send(model, OK), ac::fail);
	}

	/**
	 * Read paged list of tags.
	 * 
	 * @param ac
	 * @param tagFamilyUuid
	 */
	public void handleReadTagList(InternalActionContext ac, String tagFamilyUuid) {
		validateParameter(tagFamilyUuid, "tagFamilyUuid");

		HandlerUtilities.readElementList(ac, () -> {
			return getTagFamily(ac, tagFamilyUuid).getTagRoot();
		});
	}

	/**
	 * Handle a tag create request.
	 * 
	 * @param ac
	 * @param tagFamilyUuid
	 *            Uuid of the tagfamily in which the tag should be created
	 */
	public void handleCreate(InternalActionContext ac, String tagFamilyUuid) {
		validateParameter(tagFamilyUuid, "tagFamilyUuid");

		db.asyncNoTx(() -> {
			return getTagFamily(ac, tagFamilyUuid).create(ac).flatMap(tag -> {
				return db.noTx(() -> {
					// created.reload();
					return tag.transformToRest(ac, 0);
				});
			});
		}).subscribe(model -> ac.send(model, CREATED), ac::fail);
	}

	/**
	 * Handle a tag delete request.
	 * 
	 * @param ac
	 * @param tagFamilyUuid
	 *            The tags tagfamily uuid
	 * @param tagUuid
	 *            Uuid of the tag which should be deleted
	 */
	public void handleUpdate(InternalActionContext ac, String tagFamilyUuid, String tagUuid) {
		validateParameter(tagFamilyUuid, "tagFamilyUuid");
		validateParameter(tagUuid, "tagUuid");

		HandlerUtilities.updateElement(ac, tagUuid, () -> {
			return getTagFamily(ac, tagFamilyUuid).getTagRoot();
		});

	}

	/**
	 * Handle a tag read request.
	 * 
	 * @param ac
	 * @param tagFamilyUuid
	 *            Uuid of the tagfamily to which the tag belongs
	 * @param tagUuid
	 *            Uuid of the tag which should be read
	 */
	public void handleRead(InternalActionContext ac, String tagFamilyUuid, String tagUuid) {
		validateParameter(tagFamilyUuid, "tagFamilyUuid");
		validateParameter(tagUuid, "tagUuid");

		HandlerUtilities.readElement(ac, tagUuid, () -> {
			return getTagFamily(ac, tagFamilyUuid).getTagRoot();
		});

	}

	/**
	 * Handle a tag delete request.
	 * 
	 * @param ac
	 * @param tagFamilyUuid
	 *            Uuid of the tagfamily to which the tag belongs
	 * @param tagUuid
	 *            Uuid of the tag which should be deleted
	 */
	public void handleDelete(InternalActionContext ac, String tagFamilyUuid, String tagUuid) {
		validateParameter(tagFamilyUuid, "tagFamilyUuid");
		validateParameter(tagUuid, "tagUuid");

		HandlerUtilities.deleteElement(ac, () -> {
			return getTagFamily(ac, tagFamilyUuid).getTagRoot();
		}, tagUuid);

	}

}
