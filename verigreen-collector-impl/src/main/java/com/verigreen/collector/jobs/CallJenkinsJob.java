package com.verigreen.collector.jobs;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.verigreen.collector.buildverification.CommitItemVerifier;
import com.verigreen.collector.buildverification.JenkinsUpdater;
import com.verigreen.collector.buildverification.JenkinsVerifier;
import com.verigreen.collector.common.VerigreenNeededLogic;
import com.verigreen.restclient.RestClientImpl;
import com.verigreen.restclient.RestClientResponse;
import com.verigreen.spring.common.CollectorApi;

@DisallowConcurrentExecution
public class CallJenkinsJob implements Job {
	
	JenkinsUpdater jenkinsUpdater = new JenkinsUpdater();
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
	          CommitItemVerifier.createCommitItems.remove(i);
			
		}
	}

	private RestClientResponse createRestCall(String param) {
		String jenkinsUrl = VerigreenNeededLogic.properties.getProperty("jenkins.url");
		String jobName = VerigreenNeededLogic.properties.getProperty("jenkins.jobName");
		
		RestClientResponse result = new RestClientImpl().get(CollectorApi.getJenkinsCallRequest(jenkinsUrl, jobName, param));
		return result;
	}

	private void calllingJenkinsForUpdate() {
		String formatOutput ="json?pretty=true&depth=2&tree=builds[number,result]";
		
		RestClientResponse resultingJson = createRestCall(formatOutput);
		String jsonString = resultingJson.toString();
		System.out.println("#########################################################################");
		System.out.println(jsonString);
		System.out.println("#########################################################################");
		
	}
	
	private String parsingJSON(String json){
		return null;
	}

	
	private void analyzeResults(){
		
	}
	
}
