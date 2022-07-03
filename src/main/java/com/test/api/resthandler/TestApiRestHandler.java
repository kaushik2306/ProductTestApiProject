package com.test.api.resthandler;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TestApiRestHandler {

	StringBuffer sb = new StringBuffer();

	private Process process;

	private String productionUID ;

	private String url;

	private long serverPort = -1;

	private String productTestApiProjectVersion;

	private String productRestApiProjectVersion = "0.0.1";

	private File currentWorkingDirectory;
	
	private File fatJarDirectory;

	private boolean executionStatus;

	private Set<String> uidsSet;

	public static final String MediaType_PlainText = "text/plain";

	public static final String EMPTY_BODY = "{\n}";

	Logger logger = LoggerFactory.getLogger(getClass());

	long timeOut = 60;//in minutes

	int responseCode = -1;

	String responseBody = "";

	public Set<String> getUidsSet() {
		return uidsSet;
	}


	public void setUidsSet(Set<String> uidsSet) {
		this.uidsSet = uidsSet;
	}

	public StringBuffer getSb() {
		return sb;
	}

	public void setSb(StringBuffer sb) {
		this.sb = sb;
	}

	public String getProductTestApiProjectVersion() {
		return productTestApiProjectVersion;
	}

	public void setProductTestApiProjectVersion(String productTestApiProjectVersion) {
		this.productTestApiProjectVersion = productTestApiProjectVersion;
	}

	public String getProductRestApiProjectVersion() {
		return productRestApiProjectVersion;
	}

	public void setProductRestApiProjectVersion(String productRestApiProjectVersion) {
		this.productRestApiProjectVersion = productRestApiProjectVersion;
	}

	public File getCurrentWorkingDirectory() {
		return currentWorkingDirectory;
	}

	public void setCurrentWorkingDirectory(File currentWorkingDirectory) {
		this.currentWorkingDirectory = currentWorkingDirectory;
	}

	public boolean isExecutionStatus() {
		return executionStatus;
	}

	public void setExecutionStatus(boolean executionStatus) {
		this.executionStatus = executionStatus;
	}

	public Process getProcess() {
		return process;
	}

	public void setProcess(Process process) {
		this.process = process;
	}

	public String getProductUID() {
		return productionUID;
	}

	public void setProductUID(String productionUid) {
		this.productionUID = productionUid;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public long getServerPort() {
		return serverPort;
	}

	public void setServerPort(long serverPort) {
		this.serverPort = serverPort;
	}

	public String getTestApiUrl(){
		return this.url!=null ? this.url+"testApi":"testApi";
	}
	
	public File getFatJarDirectory() {
		return fatJarDirectory;
	}

	public void setFatJarDirectory(File fatJarDirectory) {
		this.fatJarDirectory = fatJarDirectory;
	}


	public void start() {
		try {
			this.currentWorkingDirectory = new File(Paths.get(".").toAbsolutePath().normalize().toString());
			sb.append("PATH TO CREATE FILE AT "+this.currentWorkingDirectory);
			if(this.getServerPort()==-1){
				createPackage();
				this.executionStatus = startService();
				if(!executionStatus){
					throw new Exception("Could not Find the Spring Server-Port");
				}
			}else{
				this.url = "http://localhost:"+this.getServerPort()+"/api/";
				this.executionStatus = true;
			}
		} catch (Exception e) {
			this.executionStatus = false;
			sb.append("[EXCEPTION-START]\n"+ExceptionUtils.getStackTrace(e));
			sb.append("[EXCEPTION-END]\n");
		}
	}
	
	
	public void serviceBootProcessLogs(){
		logger.info(sb.toString());
		sb = new StringBuffer();
	}

	private void createPackage() throws Exception{
		InputStream pomInputStream = ClassLoader.getSystemClassLoader().getResourceAsStream("wrapperpom/pom.xml");
		File wrapperPomDestination = new File(this.currentWorkingDirectory.getAbsolutePath()+File.separator+"MultiVersion"+File.separator+this.productionUID+File.separator+"pom.xml");
		wrapperPomDestination.setReadable(true);
		wrapperPomDestination.setWritable(true);

		File file = new File(this.currentWorkingDirectory.getAbsolutePath()+File.separator+"MultiVersion"+File.separator+this.productionUID);
		if(!file.exists()){
			file.mkdirs();
		}

		FileUtils.copyInputStreamToFile(pomInputStream, wrapperPomDestination);
		pomInputStream.close();
		List<String> commands = new ArrayList<String>();
		sb.append("======== STEP-1 Create Fat-Jar ===========\n");
		this.fatJarDirectory = new File(this.currentWorkingDirectory.getAbsolutePath()+File.separator+"MultiVersion"+File.separator+this.productionUID);
		commands.add(System.getenv("M2_HOME")+File.separator+"bin"+File.separator+"mvn.cmd");
		commands.add("-Dproject.build.directory="+this.fatJarDirectory);
		commands.add("-DProductTestApiProject.version="+this.productTestApiProjectVersion);
		commands.add("-DProductRestApiProject.version="+this.productRestApiProjectVersion);	
		commands.add("-f");
		commands.add(wrapperPomDestination.getAbsolutePath());
		commands.add("-e");
		commands.add("-B");
		commands.add("package");
		ProcessBuilder pb = new ProcessBuilder(commands);
		pb.redirectErrorStream(true);
		pb.directory(this.currentWorkingDirectory);
		process = pb.start();
		boolean step1Result = processTestOutput(process);
		process.waitFor();
		if(process!=null && process.isAlive()){
			process.destroy();
		}

		if(!step1Result){
			throw new Exception("Could not create package jar for shelf "+this.productionUID);
		}
	}

	private boolean startService() throws Exception{
		sb.append("STEP-2 Start API Service\n");
		List<String> commandsToStartJar = new ArrayList<String>();
		commandsToStartJar.add("java");
		commandsToStartJar.add("-DisRestAgent=true");
		//You can set additional system property
		commandsToStartJar.add("-jar");
		commandsToStartJar.add(this.fatJarDirectory+File.separator+"TestServices-0.0.1.jar");
		ProcessBuilder newProcessBuilder = new ProcessBuilder(commandsToStartJar);
		newProcessBuilder.redirectErrorStream(true);
		newProcessBuilder.directory(this.currentWorkingDirectory);
		process = newProcessBuilder.start();
		boolean step2Result = processTestOutput(process);
		System.out.println("Service Started");
		return step2Result;
	}

	public boolean processTestOutput(Process process){
		InputStream is = process.getInputStream();
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		String line;
		boolean foundExpectedLine = false;
		try {
			while ((line = br.readLine()) != null) {
				sb.append(line+"\n");
				if(line.startsWith("TESTNG-SERVICE-PORT=")){
					this.serverPort = Long.valueOf(line.split("\\=")[1].trim());
					this.url = "http://localhost:"+this.serverPort+"/api/";
					sb.append("<<< Spring-Service Server-Port-Number "+this.serverPort+" >>>\n");
					sb.append("<<< Spring-Service BASE-URL for identifier "+this.productionUID+" is "+this.url+" >>>\n");
				}

				if(line.matches("[\\w\\W]+BUILD SUCCESS[\\w\\W]*")){
					foundExpectedLine = true;
					break;
				}
				if(this.serverPort!=-1){
					foundExpectedLine = true;
					break;
				}
			}
			if(isr!=null)
				isr.close();
			if(is!=null)
				is.close();
			if(br!=null)
				br.close();
		}catch (Exception e) {
			e.printStackTrace();
		}
		return foundExpectedLine;
	}

	public int getResponseCode() {
		return responseCode;
	}

	public String getResponseBody() {
		return responseBody;
	}

	
	public Response sendPostRequest(String url,Map<String,Object> requestBodyContent) throws IOException{
		responseBody = "";
		responseCode = -1;
		OkHttpClient client = new OkHttpClient().newBuilder()
				.connectTimeout(timeOut, TimeUnit.MINUTES)
				.writeTimeout(timeOut, TimeUnit.MINUTES)
				.readTimeout(timeOut, TimeUnit.MINUTES)
				.retryOnConnectionFailure(false)
				.pingInterval(5, TimeUnit.MINUTES)
				.build();
		MediaType mediaType = MediaType.parse("application/json");
		ObjectMapper mapper = new ObjectMapper();
		String requestBody = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(requestBodyContent);
		RequestBody body = RequestBody.create(mediaType, requestBody);
		Request request=null;

		request = new Request.Builder()
				.url(url)
				.post(body)
				.addHeader("Accept", "application/json")
				.addHeader("Content-Type", "application/json")
				.build();

		logger.info("Sending REST-API Request");
		logger.info("\nRequest:\n"+request.toString());
		logger.info("\nRequest-Boby:\n"+requestBodyContent);
		Response response = send(url,client, request);
		if(response!=null){
			printResponse(response);
		}
		return response;
	}

	public void printResponse(Response response) throws IOException{
		this.responseCode = response.code();
		this.responseBody = response.body().string();
		logger.info("REST-API Response-Received");
		logger.info("Response-Code: "+String.valueOf(this.responseCode));
		logger.info("\nResponse-Body:\n"+this.responseBody);
		logger.info("Sent-Request At: "+new Date(response.sentRequestAtMillis()));
		logger.info("Received-Response At: "+new Date(response.receivedResponseAtMillis()));
		long totalTime = response.receivedResponseAtMillis()-response.sentRequestAtMillis();
		if(totalTime>=0){
			logger.info("REST-API Total Respons-Time is "+(totalTime/1000)+" seconds");
		}
		logger.info("========================================================");
	}

	private Response send(String url, OkHttpClient client, Request request) throws IOException{
		int waitTime = 180000;
		int maxRetry = 2;
		int maxTryTimeout=waitTime*maxRetry;
		Date beforeSending=null;

		for(int count=0;count<maxRetry;count++){
			try {
				beforeSending = new Date();
				if(count==0){
					logger.info("Please wait for REST Response...");
				}else{
					logger.info("Re-sending REST Request wait for REST Response...");
				}
				return client.newCall(request).execute();
			} catch (IOException e) {
				if(verifyIfMaxTimeOutReached(beforeSending,new Date())){
					throw new IOException("Timeout of "+timeOut+" mins exceeded for REST Response. It is not expected");
				}
				if(count==maxRetry-1){
					logger.error("URL "+url+" is NOT Reachable within the max-timeout of "+maxTryTimeout+" milliseconds");
					throw e;
				}
				try {
					logger.error(e.getMessage());
					logger.info("URL "+url+" is NOT Reachable. Wait "+waitTime+" milliseconds and re-try");
					Thread.sleep(waitTime);
				} catch (InterruptedException e1) {
					logger.error("Thread.sleep Failed to wait for "+waitTime+" milliseconds because of "+e.getMessage(),e);
				}
			}
		}
		return null;
	}

	private boolean verifyIfMaxTimeOutReached(Date before, Date after) {
		if(before==null||after==null){
			logger.debug("Before sending request timestamp is "+before);
			logger.debug("After sending request timestamp is "+after);
			return false;
		}
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss"); 
		String beforeDateTime = simpleDateFormat.format(before);

		String afterDateTime = simpleDateFormat.format(after);

		Date date1;
		Date date2 ;
		long time_difference=0;
		try {
			date1 = simpleDateFormat.parse(beforeDateTime);
			date2 = simpleDateFormat.parse(afterDateTime);
			logger.debug("Before sending request timestamp is "+beforeDateTime);
			logger.debug("After sending request timestamp is "+afterDateTime);

			// Calculate time difference in milliseconds   
			time_difference = date2.getTime() - date1.getTime();
		} catch (ParseException e) {
			logger.error("Failed to verify max-timeout because of "+e.getMessage(),e);
		}
		return time_difference>timeOut*60000;
	}
}
