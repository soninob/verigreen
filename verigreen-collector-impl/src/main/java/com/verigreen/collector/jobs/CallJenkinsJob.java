package com.verigreen.collector.jobs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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
import com.verigreen.collector.api.VerificationStatus;
import com.verigreen.collector.buildverification.CommitItemVerifier;
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
	private int _maximumRetries = getNumberOfRetriesCounter();
	private int _maximumTimeout = getTriggerTimeoutCounter();
	
	
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

	private int getTriggerTimeoutCounter()
	{
		int counterTimeout = Integer.parseInt(VerigreenNeededLogic.properties.getProperty("timeout.counter"));
		return counterTimeout;

	}
	
	private int getNumberOfRetriesCounter()
	{
		int counterRetries = Integer.parseInt(VerigreenNeededLogic.properties.getProperty("retry.counter"));
		return counterRetries;
	}
	
	private void calllingJenkinsForCreate() {
		VerigreenLogger.get().log(getClass().getName(), RuntimeUtils.getCurrentMethodName(), " - Method started");


		
		for (Iterator<CommitItem> iterator = CommitItemVerifier.getInstance().getCommitItems().iterator(); iterator.hasNext();) {
			CommitItem ci = iterator.next();
			iterator.remove();
			JenkinsVerifier.triggerJob(com.verigreen.collector.spring.CollectorApi.getCommitItemContainer().get(ci.getKey()));
			
			// Remove the current element from the iterator and the list.
	        
		}
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

		String result;
		VerigreenLogger.get().log(getClass().getName(), RuntimeUtils.getCurrentMethodName(), " - Method started");
		int sizeObservers = jenkinsUpdater.getObservers().size();
		VerigreenLogger.get().log(
	             getClass().getName(),
	             RuntimeUtils.getCurrentMethodName(),
	             String.format(
	                     "Jenkins called for update on [%s] not updated items...",
	                     sizeObservers ));
		
		if (sizeObservers > 0){
			RestClientResponse response = createRestCall("api/json?depth=1&pretty=true&tree=builds[number,result,building,timestamp,actions[parameters[value]]]");
			result = response.getEntity(String.class);
			try {
				Map<String, MinJenkinsJob> parsedResults = parsingJSON(result);
				
				List<Observer> analyzedResults = analyzeResults(parsedResults);
				
				
				if(!analyzedResults.isEmpty())
				{
					/*jenkinsUpdater.notifyObserver(analyzedResults, parsedResults);*/
					jenkinsUpdater.notifyObserver(jenkinsUpdater.calculateRelevantObservers(analyzedResults, parsedResults));
				}
			} catch (JSONException e) {
				VerigreenLogger.get().error(
			             getClass().getName(),
			             RuntimeUtils.getCurrentMethodName(),
			             "Bad JSON response: " + result);//for security reasons - remove the result from the exception.
			}
		}
		
		VerigreenLogger.get().log(getClass().getName(), RuntimeUtils.getCurrentMethodName(), " - Method ended");
		
	}
	private Map<String, MinJenkinsJob> parsingJSON(String json) throws JSONException {
		
		VerigreenLogger.get().log(getClass().getName(), RuntimeUtils.getCurrentMethodName(), " - Method started");
		Map<String, MinJenkinsJob> buildsAndStatusesMap = new HashMap<String, MinJenkinsJob>();
		JsonParser parser = new JsonParser();
		JsonObject mainJson = (JsonObject) parser.parse(json);
		
		JsonObject parameterJsonObjectArray = null;

		JsonArray jsonBuildsArray = mainJson.getAsJsonArray("builds");
		for (int i = 0; i < jsonBuildsArray.size(); i++) 
		{  // **line 2**
				 JsonObject childJsonObject = (JsonObject) jsonBuildsArray.get(i);
				 String buildNumber = childJsonObject.get("number").getAsString();
				 Object jenkinsResult = childJsonObject.get("result");
				 
				 MinJenkinsJob values = new MinJenkinsJob();
				 values.setBuildNumber(buildNumber);
				 if(jenkinsResult == null)
				 {
					 values.setJenkinsResult("null");
				 }
				 else{
					 values.setJenkinsResult(jenkinsResult.toString().replace("\"",""));
				 }
				 
				 
//				 String timestamp = childJsonObject.get("timestamp").getAsString();
						 
				 //buildsAndStatusesMap.put(buildNumber,jenkinsResult);
				 
				 JsonArray actionsJsonArray = childJsonObject.get("actions").getAsJsonArray();
				 for(int j = 0 ; j < actionsJsonArray.size() ; j ++)
				 {
					 if(((JsonObject)actionsJsonArray.get(j)).getAsJsonArray("parameters")!= null) 
					 {
						 parameterJsonObjectArray = (JsonObject) actionsJsonArray.get(j);
						 break;
					 }
				 }
				 JsonArray jsonParametersArray =  parameterJsonObjectArray.getAsJsonArray("parameters");
				 
				 JsonObject parameterJsonObject = (JsonObject) jsonParametersArray.get(0);
				 
				 values.setBranchName(parameterJsonObject.get("value").getAsString());
				 
				 buildsAndStatusesMap.put(values.getBranchName(), values);	
				 
		}
		VerigreenLogger.get().log(getClass().getName(), RuntimeUtils.getCurrentMethodName(), " - Method ended");
		return buildsAndStatusesMap;
	}
	private void checkTriggerAndRetryMechanism(Observer observer)
	{
		/*TODO check the observer, if the observer (CommitItem) doesn't have _buildnumber 
		 * then check the parsedResults, and if there is no value calculate timeout.
		 * this method will do:
		 * 1) if both counters reaches their limits if so = trigger failed
		 * 	  if timeoutConter didn't reach the limit then:
			 * 		++timeoutcounter
			 * 		change the triggerAtemoted to false
			 * 		unregister the observer from subject (update)
			 * 		adding it (commitItem) to the commitItemVerifier list. 
			 * else
		 * 			++triggercounter;
		 * 			timeoutCounter = 0;
		 * unregister the observer from subject (update)
			 * 		adding it (commitItem) to the commitItemVerifier list.
		*/
		/*if(!parsedResults.get(((CommitItem)observer).getMergedBranchName()).equals("null"))
		{
			relevantObservers.add(observer);
		}*/
		
		int retriableCounter = ((CommitItem)observer).getRetriableCounter();
		int timeoutCounter = ((CommitItem)observer).getTimeoutCounter();
		
		
		if(timeoutCounter >= _maximumTimeout && retriableCounter >= _maximumRetries)
		{
			observer.update(VerificationStatus.TRIGGER_FAILED);
			jenkinsUpdater.unregister(observer);
			//TODO save the commit item
		}
		else if(((CommitItem)observer).getTimeoutCounter() < _maximumTimeout)
		{

			timeoutCounter++;
			((CommitItem)observer).setTimeoutCounter(timeoutCounter);
			
			((CommitItem)observer).setTriggeredAttempt(false);
			
			jenkinsUpdater.unregister(observer);
			CommitItemVerifier.getInstance().getCommitItems().add((CommitItem)observer);
		}
		else{

			retriableCounter++;
			((CommitItem)observer).setRetriableCounter(retriableCounter);
			
			((CommitItem)observer).setTimeoutCounter(0);
			
			jenkinsUpdater.unregister(observer);
			CommitItemVerifier.getInstance().getCommitItems().add((CommitItem)observer);
		}
	}
	
	private List<Observer> analyzeResults(Map<String, MinJenkinsJob> parsedResults){
		
		VerigreenLogger.get().log(getClass().getName(), RuntimeUtils.getCurrentMethodName(), " - Method started");
		List<Observer> observers =  jenkinsUpdater.getObservers();
		List<Observer> relevantObservers = new ArrayList<Observer>();
		for(Observer observer : observers)
		{
			try {
				//the default build url for an untriggered item is 0, also check for null value in the parsed results, that means that 
				//the MinJenkinsJob didn't get any response for that particular commit item 
				/*if(parsedResults.get(((CommitItem)observer).getMergedBranchName()).equals("null"))
				{
					checkTriggerAndRetryMechanism(observer);
				}
				else */
				if(((CommitItem)observer).getBuildNumber() < 0)
				{
					if(parsedResults.get(((CommitItem)observer).getMergedBranchName()).equals("null"))
					{
						int timeoutCounter = ((CommitItem)observer).getTimeoutCounter();
						timeoutCounter++;
						((CommitItem)observer).setTimeoutCounter(timeoutCounter);
						
						((CommitItem)observer).setTriggeredAttempt(false);
						
						jenkinsUpdater.unregister(observer);
						CommitItemVerifier.getInstance().getCommitItems().add((CommitItem)observer);
						
					}	
					checkTriggerAndRetryMechanism(observer);
				}
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
