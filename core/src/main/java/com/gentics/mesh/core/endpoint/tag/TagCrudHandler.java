package com.gentics.mesh.core.endpoint.tag;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import javax.inject.Inject;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.actions.impl.TagDAOActionsImpl;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.endpoint.handler.AbstractHandler;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.parameter.NodeParameters;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.util.ResultInfo;

/**
 * Main CRUD handler
 */
public class TagCrudHandler extends AbstractHandler {

	private final HandlerUtilities utils;
	private final MeshOptions options;
	private final WriteLock globalLock;

	@Inject
	public TagCrudHandler(MeshOptions options, HandlerUtilities utils, WriteLock writeLock) {
		this.options = options;
		this.utils = utils;
		this.globalLock = writeLock;
	}

	private TagDAOActionsImpl crudActions() {
		return new TagDAOActionsImpl();
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

		try (WriteLock lock = globalLock.lock(ac)) {
			utils.syncTx(ac, tx -> {
				PagingParameters pagingParams = ac.getPagingParameters();
				NodeParameters nodeParams = ac.getNodeParameters();
				Tag tag =crudActions().load(tx, ac, tagUuid, READ_PERM, true);
				TransformablePage<? extends Node> page = tag.findTaggedNodes(ac.getUser(), ac.getBranch(), nodeParams.getLanguageList(options),
					ContainerType.forVersion(ac.getVersioningParameters().getVersion()), pagingParams);
				return page.transformToRestSync(ac, 0);
			}, model -> ac.send(model, OK));
		}
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

		utils.readElementList(ac, crudActions());
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

		try (WriteLock lock = globalLock.lock(ac)) {
			utils.syncTx(ac, tx -> {
				ResultInfo info = utils.eventAction(batch -> {
					Tag tag = crudActions().create(tx, ac, batch, null);
					TagResponse model = tag.transformToRestSync(ac, 0);
					String path = tag.getAPIPath(ac);
					ResultInfo resultInfo = new ResultInfo(model);
					resultInfo.setProperty("path", path);
					return resultInfo;
				});

				String path = info.getProperty("path");
				ac.setLocation(path);
				return info.getModel();
			}, model -> ac.send(model, CREATED));
		}

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

		utils.updateElement(ac, tagUuid, crudActions());

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

		utils.readElement(ac, tagUuid, crudActions(), READ_PERM);

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

		utils.deleteElement(ac, crudActions(), tagUuid);

	}

}
