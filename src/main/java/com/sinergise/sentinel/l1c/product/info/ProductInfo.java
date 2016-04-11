package com.sinergise.sentinel.l1c.product.info;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sinergise.sentinel.l1c.product.L1CProductConstants;
import com.sinergise.sentinel.l1c.product.mapping.S3Product;

public class ProductInfo {
	@JsonProperty("name")
	String name;
	
	@JsonProperty("id")
	String id;
	
	@JsonProperty("path")
	String path;
	
	@JsonProperty("timestamp")
	Date timestamp;
	
	@JsonProperty("datatakeIdentifier")
	String datatakeIdnetifier;
	
	@JsonProperty("sciHubIngestion")
	Date sciHubIngestion;
	
	@JsonProperty("s3Ingestion")
	Date s3Ingestion;
	
	@JsonProperty("tiles")
	List<TileInfo> tiles;
	
	@JsonProperty("datastrips")
	List<DatastripInfo> datastrips;

	
	@SuppressWarnings("unused")
	private ProductInfo() {
	}
	
	public ProductInfo(S3Product product) {
		this.name = product.getName();
		this.path = L1CProductConstants.getS3ObjectName(product.getBaseDirectory());
		this.id = product.getId();
		this.datatakeIdnetifier = product.getDatatakeIdentifier();
		this.timestamp = product.getTimestamp();
		this.s3Ingestion = new Date();
		this.sciHubIngestion = product.getSciHubIngestionTs();

		tiles = product.getTiles()
					.stream()
					.map(tile -> new TileInfo(tile))
					.collect(Collectors.toList());
		
		datastrips = product.getDatastrips()
						.stream()
						.map(dStrip -> new DatastripInfo(dStrip))
						.collect(Collectors.toList());
		
	}

	public void setDataTakeId(String value) {
		datatakeIdnetifier = value;
	}

	public void setDatastrips(ArrayList<DatastripInfo> datastrips) {
		this.datastrips = datastrips;
	}

	public String getName() {
		return name;
	}
	
	public String getPath() {
		return path;
	}
	
	public String getId() {
		return id;
	}
	
	public List<TileInfo> getTiles() {
		return tiles;
	}
	
	public Date getSciHubIngestion() {
		return sciHubIngestion;
	}

}
