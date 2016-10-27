package com.sinergise.sentinel.l1c.product;

import java.io.InputStream;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;

public class TileMetadata {
	
	private String datastripId;
	private Date sensingTime;
	private String tileId;
	
	private int epsgCode;
	
	private Geometry tileDataGeometry;
	private Polygon tileGeometry;
	private Point tileOrigin;
	
	private double cloudyPixelsPercentage;
	
		
	public double getCloudyPixelsPercentage() {
		return cloudyPixelsPercentage;
	}
	
	public String getDatastripId() {
		return datastripId;
	}
	
	public int getEpsgCode() {
		return epsgCode;
	}
	
	public Point getTileOrigin() {
		return tileOrigin;
	}
	
	public Polygon getTileGeometry() {
		return tileGeometry;
	}

	public void setTileDataGeometry(Geometry tileDataGeometry) {
		this.tileDataGeometry = tileDataGeometry;
	}
	
	public Geometry getTileDataGeometry() {
		return tileDataGeometry;
	}

	public String getTileId() {
		return tileId;
	}

	public Date getSensingTime() {
		return sensingTime;
	}

	public void setDatastripId(String id) {
		this.datastripId=id;
	}
	
	public void setCloudyPixelsPercentage(double cloudyPixelsPercentage) {
		this.cloudyPixelsPercentage = cloudyPixelsPercentage;
	}
	
	public void setEpsgCode(int epsgCode) {
		this.epsgCode = epsgCode;
	}
	
	public void setSensingTime(Date sensingTime) {
		this.sensingTime = sensingTime;
	}
	
	public void setTileGeometry(Polygon tileGeometry) {
		this.tileGeometry = tileGeometry;
	}
	
	public void setTileId(String tileId) {
		this.tileId = tileId;
	}
	
	public void setTileOrigin(Point tileOrigin) {
		this.tileOrigin = tileOrigin;
	}
	
}
