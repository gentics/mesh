package com.gentics.mesh.core.data.node.field;

import java.util.function.Function;
import java.util.function.Supplier;

import com.gentics.mesh.core.data.node.field.list.HibBooleanFieldList;
import com.gentics.mesh.core.data.node.field.list.HibDateFieldList;
import com.gentics.mesh.core.data.node.field.list.HibHtmlFieldList;
import com.gentics.mesh.core.data.node.field.list.HibMicronodeFieldList;
import com.gentics.mesh.core.data.node.field.list.HibNodeFieldList;
import com.gentics.mesh.core.data.node.field.list.HibNumberFieldList;
import com.gentics.mesh.core.data.node.field.list.HibStringFieldList;
import com.gentics.mesh.core.data.node.field.nesting.HibMicronodeField;
import com.gentics.mesh.core.data.node.field.nesting.HibNodeField;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.s3binary.S3HibBinaryField;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.node.field.BooleanField;
import com.gentics.mesh.core.rest.node.field.DateField;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.HtmlField;
import com.gentics.mesh.core.rest.node.field.MicronodeField;
import com.gentics.mesh.core.rest.node.field.NodeField;
import com.gentics.mesh.core.rest.node.field.NumberField;
import com.gentics.mesh.core.rest.node.field.S3BinaryField;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.node.field.list.MicronodeFieldList;
import com.gentics.mesh.core.rest.node.field.list.NodeFieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.BooleanFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.HtmlFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NumberFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.parameter.LinkType;

public interface RestTransformers {

	FieldTransformer<StringField> STRING_TRANSFORMER = (container, ac, fieldKey, fieldSchema, languageTags, level, parentNode) -> {
		CommonTx tx = CommonTx.get();
		WebRootLinkReplacer webRootLinkReplacer = tx.data().mesh().webRootLinkReplacer();
		// TODO validate found fields has same type as schema
		// StringGraphField graphStringField = new com.gentics.webRootLinkReplacer.core.data.node.field.impl.basic.StringGraphFieldImpl(
		// fieldKey, this);
		HibStringField graphStringField = container.getString(fieldKey);
		if (graphStringField == null) {
			return null;
		} else {
			StringField field = graphStringField.transformToRest(ac);
			if (ac.getNodeParameters().getResolveLinks() != LinkType.OFF) {
				HibProject project = tx.getProject(ac);
				if (project == null) {
					project = parentNode.get().getProject();
				}
				field.setString(webRootLinkReplacer.replace(ac, tx.getBranch(ac).getUuid(),
						ContainerType.forVersion(ac.getVersioningParameters().getVersion()), field.getString(),
						ac.getNodeParameters().getResolveLinks(), project.getName(), languageTags));
			}
			return field;

		}
	};

	FieldTransformer<StringFieldListImpl> STRING_LIST_TRANSFORMER = (container, ac, fieldKey, fieldSchema, languageTags, level,
																				   parentNode) -> {
		HibStringFieldList stringFieldList = container.getStringList(fieldKey);
		if (stringFieldList == null) {
			return null;
		} else {
			return stringFieldList.transformToRest(ac, fieldKey, languageTags, level);
		}
	};

	FieldTransformer<NumberField> NUMBER_TRANSFORMER = (container, ac, fieldKey, fieldSchema, languageTags, level, parentNode) -> {
		return nullSafeTransform(() -> container.getNumber(fieldKey), (field) -> field.transformToRest(ac));
	};

	FieldTransformer<NumberFieldListImpl> NUMBER_LIST_TRANSFORMER = (container, ac, fieldKey, fieldSchema, languageTags, level,
																				   parentNode) -> {
		HibNumberFieldList numberFieldList = container.getNumberList(fieldKey);
		if (numberFieldList == null) {
			return null;
		} else {
			return numberFieldList.transformToRest(ac, fieldKey, languageTags, level);
		}
	};

	FieldTransformer<DateField> DATE_TRANSFORMER = (container, ac, fieldKey, fieldSchema, languageTags, level, parentNode) -> {
		HibDateField graphDateField = container.getDate(fieldKey);
		if (graphDateField == null) {
			return null;
		} else {
			return graphDateField.transformToRest(ac);
		}
	};

	FieldTransformer<DateFieldListImpl> DATE_LIST_TRANSFORMER = (container, ac, fieldKey, fieldSchema, languageTags, level,
																			   parentNode) -> {
		HibDateFieldList dateFieldList = container.getDateList(fieldKey);
		if (dateFieldList == null) {
			return null;
		} else {
			return dateFieldList.transformToRest(ac, fieldKey, languageTags, level);
		}
	};

	FieldTransformer<BooleanField> BOOLEAN_TRANSFORMER = (container, ac, fieldKey, fieldSchema, languageTags, level,
																		parentNode) -> {
		HibBooleanField graphBooleanField = container.getBoolean(fieldKey);
		if (graphBooleanField == null) {
			return null;
		} else {
			return graphBooleanField.transformToRest(ac);
		}
	};

