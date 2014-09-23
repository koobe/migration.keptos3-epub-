package com.koobe.tool.worker;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.io.FileDeleteStrategy;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import com.koobe.common.data.domain.EpubConvertResult;
import com.koobe.common.data.repository.EpubConvertResultRepository;
import com.koobe.common.storage.AmazonS3Storage;
import com.koobe.tool.external.KepConverterProcessRunner;
import com.koobe.tool.worker.enums.Status;

public class ConvertAndUploadWorker implements Runnable {
	
	protected Logger log = LoggerFactory.getLogger(getClass());
	
	private JdbcTemplate jdbcTemplate;
	
	private EpubConvertResultRepository logRepository;
	
	private AmazonS3Storage storage;
		
	private String kepRootFolder;
	
	private String epubPlaceFolder;
	
	private String convertToEpubBatchPath;
	
	private String bookGuid;
	
	private Status status;
	
	private Long copyFileElapsed;
	
	private Long runConverterProcessElapsed;
	
	private Long runUploaderElapsed;
	
	private String workerExMsg;

	public ConvertAndUploadWorker(JdbcTemplate jdbcTemplate,
			AmazonS3Storage storage, String kepRootFolder,
			String convertToEpubBatchPath, String bookGuid, EpubConvertResultRepository logRepository,
			String epubPlaceFolder) {
		super();
		this.jdbcTemplate = jdbcTemplate;
		this.storage = storage;
		this.kepRootFolder = kepRootFolder;
		this.convertToEpubBatchPath = convertToEpubBatchPath;
		this.bookGuid = bookGuid;
		this.logRepository = logRepository;
		this.epubPlaceFolder = epubPlaceFolder;
	}

	public void run() {
		
		String kepFilePath = kepRootFolder + File.separator 
				+ bookGuid.substring(0,1) + File.separator 
				+ bookGuid.substring(1,2) + File.separator
				+ bookGuid + ".kep";
		
		String epubFolderPath = epubPlaceFolder + File.separator
				+ bookGuid.substring(0,1) + File.separator 
				+ bookGuid.substring(1,2) + File.separator
				+ bookGuid.substring(2,3);
		
		String placeEpubFilePath = epubFolderPath + File.separator + bookGuid + ".epub";
		
		String kepTempFileDir = System.getProperty("java.io.tmpdir") + bookGuid;
		
		String kepTempFilePath = kepTempFileDir + File.separator + bookGuid + ".kep";
		
		String epubTempFileDir = System.getProperty("java.io.tmpdir") + bookGuid + "epub";
		
		File epubFile = null;
		
		KepConverterProcessRunner converterRunner = null;
		S3EpubUploader uploader = null;
		
		try {
			
			File kepFile = new File(kepFilePath);
			File localKepTempFile = new File(kepTempFilePath);
			
			if (kepFile.exists()) {
				
				long start = System.currentTimeMillis();
				FileUtils.copyFile(kepFile, localKepTempFile);
				long elapsed = System.currentTimeMillis() - start;
				copyFileElapsed = elapsed;
				log.info("[{}] copy book to temp folder: {}, elapsed: {}", bookGuid, kepTempFilePath, elapsed);
				
				File fileEpubFolder = new File(epubTempFileDir);
				fileEpubFolder.mkdir();
				log.info("[{}] make epub temp folder: {}", bookGuid, epubTempFileDir);
				
				boolean placeEpub = true;
				File placeEpubFile = new File(placeEpubFilePath);
				if (placeEpubFile.exists()) {
					// epub file exist, skip convert from kep
					log.info("[{}] epub file exist, skip convert from kep", bookGuid);
					epubFile = new File(epubTempFileDir + File.separator + bookGuid + ".epub");					
					FileUtils.copyFile(placeEpubFile, epubFile);
					placeEpub = false;
				} else {
					// convert to epub
					log.info("[{}] run converter {} to {} ", bookGuid, kepTempFileDir, epubTempFileDir);
					start = System.currentTimeMillis();
					converterRunner = new KepConverterProcessRunner(convertToEpubBatchPath, kepTempFileDir, epubTempFileDir);
					converterRunner.start();
					elapsed = System.currentTimeMillis() - start;
					runConverterProcessElapsed = elapsed;
					log.info("[{}] is process executed: {}, elapsed: {}", bookGuid, converterRunner.getRunProcessSuccess(), elapsed);
				}
				
				// upload to s3
				if (epubFile == null){
					epubFile = new File(epubTempFileDir + File.separator + bookGuid + ".epub");
				}
				if (epubFile.exists()) {
					
					start = System.currentTimeMillis();
					uploader = new S3EpubUploader(bookGuid, epubFile, storage);
					uploader.start();
					elapsed = System.currentTimeMillis() - start;
					runUploaderElapsed = elapsed;
					log.info("[{}] epub file uploaded, elapsed: {}", bookGuid, elapsed);
					
					if (placeEpub) {
						// copy epub file to place
						File epubFolderFile = new File(epubFolderPath);
						epubFolderFile.mkdir();
						File destFile = new File(placeEpubFilePath);
						FileUtils.copyFile(epubFile, destFile);
					}
					
					if (uploader.getUploadSuccess()) {
						status = Status.SUCCESS;
						
					} else {
						status = Status.UPLOADFAIL;
					}
					
				} else {
					status = Status.EPUBNOTCONVERTSUCCESS;
				}
			} else {
				status = Status.KEPFILENOTFOUND;
			}
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			workerExMsg = sw.toString();
			
			status = Status.FAIL;
		} finally {
			try {
				FileDeleteStrategy.FORCE.delete(new File(kepTempFileDir));
				FileDeleteStrategy.FORCE.delete(new File(epubTempFileDir));
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			String hostName = "unknow";
			
			try {
				hostName = InetAddress.getLocalHost().getHostName();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
			
			String convertProcessExitValue = null;
			String convertProcessOutput = null;
			String convertProcessErrorOutput = null;
			Boolean uploadSuccess = null;
			String uploaderExMsg = null;
			String runningExMsg = workerExMsg;
			
			if (converterRunner != null) {
				convertProcessExitValue = converterRunner.getExitValue().toString();
				convertProcessOutput = converterRunner.getOutputString();
				convertProcessErrorOutput = converterRunner.getErrorString();
			}
			if (uploader != null) {
				uploadSuccess = uploader.getUploadSuccess();
				uploaderExMsg = uploader.getUploadExMsg();
			}
			
			EpubConvertResult result = new EpubConvertResult();
			result.setGuid(bookGuid);
			result.setRunningHost(hostName);
			result.setRunningStatus(status.toString());
			if (copyFileElapsed != null) {
				result.setCopyFileElapsed(copyFileElapsed.intValue());
			}
			if (runConverterProcessElapsed != null) {
				result.setRunConverterProcessElapsed(runConverterProcessElapsed.intValue());
			}
			if (runUploaderElapsed != null) {
				result.setRunUploaderElapsed(runUploaderElapsed.intValue());
			}
			result.setConvertProcessExitValue(convertProcessExitValue);
			result.setConvertProcessOutput(convertProcessOutput);
			result.setConvertProcessErrorOutput(convertProcessErrorOutput);
			result.setUploaderExMsg(uploaderExMsg);
			result.setRunningExMsg(runningExMsg);
			result.setUploadSuccess(uploadSuccess);
			result.setRunningTime(new java.util.Date());
			
			logRepository.save(result);
		}
	}
}
