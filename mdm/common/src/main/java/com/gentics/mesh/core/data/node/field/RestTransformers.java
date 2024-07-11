package com.gentics.mesh.core.data.node.field;

import java.util.function.Function;
import java.util.function.Supplier;

import com.gentics.mesh.core.data.node.field.list.BooleanFieldList;
import com.gentics.mesh.core.data.node.field.list.DateFieldList;
import com.gentics.mesh.core.data.node.field.list.HtmlFieldList;
import com.gentics.mesh.core.data.node.field.list.MicronodeFieldList;
import com.gentics.mesh.core.data.node.field.list.NodeFieldList;
import com.gentics.mesh.core.data.node.field.list.NumberFieldList;
import com.gentics.mesh.core.data.node.field.list.StringFieldList;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeField;
import com.gentics.mesh.core.data.node.field.nesting.NodeField;
import com.gentics.mesh.core.data.project.Project;
import com.gentics.mesh.core.data.s3binary.S3BinaryField;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.node.field.BinaryFieldModel;
import com.gentics.mesh.core.rest.node.field.BooleanFieldModel;
import com.gentics.mesh.core.rest.node.field.DateFieldModel;
import com.gentics.mesh.core.rest.node.field.FieldModel;
import com.gentics.mesh.core.rest.node.field.HtmlFieldModel;
import com.gentics.mesh.core.rest.node.field.MicronodeFieldModel;
import com.gentics.mesh.core.rest.node.field.NodeFieldModel;
import com.gentics.mesh.core.rest.node.field.NumberFieldModel;
import com.gentics.mesh.core.rest.node.field.S3BinaryFieldModel;
import com.gentics.mesh.core.rest.node.field.StringFieldModel;
import com.gentics.mesh.core.rest.node.field.list.MicronodeFieldListModel;
import com.gentics.mesh.core.rest.node.field.list.NodeFieldListModel;
import com.gentics.mesh.core.rest.node.field.list.impl.BooleanFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.HtmlFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NumberFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.parameter.LinkType;

public interface RestTransformers {

