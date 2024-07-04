package com.gentics.mesh.plugin.search.handler.custom;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.search.HandlerType;
import com.gentics.mesh.plugin.search.config.CustomQueryOptions;
import com.gentics.mesh.plugin.search.extension.CustomHandler;
import com.gentics.mesh.plugin.search.service.QueryService;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * A simple custom handler which will load the custom query specified in the constructor.
 *
 * <p>
 *	 For {@link #handle(PluginContext) handle()} and {@link #processResult(PluginContext, Object) processResult()} the
 *	 default implementations are used.
 * </p>
 *
 * @see QueryService#getCustomQuery(PluginContext, String)
 */
public class CustomQueryHandler implements CustomHandler {

	private static final Logger log = LoggerFactory.getLogger(CustomQueryHandler.class);

	private final QueryService queryService;
	private final HandlerType type;
	private final String name;
	private final String resultQuery;
	private final int pageSize;

	/**
	 * Default constructor
	 *
	 * @param queryService The query service to load the custom query from
	 * @param options      The name and type of this custom query
	 */
	public CustomQueryHandler(QueryService queryService, CustomQueryOptions options) {
		this.queryService = queryService;
		this.type = options.getType();
		this.name = options.getName();
		this.pageSize = options.getPageSize();
		this.resultQuery = options.getResultQuery();

		log.info("Custom query {} handler created for query {}", type, name);
	}

	@Override
	public HandlerType type() {
		return type;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public Single<JsonObject> getQuery(PluginContext context) {
		return queryService.getCustomQuery(context, name)
				.doOnSuccess(json -> {
					if (pageSize > 0) {
						int page = NumberUtils.toInt(context.request().getParam("p"), 0);

						json.put("size", pageSize)
								.put("from", page * pageSize);

					}
				});
	}

	@Override
	public String getResultQueryName() {
		return resultQuery;
	}
}
