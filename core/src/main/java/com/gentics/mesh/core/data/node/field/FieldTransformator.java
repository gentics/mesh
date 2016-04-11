package com.gentics.mesh.core.data.node.field;

import java.util.List;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.schema.FieldSchema;

import rx.Observable;

@FunctionalInterface
public interface FieldTransformator {

	Observable<? extends Field> transform(GraphFieldContainer container, InternalActionContext ac, String fieldKey, FieldSchema fieldSchema, List<String> languageTags, int level, Node parentNode);

}
