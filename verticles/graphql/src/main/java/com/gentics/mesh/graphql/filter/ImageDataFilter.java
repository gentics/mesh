package com.gentics.mesh.graphql.filter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.gentics.graphqlfilter.filter.EnumFilter;
import com.gentics.graphqlfilter.filter.FilterField;
import com.gentics.graphqlfilter.filter.MainFilter;
import com.gentics.graphqlfilter.filter.MappedFilter;
import com.gentics.graphqlfilter.filter.NumberFilter;
import com.gentics.graphqlfilter.filter.StringFilter;
import com.gentics.mesh.core.data.HibAntivirableBinaryElement;
import com.gentics.mesh.core.data.HibImageDataElement;
import com.gentics.mesh.core.rest.node.field.BinaryCheckStatus;

public abstract class ImageDataFilter<T extends HibImageDataElement> extends MainFilter<T> {

	protected final String owner;

	protected ImageDataFilter(String name, String description, String owner) {
		super(name, description, Optional.empty());
		this.owner = owner;
	}

	@Override
	protected List<FilterField<T, ?>> getFilters() {
		List<FilterField<T, ?>> filters = new ArrayList<>();
		filters.add(new MappedFilter<>(owner, "uuid", "Filters by uuid", StringFilter.filter(), 
				content -> content == null ? null : content.getUuid()));
		filters.add(new MappedFilter<>(owner, "size", "Filters by file size", NumberFilter.filter(),
			content -> content == null ? null : new BigDecimal(content.getSize())));
		filters.add(new MappedFilter<>(owner, "width", "Filters by width", NumberFilter.filter(),
			content -> (content == null || content.getImageWidth() == null) ? null : new BigDecimal(content.getImageWidth())));
		filters.add(new MappedFilter<>(owner, "height", "Filters by height", NumberFilter.filter(),
				content -> (content == null || content.getImageHeight() == null) ? null : new BigDecimal(content.getImageHeight())));
		filters.add(new MappedFilter<>(owner, "checkStatus", "Filters by virus check status", EnumFilter.filter(BinaryCheckStatus.class),
				content -> (content != null && content instanceof HibAntivirableBinaryElement) ? ((HibAntivirableBinaryElement) content).getCheckStatus() : null));
		return filters;
	}

	@Override
	public boolean isSortable() {
		// TODO implement later
		return false;
	}
}
