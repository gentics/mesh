package com.gentics.mesh.core.data.page;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.dao.DaoTransformable;
import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.util.ETag;

/**
 * REST transformer for {@link Page} objects.
 */
@Singleton
public class PageTransformer {
	private final Map<ElementType, DaoTransformable<HibCoreElement<? extends RestModel>, RestModel>> daos;

	@Inject
	public PageTransformer(Map<ElementType, DaoTransformable<HibCoreElement<? extends RestModel>, RestModel>> daos) {
		this.daos = daos;
	}

	/**
	 * Transform the given page to a REST list response.
	 * 
	 * @param page
	 * @param ac
	 * @param level Level of depth to be used for nested element transformation
	 * @return
	 */
	public ListResponse<RestModel> transformToRestSync(Page<? extends HibCoreElement<? extends RestModel>> page, InternalActionContext ac, int level) {
		List<RestModel> responses = new ArrayList<>();
		for (HibCoreElement<? extends RestModel> element : page) {
			RestModel restModel = daos.get(element.getTypeInfo().getType())
				.transformToRestSync(element, ac, level);
			responses.add(restModel);
		}
		ListResponse<RestModel> listResponse = new ListResponse<>();
		page.setPaging(listResponse);
		listResponse.getData().addAll(responses);
		return listResponse;
	}

	/**
	 * Return the eTag of the page. The etag is calculated using the following information:
	 * <ul>
	 * <li>Number of total elements (all pages)</li>
	 * <li>All etags for all found elements</li>
	 * <li>Number of the current page</li>
	 * </ul>
	 *
	 * @param ac
	 * @return
	 */
	public String getETag(Page<? extends HibCoreElement<? extends RestModel>> page, InternalActionContext ac) {
		StringBuilder builder = new StringBuilder();
		builder.append(page.getTotalElements());
		builder.append(page.getNumber());
		builder.append(page.getPerPage());
		for (HibCoreElement<? extends RestModel> element : page) {
			builder.append("-");
			String eTag = daos.get(element.getTypeInfo().getType())
				.getETag(element, ac);
			builder.append(eTag);
		}
		return ETag.hash(builder.toString());
	}

}
