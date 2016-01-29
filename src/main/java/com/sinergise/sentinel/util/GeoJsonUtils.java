package com.sinergise.sentinel.util;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.geojson.Crs;
import org.geojson.GeoJsonObject;
import org.geojson.LngLatAlt;
import org.geojson.jackson.CrsType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;

public class GeoJsonUtils {
	public static final String CRS_URN_BASE_EPSG = "urn:ogc:def:crs:EPSG:8.8.1:";

	private static Crs createCrsForEpsgCode(int epsgCode) {
		Crs crs = new Crs();
		crs.setType(CrsType.name);
		String crsURN = CRS_URN_BASE_EPSG + epsgCode;
		crs.setProperties(Collections.unmodifiableMap(Stream.of(new SimpleEntry<>("name", crsURN))
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue))));
		return crs;
	}

	public static org.geojson.GeoJsonObject toGeoJson(Geometry jtsGeometry) {
		if (jtsGeometry instanceof MultiPolygon) {
			return toGeoJson((MultiPolygon)jtsGeometry);
		} else if (jtsGeometry instanceof Polygon) {
			return toGeoJson((Polygon)jtsGeometry);
		} else	if (jtsGeometry instanceof Point) {
			return toGeoJson((Point)jtsGeometry);
		}
		throw new IllegalArgumentException("Unsupportedd type: "+jtsGeometry.getGeometryType());
		
	}

	public static org.geojson.Point toGeoJson(Point jtsPoint) {
		if (jtsPoint == null)
			return null;
		org.geojson.Point geoJsonPoint = new org.geojson.Point(new LngLatAlt(jtsPoint.getX(), jtsPoint.getY()));
		geoJsonPoint.setCrs(createCrsForEpsgCode(jtsPoint.getSRID()));
		return geoJsonPoint;
	}

	public static org.geojson.Polygon toGeoJson(Polygon jtsPolygon) {
		if (jtsPolygon == null)
			return null;

		org.geojson.Polygon geoJsonPoly = new org.geojson.Polygon(Arrays.asList(jtsPolygon.getCoordinates()).stream()
				.map(p -> new LngLatAlt(p.x, p.y)).collect(Collectors.toList()));
		geoJsonPoly.setCrs(createCrsForEpsgCode(jtsPolygon.getSRID()));
		return geoJsonPoly;
	}
	
	public static org.geojson.MultiPolygon toGeoJson (MultiPolygon jtsPolygon) {
		if (jtsPolygon == null)
			return null;

		org.geojson.MultiPolygon multiPolygon = new org.geojson.MultiPolygon();
		for (int i=0;i<jtsPolygon.getNumGeometries();i++) {
			multiPolygon.add(toGeoJson((Polygon)jtsPolygon.getGeometryN(i)));
		}
		multiPolygon.setCrs(createCrsForEpsgCode(jtsPolygon.getSRID()));
		return multiPolygon;
	}
	
	public static Polygon toJts(org.geojson.Polygon polygon) {
		if (polygon.getInteriorRings().size() > 0) {
			throw new UnsupportedOperationException("Only polygons without holes are supported!");
		}
		String geoJsonCrs = polygon.getCrs().getProperties().get(org.geojson.jackson.CrsType.name.name()).toString();

		GeometryFactory gf = new GeometryFactory(new PrecisionModel(),
				Integer.parseInt(geoJsonCrs.substring(CRS_URN_BASE_EPSG.length())));

		return toJts(polygon.getExteriorRing(), gf);
	}
	
	
	private static Polygon toJts(List<LngLatAlt> geoJsonExtRing, GeometryFactory gf) {
		List<Coordinate> coordinates = geoJsonExtRing.stream()
				.map(lla -> new Coordinate(lla.getLongitude(), lla.getLatitude())).collect(Collectors.toList());

		return gf.createPolygon(coordinates.toArray(new Coordinate[coordinates.size()]));
	}
	
	public static MultiPolygon toJts(org.geojson.MultiPolygon mPoly) {
		String geoJsonCrs = mPoly.getCrs().getProperties().get(org.geojson.jackson.CrsType.name.name()).toString();

		GeometryFactory gf = new GeometryFactory(new PrecisionModel(),
				Integer.parseInt(geoJsonCrs.substring(CRS_URN_BASE_EPSG.length())));
		
		
		List<Polygon> polys = new ArrayList<>();
		for (List<List<LngLatAlt>> polygonsCoordinates:mPoly.getCoordinates()) {
			List<LngLatAlt> extRing = polygonsCoordinates.get(0);
			polys.add(toJts(extRing, gf));
		}
		
		return gf.createMultiPolygon(polys.toArray(new Polygon[polys.size()]));
	}
	
	public static Geometry toJts(GeoJsonObject geoJsonObject) {
		if (geoJsonObject instanceof org.geojson.Polygon) {
			return toJts((org.geojson.Polygon)geoJsonObject);
		} else if (geoJsonObject instanceof org.geojson.MultiPolygon) {
			return toJts((org.geojson.MultiPolygon)geoJsonObject);
		} else {
			throw new IllegalArgumentException("Unsupported geoJsonObject type: " + geoJsonObject.getClass());
		}
	}
	
	
}
