package com.test.api.aspect;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.CodeSignature;
import org.aspectj.lang.reflect.MethodSignature;
import org.testng.collections.CollectionUtils;

import com.test.api.constants.IConstant;
import com.test.api.listeners.MySuiteListener;
import com.test.api.resthandler.TestApiRestHandler;

@SuppressWarnings("unused")
@Aspect
public class TestNGTestApiAspect {

	@Around("execution(* *(..)) && @annotation(Test)")
	public Object around(ProceedingJoinPoint point) throws Throwable {
		String classname = point.getSignature().getDeclaringTypeName();
		MethodSignature signature = (MethodSignature) point.getSignature();
		Method method = signature.getMethod();
		
		Object result = null;

		TestApiRestHandler restService = this.isRestApi(point);
		if(restService!=null){
			List<Object> parameters = Arrays.asList(point.getArgs());
			String apiname = point.getSignature().getName();
			Map<String,Object> body = new LinkedHashMap<>();
			body.put("classname", classname);
			body.put("apiname", apiname);
			body.put("parameters", parameters);
			
			restService.sendPostRequest(restService.getTestApiUrl(), body);
			
			if(restService.getResponseCode()!=200){
				throw new Exception("Failed to execute API on Rest-URL: "+ restService.getTestApiUrl()+"\n"+restService.getResponseBody());
			}else{
				result = Void.class;
			}
		}else{
			result = point.proceed();
		}
		return result;
	}

	public TestApiRestHandler isRestApi(ProceedingJoinPoint point){
		if(MySuiteListener.getRestServiceList()!=null && !MySuiteListener.getRestServiceList().isEmpty()){
			String []paramNames = ((CodeSignature)point.getSignature()).getParameterNames();
			Object[] paramValues = point.getArgs();
			List<String> uidParameterValue=null;
			for(int index=0; index<paramNames.length;index++){
				String parameterName = paramNames[index].trim();
				if(parameterName.toUpperCase().endsWith("UID")){
					uidParameterValue = Arrays.asList(((String)paramValues[index]).split(IConstant.VALUE_SEPRATOR));
					break;
				}
			}
			if(uidParameterValue!=null){
				TestApiRestHandler restService = null;
				for(TestApiRestHandler rest : MySuiteListener.getRestServiceList()){
					if(rest.getUidsSet()!=null && containsUid(rest.getUidsSet(), uidParameterValue)){
						restService = rest;
						break;
					}
				}
				if(restService!=null){
					return restService;
				}else{
					return null;
				}
			}
		}else{
			return null;
		}
		return null;

	}
	
	private boolean containsUid(Set<String> uidSet, List<String> parameterList){
		for(String uid : uidSet){
			if(parameterList.contains(uid.trim())){
				return true;
			}
		}
		return false;
	}
}
