package com.test.api.constants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public interface IConstant {

	public static final String VALUE_SEPRATOR = "#";
	
	public static final String MULTI_VERSION ="isMultiVersion";
	
	public static final String REST_AGENT ="isRestAgent";
	
	public static final String INPUT_FILE_PATH  = "inputFilePath";
	
	public static final String REST_DATA_PROVIDER_NAME = "REST";
	
	default InputStream getInputStreamFromFile(String inputFilePath) throws FileNotFoundException{
		InputStream inputStreamToReturn = null;
		try{
			//1. First try to read the file from src-main-resource directory
			inputStreamToReturn = ClassLoader.getSystemClassLoader().getResourceAsStream(inputFilePath);
			//2. If the input-resource-stream is null check for file-path
			if(inputStreamToReturn == null){

				File file = new File(inputFilePath);
				inputStreamToReturn = new FileInputStream(file);
			}
		}catch (Exception e) {
			throw new FileNotFoundException("Not able to File. File location in TestNG Suite: "+inputFilePath);
		}
		return inputStreamToReturn;
	}
}
