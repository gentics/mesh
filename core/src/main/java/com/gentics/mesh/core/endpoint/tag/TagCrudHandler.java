package com.gentics.mesh.core.endpoint.tag;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import javax.inject.Inject;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.endpoint.handler.AbstractHandler;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.parameter.NodeParameters;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.util.ResultInfo;

/**
 * Main CRUD handler
 */
public class TagCrudHandler extends AbstractHandler {

	private Database db;

	private HandlerUtilities utils;

	@Inject
	public TagCrudHandler(Database db, HandlerUtilities utils) {
		this.db = db;
		this.utils = utils;
	}

	public TagFamily getTagFamily(InternalActionContext ac, String tagFamilyUuid) {
		validateParameter(tagFamilyUuid, "tagFamilyUuid");

		return ac.getProject().getTagFamilyRoot().findByUuid(tagFamilyUuid);
	}

	/**
	 * Add the handler that returns a node list for a specified tag.
	 * 
	 * @param ac
	 *            Action Context
	 * @param tagFamilyUuid
	 *            Uuid of the tag's parent tag family
	 * @param tagUuid
	 *            Uuid of the tag
	 */
	public void handleTaggedNodesList(InternalActionContext ac, String tagFamilyUuid, String tagUuid) {
		validateParameter(tagFamilyUuid, "tagFamilyUuid");
		validateParameter(tagUuid, "tagUuid");

		utils.rxSyncTx(ac, tx -> {
			PagingParameters pagingParams = ac.getPagingParameters();
			NodeParameters nodeParams = ac.getNodeParameters();
			Tag tag = getTagFamily(ac, tagFamilyUuid).loadObjectByUuid(ac, tagUuid, READ_PERM);
			TransformablePage<? extends Node> page = tag.findTaggedNodes(ac.getUser(), ac.getBranch(), nodeParams.getLanguageList(),
				ContainerType.forVersion(ac.getVersioningParameters().getVersion()), pagingParams);
			return page.transformToRest(ac, 0);
		}, model -> ac.send(model, OK));
	}

	/**
	 * Read paged list of tags.
	 * 
	 * @param ac
	 *            Action Context
	 * @param tagFamilyUuid
	 */
	public void handleReadTagList(InternalActionContext ac, String tagFamilyUuid) {
		validateParameter(tagFamilyUuid, "tagFamilyUuid");

		utils.readElementList(ac, () -> {
			return getTagFamily(ac, tagFamilyUuid);
		});
	}

	/**
	 * Handle a tag create request.
	 * 
	 * @param ac
	 *            Action Context
	 * @param tagFamilyUuid
	 *            Uuid of the tagfamily in which the tag should be created
	 */
	public void handleCreate(InternalActionContext ac, String tagFamilyUuid) {
		validateParameter(tagFamilyUuid, "tagFamilyUuid");

		utils.syncTx(ac, tx -> {
			ResultInfo info = db.tx(() -> {
				EventQueueBatch batch = EventQueueBatch.create();
				Tag tag = getTagFamily(ac, tagFamilyUuid).create(ac, batch);
				TagResponse model = tag.transformToRestSync(ac, 0);
				String path = tag.getAPIPath(ac);
				ResultInfo resultInfo = new ResultInfo(model, batch);
				resultInfo.setProperty("path", path);
				return resultInfo;
			});

			String path = info.getProperty("path");
			ac.setLocation(path);
			info.getBatch().dispatch();
			return info.getModel();
		}, model -> ac.send(model, CREATED));

	}

	/**
	 * Handle a tag delete request.
	 * 
	 * @param ac
	 *            Action Context
	 * @param tagFamilyUuid
	 *            The tags tagfamily uuid
	 * @param tagUuid
	 *            Uuid of the tag which should be deleted
	 */
	public void handleUpdate(InternalActionContext ac, String tagFamilyUuid, String tagUuid) {
		validateParameter(tagFamilyUuid, "tagFamilyUuid");
		validateParameter(tagUuid, "tagUuid");

		utils.updateElement(ac, tagUuid, () -> {
			return getTagFamily(ac, tagFamilyUuid);
		});

	}

	/**
	 * Handle a tag read request.
	 * 
	 * @param ac
	 *            Action Context
	 * @param tagFamilyUuid
	 *            Uuid of the tagfamily to which the tag belongs
	 * @param tagUuid
	 *            Uuid of the tag which should be read
	 */
	public void handleRead(InternalActionContext ac, String tagFamilyUuid, String tagUuid) {
		validateParameter(tagFamilyUuid, "tagFamilyUuid");
		validateParameter(tagUuid, "tagUuid");

		utils.readElement(ac, tagUuid, () -> {
			return getTagFamily(ac, tagFamilyUuid);
		}, READ_PERM);

	}

	/**
	 * Handle a tag delete request.
	 * 
	 * @param ac
	 *            Action Context
	 * @param tagFamilyUuid
	 *            Uuid of the tagfamily to which the tag belongs
	 * @param tagUuid
	 *            Uuid of the tag which should be deleted
	 */
	public void handleDelete(InternalActionContext ac, String tagFamilyUuid, String tagUuid) {
		validateParameter(tagFamilyUuid, "tagFamilyUuid");
		validateParameter(tagUuid, "tagUuid");

		utils.deleteElement(ac, () -> {
			return getTagFamily(ac, tagFamilyUuid);
		}, tagUuid);

	}

}
