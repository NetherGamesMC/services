package org.nethergames.gsms.domain.model.filter;

import io.swagger.v3.oas.annotations.Parameter;
import org.nethergames.common.domain.model.BaseFilter;
import org.springdoc.core.annotations.ParameterObject;

import java.util.List;

@ParameterObject
public class ProxyServerFilter extends BaseFilter {

	@Parameter(description = "The pod uuid")
	private List<String> ids;

	public List<String> getIds() {
		return ids;
	}

	public void setIds(List<String> ids) {
		this.ids = ids;
	}
}
