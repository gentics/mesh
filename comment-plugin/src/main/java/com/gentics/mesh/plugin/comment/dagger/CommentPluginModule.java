package com.gentics.mesh.plugin.comment.dagger;

import com.gentics.mesh.plugin.comment.config.Configuration;
import com.gentics.mesh.plugin.comment.config.ProjectConfiguration;
import com.gentics.mesh.plugin.common.helper.DateHelper;
import dagger.Module;
import dagger.Provides;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.mail.MailClient;
import io.vertx.reactivex.ext.web.common.template.TemplateEngine;
import io.vertx.reactivex.ext.web.templ.handlebars.HandlebarsTemplateEngine;

import javax.inject.Named;
import javax.inject.Singleton;

@Module
public class CommentPluginModule {

	@Singleton
	@Provides
	static Configuration provideConfiguration(@Named("original") Configuration configuration, JsonObject project) {
		ProjectConfiguration projectConfiguration = configuration.getProjectConfig().get(project.getString("name"));
		if (projectConfiguration == null) {
			return configuration;
		}

		// Override all fields that are present in the project config
		Configuration newConfig = new Configuration();
		projectConfiguration.isAllowAnonymous().ifPresent(newConfig::setAllowAnonymous);
		projectConfiguration.isRequiresApproval().ifPresent(newConfig::setRequiresApproval);
		projectConfiguration.getCommentRoot().ifPresent(newConfig::setCommentRoot);
		projectConfiguration.getModeratorEmails().ifPresent(newConfig::setModeratorEmails);
		projectConfiguration.getRequiredForAnonymous().ifPresent(newConfig::setRequiredForAnonymous);
		projectConfiguration.getNodeConfigField().ifPresent(newConfig::setNodeConfigField);
		return newConfig;
	}

	@Singleton
	@Provides
	static MailClient provideMailClient(Vertx vertx, Configuration config) {
		return MailClient.createShared(vertx, config.getMailServerConfig());
	}

	@Singleton
	@Provides
	static TemplateEngine provideTemplateEngine(Vertx vertx) {
		HandlebarsTemplateEngine handlebarsTemplateEngine = HandlebarsTemplateEngine.create(vertx);
		handlebarsTemplateEngine.getDelegate().unwrap().registerHelper("date", new DateHelper());
		return handlebarsTemplateEngine;
	}
}
