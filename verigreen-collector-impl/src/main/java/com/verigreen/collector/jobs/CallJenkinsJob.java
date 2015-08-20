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

		calllingJenkinsForCreate();
		calllingJenkinsForUpdate();
		calllingJenkinsForCancel();

	}

	private void calllingJenkinsForCancel() {
		// TODO Auto-generated method stub
		
	}

	private void calllingJenkinsForCreate() {
		
		for( int i = 0; i < CommitItemVerifier.createCommitItems.size(); i++) {
			
	          JenkinsVerifier.triggerJob(CommitItemVerifier.createCommitItems.get(i));
	          jenkinsUpdater.register(CommitItemVerifier.createCommitItems.get(i));
	          
		}
		
		CommitItemVerifier.createCommitItems.clear();
	}

	private RestClientResponse createRestCall(String param) {
		String jenkinsUrl = VerigreenNeededLogic.properties.getProperty("jenkins.url");
		String jobName = VerigreenNeededLogic.properties.getProperty("jenkins.jobName");
		
		RestClientResponse result = new RestClientImpl().get(CollectorApi.getJenkinsCallRequest(jenkinsUrl, jobName, param));
		return result;
	}

	private void calllingJenkinsForUpdate() {

			RestClientResponse response = createRestCall("api/json?depth=1&pretty=true&tree=builds[number,result,building,timestamp,actions[parameters[value]]]");
			String result = response.getEntity(String.class);
			
			try {
				Map<String, List<String>> parsedResults = parsingJSON(result);
			
				List<Observer> analyzedResults = analyzeResults(parsedResults);
				
				VerigreenLogger.get().log(
			             getClass().getName(),
			             RuntimeUtils.getCurrentMethodName(),
			             String.format(
			                     "Jenkins called for update on [%s] not updated items...",
			                     analyzedResults.size()));

				if(!analyzedResults.isEmpty())
				{
					jenkinsUpdater.notifyObserver(analyzedResults, parsedResults);
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}		
	
	}
	private Map<String, List<String>> parsingJSON(String json) throws JSONException
	{	
		Map<String, List<String>> buildsAndStatusesMap = new HashMap<String, List<String>>();
		JsonParser parser = new JsonParser();
		JsonObject mainJson = (JsonObject) parser.parse(json);
		
		JsonObject parameterJsonObjectArray;

		JsonArray jsonBuildsArray = mainJson.getAsJsonArray("builds");
		for (int i = 0; i < jsonBuildsArray.size(); i++) 
		{  // **line 2**
				 JsonObject childJsonObject = (JsonObject) jsonBuildsArray.get(i);
				 String buildNumber = childJsonObject.get("number").getAsString();
				 String jenkinsResult = childJsonObject.get("result").getAsString();
				 
				 List<String> values = new ArrayList<>();
				 values.add(buildNumber);
				 values.add(jenkinsResult);
				 
				 String timestamp = childJsonObject.get("timestamp").getAsString();
						 
				 //buildsAndStatusesMap.put(buildNumber,jenkinsResult);
				 
				 JsonArray actionsJsonArray = childJsonObject.get("actions").getAsJsonArray();
				 
				 if(((JsonObject)actionsJsonArray.get(0)).getAsJsonArray("parameters")!= null) {
					 parameterJsonObjectArray = (JsonObject) actionsJsonArray.get(0);
				 } else {
					 parameterJsonObjectArray = (JsonObject) actionsJsonArray.get(1);
				 }
				 
				 JsonArray jsonParametersArray =  parameterJsonObjectArray.getAsJsonArray("parameters");
				 
				 JsonObject parameterJsonObject = (JsonObject) jsonParametersArray.get(0);
				 
				 String branch = parameterJsonObject.get("value").getAsString();
				 
				 buildsAndStatusesMap.put(branch, values);
				 
					 
					 /*if(parameterJsonObject.get("name").getAsString().equals("status"))
					 {
						 String resultStatus = parameterJsonObject.get("value").getAsString();
						 VerificationStatus status = convertToVerifStatusFromString(resultStatus);
						 buildsAndStatusesMap.put(buildNumber, status);
						 break;
					 }*/		
				 
		}
		return buildsAndStatusesMap;
	}

	
	private List<Observer> analyzeResults(Map<String, List<String>> parsedResults)
	{
		List<Observer> observers =  jenkinsUpdater.getObservers();
		List<Observer> relevantObservers = new ArrayList<Observer>();
		for(Observer observer : observers)
		{
			if(!parsedResults.get(((CommitItem)observer).getMergedBranchName()).equals("null"))
			{
				relevantObservers.add(observer);
			}
		}
		return relevantObservers;
	}

	
}
