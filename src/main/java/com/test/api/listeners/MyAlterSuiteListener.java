package com.test.api.listeners;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.IAlterSuiteListener;
import org.testng.xml.XmlSuite;

import com.company.inputmodel.InputModel;
import com.company.inputmodel.product.ProductModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.api.constants.IConstant;
import com.test.api.resthandler.TestApiRestHandler;


public class MyAlterSuiteListener implements IAlterSuiteListener, IConstant {

	Logger logger = LoggerFactory.getLogger(getClass());
	
	private boolean isRestAgent = Boolean.valueOf(System.getProperty(REST_AGENT,"false"));


	@Override
	public void alter(List<XmlSuite> suiteXmls) {
		for(XmlSuite xmlSuite : suiteXmls){	
			try {
				initRestServceDetails(xmlSuite);				
			} catch (Exception e) {
				Assert.fail(e.getMessage());
			}
		}
	}

	public void initRestServceDetails(XmlSuite xmlSuite) throws Exception{
		if(!isRestAgent){
			String inputFilePath = xmlSuite.getParameter(INPUT_FILE_PATH);
			if(inputFilePath!=null){
				InputModel inputModel = parseInpuFile(inputFilePath);
				if(inputFilePath!=null){
					List<TestApiRestHandler> restServiceDetails = new ArrayList<>();
					Set<String> uidsSet = inputModel.getProductList().stream().filter(e -> e.getRestPort()!=-1).map(b -> b.getUid()).collect(Collectors.toSet());
					for(ProductModel product : inputModel.getProductList()){
						if(product.getRestPort()!=-1){				
							TestApiRestHandler restService = new TestApiRestHandler();
							restService.setProductUID(product.getUid().trim());
							restService.setUidsSet(uidsSet);
							restService.setServerPort(product.getRestPort());
							restServiceDetails.add(restService);
							logger.info("Added product to rest service "+product.getUid());
						}
					}

					if(!restServiceDetails.isEmpty()){
						MySuiteListener.setRestServiceList(restServiceDetails);
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
		MyAlterSuiteListener alt = new MyAlterSuiteListener();
		XmlSuite xmlSuite = new XmlSuite();
		Map<String, String> parameters = new HashMap<>();
		parameters.put(INPUT_FILE_PATH, "C:\\workspace\\ProductSuiteProject\\src\\main\\resources\\FeatureA\\inputfiles\\Test_Input.json");
		xmlSuite.setParameters(parameters);
		
		try {
			alt.initRestServceDetails(xmlSuite);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		MySuiteListener suite = new MySuiteListener();
		suite.startRestServices(xmlSuite);
		
		
	}
}
