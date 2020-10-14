package com.gentics.mesh.plugin.forms.dagger;

import java.io.File;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.common.dagger.AdminClient;
import com.gentics.mesh.plugin.common.dagger.StorageDir;
import com.gentics.mesh.plugin.common.dagger.UserClient;
import com.gentics.mesh.plugin.forms.SchemaInitializer;
import com.gentics.mesh.plugin.forms.config.FormsConfig;
import com.gentics.mesh.plugin.forms.handler.DeleteFormDataHandler;
import com.gentics.mesh.plugin.forms.handler.DeleteFormHandler;
import com.gentics.mesh.plugin.forms.handler.ExportHandler;
import com.gentics.mesh.plugin.forms.handler.FindFormDataHandler;
import com.gentics.mesh.plugin.forms.handler.ListFormsHandler;
import com.gentics.mesh.plugin.forms.handler.LoadFilesZipHandler;
import com.gentics.mesh.plugin.forms.handler.LoadFormDataBinaryHandler;
import com.gentics.mesh.plugin.forms.handler.LoadFormDataHandler;
import com.gentics.mesh.plugin.forms.handler.LoadFormElementsHandler;
import com.gentics.mesh.plugin.forms.handler.LoadFormHandler;
import com.gentics.mesh.plugin.forms.handler.LoadFormSchemaHandler;
import com.gentics.mesh.plugin.forms.handler.PostDataHandler;
import com.gentics.mesh.plugin.forms.handler.PostJsonDataHandler;
import com.gentics.mesh.plugin.forms.handler.RenderFormDataHandler;
import com.gentics.mesh.plugin.forms.handler.RenderFormHandler;
import com.gentics.mesh.plugin.forms.handler.RenderFormPreviewAsJsonHandler;
import com.gentics.mesh.plugin.forms.handler.RenderFormPreviewHandler;
import com.gentics.mesh.plugin.forms.handler.UpdatePostedDataHandler;
import com.gentics.mesh.plugin.forms.handler.UpdatePostedJsonDataHandler;
import com.gentics.mesh.plugin.forms.handler.UpsertFormHandler;
import com.gentics.mesh.plugin.forms.handler.status.ActivateHandler;
import com.gentics.mesh.plugin.forms.handler.status.CheckStatusHandler;
import com.gentics.mesh.plugin.forms.handler.status.DeactivateHandler;
import com.gentics.mesh.rest.client.MeshRestClient;

import dagger.BindsInstance;
import dagger.Component;
import io.vertx.core.Vertx;

@Singleton
@Component(modules = { FormsPluginModule.class })
public interface FormsPluginComponent {
	SchemaInitializer schemaInitializer();

	CheckStatusHandler checkStatusHandler();

	ActivateHandler activateHandler();

	DeactivateHandler deactivateHandler();

	UpsertFormHandler upsertFormHandler();

	ListFormsHandler listFormsHandler();

	LoadFormHandler loadFormHandler();

	DeleteFormHandler deleteFormHandler();

	LoadFormElementsHandler loadFormElementsHandler();

	LoadFormSchemaHandler loadFormSchemaHandler();

	RenderFormHandler renderFormHandler();

	RenderFormPreviewHandler renderFormPreviewHandler();

	RenderFormPreviewAsJsonHandler renderFormPreviewAsJsonHandler();

	PostDataHandler postDataHandler();

	PostJsonDataHandler postJsonDataHandler();

	UpdatePostedDataHandler updatePostedDataHandler();

	UpdatePostedJsonDataHandler updatePostedJsonDataHandler();

	ExportHandler exportHandler();

	LoadFormDataHandler loadFormDataHandler();

	FindFormDataHandler findFormDataHandler();

	LoadFormDataBinaryHandler loadFormDataBinaryHandler();

	DeleteFormDataHandler deleteFormDataHandler();

	RenderFormDataHandler renderFormDataHandler();

	LoadFilesZipHandler loadFilesZipHandler();

	@Component.Builder
	interface Builder {
		@BindsInstance
		Builder configuration(FormsConfig configuration);

		@BindsInstance
		Builder adminClient(@AdminClient MeshRestClient client);

		@BindsInstance
		@Nullable
		Builder userClient(@UserClient @Nullable MeshRestClient client);

		@BindsInstance
		Builder rootNodeUuids(@RootNodeUuids Map<String, String> rootNodeUuids);

		@BindsInstance
		Builder baseUrls(@BaseUrls Map<String, String> baseUrls);

		@BindsInstance
		Builder vertx(Vertx vertx);

		@BindsInstance
		Builder storageDir(@StorageDir File storageDir);

		FormsPluginComponent build();
	}
}
