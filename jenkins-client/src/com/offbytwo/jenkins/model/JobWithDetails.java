/*
 * Copyright 2013, 2014013 Rising Oak LLC.
 *
 * Distributed under the MIT license: http://opensource.org/licenses/MIT
 */

package com.offbytwo.jenkins.model;

import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.List;

public class JobWithDetails extends Job {
    String displayName;
    boolean buildable;
    List<Build> builds;
    Build lastBuild;
    Build lastCompletedBuild;
    Build lastFailedBuild;
    Build lastStableBuild;
    Build lastSuccessfulBuild;
    Build lastUnstableBuild;
    Build lastUnsuccessfulBuild;
    int nextBuildNumber;
    List<Job> downstreamProjects;
    List<Job> upstreamProjects;

    public String getDisplayName() {
        return displayName;
    }
    
    public boolean isBuildable() {
    	return buildable;
    }

    public List<Build> getBuilds() {
        return Lists.transform(builds, new Function<Build, Build>() {
            @Override
            public Build apply(Build from) {
                return buildWithClient(from);
            }
        });
    }

    private Build buildWithClient(Build from) {
        Build ret = new Build(from);
        ret.setClient(client);
        return ret;
    }

    public Build getLastBuild() {
        return buildWithClient(lastBuild);
    }

    public Build getLastCompletedBuild() {
        return buildWithClient(lastCompletedBuild);
    }

    public Build getLastFailedBuild() {
        return buildWithClient(lastFailedBuild);
    }

    public Build getLastStableBuild() {
        return buildWithClient(lastStableBuild);
    }

    public Build getLastSuccessfulBuild() {
        return buildWithClient(lastSuccessfulBuild);
    }

    public Build getLastUnstableBuild() {
        return buildWithClient(lastUnstableBuild);
    }

    public Build getLastUnsuccessfulBuild() {
        return buildWithClient(lastUnsuccessfulBuild);
    }

    public int getNextBuildNumber() {
        return nextBuildNumber;
    }

    public List<Job> getDownstreamProjects() {
        return Lists.transform(downstreamProjects, new JobWithClient());
    }

    public List<Job> getUpstreamProjects() {
        return Lists.transform(upstreamProjects, new JobWithClient());
    }

    private class JobWithClient implements Function<Job, Job> {
        @Override
        public Job apply(Job job) {
            job.setClient(client);
            return job;
        }
    }
    
    public Build getBuildByNumber(final int buildNumber) {
        
        Predicate<Build> isMatchingBuildNumber = new Predicate<Build>() {
            
            @Override
            public boolean apply(Build input) {
                return input.getNumber() == buildNumber;
            }
        };
        
        Optional<Build> optionalBuild = Iterables.tryFind(builds, isMatchingBuildNumber);
        return optionalBuild.orNull() == null ? null : buildWithClient(optionalBuild.orNull());
    }
}
