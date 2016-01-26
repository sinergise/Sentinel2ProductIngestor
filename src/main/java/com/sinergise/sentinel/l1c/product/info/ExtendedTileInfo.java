package com.sinergise.sentinel.l1c.product.info;

import org.geojson.Point;
import org.geojson.Polygon;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sinergise.sentinel.l1c.product.TileMetadata;
import com.sinergise.sentinel.util.GeoJsonUtils;

@JsonInclude(Include.NON_NULL)
public class ExtendedTileInfo extends TileInfo {
	@JsonProperty("tileGeometry")
	private Polygon tileGeometry;

	@JsonProperty("tileDataGeometry")
	private Polygon tileDataGeometry;
	
	@JsonProperty("tileOrigin")
	private Point tileOrigin;
	
	@JsonProperty("dataCoveragePercentage")
	private Double dataCoveragePercentage;

	@JsonProperty("cloudyPixelPercentage")
	private Double cloudyPixelPercentage;
		
	@JsonProperty("productName")
	private String productName;
	
	@JsonProperty("productPath")
	private String productPath;
	
	
	@SuppressWarnings("unused")
	private ExtendedTileInfo() {
		super();
	}

	public ExtendedTileInfo(TileInfo tileInfo, ProductInfo productInfo, TileMetadata s3Tile) {
		super(tileInfo);
		setProductName(productInfo.getName());
		setProductPath(productInfo.getPath());
		setTileOrigin(GeoJsonUtils.toGeoJson(s3Tile.getTileOrigin()));
		setTileGeometry(GeoJsonUtils.toGeoJson(s3Tile.getTileGeometry()));
		if (s3Tile.getTileDataGeometry() != null) {
			setTileDataGeometry(GeoJsonUtils.toGeoJson(s3Tile.getTileDataGeometry()));
			dataCoveragePercentage =  Math.round(10000.*s3Tile.getTileDataGeometry().getArea()/s3Tile.getTileGeometry().getArea())/100.0;
		}
		cloudyPixelPercentage = Math.round(100.*s3Tile.getCloudyPixelsPercentage())/100.;
	}

	
	public ExtendedTileInfo(TileInfo tileInfo) {
		super(tileInfo);
	}

	public void setTileDataGeometry(Polygon tileDataGeometry) {
		this.tileDataGeometry = tileDataGeometry;
	}

	public Polygon getTileDataGeometry() {
		return tileDataGeometry;
	}

	public void setTileGeometry(Polygon tileGeometry) {
		this.tileGeometry = tileGeometry;
	}

	public Polygon getTileGeometry() {
		return tileGeometry;
	}
	
	public void setTileOrigin(Point tileOrigin) {
		this.tileOrigin = tileOrigin;
	}
	
	public Point getTileOrigin() {
		return tileOrigin;
	}

	public String getProductName() {
		return productName;
	}

	public ExtendedTileInfo setProductName(String productName) {
		this.productName = productName;
		return this;
	}

	public String getProductPath() {
		return productPath;
	}

	public ExtendedTileInfo setProductPath(String productPath) {
		this.productPath = productPath;
		return this;
	}
	
	public void setCloudyPixelPercentage(Double cloudyPixelPercentage) {
		this.cloudyPixelPercentage = cloudyPixelPercentage;
	}
	
	public Double getCloudyPixelPercentage() {
		return cloudyPixelPercentage;
	}
	
	public void setDataCoveragePercentage(Double dataCoveragePercentage) {
		this.dataCoveragePercentage = dataCoveragePercentage;
	}
	
	public Double getDataCoveragePercentage() {
		return dataCoveragePercentage;
	}
		
}
