package com.test.api.listeners;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

import com.test.api.constants.IConstant;


public class TestAnnotationTransformer implements IAnnotationTransformer,IConstant {

	@Override
	public void transform(ITestAnnotation annotation, Class testClass, Constructor testConstructor, Method testMethod) {
		if(testMethod!=null){	
			if(Boolean.valueOf(System.getProperty(MULTI_VERSION, "false"))){
				annotation.setDataProvider(REST_DATA_PROVIDER_NAME);
				annotation.setDataProviderClass(TestApiDataProvider.class);
			}
		}
	}
}
