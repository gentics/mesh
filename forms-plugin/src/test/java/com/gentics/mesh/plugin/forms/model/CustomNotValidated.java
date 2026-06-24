package com.gentics.mesh.plugin.forms.model;

import java.util.Optional;

import com.gentics.mesh.plugin.forms.model.schema.FormUiSchema;

/**
 * Custom form element which is a {@link ValueListElement}, but its validation will always succeed.
 */
@Reportable
public class CustomNotValidated extends ValueListElement {

	public CustomNotValidated() {
	}

	protected CustomNotValidated(CustomNotValidated other, boolean deep) {
		super(other, deep);
	}

	@Override
	public Optional<ValidationError> validateForm(String language, FormUiSchema uiSchema) {
		return Optional.empty();
	}

	@Override
	protected FormElement clone(boolean deep) {
		return new CustomNotValidated(this, deep);
	}
}
