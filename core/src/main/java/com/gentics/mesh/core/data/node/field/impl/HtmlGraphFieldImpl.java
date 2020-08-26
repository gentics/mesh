package com.gentics.mesh.core.data.node.field.impl;

import java.util.Objects;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.node.field.AbstractBasicField;
import com.gentics.mesh.core.data.node.field.FieldGetter;
import com.gentics.mesh.core.data.node.field.FieldTransformer;
import com.gentics.mesh.core.data.node.field.FieldUpdater;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.HtmlGraphField;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.graph.GraphAttribute;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.node.field.HtmlField;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.parameter.LinkType;
import com.syncleus.ferma.AbstractVertexFrame;

public class HtmlGraphFieldImpl extends AbstractBasicField<HtmlField> implements HtmlGraphField {

	public static FieldTransformer<HtmlField> HTML_TRANSFORMER = (container, ac, fieldKey, fieldSchema, languageTags, level, parentNode) -> {
		MeshComponent mesh = container.getGraphAttribute(GraphAttribute.MESH_COMPONENT);
		HtmlGraphField graphHtmlField = container.getHtml(fieldKey);
		if (graphHtmlField == null) {
			return null;
		} else {
			HtmlField field = graphHtmlField.transformToRest(ac);
			// If needed resolve links within the html
			if (ac.getNodeParameters().getResolveLinks() != LinkType.OFF) {
				HibProject project = ac.getProject();
				if (project == null) {
					project = parentNode.get().getProject();
				}
				field.setHTML(mesh.webRootLinkReplacer().replace(ac, ac.getBranch().getUuid(),
						ContainerType.forVersion(ac.getVersioningParameters().getVersion()), field.getHTML(),
						ac.getNodeParameters().getResolveLinks(), project.getName(), languageTags));
			}
			return field;
		}
	};

	public static FieldUpdater HTML_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		HtmlField htmlField = fieldMap.getHtmlField(fieldKey);
		HtmlGraphField htmlGraphField = container.getHtml(fieldKey);
		boolean isHtmlFieldSetToNull = fieldMap.hasField(fieldKey) && (htmlField == null || htmlField.getHTML() == null);
		GraphField.failOnDeletionOfRequiredField(htmlGraphField, isHtmlFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean isHtmlFieldNull = htmlField == null || htmlField.getHTML() == null;

		// Skip this check for no migrations
		if (!ac.isMigrationContext()) {
			GraphField.failOnMissingRequiredField(htmlGraphField, isHtmlFieldNull, fieldSchema, fieldKey, schema);
		}

		// Handle Deletion - The field was explicitly set to null and is currently set within the graph thus we must remove it.
		if (isHtmlFieldSetToNull && htmlGraphField != null) {
			htmlGraphField.removeField(container);
			return;
		}

		// Rest model is empty or null - Abort
		if (isHtmlFieldNull) {
			return;
		}

		// Handle Update / Create - Create new graph field if no existing one could be found
		if (htmlGraphField == null) {
			container.createHTML(fieldKey).setHtml(htmlField.getHTML());
		} else {
			htmlGraphField.setHtml(htmlField.getHTML());
		}
	};

	public static FieldGetter HTML_GETTER = (container, fieldSchema) -> {
		return container.getHtml(fieldSchema.getName());
	};

	public HtmlGraphFieldImpl(String fieldKey, AbstractVertexFrame parentContainer) {
		super(fieldKey, parentContainer);
	}

	@Override
	public void setHtml(String html) {
		setFieldProperty("html", html);
	}

	@Override
	public String getHTML() {
		return getFieldProperty("html");
	}

	@Override
	public HtmlField transformToRest(ActionContext ac) {
		HtmlFieldImpl htmlField = new HtmlFieldImpl();
		String html = getHTML();
		// TODO really empty string for unset field value?!
		htmlField.setHTML(html == null ? "" : html);
		return htmlField;
	}

	@Override
	public void removeField(BulkActionContext bac, GraphFieldContainer container) {
		//TODO remove the vertex from the graph if it is no longer be used by other containers 
		setFieldProperty("html", null);
		setFieldKey(null);
	}

	@Override
	public GraphField cloneTo(GraphFieldContainer container) {
		HtmlGraphField clone = container.createHTML(getFieldKey());
		clone.setHtml(getHTML());
		return clone;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof HtmlGraphField) {
			String htmlA = getHTML();
			String htmlB = ((HtmlGraphField) obj).getHTML();
			return Objects.equals(htmlA, htmlB);
		}
		if (obj instanceof HtmlField) {
			String htmlA = getHTML();
			String htmlB = ((HtmlField) obj).getHTML();
			return Objects.equals(htmlA, htmlB);
		}
		return false;
	}

}
