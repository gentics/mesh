package com.gentics.mesh.graphql.filter;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.gentics.graphqlfilter.filter.Filter;
import com.gentics.graphqlfilter.filter.FilterField;
import com.gentics.graphqlfilter.filter.NumberFilter;
import com.gentics.graphqlfilter.filter.StringFilter;
import com.gentics.mesh.core.data.ImageDataElement;
import com.gentics.mesh.core.data.node.field.ImageDataField;
import com.gentics.mesh.core.data.node.field.nesting.ReferenceField;

public abstract class ImageDataFieldFilter<E extends ImageDataElement, T extends ReferenceField<E> & ImageDataField> extends EntityReferenceFilter<E, T, Map<String, ?>> {

	protected final String owner;

	protected ImageDataFieldFilter(String name, String description, String fieldName, Filter<E, Map<String, ?>> referenceFilter, String owner) {
		super(name, description, fieldName, referenceFilter, Optional.of(owner));
		this.owner = owner;
	}

	@Override
	protected List<FilterField<T, ?>> getFilters() {
		List<FilterField<T, ?>> filters = super.getFilters();
		//filters.add(makeWrappedFieldFilter("filename", "Filters by filename", StringFilter.filter(), HibImageDataField::getFileName));
		//filters.add(makeWrappedFieldFilter("mime", "Filters by MIME type", StringFilter.filter(), HibImageDataField::getMimeType));
		filters.add(makeWrappedFieldFilter("imageDominantColor", "Filters by image dominant color", StringFilter.filter(), ImageDataField::getImageDominantColor));
		filters.add(makeWrappedFieldFilter("plainText", "Filters by text data", StringFilter.filter(), ImageDataField::getPlainText));
		filters.add(makeWrappedFieldFilter("altitude", "Filters by altitude", NumberFilter.filter(), ImageDataField::getLocationAltitude));
		filters.add(makeWrappedFieldFilter("latitude", "Filters by latitude", NumberFilter.filter(), ImageDataField::getLocationLatitude));
		filters.add(makeWrappedFieldFilter("longitude", "Filters by longitude", NumberFilter.filter(), ImageDataField::getLocationLongitude));	
		filters.add(makeWrappedFieldFilter("focalPointX", "Filters by longitude", NumberFilter.filter(), 
				edge -> (edge != null && edge.getImageFocalPoint() != null) ? edge.getImageFocalPoint().getX() : null));	
		filters.add(makeWrappedFieldFilter("focalPointY", "Filters by longitude", NumberFilter.filter(), 
				edge -> (edge != null && edge.getImageFocalPoint() != null) ? edge.getImageFocalPoint().getY() : null));	
		return filters;
	}

	protected abstract <B extends ImageDataElement, F extends ImageDataFilter<B>> F getBinaryFilter();
}
