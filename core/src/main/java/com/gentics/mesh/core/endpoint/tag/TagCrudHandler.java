package com.gentics.mesh.core.endpoint.tag;

import static com.gentics.mesh.core.action.DAOActionContext.context;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.util.function.Function;

import javax.inject.Inject;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.action.TagDAOActions;
import com.gentics.mesh.core.action.TagFamilyDAOActions;
import com.gentics.mesh.core.data.dao.TagDaoWrapper;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.PageTransformer;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.endpoint.handler.AbstractHandler;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.etc.config.AbstractMeshOptions;
import com.gentics.mesh.parameter.NodeParameters;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.util.ResultInfo;

/**
 * Main CRUD handler
 */
public class TagCrudHandler extends AbstractHandler {

	private final HandlerUtilities utils;
	private final AbstractMeshOptions options;
	private final WriteLock globalLock;
	private final TagDAOActions tagActions;
	private final TagFamilyDAOActions tagFamilyActions;
	private final PageTransformer pageTransformer;

	@Inject
	public TagCrudHandler(AbstractMeshOptions options, HandlerUtilities utils, WriteLock writeLock, TagDAOActions tagActions,
		TagFamilyDAOActions tagFamilyActions, PageTransformer pageTransformer) {
		this.options = options;
		this.utils = utils;
		this.globalLock = writeLock;
		this.tagActions = tagActions;
		this.tagFamilyActions = tagFamilyActions;
		this.pageTransformer = pageTransformer;
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
				TagDaoWrapper tagDao = tx.tagDao();
				PagingParameters pagingParams = ac.getPagingParameters();
				NodeParameters nodeParams = ac.getNodeParameters();
				HibTagFamily tagFamily = tagFamilyActions.loadByUuid(context(tx, ac), tagFamilyUuid, READ_PERM, true);
				HibTag tag = tagActions.loadByUuid(context(tx, ac, tagFamily), tagUuid, READ_PERM, true);
				Page<? extends HibNode> page = tagDao.findTaggedNodes(tag, ac.getUser(), tx.getBranch(ac),
					nodeParams.getLanguageList(options),
					ContainerType.forVersion(ac.getVersioningParameters().getVersion()), pagingParams);
				return pageTransformer.transformToRestSync(page, ac, 0);
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

		Function<Tx, Object> tagFamilyLoader = tx -> {
			return tx.tagFamilyActions().loadByUuid(context(tx, ac), tagFamilyUuid, READ_PERM, true);
		};
		utils.readElementList(ac, tagFamilyLoader, tagActions);
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
				TagDaoWrapper tagDao = tx.tagDao();
				ResultInfo info = utils.eventAction(batch -> {
					// TODO use DAOActionContext and load tagFamily by uuid first. Without a parent this is inconsistent.
					HibTag tag = tagActions.create(tx, ac, batch, null);
					TagResponse model = tagDao.transformToRestSync(tag, ac, 0);
					String path = tagDao.getAPIPath(tag, ac);
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

		Function<Tx, Object> tagFamilyLoader = tx -> {
			return tx.tagFamilyActions().loadByUuid(context(tx, ac), tagFamilyUuid, READ_PERM, true);
		};
		utils.createOrUpdateElement(ac, tagFamilyLoader, tagUuid, tagActions);
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

		Function<Tx, Object> tagFamilyLoader = tx -> {
			return tx.tagFamilyActions().loadByUuid(context(tx, ac), tagFamilyUuid, READ_PERM, true);
		};
		utils.readElement(ac, tagFamilyLoader, tagUuid, tagActions, READ_PERM);

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

		Function<Tx, Object> tagFamilyLoader = tx -> {
			return tagFamilyActions.loadByUuid(context(tx, ac), tagFamilyUuid, READ_PERM, true);
		};
		utils.deleteElement(ac, tagFamilyLoader, tagActions, tagUuid);

	}

}
