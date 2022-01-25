package com.gentics.mesh.core.data.node.field.list.impl;

import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;

import java.util.Optional;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.HibMicronode;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.field.impl.MicronodeGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.list.AbstractReferencingGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.MicronodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.nesting.HibMicronodeField;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeGraphField;
import com.gentics.mesh.core.data.node.impl.MicronodeImpl;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.rest.node.field.list.MicronodeFieldList;

/**
 * @see MicronodeGraphFieldList
 */
public class MicronodeGraphFieldListImpl extends AbstractReferencingGraphFieldList<HibMicronodeField, MicronodeFieldList, HibMicronode>
	implements MicronodeGraphFieldList {

	/**
	 * Initialize the vertex type and index.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(MicronodeGraphFieldListImpl.class, MeshVertexImpl.class);
	}

	@Override
	public Class<? extends MicronodeGraphField> getListType() {
		return MicronodeGraphFieldImpl.class;
	}

	@Override
	public Micronode createMicronodeAt(Optional<Integer> maybeIndex, HibMicroschemaVersion microschemaContainerVersion) {
		MicronodeImpl micronode = getGraph().addFramedVertex(MicronodeImpl.class);
		if (maybeIndex.isPresent()) {
			addItem(String.valueOf(maybeIndex.get() + 1), micronode);
		}
		micronode.setSchemaContainerVersion(microschemaContainerVersion);
		return micronode;
	}

	@Override
	public void delete(BulkActionContext bac) {
		getList().stream().map(HibMicronodeField::getMicronode).forEach(micronode -> {
			toGraph(micronode).delete(bac);
		});
		getElement().remove();
	}
}
