package com.gentics.mesh.plugin.forms.dagger;

import java.io.File;

import javax.annotation.Nullable;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.PluginManifest;
import com.gentics.mesh.plugin.common.dagger.AdminClient;
import com.gentics.mesh.plugin.common.dagger.StorageDir;
import com.gentics.mesh.plugin.common.dagger.UserClient;
import com.gentics.mesh.plugin.forms.config.FormsConfig;
import com.gentics.mesh.plugin.forms.handler.DeleteFormDataHandler;
import com.gentics.mesh.plugin.forms.handler.FindFormDataHandler;
import com.gentics.mesh.plugin.forms.handler.LoadFormDataBinaryHandler;
import com.gentics.mesh.plugin.forms.handler.PurgeStaleDataHandler;
import com.gentics.mesh.plugin.forms.handler.SaveDataHandler;
import com.gentics.mesh.plugin.forms.handler.SubmitDataHandler;
import com.gentics.mesh.plugin.forms.handler.ValidationHandler;
import com.gentics.mesh.plugin.forms.handler.downloads.CheckDownloadHandler;
import com.gentics.mesh.plugin.forms.handler.downloads.DownloadHandler;
import com.gentics.mesh.plugin.forms.handler.downloads.ExportHandler;
import com.gentics.mesh.plugin.forms.handler.downloads.LoadFilesZipHandler;
import com.gentics.mesh.plugin.forms.handler.downloads.RequestExportHandler;
import com.gentics.mesh.plugin.forms.handler.downloads.RequestFilesZipHandler;
import com.gentics.mesh.plugin.forms.handler.form.DeleteFormHandler;
import com.gentics.mesh.plugin.forms.handler.form.GetFormOnlineStatusHandler;
import com.gentics.mesh.plugin.forms.handler.form.ListFormsHandler;
import com.gentics.mesh.plugin.forms.handler.form.LoadFormElementsHandler;
import com.gentics.mesh.plugin.forms.handler.form.LoadFormGenHandler;
import com.gentics.mesh.plugin.forms.handler.form.LoadFormHandler;
import com.gentics.mesh.plugin.forms.handler.form.LoadFormSchemaHandler;
import com.gentics.mesh.plugin.forms.handler.form.LoadPollResultsHandler;
import com.gentics.mesh.plugin.forms.handler.form.SetFormOfflineHandler;
import com.gentics.mesh.plugin.forms.handler.form.SetFormOnlineHandler;
import com.gentics.mesh.plugin.forms.handler.form.UpsertFormHandler;
import com.gentics.mesh.plugin.forms.handler.formtypeconfig.SetFormtypeConfigsHandler;
import com.gentics.mesh.plugin.forms.handler.translation.GetTranslationsHandler;
import com.gentics.mesh.plugin.forms.handler.translation.SetTranslationsHandler;
import com.gentics.mesh.plugin.forms.handler.status.ActivateHandler;
import com.gentics.mesh.plugin.forms.handler.status.CheckStatusHandler;
import com.gentics.mesh.plugin.forms.handler.status.DeactivateHandler;
import com.gentics.mesh.plugin.forms.handler.status.GetLanguagesHandler;
import com.gentics.mesh.plugin.forms.handler.status.SetLanguagesHandler;
import com.gentics.mesh.plugin.forms.service.FormDataService;
import com.gentics.mesh.plugin.forms.service.PurgeService;
import com.gentics.mesh.plugin.forms.service.StatusService;
import com.gentics.mesh.rest.client.MeshRestClient;

import dagger.BindsInstance;
import dagger.Component;
import io.vertx.core.Vertx;

@Singleton
@Component(modules = { FormsPluginModule.class })
public interface FormsPluginComponent {
	StatusService statusService();

	FormDataService formDataService();

	PurgeService purgeService();

	CheckStatusHandler checkStatusHandler();

	ActivateHandler activateHandler();

	DeactivateHandler deactivateHandler();

	GetLanguagesHandler getLanguagesHandler();

	SetLanguagesHandler setLanguagesHandler();

	SetFormtypeConfigsHandler setFormtypeConfigsHandler();

	GetTranslationsHandler getTranslationsHandler();

	SetTranslationsHandler setTranslationsHandler();

	UpsertFormHandler upsertFormHandler();

	ListFormsHandler listFormsHandler();

	LoadFormGenHandler loadFormGenHandler();

	LoadPollResultsHandler loadPollResultsHandler();

	LoadFormHandler loadFormHandler();

	GetFormOnlineStatusHandler getFormOnlineStatusHandler();

	SetFormOnlineHandler setFormOnlineHandler();

	SetFormOfflineHandler setFormOfflineHandler();

	DeleteFormHandler deleteFormHandler();

	LoadFormElementsHandler loadFormElementsHandler();

	LoadFormSchemaHandler loadFormSchemaHandler();

	SaveDataHandler saveDataHandler();

	SubmitDataHandler submitDataHandler();

	ValidationHandler validationHandler();

	ExportHandler exportHandler();

	RequestExportHandler requestExportHandler();

	CheckDownloadHandler checkDownloadHandler();

	DownloadHandler downloadHandler();

	FindFormDataHandler findFormDataHandler();

	LoadFormDataBinaryHandler loadFormDataBinaryHandler();

	DeleteFormDataHandler deleteFormDataHandler();

	LoadFilesZipHandler loadFilesZipHandler();

	RequestFilesZipHandler requestFilesZipHandler();

	PurgeStaleDataHandler purgeStaleDataHandler();

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
		Builder vertx(Vertx vertx);

		@BindsInstance
		Builder storageDir(@StorageDir File storageDir);

		@BindsInstance
		Builder manifest(PluginManifest manifest);

		FormsPluginComponent build();
	}
}
