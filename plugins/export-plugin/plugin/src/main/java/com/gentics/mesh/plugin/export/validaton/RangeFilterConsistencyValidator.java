package com.gentics.mesh.plugin.export.validaton;

import com.gentics.mesh.plugin.export.util.Utils;
import com.gentics.mesh.plugin.export.filter.RangeFilter;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Date;

public class RangeFilterConsistencyValidator implements ConstraintValidator<RangeFilterConsistency, RangeFilter> {
    @Override
    public void initialize(RangeFilterConsistency rangeFilterConsistency) {
    }

    @Override
    public boolean isValid(RangeFilter filter, ConstraintValidatorContext context) {
        if (filter.lt == null && filter.lte == null && filter.gt == null && filter.gte == null) {
            Utils.addMessageToContext(context, "A range filter needs to specify one of the following options: ['lt', 'lte', 'gt' 'gte']!");
            return false;
        }

        if (filter.lt != null && filter.lte != null) {
            Utils.addMessageToContext(context, "A range filter may only specify either 'lt' or 'lte', but not both!");
            return false;
        }

        if (filter.gt != null && filter.gte != null) {
            Utils.addMessageToContext(context, "A range filter may only specify either 'gt' or 'gte', but not both!");
            return false;
        }

        if (filter.lt != null && !(filter.lt instanceof String || filter.lt instanceof Number || filter.lt instanceof Date)) {
            Utils.addMessageToContext(context, "The field 'lt' has to be of the following types: 'String', 'Number' or 'Date'!");
            return false;
        }

        if (filter.lte != null && !(filter.lte instanceof String || filter.lte instanceof Number || filter.lte instanceof Date)) {
            Utils.addMessageToContext(context, "The field 'lte' has to be of the following types: 'String', 'Number' or 'Date'!");
            return false;
        }

        if (filter.gt != null && !(filter.gt instanceof String || filter.gt instanceof Number || filter.gt instanceof Date)) {
            Utils.addMessageToContext(context, "The field 'gt' has to be of the following types: 'String', 'Number' or 'Date'!");
            return false;
        }

        if (filter.gte != null && !(filter.gte instanceof String || filter.gte instanceof Number || filter.gte instanceof Date)) {
            Utils.addMessageToContext(context, "The field 'gte' has to be of the following types: 'String', 'Number' or 'Date'!");
            return false;
        }

        return true;
    }
}
