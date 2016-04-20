package com.sinergise.sentinel.l1c.product;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class L1CProductConstants {

	
	public static SimpleDateFormat getS3BucketDateFormat() {
		return  new SimpleDateFormat("yyyy'" + File.separator + "'M'" + File.separator + "'d");
	}

	public static SimpleDateFormat getMetadataXmlDateFormat() {
		return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
	}

	public static SimpleDateFormat getFilenameDateFormat() {
		return  new SimpleDateFormat("yyyyMMdd'T'HHmmss");
	}
	
	
	public static String getS3ObjectName(File file) {
		String key = file.getPath().replace("\\", "/");
		return key.startsWith("/") ? key.substring(1) : key;
	}

	public static String buildS3ProductInfoPath(File basePath, Date productTimestamp, String productName) {
		File productInfoFile = new File(
				L1CProductConstants.getProductBaseDirectory(createProductBasePath(basePath), productTimestamp,
						productName),
				ProductTransformer.PRODUCT_INFO_FILENAME);
		return getS3ObjectName(productInfoFile);
	}

	public static File getProductBaseDirectory(File s3ProductBase, Date productTimestamp, String productName) {
		String dateString = getS3BucketDateFormat().format(productTimestamp);
		return new File(s3ProductBase, dateString + File.separator + productName);
	}

	public static File createProductBasePath(File basePath) {
		return new File(basePath, "products");
	}
}
