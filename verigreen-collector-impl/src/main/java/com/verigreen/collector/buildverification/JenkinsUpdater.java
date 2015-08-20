package com.verigreen.collector.buildverification;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.verigreen.collector.api.VerificationStatus;
import com.verigreen.collector.common.VerigreenNeededLogic;
import com.verigreen.collector.common.log4j.VerigreenLogger;
import com.verigreen.collector.model.CommitItem;
import com.verigreen.collector.observer.Observer;
import com.verigreen.collector.observer.Subject;
import com.verigreen.collector.spring.CollectorApi;
import com.verigreen.common.concurrency.RuntimeUtils;

public class JenkinsUpdater implements Subject {

	private ArrayList<Observer> observers = new ArrayList<>();
	private Map<Integer,BuildVerificationResult> resultsMap = new HashMap<Integer, BuildVerificationResult>();
	
	private static final Map<String,VerificationStatus> _verificationStatusesMap;
    static
    {
    	_verificationStatusesMap = new HashMap<String, VerificationStatus>();
    	_verificationStatusesMap.put("SUCCESS", VerificationStatus.PASSED);
    	_verificationStatusesMap.put("ABORTED", VerificationStatus.FAILED);
    }
	
	 private static JenkinsUpdater instance = null;
	   protected JenkinsUpdater() {
	      // Exists only to defeat instantiation.
	   }
	   public static JenkinsUpdater getInstance() {
	      if(instance == null) {
	         instance = new JenkinsUpdater();
	      }
	      return instance;
	   }
	
	public List<Observer> getObservers(){
		return this.observers;
	}
	
	public void addBuildVerificationResultToMap(BuildVerificationResult result)
	{
		resultsMap.put(result.getBuildNumber(), result);
	}

	@Override
	public void register(Observer o) {

		observers.add((CommitItem) o);
		VerigreenLogger.get().log(
             getClass().getName(),
             RuntimeUtils.getCurrentMethodName(),
             String.format(
                     "Observer registered: %s",
                     o.toString()));
	}
	
	@Override
	public void unregister(Observer o) {
	
		observers.remove(o);
		VerigreenLogger.get().log(
	             getClass().getName(),
	             RuntimeUtils.getCurrentMethodName(),
	             String.format(
	                     "Observer removed: %s",
	                     o.toString()));
	}

	@Override
	public void notifyObserver(List<Observer> relevantObservers, Map <String, List<String>> results) {
		List<Observer> notifiedObservers = new ArrayList<Observer>();
		for(Observer observer : relevantObservers)
		{
				List<String> result = results.get(((CommitItem)observer).getMergedBranchName());
				observer.updateBuildNumber(Integer.parseInt(result.get(0)));
				try {
					observer.updateBuildUrl(new URI(VerigreenNeededLogic.properties.getProperty("jenkins.url")+"job/"+VerigreenNeededLogic.properties.getProperty("jenkins.jobName")+"/"+result.get(0)+"/"));
				} catch (URISyntaxException e) {
					e.printStackTrace();
				}
				observer.update(_verificationStatusesMap.get(result.get(1)));
				notifiedObservers.add(observer);
				unregister(observer);
				VerigreenLogger.get().log(
			             getClass().getName(),
			             RuntimeUtils.getCurrentMethodName(),
			             String.format(
			                     "Successfully updated and saved observer: %s",
			                     observer.toString()));
			
	
		}
		
		CollectorApi.getCommitItemContainer().save((CommitItem) notifiedObservers);
		notifiedObservers = null;
		
	}
	
}
	