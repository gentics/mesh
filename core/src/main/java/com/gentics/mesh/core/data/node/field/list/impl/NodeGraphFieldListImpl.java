package com.gentics.mesh.core.data.node.field.list.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.impl.NodeGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.list.AbstractReferencingGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.nesting.NodeGraphField;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.NodeFieldListItem;
import com.gentics.mesh.core.rest.node.field.list.NodeFieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListItemImpl;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.parameter.impl.LinkType;
import com.gentics.mesh.parameter.impl.NodeParameters;
import com.gentics.mesh.parameter.impl.VersioningParameters;
import com.gentics.mesh.util.CompareUtils;
import rx.Observable;
import rx.Single;

public class NodeGraphFieldListImpl extends AbstractReferencingGraphFieldList<NodeGraphField, NodeFieldList, Node> implements NodeGraphFieldList {

	public static void checkIndices(Database database) {
		database.addVertexType(NodeGraphFieldListImpl.class, MeshVertexImpl.class);
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
	public void delete(SearchQueueBatch batch) {
		getElement().remove();
	}

	@Override
	public Single<NodeFieldList> transformToRest(InternalActionContext ac, String fieldKey, List<String> languageTags, int level) {

		// Check whether the list should be returned in a collapsed or expanded format
		NodeParameters parameters = ac.getNodeParameters();
		boolean expandField = parameters.getExpandedFieldnameList().contains(fieldKey) || parameters.getExpandAll();
		String[] lTagsArray = languageTags.toArray(new String[languageTags.size()]);

		if (expandField && level < Node.MAX_TRANSFORMATION_LEVEL) {
			NodeFieldList restModel = new NodeFieldListImpl();

			List<Single<NodeResponse>> obs = new ArrayList<>();
			for (com.gentics.mesh.core.data.node.field.nesting.NodeGraphField item : getList()) {
				obs.add(item.getNode().transformToRestSync(ac, level, lTagsArray));
			}

			// Transform the list - otherwise we can't use Observable#from
			List<Observable<NodeResponse>> list = obs.stream().map(ele -> ele.toObservable()).collect(Collectors.toList());

			return Observable.concat(Observable.from(list)).collect(() -> {
				return restModel.getItems();
			}, (x, y) -> {
				x.add(y);
			}).map(i -> {
				return restModel;
			}).toSingle();

		} else {
			NodeFieldList restModel = new NodeFieldListImpl();
			String releaseUuid = ac.getRelease(null).getUuid();
			ContainerType type = ContainerType.forVersion(new VersioningParameters(ac).getVersion());
			for (com.gentics.mesh.core.data.node.field.nesting.NodeGraphField item : getList()) {
				// Create the rest field and populate the fields
				NodeFieldListItemImpl listItem = new NodeFieldListItemImpl(item.getNode().getUuid());

				if (ac.getNodeParameters().getResolveLinks() != LinkType.OFF) {
					listItem.setUrl(WebRootLinkReplacer.getInstance()
							.resolve(releaseUuid, type, item.getNode(), ac.getNodeParameters().getResolveLinks(), lTagsArray).toBlocking().value());
				}

				restModel.add(listItem);
			}
			return Single.just(restModel);

		}

	}

	@Override
	public List<Node> getValues() {
		return getList().stream().map(NodeGraphField::getNode).collect(Collectors.toList());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof NodeGraphFieldList) {
			List<? extends NodeGraphField> listA = getList();
			List<? extends NodeGraphField> listB = ((NodeGraphFieldList) obj).getList();
			return CompareUtils.equals(listA, listB);
		}
		if (obj instanceof NodeFieldList) {
			List<? extends NodeGraphField> listA = getList();
			List<NodeFieldListItem> listB = ((NodeFieldList) obj).getItems();
			return CompareUtils.equals(listA, listB);
		}
		return false;
	}
}
