package com.gentics.mesh.plugin.export.validaton;

import com.gentics.mesh.plugin.export.util.Utils;
import com.gentics.mesh.plugin.export.model.AbstractCsvMappingDefinition;
import com.gentics.mesh.plugin.export.model.CsvMappingDefinition;
import com.gentics.mesh.plugin.export.model.CsvSubMappingDefinition;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.regex.Pattern;

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
		AbstractCsvMappingDefinition mapping,
		ConstraintValidatorContext context
	) {
		if (mapping.getType() == null) {
			Utils.addMessageToContext(context, "You need to specify a type!");
			return false;
		}

		switch (mapping.getType()) {
			case LIST:
				return isValidListMapping(mapping, context);

			case CONFIG_VALUE:
				if (!isValidConfigValueMapping(mapping, context)) {
					return false;
				}
				break;

            case TEMPLATE:
                if (!isValidTemplateMapping(mapping, context)) {
                    return false;
                }
                break;

			case REGEXP:
				if (!isValidRegexpMapping(mapping, context)) {
					return false;
				}
				break;
		}

		if (mapping.getDefaultValue() != null) {
			Object val = mapping.getDefaultValue();
			boolean validDefault = true;

			switch (mapping.getType()) {
				case LIST:
					if (!(val instanceof JsonArray)) {
						validDefault = false;
					}
					break;
				case NUMBER:
					if (!(val instanceof Number)) {
						validDefault = false;
					}
					break;
				case CONFIG_VALUE:
				case STRING:
				case REGEXP:
				case DATE:
					if (!(val instanceof String)) {
						validDefault = false;
					}
					break;
				case FOREIGN_REFERENCE:
				case SUB_NODE:
					if (!(val instanceof JsonObject)) {
						validDefault = false;
					}
					break;
				case BOOLEAN:
					if (!(val instanceof Boolean)) {
						validDefault = false;
					}
					break;
			}

			if (!validDefault) {
				Utils.addMessageToContext(context, "The default value has to match with the Mapping type!");
				return false;
			}
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

            case TEMPLATE:
                if (!isValidTemplateMapping(mapping, context)) {
                    return false;
                }
                break;

			case REGEXP:
				if (!isValidRegexpMapping(mapping, context)) {
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
		AbstractCsvMappingDefinition mapping,
        ConstraintValidatorContext context
	) {
		if (StringUtils.trimToEmpty(mapping.getConfigKey()).isEmpty()) {
			Utils.addMessageToContext(context, "Config key cannot be empty when data type is 'CONFIG_VALUE'");
			return false;
		}

		return true;
	}

	public static boolean isValidTemplateMapping(
	    AbstractCsvMappingDefinition mapping,
        ConstraintValidatorContext context
    ) {
	    if (StringUtils.trimToEmpty(mapping.getTemplateDefinition()).isEmpty() ||
            !mapping.getTemplateDefinition().contains(AbstractCsvMappingDefinition.TEMPLATE_REPLACE_TOKEN)) {
	        Utils.addMessageToContext(context, "The field 'templateDefinition' may not be empty and must contain the replace token '" + AbstractCsvMappingDefinition.TEMPLATE_REPLACE_TOKEN + "'!");
	        return false;
        }

	    return true;
    }

    public static boolean isValidRegexpMapping(
    	AbstractCsvMappingDefinition mapping,
		ConstraintValidatorContext context
	) {
		if (StringUtils.trimToEmpty(mapping.getRegexpDefinition()).isEmpty()) {
			Utils.addMessageToContext(context, "The field 'regexpDefinition' may not be empty!");
			return false;
		}

		try {
			Pattern.compile(mapping.getRegexpDefinition(), mapping.getRegexpFlags());
		} catch (Exception e) {
			Utils.addMessageToContext(context, "Invalid regexp in field 'regexpDefinition'! " + e.getMessage());
			return false;
		}

		if (StringUtils.trimToEmpty(mapping.getRegexpTemplate()).isEmpty()) {
			Utils.addMessageToContext(context, "The field 'regexpTemplate' may not be empty!");
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

		if (mapping.getReferenceMaximalNodes() == null || mapping.getReferenceMaximalNodes() < 1) {
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
