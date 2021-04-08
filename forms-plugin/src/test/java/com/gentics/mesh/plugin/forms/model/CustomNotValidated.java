package com.gentics.mesh.plugin.forms.model;

import java.util.Optional;

import com.gentics.mesh.plugin.forms.model.Reportable;
import com.gentics.mesh.plugin.forms.model.ValidationError;
import com.gentics.mesh.plugin.forms.model.ValueListElement;

/**
 * Custom form element which is a {@link ValueListElement}, but its validation will always succeed.
 */
@Reportable
public class CustomNotValidated extends ValueListElement {

	@Override
	public Optional<ValidationError> validateForm() {
		return Optional.empty();
	}
}
