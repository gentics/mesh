package com.gentics.mesh.plugin.search.dagger;

import com.gentics.mesh.plugin.search.config.SearchConfiguration;
import com.gentics.mesh.plugin.search.extension.HitTransformer;
import com.gentics.mesh.plugin.search.extension.SearchQueryBuilder;
import com.gentics.mesh.plugin.search.service.query.TemplateSearchQueryBuilder;
import com.gentics.mesh.plugin.search.transformer.BasicHitTransformer;
import dagger.Module;
import dagger.Provides;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.pf4j.ExtensionPoint;
import org.pf4j.PluginManager;

import java.util.List;
import java.util.Optional;

@Module
public class SearchPluginModule {

	private static final Logger log = LoggerFactory.getLogger(SearchPluginModule.class);

	@Provides
	public HitTransformer hitTransformer(PluginManager manager) {
		return getExtension(manager, HitTransformer.class).orElse(new BasicHitTransformer());
	}

	@Provides
	public SearchQueryBuilder searchQueryBuilder(PluginManager manager, SearchConfiguration config) {
		return getExtension(manager, SearchQueryBuilder.class).orElse(new TemplateSearchQueryBuilder())
			.init(config);
	}

	private <T extends ExtensionPoint> Optional<T> getExtension(PluginManager manager, Class<T> type) {
		List<T> extensions = manager.getExtensions(type);

		if (extensions.isEmpty()) {
			if (log.isDebugEnabled()) {
				log.debug("No extensions found for {}", type.getSimpleName());
			}

			return Optional.empty();
		}

		T ret = extensions.get(0);

		if (extensions.size() > 1) {
			if (log.isWarnEnabled()) {
				log.warn(
					"Multiple extensions found for {} ({}), using first one found",
					type.getSimpleName(),
					extensions.size(),
					ret.getClass().getName());
			}
		}

		if (log.isDebugEnabled()) {
			log.debug("Using {} extension: {}", type.getSimpleName(), ret.getClass().getName());
		}

		return Optional.of(ret);
	}
}
