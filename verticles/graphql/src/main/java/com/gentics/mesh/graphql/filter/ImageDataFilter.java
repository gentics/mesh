package com.gentics.mesh.graphql.filter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.gentics.graphqlfilter.filter.EnumFilter;
import com.gentics.graphqlfilter.filter.FilterField;
import com.gentics.graphqlfilter.filter.MappedFilter;
import com.gentics.graphqlfilter.filter.NumberFilter;
import com.gentics.graphqlfilter.filter.StringFilter;
import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.data.HibImageDataElement;
import com.gentics.mesh.core.rest.node.field.BinaryCheckStatus;

public abstract class ImageDataFilter<T extends HibImageDataElement> extends EntityFilter<T> {

	protected static final ElementType ELEMENT = ElementType.NODE;
	protected static final String OWNER = ELEMENT.name();

	protected ImageDataFilter(String name, String description) {
		super(name, description, Optional.of(OWNER));
	}

	@Override
	protected ElementType getEntityType() {
		return ELEMENT;
	}

	@Override
	protected List<FilterField<T, ?>> getFilters() {
		List<FilterField<T, ?>> filters = new ArrayList<>();
		filters.add(new MappedFilter<>(OWNER, "uuid", "Filters by uuid", StringFilter.filter(), 
				content -> content.getUuid()));
		filters.add(new MappedFilter<>(OWNER, "size", "Filters by file size", NumberFilter.filter(),
			content -> new BigDecimal(content.getSize())));
		filters.add(new MappedFilter<>(OWNER, "width", "Filters by width", NumberFilter.filter(),
			content -> new BigDecimal(content.getImageWidth())));
		filters.add(new MappedFilter<>(OWNER, "height", "Filters by height", NumberFilter.filter(),
				content -> new BigDecimal(content.getImageHeight())));
		filters.add(new MappedFilter<>(OWNER, "checkStatus", "Filters by virus check status", EnumFilter.filter(BinaryCheckStatus.class),
				content -> content.getCheckStatus()));
		return filters;
	}
}
