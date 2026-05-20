package com.gentics.mesh.plugin.forms.model;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.gentics.mesh.plugin.forms.model.schema.FormUiSchema;

import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.plugin.forms.util.RxUtil;

import io.reactivex.Maybe;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.FileUpload;
import io.vertx.reactivex.core.Vertx;
import okhttp3.OkHttpClient;

/**
 * Captcha Element. Implementation uses FriendlyCaptcha.
 * Validation is performed at the service level in FormDataService; this element is a no-op.
 */
@FormElementType("friendlycaptcha")
public class FriendlyCaptchaElement extends FormElement {
	public final static String TITLE_KEY = "error.validation.captcha.title";
	public final static String MESSAGE_KEY = "error.validation.captcha";
	protected final static List<String> FAST_FAILER_SOLUTIONS = Arrays.asList(".FETCHING", ".UNSTARTED", ".UNFINISHED");

	public FriendlyCaptchaElement() {
	}

	protected FriendlyCaptchaElement(FriendlyCaptchaElement other, boolean deep) {
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
		return new FriendlyCaptchaElement(this, deep);
	}

	@Override
	public Maybe<ValidationError> validate(MultiMap postedData, Set<FileUpload> fileUploads, NodeResponse formDataNode, Vertx rxVertx, OkHttpClient httpClient, FormUiSchema uiSchema) {
		// Captcha validation is handled at the service level (FormDataService.validateFriendlyCaptcha)
		return RxUtil.emptyMaybe();
	}

}
