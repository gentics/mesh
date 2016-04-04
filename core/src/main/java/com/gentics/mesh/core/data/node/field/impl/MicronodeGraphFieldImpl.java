package com.gentics.mesh.core.data.node.field.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.List;

import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.generic.MeshEdgeImpl;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeGraphField;
import com.gentics.mesh.core.data.node.impl.MicronodeImpl;
import com.gentics.mesh.core.rest.node.field.Field;

import rx.Observable;

public class MicronodeGraphFieldImpl extends MeshEdgeImpl implements MicronodeGraphField {

	@Override
	public void setFieldKey(String key) {
		setProperty(GraphField.FIELD_KEY_PROPERTY_KEY, key);
	}

	@Override
	public String getFieldKey() {
		return getProperty(GraphField.FIELD_KEY_PROPERTY_KEY);
	}

	@Override
	public Micronode getMicronode() {
		return inV().has(MicronodeImpl.class).nextOrDefaultExplicit(MicronodeImpl.class, null);
	}

	@Override
	public Observable<? extends Field> transformToRest(InternalActionContext ac, String fieldKey, List<String> languageTags, int level) {
		Micronode micronode = getMicronode();
		if (micronode == null) {
			// TODO is this correct?
			throw error(BAD_REQUEST, "error_name_must_be_set");
		} else {
			if (languageTags != null) {
				return micronode.transformToRestSync(ac, level, languageTags.toArray(new String[languageTags.size()]));
			} else {
				return micronode.transformToRestSync(ac, level);
			}
		}
	}

	@Override
	public void removeField(GraphFieldContainer container) {
		Micronode micronode = getMicronode();
		remove();
		if (micronode != null) {
			micronode.delete();
		}
	}

	@Override
	public GraphField cloneTo(GraphFieldContainer container) {
		Micronode micronode = getMicronode();

		MicronodeGraphField field = getGraph().addFramedEdge(container.getImpl(), micronode.getImpl(), HAS_FIELD, MicronodeGraphFieldImpl.class);
		field.setFieldKey(getFieldKey());
		return field;
	}

	@Override
	public void validate() {
		getMicronode().validate();
	}
}
