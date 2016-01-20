package com.sinergise.sentinel.l1c.product.mapping;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class SciHubProductBundle {
	private static final int BUFFER_SIZE = 16 * 1024;

	private String productName;
	private File productZip;

	public File unpack(File targetDirectory) throws FileNotFoundException, IOException {
		try (ZipInputStream zis = new ZipInputStream(new FileInputStream(productZip))) {
			ZipEntry ze = zis.getNextEntry();
			if (!ze.isDirectory())
				throw new IllegalArgumentException("Probably not a sentinel 2 product!");
			// TODO: parse and validate product string
			productName = ze.getName();
			File unpackedFolder = new File(targetDirectory, ze.getName());
			while (ze != null) {
				File file = new File(targetDirectory, ze.getName());
				if (ze.isDirectory()) {
					file.mkdirs();
				} else {
					extractFile(zis, file);
				}
				zis.closeEntry();
				ze = zis.getNextEntry();
			}
			return unpackedFolder;
		}
	}

	public String getProductName() {
		return productName;
	}

	private void extractFile(ZipInputStream zis, File file) throws IOException {
		try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file))) {
			byte[] bytesIn = new byte[BUFFER_SIZE];
			int read = 0;
			while ((read = zis.read(bytesIn)) != -1) {
				bos.write(bytesIn, 0, read);
			}
		}
	}

}
