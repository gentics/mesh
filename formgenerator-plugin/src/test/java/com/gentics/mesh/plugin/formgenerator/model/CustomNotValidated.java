package com.gentics.mesh.plugin.formgenerator.model;

import java.util.Optional;

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
