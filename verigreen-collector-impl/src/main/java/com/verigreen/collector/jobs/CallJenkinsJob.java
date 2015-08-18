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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.verigreen.collector.buildverification.JenkinsUpdater;
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
		
		calllingJenkinsForUpdate();
		//calllingJenkinsForCreate();
		//calllingJenkinsForUpdate();
		//calllingJenkinsForCancel();
	}

	private void calllingJenkinsForCancel() {
		// TODO Auto-generated method stub
		
	}

	private void calllingJenkinsForCreate() {
		//TBD
		
	}

	private RestClientResponse createRestCall(String param) {
		String jenkinsUrl = VerigreenNeededLogic.properties.getProperty("jenkins.url");
		String jobName = VerigreenNeededLogic.properties.getProperty("jenkins.jobName");
		
		RestClientResponse result = new RestClientImpl().get(CollectorApi.getJenkinsCallRequest(jenkinsUrl, jobName, param));
		return result;
	}

	private void calllingJenkinsForUpdate() {
			RestClientResponse response = createRestCall("api/json?depth=1&pretty=true&tree=builds[number,result]");
			String result = response.getEntity(String.class);
			
			try {
				Map<Integer, String> parsedResults = parsingJSON(result);
			
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
	private Map<Integer, String> parsingJSON(String json) throws JSONException
	{	
		Map<Integer, String> buildsAndStatusesMap = new HashMap<Integer, String>();
		JsonParser parser = new JsonParser();
		JsonObject mainJson = (JsonObject) parser.parse(json);

		JsonArray jsonBuildsArray = mainJson.getAsJsonArray("builds");
		for (int i = 0; i < jsonBuildsArray.size(); i++) 
		{  // **line 2**
				 JsonObject childJsonObject = (JsonObject) jsonBuildsArray.get(i);
				 int buildNumber = childJsonObject.get("number").getAsInt();
				 String jenkinsResult = childJsonObject.get("result").getAsString();

				 
				 buildsAndStatusesMap.put(buildNumber,jenkinsResult);
				 
				 /**
				 JsonArray actionsJsonArray = childJsonObject.get("actions").getAsJsonArray();
				 
				 JsonObject parameterJsonObjectArray = (JsonObject) actionsJsonArray.get(0);
		
				 JsonArray jsonParametersArray =  parameterJsonObjectArray.getAsJsonArray("parameters");
				 
				 for (int j = 0; j < jsonParametersArray.size();j++) {
					 JsonObject parameterJsonObject = (JsonObject) jsonParametersArray.get(j);
					 if(parameterJsonObject.get("name").getAsString().equals("status"))
					 {
						 String resultStatus = parameterJsonObject.get("value").getAsString();
						 VerificationStatus status = convertToVerifStatusFromString(resultStatus);
						 map.put(buildNumber, status);
						 break;
					 }		
				 }	
				 */	 
				 
		}
		return buildsAndStatusesMap;
	}

	
	private List<Observer> analyzeResults(Map<Integer, String> recievedJsonResults)
	{
		List<Observer> observers =  jenkinsUpdater.getObservers();
		List<Observer> relevantObservers = new ArrayList<Observer>();
		for(Observer observer : observers)
		{
			if(!recievedJsonResults.get(((CommitItem)observer).getBuildNumber()).equals("null"))
			{
				relevantObservers.add(observer);
			}
		}
		return relevantObservers;
	}

	
}
