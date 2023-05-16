package com.gentics.mesh.graphql.filter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.gentics.graphqlfilter.filter.FilterField;
import com.gentics.graphqlfilter.filter.MappedFilter;
import com.gentics.graphqlfilter.filter.NumberFilter;
import com.gentics.graphqlfilter.filter.StringFilter;
import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.data.HibImageDataElement;
import com.gentics.mesh.core.data.node.field.HibImageDataField;

public abstract class ImageDataFieldFilter<T extends HibImageDataField> extends EntityFilter<T> {

	protected static final ElementType ELEMENT = ElementType.NODE;
	protected static final String OWNER = ELEMENT.name();

	protected ImageDataFieldFilter(String name, String description) {
		super(name, description, Optional.of(OWNER));
	}

	@Override
	protected ElementType getEntityType() {
		return ELEMENT;
	}

	@Override
	protected List<FilterField<T, ?>> getFilters() {
		List<FilterField<T, ?>> filters = new ArrayList<>();
		filters.add(new MappedFilter<>(OWNER, "filename", "Filters by filename", StringFilter.filter(), content -> content.getFileName()));
		filters.add(new MappedFilter<>(OWNER, "imageDominantColor", "Filters by image dominant color", StringFilter.filter(),
			content -> content.getImageDominantColor()));
		filters.add(new MappedFilter<>(OWNER, "mime", "Filters by MIME type", StringFilter.filter(),
			content -> content.getMimeType()));
		filters.add(new MappedFilter<>(OWNER, "plainText", "Filters by text data", StringFilter.filter(),
				content -> content.getPlainText()));
		filters.add(new MappedFilter<>(OWNER, "altitude", "Filters by altitude", NumberFilter.filter(),
				content -> new BigDecimal(content.getLocationAltitude())));
		filters.add(new MappedFilter<>(OWNER, "longitude", "Filters by longitude", NumberFilter.filter(),
				content -> new BigDecimal(content.getLocationLongitude())));
		filters.add(new MappedFilter<>(OWNER, "latitude", "Filters by latitude", NumberFilter.filter(),
				content -> new BigDecimal(content.getLocationLatitude())));
		filters.add(new MappedFilter<>(OWNER, "binary", "Filters by text data", getBinaryFilter(),
				content -> content.getBinary()));
		return filters;
	}

	protected abstract <B extends HibImageDataElement, F extends ImageDataFilter<B>> F getBinaryFilter();
}
