package com.gentics.mesh.plugin.forms.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.validator.routines.EmailValidator;

import com.gentics.mesh.plugin.forms.i18n.I18nHelper;

import io.reactivex.Maybe;

/**
 * Enumeration of input validators
 */
public enum Validator {
	number(v -> isNumber(v)), telephone(v -> isTelephone(v)), socialsecurity(v -> isSocialSecurity(v)), date(v -> isDate(v)), time(v -> isTime(v)), 
	email(v -> isEmail(v)), datepicker(v -> isDatepicker(v)), zaehlerpunkt(v -> isZaehlerpunkt(v));

	private static Pattern TELEPHONE_PATTERN = Pattern.compile("^[\\s\\d\\+\\/]*$");

	private static Pattern SOCIALSECURITY_PATTERN = Pattern.compile("^\\s*(\\d{4})(?<date>\\d{6})\\s*$");
	
	private static Pattern ZAEHLERPUNKT_PATTERN = Pattern.compile("^.*\\.\\d{8}$");

	/**
	 * Check whether the value is a number
	 * @param value value to check
	 * @return true for number
	 */
	private static boolean isNumber(String value) {
		try {
			Integer.parseInt(value);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	/**
	 * Check whether the value is a telephone number
	 * @param value value to check
	 * @return true for telephone number
	 */
	private static boolean isTelephone(String value) {
		return TELEPHONE_PATTERN.matcher(value).matches();
	}

	/**
	 * Check whether the value is a social security number
	 * @param value value to check
	 * @return true for social security number
	 */
	private static boolean isSocialSecurity(String value) {
		Matcher matcher = SOCIALSECURITY_PATTERN.matcher(value);
		if (!matcher.matches()) {
			return false;
		}
		SimpleDateFormat format = new SimpleDateFormat("ddMMyy");
		try {
			Date date = format.parse(matcher.group("date"));
			return date.before(new Date());
		} catch (ParseException e) {
			return false;
		}
	}
	
	/**
	 * Check whether the value is a valid zaehlerpunkt (only digits, min/max 8 characters)
	 * 
	 * @param value value to check
	 * @return true for valid zaehlerpunkt values
	 */
	private static boolean isZaehlerpunkt(String value) {
		Matcher matcher = ZAEHLERPUNKT_PATTERN.matcher(value);
		if (!matcher.matches()) {
			return false;
		}
		return true;
	}

	/**
	 * Check whether the value is a date
	 * @param value value to check
	 * @return true for date
	 */
	private static boolean isDate(String value) {
//		SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		try {
			format.parse(value);
			return true;
		} catch (ParseException e) {
			return false;
		}
	}
	
	private static boolean isDatepicker(String value) {
		SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
		try {
			format.parse(value);
			return true;
		} catch (ParseException e) {
			return false;
		}
	}

	/**
	 * Check whether the value is a time
	 * @param value value to check
	 * @return true for time
	 */
	private static boolean isTime(String value) {
		SimpleDateFormat format = new SimpleDateFormat("HH:mm");
		try {
			format.parse(value);
			return true;
		} catch (ParseException e) {
			return false;
		}
	}

	/**
	 * Check whether the value is an email address
	 * @param value value to check
	 * @return true for email address
	 */
	private static boolean isEmail(String value) {
		return EmailValidator.getInstance().isValid(value);
	}

	/**
	 * Get the validator or null if not known
	 * @param type type string
	 * @return type or null
	 */
	public static Validator get(String type) {
		try {
			return valueOf(type);
		} catch (Throwable t) {
			return null;
		}
	}

	/**
	 * Predicate to check validity
	 */
	private Predicate<? super String> predicate;

	/**
	 * Create instance
	 * @param predicate predicate
	 */
	private Validator(Predicate<? super String> predicate) {
		this.predicate = predicate;
	}

	/**
	 * Validate a list of values. Return ValidationError for first invalid value or null if all values are valid
	 * @param name field name
	 * @param values values to validate
	 * @return ValidationError or null
	 */
	public Maybe<ValidationError> validate(String name, List<String> values) {
		String invalidValue = values.stream()
			.filter(Objects::nonNull)
			.filter(predicate.negate())
			.findFirst()
			.orElse(null);

		if (invalidValue != null) {
			String titleKey = String.format("error.validation.%s.title", name());
			String messageKey = String.format("error.validation.%s", name());
			return Maybe.just(new ValidationError().setName(name).setTitle(I18nHelper.get(Locale.GERMAN, titleKey))
					.setMessage(I18nHelper.get(Locale.GERMAN, messageKey, invalidValue)));
		} else {
			return Maybe.empty();
		}
	}
}
