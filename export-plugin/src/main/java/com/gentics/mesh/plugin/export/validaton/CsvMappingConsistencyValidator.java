package com.gentics.mesh.plugin.export.validaton;

import com.gentics.mesh.plugin.export.Utils;
import com.gentics.mesh.plugin.export.model.AbstractCsvMappingDefinition;
import com.gentics.mesh.plugin.export.model.CsvMappingDefinition;
import com.gentics.mesh.plugin.export.model.CsvSubMappingDefinition;
import org.apache.commons.lang3.StringUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;

public class CsvMappingConsistencyValidator implements ConstraintValidator<CsvMappingConsistency, CsvMappingDefinition> {

    @Override
    public void initialize(CsvMappingConsistency csvRequestValidator) {
    }

    @Override
    public boolean isValid(CsvMappingDefinition mapping, ConstraintValidatorContext context) {
        if (!isValidAbstractMapping(mapping, context)) {
            return false;
        }

        if (mapping.getType().isMultiColumn()) {
            if (!(mapping instanceof CsvMappingDefinition)) {
                Utils.addMessageToContext(context, "The mapping could not be interpreted as proper multi-column mapping.");
                return false;
            }

            switch (mapping.getType()) {
                case SUB_NODE:
                    return isValidMultiColumnMapping(mapping, context);

                case FOREIGN_REFERENCE:
                    return isValidForeignReferenceMapping(mapping, context);
            }
        } else {
            if ((mapping.getListType() == null || !mapping.getListType().isMultiColumn()) && StringUtils
                    .trimToEmpty(mapping.getLabel())
                    .isEmpty()) {
                Utils.addMessageToContext(context, "Mappings without sub-mappings are required to define a Label!");
                return false;
            }
            if ((mapping.getListType() == null || !mapping.getListType().isMultiColumn()) && mapping.getSubFilter() != null) {
                Utils.addMessageToContext(context, "You may not define the 'subFilter' when it is not a multi-column definition!");
                return false;
            }
        }

        return true;
    }


    public static boolean isValidAbstractMapping(
            AbstractCsvMappingDefinition mapping, ConstraintValidatorContext context
    ) {
        switch (mapping.getType()) {
            case LIST:
                return isValidListMapping(mapping, context);

            case CONFIG_VALUE:
                if (!isValidConfigValueMapping(mapping, context)) {
                    return false;
                }
                break;
        }

        return true;
    }

    public static boolean isValidListMapping(AbstractCsvMappingDefinition mapping, ConstraintValidatorContext context) {
        if (mapping.getListType() == null) {
            Utils.addMessageToContext(context, "The data type for the list cannot be empty");
            return false;
        }

        switch (mapping.getListType()) {
            case LIST:
                Utils.addMessageToContext(context, "'LIST' data type is not allowed in the listType property");
                return false;

            case CONFIG_VALUE:
                if (!isValidConfigValueMapping(mapping, context)) {
                    return false;
                }
                break;
        }

        if (mapping.getListType().isMultiColumn()) {
            if (!(mapping instanceof CsvMappingDefinition)) {
                Utils.addMessageToContext(context, "The mapping could not be interpreted as proper multi-column mapping.");
                return false;
            }

            CsvMappingDefinition def = (CsvMappingDefinition) mapping;

            switch (mapping.getListType()) {
                case SUB_NODE:
                    return isValidMultiColumnMapping(def, context);

                case FOREIGN_REFERENCE:
                    return isValidForeignReferenceMapping(def, context);
            }
        }

        return true;
    }

    public static boolean isValidConfigValueMapping(
            AbstractCsvMappingDefinition mapping, ConstraintValidatorContext context
    ) {
        if (StringUtils.trimToEmpty(mapping.getConfigKey()).isEmpty()) {
            Utils.addMessageToContext(context, "Config key cannot be empty when data type is 'CONFIG_VALUE'");
            return false;
        }

        return true;
    }

    private static boolean isValidForeignReferenceMapping(
            CsvMappingDefinition mapping, ConstraintValidatorContext context
    ) {
        if (!isValidMultiColumnMapping(mapping, context)) {
            return false;
        }

        if (StringUtils.trimToEmpty(mapping.getReferenceGraphQuery()).isEmpty()) {
            Utils.addMessageToContext(context, "The field 'referenceGraphQuery' may not be empty!");
            return false;
        }

        if (StringUtils.trimToEmpty(mapping.getReferenceSearchQuery()).isEmpty()) {
            Utils.addMessageToContext(context, "The field 'referenceSearchQuery' may not be empty!");
            return false;
        }

        if (mapping.getReferenceMaxCount() == null || mapping.getReferenceMaxCount() < 1) {
            Utils.addMessageToContext(context, "The field 'referenceMaxCount' has to be bigger than zero!");
            return false;
        }

        return true;
    }

    private static boolean isValidMultiColumnMapping(CsvMappingDefinition mapping, ConstraintValidatorContext context) {
        List<CsvSubMappingDefinition> subMapping = mapping.getSubMapping();
        if (subMapping == null || subMapping.isEmpty()) {
            Utils.addMessageToContext(context, "A multi-column definition requires a sub-mapping to be present!");
            return false;
        }

        return true;
    }
}
