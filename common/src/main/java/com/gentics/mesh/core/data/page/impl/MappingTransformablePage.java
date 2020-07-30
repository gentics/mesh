package com.gentics.mesh.core.data.page.impl;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.gentics.mda.entity.ATransformableElement;
import com.gentics.mda.page.ATransformablePage;
import com.gentics.mesh.core.data.TransformableElement;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.rest.common.RestModel;

public class MappingTransformablePage<T extends TransformableElement<? extends RestModel>, R extends ATransformableElement<? extends RestModel>> implements ATransformablePage<R> {
	private final TransformablePage<T> input;
	private final Function<? super T, ? extends R> mapper;

	public MappingTransformablePage(TransformablePage<T> input, Function<? super T, ? extends R> mapper) {
		this.input = input;
		this.mapper = mapper;
	}

	@Override
	public Long getPerPage() {
		return input.getPerPage();
	}

	@Override
	public long getPageCount() {
		return input.getPageCount();
	}

	@Override
	public List<? extends R> getWrappedList() {
		return input.getWrappedList().stream().map(mapper).collect(Collectors.toList());
	}

	@Override
	public boolean hasNextPage() {
		return input.hasNextPage();
	}

	@Override
	public long getNumber() {
		return input.getNumber();
	}

	@Override
	public long getTotalElements() {
		return input.getTotalElements();
	}


}
