package com.gentics.mesh.plugin.export.validaton;

import com.gentics.mesh.plugin.export.util.Utils;
import com.gentics.mesh.plugin.export.model.CsvSubMappingDefinition;
import org.apache.commons.lang3.StringUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class CsvSubMappingConsistencyValidator implements ConstraintValidator<CsvSubMappingConsistency, CsvSubMappingDefinition> {

    @Override
    public void initialize(CsvSubMappingConsistency consistency) {
    }

    @Override
    public boolean isValid(CsvSubMappingDefinition definition, ConstraintValidatorContext context) {
        if (definition.getType().isMultiColumn()) {
            Utils.addMessageToContext(context,
                    "A Sub-Mapping may not contain another defintion of a Multi-Column type ('" + definition
                            .getType()
                            .name() + "')!");
            return false;
        }

        if (!CsvMappingConsistencyValidator.isValidAbstractMapping(definition, context)) {
            return false;
        }

        if (StringUtils.trimToEmpty(definition.getLabel()).isEmpty()) {
            Utils.addMessageToContext(context, "A Sub-Mapping defintion requires a Label to be set!");
            return false;
        }

        return true;
    }
}
