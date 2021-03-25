package com.gentics.mesh.plugin.export.validaton;

import com.gentics.mesh.plugin.export.util.Utils;
import com.gentics.mesh.plugin.export.model.Filter;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class FilterConsistencyValidator implements ConstraintValidator<FilterConsistency, Filter> {

    @Override
    public void initialize(FilterConsistency filterConsistency) {
    }

    @Override
    public boolean isValid(Filter filter, ConstraintValidatorContext context) {
        int count = 0;
        if (filter.and != null) {
            count++;
        }
        if (filter.or != null) {
            count++;
        }
        if (filter.not != null) {
            count++;
        }
        if (filter.empty != null) {
            count++;
        }
        if (filter.exists != null) {
            count++;
        }
        if (filter.equals != null) {
            count++;
        }
        if (filter.range != null) {
            count++;
        }
        if (filter.in != null) {
            count++;
        }

        if (count == 0) {
            Utils.addMessageToContext(context, "You need to specify a type of filter!");
            return false;
        }

        if (count != 1) {
            Utils.addMessageToContext(context, "A Filter may only specify one type!");
            return false;
        }

        if (filter.and != null && filter.and.isEmpty()) {
            Utils.addMessageToContext(context, "The 'add' filter may not be empty!");
            return false;
        }

        if (filter.or != null && filter.or.isEmpty()) {
            Utils.addMessageToContext(context, "The 'or' filter may not be empty!");
            return false;
        }

        return true;
    }
}
