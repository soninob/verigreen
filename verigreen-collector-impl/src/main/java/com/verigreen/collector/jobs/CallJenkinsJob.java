package com.verigreen.collector.jobs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.verigreen.collector.buildverification.CommitItemVerifier;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.verigreen.collector.buildverification.JenkinsUpdater;
import com.verigreen.collector.buildverification.JenkinsVerifier;
import com.verigreen.collector.common.VerigreenNeededLogic;
import com.verigreen.collector.common.log4j.VerigreenLogger;
import com.verigreen.collector.model.CommitItem;
import com.verigreen.collector.model.MinJenkinsJob;
import com.verigreen.collector.observer.Observer;
import com.verigreen.common.concurrency.RuntimeUtils;
import com.verigreen.restclient.RestClientImpl;
import com.verigreen.restclient.RestClientResponse;
import com.verigreen.spring.common.CollectorApi;

@DisallowConcurrentExecution
public class CallJenkinsJob implements Job {

	JenkinsUpdater jenkinsUpdater = JenkinsUpdater.getInstance();
	
	public CallJenkinsJob(){}
	@Override
	public void execute(JobExecutionContext context)
			throws JobExecutionException {

		calllingJenkinsForUpdate();
		calllingJenkinsForCreate();
		calllingJenkinsForCancel();

	}

	private void calllingJenkinsForCancel() {
		// TODO Auto-generated method stub
		
	}

	private void calllingJenkinsForCreate() {
		VerigreenLogger.get().log(getClass().getName(), RuntimeUtils.getCurrentMethodName(), " - Method started");
		
		int listSize = CommitItemVerifier.getInstance().getCommitItems().size();
		for( int i = 0; i < listSize; i++) {
			
	          JenkinsVerifier.triggerJob(CommitItemVerifier.getInstance().getCommitItems().get(i));
	          jenkinsUpdater.register(CommitItemVerifier.getInstance().getCommitItems().get(i));
	          
		}
		
		CommitItemVerifier.getInstance().getCommitItems().clear();
		VerigreenLogger.get().log(getClass().getName(), RuntimeUtils.getCurrentMethodName(), " - Method ended");
	}

	private RestClientResponse createRestCall(String param) {
		VerigreenLogger.get().log(getClass().getName(), RuntimeUtils.getCurrentMethodName(), " - Method started");
		String jenkinsUrl = VerigreenNeededLogic.properties.getProperty("jenkins.url");
		String jobName = VerigreenNeededLogic.properties.getProperty("jenkins.jobName");
		
		RestClientResponse result = new RestClientImpl().get(CollectorApi.getJenkinsCallRequest(jenkinsUrl, jobName, param));
		VerigreenLogger.get().log(getClass().getName(), RuntimeUtils.getCurrentMethodName(), " - Method ended");
		return result;
	}

	private void calllingJenkinsForUpdate() {

		VerigreenLogger.get().log(getClass().getName(), RuntimeUtils.getCurrentMethodName(), " - Method started");
		int sizeObservers = jenkinsUpdater.getObservers().size();
		VerigreenLogger.get().log(
	             getClass().getName(),
	             RuntimeUtils.getCurrentMethodName(),
	             String.format(
	                     "Jenkins called for update on [%s] not updated items...",
	                     sizeObservers ));
		
		
		RestClientResponse response = createRestCall("api/json?depth=1&pretty=true&tree=builds[number,result,building,timestamp,actions[parameters[value]]]");
		String result = response.getEntity(String.class);
		
		

		try {
			Map<String, MinJenkinsJob> parsedResults = parsingJSON(result);
		
			List<Observer> analyzedResults = analyzeResults(parsedResults);
			
			
			if(!analyzedResults.isEmpty())
			{
				/*jenkinsUpdater.notifyObserver(analyzedResults, parsedResults);*/
				jenkinsUpdater.notifyObserver(jenkinsUpdater.calculateRelevantObservers(analyzedResults, parsedResults));
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		VerigreenLogger.get().log(getClass().getName(), RuntimeUtils.getCurrentMethodName(), " - Method ended");
	
	}
	private Map<String, MinJenkinsJob> parsingJSON(String json) throws JSONException {
		
		VerigreenLogger.get().log(getClass().getName(), RuntimeUtils.getCurrentMethodName(), " - Method started");
		Map<String, MinJenkinsJob> buildsAndStatusesMap = new HashMap<String, MinJenkinsJob>();
		JsonParser parser = new JsonParser();
		JsonObject mainJson = (JsonObject) parser.parse(json);
		
		JsonObject parameterJsonObjectArray;

		JsonArray jsonBuildsArray = mainJson.getAsJsonArray("builds");
		for (int i = 0; i < jsonBuildsArray.size(); i++) 
		{  // **line 2**
				 JsonObject childJsonObject = (JsonObject) jsonBuildsArray.get(i);
				 String buildNumber = childJsonObject.get("number").getAsString();
				 String jenkinsResult = childJsonObject.get("result").getAsString();
				 
				 MinJenkinsJob values = new MinJenkinsJob();
				 values.setBuildNumber(buildNumber);
				 values.setJenkinsResult(jenkinsResult);
				 
//				 String timestamp = childJsonObject.get("timestamp").getAsString();
						 
				 //buildsAndStatusesMap.put(buildNumber,jenkinsResult);
				 
				 JsonArray actionsJsonArray = childJsonObject.get("actions").getAsJsonArray();
				 
				 if(((JsonObject)actionsJsonArray.get(0)).getAsJsonArray("parameters")!= null) {
					 parameterJsonObjectArray = (JsonObject) actionsJsonArray.get(0);
				 } else {
					 parameterJsonObjectArray = (JsonObject) actionsJsonArray.get(1);
				 }
				 
				 JsonArray jsonParametersArray =  parameterJsonObjectArray.getAsJsonArray("parameters");
				 
				 JsonObject parameterJsonObject = (JsonObject) jsonParametersArray.get(0);
				 
				 values.setBranchName(parameterJsonObject.get("value").getAsString());
				 
				 buildsAndStatusesMap.put(values.getBranchName(), values);
				 
					 
					 /*if(parameterJsonObject.get("name").getAsString().equals("status"))
					 {
						 String resultStatus = parameterJsonObject.get("value").getAsString();
						 VerificationStatus status = convertToVerifStatusFromString(resultStatus);
						 buildsAndStatusesMap.put(buildNumber, status);
						 break;
					 }*/		
				 
		}
		VerigreenLogger.get().log(getClass().getName(), RuntimeUtils.getCurrentMethodName(), " - Method ended");
		return buildsAndStatusesMap;
	}

	
	private List<Observer> analyzeResults(Map<String, MinJenkinsJob> parsedResults){
		
		VerigreenLogger.get().log(getClass().getName(), RuntimeUtils.getCurrentMethodName(), " - Method started");
		List<Observer> observers =  jenkinsUpdater.getObservers();
		List<Observer> relevantObservers = new ArrayList<Observer>();
		for(Observer observer : observers)
		{
			try {	
				if(!parsedResults.get(((CommitItem)observer).getMergedBranchName()).equals("null"))
				{
					relevantObservers.add(observer);
				}
			}
			catch (NullPointerException e){ //means that the update didn't get details of the new create.
				continue;
			}
		}
		VerigreenLogger.get().log(getClass().getName(), RuntimeUtils.getCurrentMethodName(), " - Method ended");
		return relevantObservers;
	}

	
}
