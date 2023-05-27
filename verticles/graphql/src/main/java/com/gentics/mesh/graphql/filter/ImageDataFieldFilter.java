package com.gentics.mesh.graphql.filter;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.gentics.graphqlfilter.filter.Filter;
import com.gentics.graphqlfilter.filter.FilterField;
import com.gentics.graphqlfilter.filter.NumberFilter;
import com.gentics.graphqlfilter.filter.StringFilter;
import com.gentics.mesh.core.data.HibImageDataElement;
import com.gentics.mesh.core.data.node.field.HibImageDataField;
import com.gentics.mesh.core.data.node.field.nesting.HibReferenceField;

public abstract class ImageDataFieldFilter<E extends HibImageDataElement, T extends HibReferenceField<E> & HibImageDataField> extends EntityReferenceFilter<E, T, Map<String, ?>> {

	protected final String owner;

	protected ImageDataFieldFilter(String name, String description, String fieldName, Filter<E, Map<String, ?>> referenceFilter, String owner) {
		super(name, description, fieldName, referenceFilter, Optional.of(owner));
		this.owner = owner;
	}

	@Override
	protected List<FilterField<T, ?>> getFilters() {
		List<FilterField<T, ?>> filters = super.getFilters();
		filters.add(makeWrappedFieldFilter("filename", "Filters by filename", StringFilter.filter(), HibImageDataField::getFileName));
		filters.add(makeWrappedFieldFilter("imageDominantColor", "Filters by image dominant color", StringFilter.filter(), HibImageDataField::getImageDominantColor));
		filters.add(makeWrappedFieldFilter("mime", "Filters by MIME type", StringFilter.filter(), HibImageDataField::getMimeType));
		filters.add(makeWrappedFieldFilter("plainText", "Filters by text data", StringFilter.filter(), HibImageDataField::getPlainText));
		filters.add(makeWrappedFieldFilter("altitude", "Filters by altitude", NumberFilter.filter(), HibImageDataField::getLocationAltitude));
		filters.add(makeWrappedFieldFilter("latitude", "Filters by latitude", NumberFilter.filter(), HibImageDataField::getLocationLatitude));
		filters.add(makeWrappedFieldFilter("longitude", "Filters by longitude", NumberFilter.filter(), HibImageDataField::getLocationLongitude));		
		return filters;
	}

	protected abstract <B extends HibImageDataElement, F extends ImageDataFilter<B>> F getBinaryFilter();
}
