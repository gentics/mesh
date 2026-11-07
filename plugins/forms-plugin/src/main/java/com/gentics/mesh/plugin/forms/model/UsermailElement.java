package com.gentics.mesh.plugin.forms.model;

import java.util.concurrent.Callable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;

import com.gentics.mesh.plugin.forms.context.FormDataCreateContext;
import com.gentics.mesh.plugin.forms.i18n.I18nHelper;
import com.gentics.mesh.plugin.forms.model.schema.FormUiSchema;
import com.gentics.mesh.plugin.forms.util.RxUtil;

import io.reactivex.Maybe;
import io.vertx.reactivex.core.Vertx;
import okhttp3.OkHttpClient;

/**
 * Usermail element
 */
@Reportable
@FormElementType("usermail")
public class UsermailElement extends FormElement {

	public UsermailElement() {
	}

	protected UsermailElement(UsermailElement other, boolean deep) {
		super(other, deep);
	}

	/**
	 * Get the mail subject in the given language (or null if not found)
	 * @param language language
	 * @return subject
	 */
	public String getSubject(String language) {
		return getStringFromFormGridOptions("mailsubject", language);
	}

	/**
	 * Get the path to the user mail template.
	 * @return The path to the user mail template.
	 */
	public String getTemplate() {
		return getStringFromFormGridOptions("mailtemplate");
	}

	/**
	 * Get the user mail template source.
	 * @return The user mail template source.
	 */
	public String getMailsource(String language) {
		return getStringFromFormGridOptions("mailsource", language);
	}

	@Override
	public Maybe<ValidationError> validate(FormDataCreateContext ctx, Vertx rxVertx, OkHttpClient httpClient, FormUiSchema uiSchema) {
		var translations = ctx.getTranslations();

		Callable<Maybe<ValidationError>> ifSuperIsEmpty = () -> {
			var value = getSingleFormData(ctx.getPostedData());
			var emailValidator = EmailValidator.getInstance();

			if (StringUtils.isNotBlank(value) && !emailValidator.isValid(value)) {
				var error = new ValidationError()
					.setName(name)
					.setTitle(I18nHelper.get(translations, "form_validation_error_email_title"))
					.setMessage(I18nHelper.get(translations, "form_validation_error_email", value));

				return Maybe.just(error);
			} else {
				return RxUtil.emptyMaybe();
			}
		};
		return super.validate(ctx, rxVertx, httpClient, uiSchema)
				.switchIfEmpty(Maybe.fromCallable(ifSuperIsEmpty).flatMap(x -> x));
	}

	@Override
	protected FormElement clone(boolean deep) {
		return new UsermailElement(this, deep);
	}
}
