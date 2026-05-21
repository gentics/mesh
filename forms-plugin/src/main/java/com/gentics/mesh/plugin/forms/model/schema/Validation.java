package com.gentics.mesh.plugin.forms.model.schema;

import com.gentics.mesh.plugin.forms.i18n.I18nHelper;
import com.gentics.mesh.plugin.forms.model.ValidationError;
import com.gentics.mesh.plugin.forms.util.RxUtil;
import io.reactivex.Maybe;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;

public class Validation {

	private Integer minValue;
	private Integer maxValue;
	private Integer minLength;
	private Integer maxLength;
	private String regex;

	public Integer getMinValue() {
		return minValue;
	}

	public Validation setMinValue(Integer minValue) {
		this.minValue = minValue;

		return this;
	}

	public Integer getMaxValue() {
		return maxValue;
	}

	public Validation setMaxValue(Integer maxValue) {
		this.maxValue = maxValue;

		return this;
	}

	public Integer getMinLength() {
		return minLength;
	}

	public Validation setMinLength(Integer minLength) {
		this.minLength = minLength;

		return this;
	}

	public Integer getMaxLength() {
		return maxLength;
	}

	public Validation setMaxLength(Integer maxLength) {
		this.maxLength = maxLength;

		return this;
	}

	public String getRegex() {
		return regex;
	}

	public Validation setRegex(String regex) {
		this.regex = regex;

		return this;
	}

	public Maybe<ValidationError> validate(String name, Collection<String> values) {
		var invalidValue = values.stream()
			.filter(Objects::nonNull)
			.filter(getFilter())
			.findFirst()
			.orElse(null);

		if (invalidValue != null) {
			var error = new ValidationError()
				.setName(name)
				.setTitle(I18nHelper.get(Locale.GERMAN, "error.validation.%s.title".formatted(name)))
				.setMessage(I18nHelper.get(Locale.GERMAN, "error.validation.%s".formatted(name), invalidValue));

			return Maybe.just(error);
		} else {
			return RxUtil.emptyMaybe();
		}
	}

	private Predicate<String> getFilter() {
		Predicate<String> filter = null;

		if (minValue != null) {
			filter = value -> !NumberUtils.isDigits(value) || Integer.parseInt(value) < minValue;
		}

		if (maxValue != null) {
			Predicate<String> f = value -> !NumberUtils.isDigits(value) || Integer.parseInt(value) > maxValue;

			filter = filter == null ? f : filter.or(f);
		}

		if (minLength != null) {
			Predicate<String> f = value -> value != null && value.length() < minLength;

			filter = filter == null ? f : filter.or(f);
		}

		if (maxLength != null) {
			Predicate<String> f = value -> value != null && value.length() > maxLength;

			filter = filter == null ? f : filter.or(f);
		}

		if (regex != null) {
			Predicate<String> f = value -> value != null && !value.matches(regex);

			filter = filter == null ? f : filter.or(f);
		}

		if (filter == null) {
			filter = value -> false;
		}

		return filter;
	}
}
