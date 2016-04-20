package com.sinergise.sentinel.l1c.product.mapping;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sinergise.sentinel.l1c.product.L1CProductConstants;

public class S3ProductTile {

	private static final Pattern GRANULE_MGRS_PATTERN = Pattern.compile("^.*_T([0-9]{2})([A-Z])([A-Z]{2}).*$");
	private static final Pattern BAND_TILE_PATTERN = Pattern
			.compile("^.*_T([0-9]{2})([A-Z]{3}).*_(B[0-9][A-Z0-9]\\.jp2).*$");

	private static final Pattern QI_REPORT_FILE_PATTERN = Pattern.compile("(.*)_(\\w+_\\w+_report\\.xml)$");
	private static final Pattern QI_MASK_FILE_PATTERN = Pattern
			.compile("^.*_(MSK_[A-Z]+).*(_B[0-9][A-Z0-9]).*(\\.gml)$");
	private static final Pattern AUX_FILE_PATTERN = Pattern.compile("^.*_AUX_([A-Z]+).*$");

	private File baseDirectory;

	private File metadataFile;
	private File previewFile;
	private File[] imageFiles;
	private File[] qiFiles;
	private File[] auxFiles;
	private File tileInfoFile;
	
	private int utmZone;
	private String latitudeBand;
	private String gridSquare;
		
	private S3ProductDatastrip datastrip;
	private SciHubProductTile sciHubTile;
	
	private S3Product s3Product;
	
	public S3ProductTile(S3Product s3Product, SciHubProductTile sciHubTile, File s3TilesBase, TileSequenceProvider tileSequenceProvider) {
		this.s3Product = s3Product;
		this.sciHubTile = sciHubTile;
		
		Matcher m = GRANULE_MGRS_PATTERN.matcher(sciHubTile.getTileName());
		if (!m.matches()) {
			throw new IllegalStateException("Tile " + sciHubTile.getMetadataFile() + " is invalid!");
		}

		datastrip = s3Product.getDatastrip(sciHubTile.getTileMetadata().getDatastripId());
		
		utmZone = Integer.parseInt(m.group(1));
		latitudeBand = m.group(2);
		gridSquare = m.group(3);
		
				
		SimpleDateFormat s3BucketDateFormat = L1CProductConstants.getS3BucketDateFormat();
		
		File baseBeforeSequence = new File(s3TilesBase, utmZone + File.separator + latitudeBand + File.separator+ gridSquare + File.separator
				+ s3BucketDateFormat.format(getSensingTime()));
		
		int tileSequence = tileSequenceProvider.getSequence(L1CProductConstants.getS3ObjectName(baseBeforeSequence), sciHubTile.getTileMetadata().getTileId());
		
		
		baseDirectory = new File(s3TilesBase, utmZone + File.separator + latitudeBand + File.separator+ gridSquare + File.separator
				+ s3BucketDateFormat.format(getSensingTime()) + File.separator + tileSequence);

		tileInfoFile = new File(baseDirectory, "tileInfo.json");
		metadataFile = new File(baseDirectory, "metadata.xml");
		previewFile = new File(baseDirectory, "preview.jp2");

		imageFiles = new File[sciHubTile.getImageFiles().length];
		for (int i = 0; i < imageFiles.length; i++) {
			File sciHubImageFile = sciHubTile.getImageFiles()[i];
			Matcher imgMatcher = BAND_TILE_PATTERN.matcher(sciHubImageFile.getName());
			if (!imgMatcher.matches()) {
				throw new IllegalStateException("Unrecognised image file:" + sciHubImageFile.getName());
			}
			imageFiles[i] = new File(baseDirectory, imgMatcher.group(3));
		}
		// QI
		File qiFileBase = new File(baseDirectory, "qi");
		qiFiles = new File[sciHubTile.getQiFiles().length];
		for (int i = 0; i < qiFiles.length; i++) {
			File sciHubQiFile = sciHubTile.getQiFiles()[i];
			Matcher mReport = QI_REPORT_FILE_PATTERN.matcher(sciHubQiFile.getName());
			Matcher mMask = QI_MASK_FILE_PATTERN.matcher(sciHubQiFile.getName());
			if (mReport.matches()) {
				qiFiles[i] = new File(qiFileBase, mReport.group(2));
			} else if (mMask.matches()) {
				qiFiles[i] = new File(qiFileBase, mMask.group(1) + mMask.group(2) + mMask.group(3));
			} else {
				throw new IllegalStateException("Unrecognised QI file:" + sciHubQiFile.getName());
			}
		}
		// AUX
		File auxFileBase = new File(baseDirectory, "auxiliary");
		auxFiles = new File[sciHubTile.getAuxFiles().length];
		for (int i = 0; i < auxFiles.length; i++) {
			File auxFile = sciHubTile.getAuxFiles()[i];
			Matcher mAux = AUX_FILE_PATTERN.matcher(auxFile.getName());
			if (!mAux.matches()) {
				throw new IllegalStateException("Unrecognised AUX file:" + auxFile.getName());
			}
			auxFiles[i] = new File(auxFileBase, mAux.group(1));
		}
	}

	public int getUtmZone() {
		return utmZone;
	}
	
	public String getLatitudeBand() {
		return latitudeBand;
	}
	
	public String getGridSquare() {
		return gridSquare;
	}
	
	public Date getSensingTime() {
		return sciHubTile.getTileMetadata().getSensingTime();
	}
	
	public File getMetadataFile() {
		return metadataFile;
	}

	public File[] getAuxFiles() {
		return auxFiles;
	}

	public File[] getQiFiles() {
		return qiFiles;
	}

	public File[] getImageFiles() {
		return imageFiles;
	}

	public File getPreviewFile() {
		return previewFile;
	}
	
	public File getTileInfoFile() {
		return tileInfoFile;
	}

	public File getBaseDirectory() {
		return baseDirectory;
	}
	
	public S3ProductDatastrip getDatastrip() {
		return datastrip;
	}

	public S3Product getS3Product() {
		return s3Product;
	}	
}
