package com.gentics.mesh.graphql.filter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.gentics.graphqlfilter.filter.FilterField;
import com.gentics.graphqlfilter.filter.MainFilter;
import com.gentics.graphqlfilter.filter.MappedFilter;
import com.gentics.graphqlfilter.filter.NumberFilter;
import com.gentics.graphqlfilter.filter.StringFilter;
import com.gentics.mesh.core.data.HibImageDataElement;
import com.gentics.mesh.core.data.node.field.HibImageDataField;

public abstract class ImageDataFieldFilter<T extends HibImageDataField> extends MainFilter<T> {

	protected final String owner;

	protected ImageDataFieldFilter(String name, String description, String owner) {
		super(name, description, Optional.of(owner));
		this.owner = owner;
	}

	@Override
	protected List<FilterField<T, ?>> getFilters() {
		List<FilterField<T, ?>> filters = new ArrayList<>();
		filters.add(new MappedFilter<>(owner, "filename", "Filters by filename", StringFilter.filter(), content -> content.getFileName()));
		filters.add(new MappedFilter<>(owner, "imageDominantColor", "Filters by image dominant color", StringFilter.filter(),
				content -> content == null ? null : content.getImageDominantColor()));
		filters.add(new MappedFilter<>(owner, "mime", "Filters by MIME type", StringFilter.filter(),
				content -> content == null ? null : content.getMimeType()));
		filters.add(new MappedFilter<>(owner, "plainText", "Filters by text data", StringFilter.filter(),
				content -> content == null ? null : content.getPlainText()));
		filters.add(new MappedFilter<>(owner, "altitude", "Filters by altitude", NumberFilter.filter(),
				content -> content == null ? null : new BigDecimal(content.getLocationAltitude())));
		filters.add(new MappedFilter<>(owner, "longitude", "Filters by longitude", NumberFilter.filter(),
				content -> content == null ? null : new BigDecimal(content.getLocationLongitude())));
		filters.add(new MappedFilter<>(owner, "latitude", "Filters by latitude", NumberFilter.filter(),
				content -> content == null ? null : new BigDecimal(content.getLocationLatitude())));
		filters.add(new MappedFilter<>(owner, "binary", "Filters by text data", getBinaryFilter(),
				content -> content == null ? null : content.getBinary()));
		return filters;
	}

	protected abstract <B extends HibImageDataElement, F extends ImageDataFilter<B>> F getBinaryFilter();
}
