package com.gentics.mesh.core.data.node.field.list.impl;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.impl.NodeGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.list.AbstractReferencingGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.nesting.NodeGraphField;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.list.NodeFieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListItemImpl;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.util.RxUtil;

import rx.Observable;

public class NodeGraphFieldListImpl extends AbstractReferencingGraphFieldList<NodeGraphField, NodeFieldList> implements NodeGraphFieldList {

	public static void checkIndices(Database database) {
		database.addVertexType(NodeGraphFieldListImpl.class);
	}

	@Override
	public NodeGraphField createNode(String key, Node node) {
		return addItem(key, node);
	}

	@Override
	public Class<? extends NodeGraphField> getListType() {
		return NodeGraphFieldImpl.class;
	}

	@Override
	public void delete() {
		// TODO Auto-generated method stub

	}

	@Override
	public Observable<NodeFieldList> transformToRest(InternalActionContext ac, String fieldKey) {

		// Check whether the list should be returned in a collapsed or expanded format
		boolean expandField = ac.getExpandedFieldnames().contains(fieldKey) || ac.getExpandAllFlag();
		if (expandField) {
			NodeFieldList restModel = new NodeFieldListImpl();

			List<Observable<NodeResponse>> futures = new ArrayList<>();
			for (com.gentics.mesh.core.data.node.field.nesting.NodeGraphField item : getList()) {
				futures.add(item.getNode().transformToRest(ac));
			}

			return RxUtil.concatList(futures).collect(() -> {
				return restModel.getItems();
			} , (x, y) -> {
				x.add(y);
			}).map(i -> {
				return restModel;
			});

		} else {
			NodeFieldList restModel = new NodeFieldListImpl();
			List<String> languageTags = ac.getSelectedLanguageTags();
			for (com.gentics.mesh.core.data.node.field.nesting.NodeGraphField item : getList()) {
				// Create the rest field and populate the fields
				NodeFieldListItemImpl listItem = new NodeFieldListItemImpl(item.getNode().getUuid());

				if (ac.getResolveLinksType() != WebRootLinkReplacer.Type.OFF) {
					listItem.setUrl(
							WebRootLinkReplacer.getInstance()
									.resolve(item.getNode(), ac.getResolveLinksType(),
											languageTags.toArray(new String[languageTags.size()]))
									.toBlocking().first());
				}

				restModel.add(listItem);
			}
			return Observable.just(restModel);

		}

	}

}
