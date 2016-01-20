package com.sinergise.sentinel.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtils {
	private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);

	public static void deleteRecursively(File f) throws IOException {
		if (f.isDirectory()) {
			for (File c : f.listFiles())
				deleteRecursively(c);
		}
		if (!f.delete())
			throw new FileNotFoundException("Failed to delete file: " + f);
	}
	
	
	public static long getDirectorySize(File directory) throws IOException {
		AtomicLong size = new AtomicLong(0);
		Files.walkFileTree(directory.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                size.addAndGet(attrs.size());
                return FileVisitResult.CONTINUE;
            }
		});
		return size.get();
	}
	
	public static void removeOldestFiles(File directory, long size) throws IOException {
		File[] files = directory.listFiles();
		Arrays.sort(files, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));
		long sum=0;
		for (File f:files) {
			logger.info("Removing {}!", f.getPath());
			sum+=f.length();
			if (!f.delete()) {
				throw new IOException("Failed to delete:"+f.getPath());
			}
			if (sum>size) break;
		}
	}
}
