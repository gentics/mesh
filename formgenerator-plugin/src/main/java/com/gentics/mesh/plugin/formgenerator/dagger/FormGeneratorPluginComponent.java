package com.gentics.mesh.plugin.formgenerator.dagger;

import java.io.File;
import java.util.Map;

import javax.inject.Singleton;

import com.gentics.mesh.plugin.common.dagger.AdminClient;
import com.gentics.mesh.plugin.common.dagger.StorageDir;
import com.gentics.mesh.plugin.formgenerator.SchemaInitializer;
import com.gentics.mesh.plugin.formgenerator.config.FormGeneratorConfig;
import com.gentics.mesh.plugin.formgenerator.handler.CreateFormHandler;
import com.gentics.mesh.plugin.formgenerator.handler.CreateTemplateHandler;
import com.gentics.mesh.plugin.formgenerator.handler.DeleteFormDataHandler;
import com.gentics.mesh.plugin.formgenerator.handler.DeleteFormHandler;
import com.gentics.mesh.plugin.formgenerator.handler.DeleteTemplateHandler;
import com.gentics.mesh.plugin.formgenerator.handler.ExportHandler;
import com.gentics.mesh.plugin.formgenerator.handler.FindFormDataHandler;
import com.gentics.mesh.plugin.formgenerator.handler.ListFormsHandler;
import com.gentics.mesh.plugin.formgenerator.handler.ListTemplatesHandler;
import com.gentics.mesh.plugin.formgenerator.handler.LoadFilesZipHandler;
import com.gentics.mesh.plugin.formgenerator.handler.LoadFormDataBinaryHandler;
import com.gentics.mesh.plugin.formgenerator.handler.LoadFormDataHandler;
import com.gentics.mesh.plugin.formgenerator.handler.LoadFormElementsHandler;
import com.gentics.mesh.plugin.formgenerator.handler.LoadFormHandler;
import com.gentics.mesh.plugin.formgenerator.handler.LoadFormSchemaHandler;
import com.gentics.mesh.plugin.formgenerator.handler.LoadTemplateHandler;
import com.gentics.mesh.plugin.formgenerator.handler.PostDataHandler;
import com.gentics.mesh.plugin.formgenerator.handler.PostJsonDataHandler;
import com.gentics.mesh.plugin.formgenerator.handler.RenderFormDataHandler;
import com.gentics.mesh.plugin.formgenerator.handler.RenderFormHandler;
import com.gentics.mesh.plugin.formgenerator.handler.UpdateFormHandler;
import com.gentics.mesh.plugin.formgenerator.handler.UpdatePostedDataHandler;
import com.gentics.mesh.plugin.formgenerator.handler.UpdatePostedJsonDataHandler;
import com.gentics.mesh.rest.client.MeshRestClient;

import dagger.BindsInstance;
import dagger.Component;
import io.vertx.core.Vertx;

@Singleton
@Component(modules = { FormGeneratorPluginModule.class })
public interface FormGeneratorPluginComponent {
	SchemaInitializer schemaInitializer();

	CreateFormHandler createFormHandler();

	ListFormsHandler listFormsHandler();

	LoadFormHandler loadFormHandler();

	UpdateFormHandler updateFormHandler();

	DeleteFormHandler deleteFormHandler();

	LoadFormElementsHandler loadFormElementsHandler();

	LoadFormSchemaHandler loadFormSchemaHandler();

	RenderFormHandler renderFormHandler();

	PostDataHandler postDataHandler();

	PostJsonDataHandler postJsonDataHandler();

	UpdatePostedDataHandler updatePostedDataHandler();

	UpdatePostedJsonDataHandler updatePostedJsonDataHandler();

	ExportHandler exportHandler();

	LoadFormDataHandler loadFormDataHandler();

	FindFormDataHandler findFormDataHandler();

	LoadFormDataBinaryHandler loadFormDataBinaryHandler();

	DeleteFormDataHandler deleteFormDataHandler();

	CreateTemplateHandler createTemplateHandler();

	ListTemplatesHandler listTemplatesHandler();

	LoadTemplateHandler loadTemplateHandler();

	DeleteTemplateHandler deleteTemplateHandler();

	RenderFormDataHandler renderFormDataHandler();

	LoadFilesZipHandler loadFilesZipHandler();

	@Component.Builder
	interface Builder {
		@BindsInstance
		Builder configuration(FormGeneratorConfig configuration);

		@BindsInstance
		Builder adminClient(@AdminClient MeshRestClient client);

		@BindsInstance
		Builder rootNodeUuids(@RootNodeUuids Map<String, String> rootNodeUuids);

		@BindsInstance
		Builder baseUrls(@BaseUrls Map<String, String> baseUrls);

		@BindsInstance
		Builder vertx(Vertx vertx);

		@BindsInstance
		Builder storageDir(@StorageDir File storageDir);

		FormGeneratorPluginComponent build();
	}
}
