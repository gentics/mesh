package com.gentics.mesh.plugin.forms.model;

import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.NodeResponse;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Combi element to select which admin email addresses should receive mail
 * notifications.
 *
 * <p>
 *     When there are no {@code AdminMailElements} present, mail sending will
 *     fall back to the admin email configured in the
 *     {@link Form#getEmail() Form}.
 * </p>
 */
@Reportable
public class AdminMailElement extends CombiElement {

	@Override
	public boolean isMultivalue() {
		return (getSelectType() != null && getSelectType().startsWith("multiselect"));
	}

	/**
	 * Get the list of selected email addresses from the submitted form data.
	 *
	 * @param formDataNode The submitted form data.
	 * @return The list of selected email addresses from the submitted form data.
	 */
	public List<String> getSelectedEmails(NodeResponse formDataNode) {
		Optional<FieldMap> fields = Optional.ofNullable(formDataNode.getFields());
		Optional<List<String>> selected = isMultivalue()
			? fields.map(f -> f.getStringFieldList(getName()).getItems())
			: fields.map(f -> f.getStringField(getName())).map(f -> Collections.singletonList(f.getString()));

		return selected
			.map(this::mapToMailAddresses)
			.orElse(Collections.emptyList());
	}

	/**
	 * Map the given option keys to the corresponding email addresses.
	 * @param selectedKeys The list of selected option keys.
	 * @return The list of corresponding email addresses.
	 */
	private List<String> mapToMailAddresses(List<String> selectedKeys) {
		return selectedKeys.stream()
			.filter(StringUtils::isNotBlank)
			.flatMap(keys -> getOptions().stream()
				.filter(item -> keys.contains(item.getKey()))
				.map(ValueListItem::getValue))
			.collect(Collectors.toList());
	}
}
