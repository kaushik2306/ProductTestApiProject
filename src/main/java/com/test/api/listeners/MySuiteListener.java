package com.test.api.listeners;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.xml.XmlSuite;

import com.company.inputmodel.InputModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.api.constants.IConstant;
import com.test.api.resthandler.TestApiRestHandler;


public class MySuiteListener implements ISuiteListener, IConstant {

	private static boolean isRestAgent = Boolean.valueOf(System.getProperty(REST_AGENT,"false"));
	
	private static boolean isMultiVersion = Boolean.valueOf(System.getProperty(MULTI_VERSION,"false"));
	
	private static List<TestApiRestHandler> restServiceList = null;

	private Logger logger = LoggerFactory.getLogger(MySuiteListener.class);
	
	private static InputModel inputModelObj;

	@Override
	public void onStart(ISuite isuite) {
		String inputFilePath = isuite.getParameter(INPUT_FILE_PATH);
		if(inputFilePath!=null){
			try {				
				inputModelObj = parseInpuFile(inputFilePath);
				if(!isRestAgent && isMultiVersion){
					System.out.println("Testing in Multi-Version Mode");
					startRestServices(isuite.getXmlSuite());
					System.out.println("Services Initialised successfully");
				}
			} catch (Exception e) {
				Assert.fail("Failed to start the suite listener",e);
			}
		}
	}
	
	@Override
	public void onFinish(ISuite arg0) {
		
	}

	public static InputModel getInputModelObj() {
		return inputModelObj;
	}

	public static void setInputModelObj(InputModel inputModelObj) {
		MySuiteListener.inputModelObj = inputModelObj;
	}
	
	public static void setRestServiceList(List<TestApiRestHandler> restServiceList) {
		MySuiteListener.restServiceList = restServiceList;
	}
	
	public static List<TestApiRestHandler> getRestServiceList() {
		return restServiceList;
	}
	
	public void startRestServices(XmlSuite xml){
		if(restServiceList!=null && !restServiceList.isEmpty()){
			List<Thread> startService = new ArrayList<>();
			//Create Thread objects
			for(TestApiRestHandler currentRestService : restServiceList){
				startService.add(new Thread(()->{
					currentRestService.start();
				},"REST-Service on "+currentRestService.getProductUID()));
			}
			//Start
			for(Thread thread : startService){
				thread.run();
			}

			for(Thread thread : startService){
				try {
					thread.join();
					logger.info("Started "+thread.getName());
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			for(TestApiRestHandler currentRestService : restServiceList){
				if(!currentRestService.isExecutionStatus()){
					boolean printError = false;
					for(String currentLine : currentRestService.getSb().toString().split("\n")){
						if(currentLine.startsWith("[EXCEPTION-START]")){
							printError = true;
							continue;
						}else if(currentLine.startsWith("[EXCEPTION-END]")){
							printError = false;
							continue;
						}
						if(printError){						
							logger.info(currentLine);
						}else{
							logger.info(currentLine);
						}
					}
				}else{
					currentRestService.serviceBootProcessLogs();
					Map<String,Object> body = new LinkedHashMap<>();
					body.put("suiteparameters", xml.getParameters());
					try {
						currentRestService.sendPostRequest(currentRestService.getUrl()+"initializeTestNG",body);
					} catch (IOException e) {
						logger.error("Exception while sending post request to "+currentRestService.getUrl()+"initialize "+e.getMessage());
					}
				}
			}
		}
	}

	private InputModel parseInpuFile(String inputFilePath) throws Exception {
		InputStream inputStream =	getInputStreamFromFile(inputFilePath);
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(inputStream, InputModel.class);
	}
	
	public static void main(String[] args) {
		MySuiteListener m  = new MySuiteListener();
		try {
			InputModel model = m.parseInpuFile("C:\\workspace\\ProductSuiteProject\\src\\main\\resources\\FeatureA\\inputfiles\\Test_Input.json");
			model.getProductList().stream().forEach(e -> System.out.println(e.getUid()));
		} catch (Exception e) {
			e.printStackTrace();
			e.printStackTrace();
		}
	}
}