	FieldTransformer<StringFieldModel> STRING_TRANSFORMER = (container, ac, fieldKey, fieldSchema, languageTags, level, parentNode) -> {
		CommonTx tx = CommonTx.get();
		WebRootLinkReplacer webRootLinkReplacer = tx.data().mesh().webRootLinkReplacer();
		// TODO validate found fields has same type as schema
		// StringGraphField graphStringField = new com.gentics.webRootLinkReplacer.core.data.node.field.impl.basic.StringGraphFieldImpl(
		// fieldKey, this);
		StringField graphStringField = container.getString(fieldKey);
		if (graphStringField == null) {
			return null;
		} else {
			StringFieldModel field = graphStringField.transformToRest(ac);
			if (ac.getNodeParameters().getResolveLinks() != LinkType.OFF) {
				Project project = tx.getProject(ac);
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
		StringFieldList stringFieldList = container.getStringList(fieldKey);
		if (stringFieldList == null) {
			return null;
		} else {
			return stringFieldList.transformToRest(ac, fieldKey, languageTags, level);
		}
	};

	FieldTransformer<NumberFieldModel> NUMBER_TRANSFORMER = (container, ac, fieldKey, fieldSchema, languageTags, level, parentNode) -> {
		return nullSafeTransform(() -> container.getNumber(fieldKey), (field) -> field.transformToRest(ac));
	};

	FieldTransformer<NumberFieldListImpl> NUMBER_LIST_TRANSFORMER = (container, ac, fieldKey, fieldSchema, languageTags, level,
																				   parentNode) -> {
		NumberFieldList numberFieldList = container.getNumberList(fieldKey);
		if (numberFieldList == null) {
			return null;
		} else {
			return numberFieldList.transformToRest(ac, fieldKey, languageTags, level);
		}
	};

	FieldTransformer<DateFieldModel> DATE_TRANSFORMER = (container, ac, fieldKey, fieldSchema, languageTags, level, parentNode) -> {
		DateField graphDateField = container.getDate(fieldKey);
		if (graphDateField == null) {
			return null;
		} else {
			return graphDateField.transformToRest(ac);
		}
	};

	FieldTransformer<DateFieldListImpl> DATE_LIST_TRANSFORMER = (container, ac, fieldKey, fieldSchema, languageTags, level,
																			   parentNode) -> {
		DateFieldList dateFieldList = container.getDateList(fieldKey);
		if (dateFieldList == null) {
			return null;
		} else {
			return dateFieldList.transformToRest(ac, fieldKey, languageTags, level);
		}
	};

	FieldTransformer<BooleanFieldModel> BOOLEAN_TRANSFORMER = (container, ac, fieldKey, fieldSchema, languageTags, level,
																		parentNode) -> {
		BooleanField graphBooleanField = container.getBoolean(fieldKey);
		if (graphBooleanField == null) {
			return null;
		} else {
			return graphBooleanField.transformToRest(ac);
		}
	};

	FieldTransformer<BooleanFieldListImpl> BOOLEAN_LIST_TRANSFORMER = (container, ac, fieldKey, fieldSchema, languageTags, level,
																					 parentNode) -> {
		BooleanFieldList booleanFieldList = container.getBooleanList(fieldKey);
		if (booleanFieldList == null) {
			return null;
		} else {
			return booleanFieldList.transformToRest(ac, fieldKey, languageTags, level);
		}
	};

	FieldTransformer<HtmlFieldModel> HTML_TRANSFORMER = (container, ac, fieldKey, fieldSchema, languageTags, level, parentNode) -> {
		CommonTx tx = CommonTx.get();
		WebRootLinkReplacer webRootLinkReplacer = tx.data().mesh().webRootLinkReplacer();
		HtmlField graphHtmlField = container.getHtml(fieldKey);
		if (graphHtmlField == null) {
			return null;
		} else {
			HtmlFieldModel field = graphHtmlField.transformToRest(ac);
			// If needed resolve links within the html
			if (ac.getNodeParameters().getResolveLinks() != LinkType.OFF) {
				Project project = tx.getProject(ac);
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
		HtmlFieldList htmlFieldList = container.getHTMLList(fieldKey);
		if (htmlFieldList == null) {
			return null;
		} else {
			return htmlFieldList.transformToRest(ac, fieldKey, languageTags, level);
		}
	};

	FieldTransformer<MicronodeFieldModel> MICRONODE_TRANSFORMER = (container, ac, fieldKey, fieldSchema, languageTags, level,
																			parentNode) -> {
		MicronodeField micronodeGraphField = container.getMicronode(fieldKey);
		if (micronodeGraphField == null) {
			return null;
		} else {
			return micronodeGraphField.transformToRest(ac, fieldKey, languageTags, level);
		}
	};

	FieldTransformer<MicronodeFieldListModel> MICRONODE_LIST_TRANSFORMER = (container, ac, fieldKey, fieldSchema, languageTags, level,
																					 parentNode) -> {
		MicronodeFieldList graphMicroschemaField = container.getMicronodeList(fieldKey);
		if (graphMicroschemaField == null) {
			return null;
		} else {
			return graphMicroschemaField.transformToRest(ac, fieldKey, languageTags, level);
		}
	};

	FieldTransformer<NodeFieldModel> NODE_TRANSFORMER = (container, ac, fieldKey, fieldSchema, languageTags, level, parentNode) -> {
		NodeField graphNodeField = container.getNode(fieldKey);
		if (graphNodeField == null) {
			return null;
		} else {
			// TODO check permissions
			return graphNodeField.transformToRest(ac, fieldKey, languageTags, level);
		}
	};

	FieldTransformer<NodeFieldListModel> NODE_LIST_TRANSFORMER = (container, ac, fieldKey, fieldSchema, languageTags, level,
																		   parentNode) -> {
		NodeFieldList nodeFieldList = container.getNodeList(fieldKey);
		if (nodeFieldList == null) {
			return null;
		} else {
			return nodeFieldList.transformToRest(ac, fieldKey, languageTags, level);
		}
	};

	FieldTransformer<BinaryFieldModel> BINARY_TRANSFORMER = (container, ac, fieldKey, fieldSchema, languageTags, level, parentNode) -> {
		BinaryField graphBinaryField = container.getBinary(fieldKey);
		if (graphBinaryField == null) {
			return null;
		} else {
			return graphBinaryField.transformToRest(ac);
		}
	};

	FieldTransformer<S3BinaryFieldModel> S3_BINARY_TRANSFORMER = (container, ac, fieldKey, fieldSchema, languageTags, level, parentNode) -> {
		S3BinaryField graphBinaryField = container.getS3Binary(fieldKey);
		if (graphBinaryField == null) {
			return null;
		} else {
			return graphBinaryField.transformToRest(ac);
		}
	};

	private static <FIELD extends BasicField<REST_FIELD>, REST_FIELD extends FieldModel> REST_FIELD
	nullSafeTransform(Supplier<FIELD> getter, Function<FIELD, REST_FIELD> transformerFunction) {
		FIELD field = getter.get();
		if (field == null) {
			return null;
		}

		return transformerFunction.apply(field);
	}
}
