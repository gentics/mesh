package com.gentics.mesh.plugin.forms.model.schema;

import com.gentics.mesh.plugin.forms.context.FormDataCreateContext;
import com.gentics.mesh.plugin.forms.i18n.I18nHelper;
import com.gentics.mesh.plugin.forms.model.ValidationError;
import com.gentics.mesh.plugin.forms.util.RxUtil;
import io.reactivex.Maybe;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

public class Validation {

	/**
	 * Composite of a predicate to check and the translation key to use for the error message.
	 *
	 * @param predicate The predicate to check.
	 * @param key The translation key to use for the error message.
	 */
	private record ValidationConstraint(Predicate<String> predicate, String key) {
	}

	/**
	 * Composite of an invalid value and the translation key to use for the error message.
	 *
	 * @param value The invalid value.
	 * @param key The translation key to use for the error message.
	 */
	public record ValidationConstraintViolation(String value, String key) {
	}

	private Integer minValue;
	private Integer maxValue;
	private Integer minLength;
	private Integer maxLength;
	private String regex;

	/**
	 * The minimum value for the element.
	 *
	 * @return The minimum value.
	 */
	public Integer getMinValue() {
		return minValue;
	}

	/**
	 * Set the minimum value for the element.
	 *
	 * @param minValue The minimum value.
	 * @return Fluent API
	 */
	public Validation setMinValue(Integer minValue) {
		this.minValue = minValue;

		return this;
	}

	/**
	 * Get the maximum value for the element.
	 *
	 * @return The maximum value.
	 */
	public Integer getMaxValue() {
		return maxValue;
	}

	/**
	 * Set the maximum value for the element.
	 *
	 * @param maxValue The maximum value.
	 * @return Fluent API
	 */
	public Validation setMaxValue(Integer maxValue) {
		this.maxValue = maxValue;

		return this;
	}

	/**
	 * Get the minimum length for the element value.
	 *
	 * @return The minimum length for the element value.
	 */
	public Integer getMinLength() {
		return minLength;
	}

	/**
	 * Set the minimum length for the element value.
	 *
	 * @param minLength The minimum length for the element value.
	 * @return Fluent API
	 */
	public Validation setMinLength(Integer minLength) {
		this.minLength = minLength;

		return this;
	}

	/**
	 * The maximum length for the element value.
	 *
	 * @return The maximum length for the element value.
	 */
	public Integer getMaxLength() {
		return maxLength;
	}

	/**
	 * Set the maximum length for the element value.
	 *
	 * @param maxLength The maximum length for the element value.
	 * @return Fluent API
	 */
	public Validation setMaxLength(Integer maxLength) {
		this.maxLength = maxLength;

		return this;
	}

	/**
	 * The regex pattern for the element value.
	 *
	 * @return The regex pattern for the element value.
	 */
	public String getRegex() {
		return regex;
	}

	/**
	 * Set the regex pattern for the element value.
	 *
	 * @param regex The regex pattern for the element value.
	 * @return Fluent API
	 */
	public Validation setRegex(String regex) {
		this.regex = regex;

		return this;
	}

	/**
	 * Validate the given values by checking them against all validation constraints.
	 *
	 * @param ctx The form data create context.
	 * @param name The name of the element.
	 * @param values The values to validate.
	 * @return A {@link Maybe} containing the first validation error or an empty {@link Maybe} if no validation error was found.
	 */
	public Maybe<ValidationError> validate(FormDataCreateContext ctx, String name, Collection<String> values) {
		var translations = ctx.getTranslations();

		return values.stream()
			.filter(Objects::nonNull)
			.flatMap(value -> findViolation(value).stream())
			.findFirst()
			.map(violation -> {
				var error = new ValidationError()
					.setName(name)
					.setTitle(I18nHelper.get(translations, "form_validation_error_%s_title".formatted(violation.key())))
					.setMessage(I18nHelper.get(translations, "form_validation_error_%s".formatted(violation.key()), violation.value()));

				return Maybe.just(error);
			})
			.orElse(RxUtil.emptyMaybe());
	}

	/**
	 * Get all validation constraints for this element.
	 *
	 * @return List of validation constraints.
	 */
	private List<ValidationConstraint> getConstraints() {
		var constraints = new ArrayList<ValidationConstraint>();

		if (minValue != null)
			constraints.add(new ValidationConstraint(
				value -> !NumberUtils.isDigits(value) || Integer.parseInt(value) < minValue,
				"min_value"
			));

		if (maxValue != null)
			constraints.add(new ValidationConstraint(
				value -> !NumberUtils.isDigits(value) || Integer.parseInt(value) > maxValue,
				"max_value"
			));

		if (minLength != null)
			constraints.add(new ValidationConstraint(
				value -> value == null || value.length() < minLength,
				"min_length"
			));

		if (maxLength != null)
			constraints.add(new ValidationConstraint(
				value -> value == null || value.length() > maxLength,
				"max_length"
			));

		if (regex != null)
			constraints.add(new ValidationConstraint(
				value -> value == null || !value.matches(regex),
				"pattern"
			));

		return constraints;
	}

	/**
	 * Find the first validation violation for the given value.
	 *
	 * @param value The value to check.
	 * @return The first validation violation or an empty {@link Optional} if no violation was found.
	 */
	private Optional<ValidationConstraintViolation> findViolation(String value) {
		return getConstraints().stream()
			.filter(c -> c.predicate().test(value))
			.map(c -> new ValidationConstraintViolation(value, c.key()))
			.findFirst();
	}

}
