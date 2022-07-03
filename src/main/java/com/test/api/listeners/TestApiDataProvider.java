package com.test.api.listeners;

import java.util.ArrayList;

import org.testng.annotations.DataProvider;

import com.test.api.constants.IConstant;
import com.test.api.resthandler.TestApiRestHandler;


public class TestApiDataProvider implements IConstant{
	
	public TestApiRestHandler restServiceDetail;

	public ArrayList<ArrayList<String>> data = new ArrayList<>();
	
	public ArrayList<String> getLastEntry(){
		return data.get(data.size()-1);
	}
	public ArrayList<String> getEntryByIndex(int index){
		return data.get(index);
	}
	public ArrayList<ArrayList<String>> getData() {
		return data;
	}

	public void addData(ArrayList<String> parameters){
		data.add(parameters);
	}
	
	public void addDataByIndex(int index,ArrayList<String> parameters){
		data.add(index,parameters);
	}
	
	public TestApiRestHandler getRestServiceDetail() {
		return restServiceDetail;
	}
	public void setRestServiceDetail(TestApiRestHandler restServiceDetail) {
		this.restServiceDetail = restServiceDetail;
	}
	
	@DataProvider(name=REST_DATA_PROVIDER_NAME)
	public Object[][] restApiParameters(){
		Object[][] parameterArray = this.data.stream().map(u -> u.toArray(new Object[0])).toArray(Object[][]::new);
		return parameterArray;
	}
}
