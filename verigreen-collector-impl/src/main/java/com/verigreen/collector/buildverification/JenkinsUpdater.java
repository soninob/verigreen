package com.verigreen.collector.buildverification;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.offbytwo.jenkins.model.Build;
import com.offbytwo.jenkins.model.BuildWithDetails;
import com.offbytwo.jenkins.model.Job;
import com.verigreen.collector.common.log4j.VerigreenLogger;
import com.verigreen.collector.model.CommitItem;
import com.verigreen.collector.observer.Observer;
import com.verigreen.collector.observer.Subject;
import com.verigreen.collector.spring.CollectorApi;
import com.verigreen.common.concurrency.RuntimeUtils;




public class JenkinsUpdater implements Subject {

	private ArrayList<Observer> observers = new ArrayList<>();
	private Map<Integer,BuildVerificationResult> resultsMap = new HashMap<Integer, BuildVerificationResult>();
	
	JenkinsVerifier jenkinsVerifier;
	public JenkinsUpdater() {
	}

	public List<Observer> getObservers(){
		return this.observers;
	}
	
	public void addBuildVerificationResultToMap(BuildVerificationResult result)
	{
		resultsMap.put(result.getBuildNumber(), result);
	}
	
	public void getBuildVerificationResultForBuild(
			BuildVerificationResult ret,
			Build build,
			String branchName,
			String jobName,
			Job job2Verify)
	{
		try {
			jenkinsVerifier.waitForCompletion(job2Verify, branchName);
		
		BuildWithDetails buildDetails = jenkinsVerifier.getDetailsWithRetry(build);
        boolean isStillBuilding = buildDetails.isBuilding();
			ret =
			        new BuildVerificationResult(buildDetails.getNumber(), new URI(
			                buildDetails.getUrl()), jenkinsVerifier.getBuildResult(
			                buildDetails,
			                isStillBuilding));
		} catch (URISyntaxException | IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
      
        VerigreenLogger.get().log(
                getClass().getName(),
                RuntimeUtils.getCurrentMethodName(),
                String.format(
                        "Build (%d) for branch (%s) has finished with %s",
                        build.getNumber(),
                        branchName,
                        ret.getStatus()));
        addBuildVerificationResultToMap(ret);
        notifyObserver();
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
	public void notifyObserver() {
		for(Observer o : observers)
		{
			BuildVerificationResult result = resultsMap.get(((CommitItem)o).getBuildNumber());
			if(!result.getStatus().equals(((CommitItem)o).getStatus()))
			{
				o.update(result.getStatus());
				CollectorApi.getCommitItemContainer().save((CommitItem)o);
				unregister(o);
				VerigreenLogger.get().log(
			             getClass().getName(),
			             RuntimeUtils.getCurrentMethodName(),
			             String.format(
			                     "Successfully updated and saved observer: %s",
			                     o.toString()));
			}
	
		}
		
	}

	
	
}
	
