package com.gentics.mesh.plugin.forms.model;

import java.util.List;
import java.util.Set;

import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.plugin.forms.model.schema.FormUiSchema;
import com.gentics.mesh.plugin.forms.util.RxUtil;

import io.reactivex.Maybe;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.FileUpload;
import io.vertx.reactivex.core.Vertx;
import okhttp3.OkHttpClient;

/**
 * Captcha Element for reCAPTCHA. Validation is performed at the service level (CaptchaService); this element is a no-op.
 */
@FormElementType("recaptcha")
public class ReCaptchaElement extends FormElement {
	public ReCaptchaElement() {
	}

	protected ReCaptchaElement(ReCaptchaElement other, boolean deep) {
		super(other, deep);
	}

	@Override
	public void addFieldSchema(FieldSchemaContainer container) {
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
	protected FormElement clone(boolean deep) {
		return new ReCaptchaElement(this, deep);
	}

	@Override
	public Maybe<ValidationError> validate(MultiMap postedData, Set<FileUpload> fileUploads, NodeResponse formDataNode, Vertx rxVertx, OkHttpClient httpClient, FormUiSchema uiSchema) {
		return RxUtil.emptyMaybe();
	}
}
