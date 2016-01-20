package com.sinergise.sentinel.l1c.product.info;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sinergise.sentinel.l1c.product.L1CProductConstants;
import com.sinergise.sentinel.l1c.product.mapping.S3ProductDatastrip;

public class DatastripInfo {
	@JsonProperty("id")
	private String id;

	@JsonProperty("path")
	private String path;

	@SuppressWarnings("unused")
	private DatastripInfo() {
	}

	public DatastripInfo(String id, String path) {
		this.id = id;
		this.path = path;
	}

	public DatastripInfo(S3ProductDatastrip datastrip) {
		this.id = datastrip.getId();
		this.path = L1CProductConstants.getS3ObjectName(datastrip.getBaseDirectory());
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getPath() {
		return path;
	}
}
