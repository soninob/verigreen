package com.verigreen.collector.buildverification;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.verigreen.collector.api.VerificationStatus;
import com.verigreen.collector.common.log4j.VerigreenLogger;
import com.verigreen.collector.model.CommitItem;
import com.verigreen.collector.observer.Observer;
import com.verigreen.collector.observer.Subject;
import com.verigreen.collector.spring.CollectorApi;
import com.verigreen.common.concurrency.RuntimeUtils;

public class JenkinsUpdater implements Subject {

	private ArrayList<Observer> observers = new ArrayList<>();
	private static final Map<String,VerificationStatus> _verificationStatusesMap;
    static
    {
    	_verificationStatusesMap = new HashMap<String, VerificationStatus>();
    	_verificationStatusesMap.put("SUCCESS", VerificationStatus.PASSED);
    	_verificationStatusesMap.put("ABORTED", VerificationStatus.FAILED);
    	_verificationStatusesMap.put("", VerificationStatus.RUNNING);
    }
    
    private static volatile JenkinsUpdater _instance; 
    
    
    public static JenkinsUpdater getInstance()
    { 
    	if(_instance == null)
    	{
    		synchronized(JenkinsUpdater.class)
    		{ 
    			if(_instance == null)
    			{ 
    				_instance = new JenkinsUpdater();  
    			}
    		}
    	}
    return _instance; 
    }

	public List<Observer> getObservers(){
		return this.observers;
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
		List<CommitItem> notifiedObservers = new ArrayList<CommitItem>();
		for(Observer observer : relevantObservers)
		{
				List<String> result = results.get(((CommitItem)observer).getMergedBranchName());
				observer.updateBuildNumber(Integer.parseInt(result.get(0)));
				try {
					observer.updateBuildUrl(new URI(JenkinsVerifier.getBuildUrl(Integer.parseInt(result.get(0)))));
				} catch (URISyntaxException e) {
					VerigreenLogger.get().error(
		                    getClass().getName(),
		                    RuntimeUtils.getCurrentMethodName(),
		                    String.format(
		                            "Illegal character in build URL: [%s]",
		                            JenkinsVerifier.getBuildUrl(Integer.parseInt(result.get(0)))),
		                    e);
				}
				observer.update(_verificationStatusesMap.get(result.get(1)));
				notifiedObservers.add((CommitItem)observer);
				unregister(observer);
				VerigreenLogger.get().log(
			             getClass().getName(),
			             RuntimeUtils.getCurrentMethodName(),
			             String.format(
			                     "Successfully updated and saved observer: %s",
			                     observer.toString()));
			
	
		}
		
		CollectorApi.getCommitItemContainer().save(notifiedObservers);
		notifiedObservers.clear();
		
	}
	
}
	