	FieldTransformer<BooleanFieldListImpl> BOOLEAN_LIST_TRANSFORMER = (container, ac, fieldKey, fieldSchema, languageTags, level,
																					 parentNode) -> {
		HibBooleanFieldList booleanFieldList = container.getBooleanList(fieldKey);
		if (booleanFieldList == null) {
			return null;
		} else {
			return booleanFieldList.transformToRest(ac, fieldKey, languageTags, level);
		}
	};

	FieldTransformer<HtmlField> HTML_TRANSFORMER = (container, ac, fieldKey, fieldSchema, languageTags, level, parentNode) -> {
		CommonTx tx = CommonTx.get();
		WebRootLinkReplacer webRootLinkReplacer = tx.data().mesh().webRootLinkReplacer();
		HibHtmlField graphHtmlField = container.getHtml(fieldKey);
		if (graphHtmlField == null) {
			return null;
		} else {
			HtmlField field = graphHtmlField.transformToRest(ac);
			// If needed resolve links within the html
			if (ac.getNodeParameters().getResolveLinks() != LinkType.OFF) {
				HibProject project = tx.getProject(ac);
				if (project == null) {
					project = parentNode.get().getProject();
				}
				field.setHTML(webRootLinkReplacer.replace(ac, tx.getBranch(ac).getUuid(),
						ContainerType.forVersion(ac.getVersioningParameters().getVersion()), field.getHTML(),
						ac.getNodeParameters().getResolveLinks(), project.getName(), languageTags));
			}
			return field;
		}
	};

	FieldTransformer<HtmlFieldListImpl> HTML_LIST_TRANSFORMER = (container, ac, fieldKey, fieldSchema, languageTags, level,
																			   parentNode) -> {
		HibHtmlFieldList htmlFieldList = container.getHTMLList(fieldKey);
		if (htmlFieldList == null) {
			return null;
		} else {
			return htmlFieldList.transformToRest(ac, fieldKey, languageTags, level);
		}
	};

	FieldTransformer<MicronodeField> MICRONODE_TRANSFORMER = (container, ac, fieldKey, fieldSchema, languageTags, level,
																			parentNode) -> {
		HibMicronodeField micronodeGraphField = container.getMicronode(fieldKey);
		if (micronodeGraphField == null) {
			return null;
		} else {
			return micronodeGraphField.transformToRest(ac, fieldKey, languageTags, level);
		}
	};

	FieldTransformer<MicronodeFieldList> MICRONODE_LIST_TRANSFORMER = (container, ac, fieldKey, fieldSchema, languageTags, level,
																					 parentNode) -> {
		HibMicronodeFieldList graphMicroschemaField = container.getMicronodeList(fieldKey);
		if (graphMicroschemaField == null) {
			return null;
		} else {
			return graphMicroschemaField.transformToRest(ac, fieldKey, languageTags, level);
		}
	};

	FieldTransformer<NodeField> NODE_TRANSFORMER = (container, ac, fieldKey, fieldSchema, languageTags, level, parentNode) -> {
		HibNodeField graphNodeField = container.getNode(fieldKey);
		if (graphNodeField == null) {
			return null;
		} else {
			// TODO check permissions
			return graphNodeField.transformToRest(ac, fieldKey, languageTags, level);
		}
	};

	FieldTransformer<NodeFieldList> NODE_LIST_TRANSFORMER = (container, ac, fieldKey, fieldSchema, languageTags, level,
																		   parentNode) -> {
		HibNodeFieldList nodeFieldList = container.getNodeList(fieldKey);
		if (nodeFieldList == null) {
			return null;
		} else {
			return nodeFieldList.transformToRest(ac, fieldKey, languageTags, level);
		}
	};

	FieldTransformer<BinaryField> BINARY_TRANSFORMER = (container, ac, fieldKey, fieldSchema, languageTags, level, parentNode) -> {
		HibBinaryField graphBinaryField = container.getBinary(fieldKey);
		if (graphBinaryField == null) {
			return null;
		} else {
			return graphBinaryField.transformToRest(ac);
		}
	};

	FieldTransformer<S3BinaryField> S3_BINARY_TRANSFORMER = (container, ac, fieldKey, fieldSchema, languageTags, level, parentNode) -> {
		S3HibBinaryField graphBinaryField = container.getS3Binary(fieldKey);
		if (graphBinaryField == null) {
			return null;
		} else {
			return graphBinaryField.transformToRest(ac);
		}
	};

	private static <FIELD extends HibBasicField<REST_FIELD>, REST_FIELD extends Field> REST_FIELD
	nullSafeTransform(Supplier<FIELD> getter, Function<FIELD, REST_FIELD> transformerFunction) {
		FIELD field = getter.get();
		if (field == null) {
			return null;
		}

		return transformerFunction.apply(field);
	}
}
