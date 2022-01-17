package com.gentics.mesh.core.data.node.field.nesting;

import static com.gentics.mesh.core.rest.common.ContainerType.forVersion;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.node.field.NodeField;
import com.gentics.mesh.core.rest.node.field.NodeFieldListItem;
import com.gentics.mesh.core.rest.node.field.impl.NodeFieldImpl;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.NodeParameters;
import com.gentics.mesh.util.CompareUtils;

/**
 * Common implementations for {@link HibNodeField}.
 * 
 * @author plyhun
 *
 */
public interface HibNodeFieldCommon extends HibNodeField {

	@Override
	default NodeField transformToRest(InternalActionContext ac, String fieldKey, List<String> languageTags, int level) {
		// TODO handle null across all types
		// if (getNode() != null) {
		NodeParameters parameters = ac.getNodeParameters();
		UserDao userDao = Tx.get().userDao();
		boolean expandField = ac.getNodeParameters().getExpandedFieldnameList().contains(fieldKey) || parameters.getExpandAll();
		HibNode node = getNode();

		// Check whether the user is allowed to read the node reference
		boolean canReadNode = userDao.canReadNode(ac.getUser(), ac, node);
		if (!canReadNode) {
			return null;
		}

		if (expandField && level < HibNode.MAX_TRANSFORMATION_LEVEL) {
			return Tx.get().nodeDao().transformToRestSync(node, ac, level, languageTags.toArray(new String[languageTags.size()]));
		} else {
			NodeFieldImpl nodeField = new NodeFieldImpl();
			nodeField.setUuid(node.getUuid());
			LinkType type = ac.getNodeParameters().getResolveLinks();
			if (type != LinkType.OFF) {
				Tx tx = Tx.get();
				ContentDao contentDao = tx.contentDao();

				WebRootLinkReplacer linkReplacer = CommonTx.get().data().mesh().webRootLinkReplacer();
				HibBranch branch = tx.getBranch(ac);
				ContainerType containerType = forVersion(ac.getVersioningParameters().getVersion());

				// Set the webroot path for the currently active language
				nodeField.setPath(linkReplacer.resolve(ac, branch.getUuid(), containerType, node, type, languageTags.toArray(new String[languageTags
					.size()])));

				// Set the languagePaths for all field containers
				Map<String, String> languagePaths = new HashMap<>();
				for (HibFieldContainer currentFieldContainer : contentDao.getFieldContainers(node, branch, containerType)) {
					String currLanguage = currentFieldContainer.getLanguageTag();
					String languagePath = linkReplacer.resolve(ac, branch.getUuid(), containerType, node, type, currLanguage);
					languagePaths.put(currLanguage, languagePath);
				}
				nodeField.setLanguagePaths(languagePaths);

			}
			return nodeField;
		}
	}

	/**
	 * A generalized implementation to {@link Object#equals(Object)}. Use it in the class implementations.
	 * Cannot be an override because of the Java limitations.
	 * 
	 * @param field
	 * @param obj
	 * @return
	 */
	static boolean equalsNodeField(HibNodeField field, Object obj) {
		if (obj instanceof HibNodeField) {
			HibNode nodeA = field.getNode();
			HibNode nodeB = ((HibNodeField) obj).getNode();
			return CompareUtils.equals(nodeA, nodeB);
		}
		if (obj instanceof NodeFieldListItem) {
			NodeFieldListItem restItem = (NodeFieldListItem) obj;
			// TODO assert key as well?
			// getNode can't be null since this is in fact an edge
			return CompareUtils.equals(restItem.getUuid(), field.getNode().getUuid());
		}
		if (obj instanceof NodeField) {
			NodeField nodeRestField = ((NodeField) obj);
			HibNode nodeA = field.getNode();
			String nodeUuid = nodeRestField.getUuid();
			// The node graph field is a edge so getNode should never be null. Lets check it anyways.
			if (nodeA != null) {
				return nodeA.getUuid().equals(nodeUuid);
			}
			// If both are null - both are equal
			if (nodeA == null && nodeRestField.getUuid() == null) {
				return true;
			}
		}
		return false;
	}
}
