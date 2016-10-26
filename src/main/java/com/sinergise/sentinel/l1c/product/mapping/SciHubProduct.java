package com.sinergise.sentinel.l1c.product.mapping;

import java.io.File;
import java.util.Date;
import java.util.List;

public interface SciHubProduct {

	public String getName();

	public String getProductId();

	public Date getIngestionDate();

	public Date getProductStopTime();

	public String getDatatakeIdentifier();

	public List<SciHubProductDatastrip> getDatastrips();

	public List<SciHubProductTile> getTiles();

	public File getMetadataFile();

	public File getInspireFile();

	public File getPreviewFile();

	public File getManifestFile();
}
