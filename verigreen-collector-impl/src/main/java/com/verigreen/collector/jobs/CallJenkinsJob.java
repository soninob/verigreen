package com.verigreen.collector.jobs;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.verigreen.collector.buildverification.JenkinsUpdater;
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
		calllingJenkinsForUpdate();
		calllingJenkinsForCreate();
		calllingJenkinsForCancel();
	}

	private void calllingJenkinsForCancel() {
		// TODO Auto-generated method stub
		
	}

	private void calllingJenkinsForCreate() {
		//TBD
		
	}

	private RestClientResponse createRestCall() {
		String jenkinsUrl = VerigreenNeededLogic.properties.getProperty("jenkins.url");
		String jobName = VerigreenNeededLogic.properties.getProperty("jenkins.jobName");
		String formatOutput ="json?pretty=true&depth=2&tree=builds[number,result]";
		
		RestClientResponse result = new RestClientImpl().get(CollectorApi.getJenkinsCallRequest(jenkinsUrl, jobName, formatOutput));
		return result;
	}

	private void calllingJenkinsForUpdate() {
		RestClientResponse resultingJson = createRestCall();
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
