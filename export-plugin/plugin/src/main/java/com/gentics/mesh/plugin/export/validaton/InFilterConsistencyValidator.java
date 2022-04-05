package com.gentics.mesh.plugin.export.validaton;

import com.gentics.mesh.plugin.export.filter.InFilter;
import com.gentics.mesh.plugin.export.util.Utils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class InFilterConsistencyValidator implements ConstraintValidator<EqualsFilterConsistency, InFilter> {

	@Override
	public void initialize(EqualsFilterConsistency filterConsistency) {
	}

	@Override
	public boolean isValid(InFilter filter, ConstraintValidatorContext context) {
		if (filter.getValues() != null && filter.getValuesPath() != null) {
			Utils.addMessageToContext(context, "You may only specify 'values' or 'valuesPath', but not both!");
			return false;
		}

		return true;
	}
}
