package com.gentics.mesh.plugin.export.validaton;

import com.gentics.mesh.plugin.export.filter.EqualsFilter;
import com.gentics.mesh.plugin.export.util.Utils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class EqualsFilterConsistencyValidator implements ConstraintValidator<EqualsFilterConsistency, EqualsFilter> {

	@Override
	public void initialize(EqualsFilterConsistency filterConsistency) {
	}

	@Override
	public boolean isValid(EqualsFilter filter, ConstraintValidatorContext context) {
		if (filter.getValue() != null && filter.getValuePath() != null) {
			Utils.addMessageToContext(context, "You may only specify 'value' or 'valuePath', but not both!");
			return false;
		}

		return true;
	}
}
