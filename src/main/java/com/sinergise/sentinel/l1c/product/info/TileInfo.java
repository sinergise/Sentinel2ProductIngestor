package com.sinergise.sentinel.l1c.product.info;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sinergise.sentinel.l1c.product.L1CProductConstants;
import com.sinergise.sentinel.l1c.product.TileMetadata;
import com.sinergise.sentinel.l1c.product.mapping.S3ProductTile;

public class TileInfo {
	@JsonProperty("path")
	private String path;
	
	@JsonProperty("timestamp")
	private Date timestamp;
	
	@JsonProperty("utmZone")
	private int utmZone;
	
	@JsonProperty("latitudeBand")
	private String latitudeBand;
	
	@JsonProperty("gridSquare")
	private String gridSquare;
	
	@JsonProperty("datastrip")
	private DatastripInfo datastrip;
	
	@SuppressWarnings("unused")
	protected TileInfo() {
	}
	
	public TileInfo(S3ProductTile s3Tile) {
		this.path = L1CProductConstants.getS3ObjectName(s3Tile.getBaseDirectory());
		this.timestamp = s3Tile.getSensingTime();
		this.utmZone = s3Tile.getUtmZone();
		this.latitudeBand = s3Tile.getLatitudeBand();
		this.gridSquare = s3Tile.getGridSquare();
		this.datastrip = new DatastripInfo(s3Tile.getDatastrip());
	}

	protected TileInfo(TileInfo tileInfo) {
		this.path = tileInfo.path;
		this.timestamp = tileInfo.timestamp;
		this.utmZone = tileInfo.utmZone;
		this.latitudeBand = tileInfo.latitudeBand;
		this.gridSquare = tileInfo.gridSquare;
		this.datastrip = tileInfo.datastrip;
	}

	public void setDatastrip(DatastripInfo datastripInfo) {
		this.datastrip = datastripInfo;				
	}
	
	public DatastripInfo getDatastrip() {
		return datastrip;
	}

	public String getPath() {
		return path;
	}
	
	public void setPath(String path) {
		this.path = path;
	}
	
	public int getUtmZone() {
		return utmZone;
	}

	public void setUtmZone(int utmZone) {
		this.utmZone = utmZone;
	}
	

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
	
	public String getLatitudeBand() {
		return latitudeBand;
	}
	
	public void setLatitudeBand(String latitudeBand) {
		this.latitudeBand = latitudeBand;
	}
	
	public String getGridSquare() {
		return gridSquare;
	}
	
	public void setGridSquare(String gridSquare) {
		this.gridSquare = gridSquare;
	}
	
}
