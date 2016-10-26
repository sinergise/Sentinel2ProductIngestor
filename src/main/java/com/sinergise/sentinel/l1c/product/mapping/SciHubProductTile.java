package com.sinergise.sentinel.l1c.product.mapping;

import java.io.File;

import com.sinergise.sentinel.l1c.product.TileMetadata;

public interface SciHubProductTile {

	File getMetadataFile();

	File getPreviewFile();

	File[] getImageFiles();

	File[] getQiFiles();

	File[] getAuxFiles();

	String getTileDirectory();

	TileMetadata getTileMetadata();

	String getTileName();		
}
