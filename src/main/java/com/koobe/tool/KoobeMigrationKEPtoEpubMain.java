package com.koobe.tool;

import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import com.koobe.common.core.KoobeApplication;
import com.koobe.common.data.KoobeDataService;
import com.koobe.common.data.repository.EpubConvertResultRepository;
import com.koobe.common.storage.AmazonS3Storage;
import com.koobe.common.storage.KoobeStorageService;
import com.koobe.tool.worker.ConvertAndUploadWorker;

public class KoobeMigrationKEPtoEpubMain {
	
	protected static Logger log = LoggerFactory.getLogger(KoobeMigrationKEPtoEpubMain.class);
	
	static KoobeApplication koobeApplication;
	static KoobeDataService koobeDataService;
	static JdbcTemplate jdbcTemplate;
	static AmazonS3Storage storage;
	
	static EpubConvertResultRepository logRepository;
	
	static ExecutorService executor;
	
	static String kepRootFolder = "Y:\\KepRaw";
	
	static String epubPlaceFolder = "Q:\\EpubRaw";
	
	static String batchFilePath = "D:\\runKepConverter.bat";
	
	static {
		koobeApplication = KoobeApplication.getInstance();
		koobeDataService = (KoobeDataService) koobeApplication.getService(KoobeDataService.class);
		
		jdbcTemplate = koobeDataService.getJdbcTemplate();
		KoobeStorageService storageService = (KoobeStorageService) koobeApplication.getService(KoobeStorageService.class);
		storage = storageService.getAmazonS3Storage();
		
		logRepository = (EpubConvertResultRepository) koobeDataService.getRepository(EpubConvertResultRepository.class);
	}

	public static void main(String[] args) throws UnknownHostException {
		
		Integer pool = Integer.valueOf(args[0]);
		String host = args[1];
		batchFilePath = args[2];
		kepRootFolder = args[3];
		epubPlaceFolder = args[4];
		
		String SQL = "SELECT eBookGuid_Lower FROM EBookNotInKGL_20140328_NoReplica";
		
		executor = Executors.newFixedThreadPool(pool, new ThreadFactory() {
			public Thread newThread(Runnable r) {
				Thread thread = new Thread(r);
				thread.setDaemon(true);
				return thread;
			}
		});
		
		SqlRowSet rowSet = koobeDataService.getJdbcTemplate().queryForRowSet(SQL);
		int count = 0;
		while(rowSet.next()) {
			String guid = rowSet.getString("eBookGuid_Lower");
			ConvertAndUploadWorker worker = new ConvertAndUploadWorker(jdbcTemplate, storage, kepRootFolder, batchFilePath, guid, logRepository, epubPlaceFolder);
			executor.submit(worker);
			count++;
		}
		log.info("Total {} tasks sended", count);
		
//		ConvertAndUploadWorker worker = new ConvertAndUploadWorker(jdbcTemplate, storage, kepRootFolder, batchFilePath, "7ac46013-2816-4be3-bba7-182a042f8ca2", logRepository, epubPlaceFolder);
//		executor.submit(worker);
		
		try {
			Thread.sleep(999999999);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
