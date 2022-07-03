package com.test.api.listeners;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.testng.IDataProviderListener;
import org.testng.IDataProviderMethod;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.annotations.Parameters;
import org.testng.xml.XmlTest;

import com.test.api.constants.IConstant;
import com.test.api.resthandler.TestApiRestHandler;

public class TestApiDataProviderListener implements IDataProviderListener, IConstant {

	@Override
	public void beforeDataProviderExecution(IDataProviderMethod dataProviderMethod, ITestNGMethod method, ITestContext iTestContext) {
		System.out.println("beforeDataProviderExecution");
		switch (dataProviderMethod.getName()) {
		case REST_DATA_PROVIDER_NAME:
			if(MySuiteListener.getRestServiceList()!=null){
				System.out.println("beforeDataProviderExecution servicelist");
				this.dataProvider(dataProviderMethod, method, iTestContext);
			}
			break;

		default:
			break;
		}
	}

	@Override
	public void afterDataProviderExecution(IDataProviderMethod dataProviderMethod, ITestNGMethod method,
			ITestContext iTestContext) {
	}


	private void dataProvider(IDataProviderMethod dataProviderMethod, ITestNGMethod method, ITestContext iTestContext){
		XmlTest xmlTest = iTestContext.getCurrentXmlTest();
		Method javaMethod = method.getConstructorOrMethod().getMethod();
		Parameters testNgParameterrs = javaMethod.getAnnotation(Parameters.class);
		String[] parametersNameSet = testNgParameterrs.value();
		String uidParameterName = null;
		//STEP-1 Find a parameter with name ends-with UID and break at 1st match
		for(String currentParameterName : parametersNameSet){
			if(currentParameterName.toUpperCase().endsWith("UID")){
				uidParameterName = currentParameterName;
				break;
			}
		}
		if(uidParameterName!=null){
			TestApiDataProvider testDataProvider = (TestApiDataProvider)dataProviderMethod.getInstance();
			String uidParameterValue = method.findMethodParameters(xmlTest).get(uidParameterName);
			Map<String, String> attributesPerRestClientUIDMap = new LinkedHashMap<>();
			List<String> otherUIDs = new ArrayList<>();
			for(String currentUIDValue : uidParameterValue.split(VALUE_SEPRATOR)){				
				Optional<TestApiRestHandler> restService = MySuiteListener.getRestServiceList().stream().filter(restClient -> restClient.getUidsSet().stream().filter(uid -> uid.equals(currentUIDValue.trim())).count()>0).findFirst();
				if(restService.isPresent()){
					TestApiRestHandler currentRestService = restService.get();
					testDataProvider.setRestServiceDetail(currentRestService);
					if(attributesPerRestClientUIDMap.containsKey(currentRestService.getProductUID())){
						String oldUIDValue = attributesPerRestClientUIDMap.get(currentRestService.getProductUID());
						String newUIDValue = oldUIDValue + VALUE_SEPRATOR + currentUIDValue.trim();
						attributesPerRestClientUIDMap.put(currentRestService.getProductUID(), newUIDValue);
					}else{						
						attributesPerRestClientUIDMap.put(currentRestService.getProductUID(), currentUIDValue.trim());
					}
				}else{
					otherUIDs.add(currentUIDValue.trim());
				}
			}

			ArrayList<String> parameterData = new ArrayList<>();
			if(!otherUIDs.isEmpty()){
				for(int count=0;count<parametersNameSet.length;count++){
					if(parametersNameSet[count].equals(uidParameterName)){
						parameterData.add(count,String.join(VALUE_SEPRATOR, otherUIDs));
					}else{
						String parameterValue;
						parameterValue = method.findMethodParameters(xmlTest).get(parametersNameSet[count]);
						if(parameterValue==null){					
							Parameter javaParameter = javaMethod.getParameters()[count];
							if(javaParameter.isAnnotationPresent(org.testng.annotations.Optional.class)){
								org.testng.annotations.Optional testNgOptional = javaParameter.getAnnotation(org.testng.annotations.Optional.class);
								parameterValue = testNgOptional.value();
							}
						}
						if(parameterValue!=null){							
							parameterData.add(count,parameterValue.trim());
						}
					}
				}
				testDataProvider.addData(parameterData);
			}
			for(Map.Entry<String, String> perRestClientUID : attributesPerRestClientUIDMap.entrySet()){
				ArrayList<String> parameterDataToPush = new ArrayList<>();
				for(int count=0;count<parametersNameSet.length;count++){
					if(parametersNameSet[count].equals(uidParameterName)){
						parameterDataToPush.add(count,perRestClientUID.getValue().trim());
					}else{
						String parameterValue;
						parameterValue = method.findMethodParameters(xmlTest).get(parametersNameSet[count]);
						if(parameterValue==null){					
							Parameter javaParameter = javaMethod.getParameters()[count];
							if(javaParameter.isAnnotationPresent(org.testng.annotations.Optional.class)){
								org.testng.annotations.Optional testNgOptional = javaParameter.getAnnotation(org.testng.annotations.Optional.class);
								parameterValue = testNgOptional.value();
							}
						}
						if(parameterValue!=null){							
							parameterDataToPush.add(count,parameterValue.trim());
						}
					}
				}
				System.out.println("DATA TO PUSH IN DATA-PROVIDER "+parameterDataToPush.toString());
				testDataProvider.addData(parameterDataToPush);
			}
		}
	}
}
