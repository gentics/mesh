package com.gentics.mesh.plugin.forms.model;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import com.gentics.mesh.core.rest.common.FieldContainer;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.plugin.forms.i18n.I18nHelper;
import com.gentics.mesh.plugin.forms.util.RxUtil;

import io.reactivex.Maybe;
import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.reactivex.core.Vertx;
import okhttp3.OkHttpClient;

/**
 * Element for mandatory groups
 */
public class MandatorygroupElement extends FormElement {

	public MandatorygroupElement() {
	}

	protected MandatorygroupElement(MandatorygroupElement other, boolean deep) {
		super(other, deep);
	}

	@Override
	public void addFieldSchema(FieldSchemaContainer container) {
	}

	@Override
	public void addField(FieldContainer container, MultiMap postedData, boolean errors) {
	}

	@Override
	public void addFormData(JsonObject object, FieldContainer container, Function<FormElement, String> key) {
	}

	@Override
	protected FormElement clone(boolean deep) {
		return new MandatorygroupElement(this, deep);
	}

	@Override
	public String getSingleFormData(MultiMap postedData) {
		return null;
	}

	@Override
	public List<String> getMultiFormData(MultiMap postedData) {
		return null;
	}

	@Override
	public Maybe<ValidationError> validate(MultiMap postedData, Set<FileUpload> fileUploads, NodeResponse formDataNode, Vertx rxVertx, OkHttpClient httpClient) {
		if (skipElement(postedData)) {
			return RxUtil.emptyMaybe();
		}

		if (!getElements().stream().filter(element -> {
			// filter elements, that are filled
			List<String> values = element.getMultiFormData(postedData);
			if (values == null) {
				return false;
			}
			return values.stream().anyMatch(v -> v != null && !"".equals(v));
		}).findFirst().isPresent()) {
			return Maybe.just(new ValidationError().setName(getName()).setTitle(I18nHelper.get(Locale.GERMAN, "error.mandatory_group_not_filled.title"))
					.setMessage(I18nHelper.get(Locale.GERMAN, "error.mandatory_group_not_filled")));
		}
		return RxUtil.emptyMaybe();
	}

	@Override
	public Optional<ValidationError> validateForm() {
		if (getElements().isEmpty()) {
			ValidationError error = new ValidationError()
				.setName(getName())
				.setTitle(I18nHelper.get(Locale.GERMAN, "error.validation.form.title"))
				.setMessage(I18nHelper.get(Locale.GERMAN, "error.validation.form.missing_mandatory_elements", getLabel()));

			return Optional.of(error);
		}

		return Optional.empty();
	}
}
