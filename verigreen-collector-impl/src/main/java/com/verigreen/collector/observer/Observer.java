package com.verigreen.collector.observer;

import java.net.URI;

import com.verigreen.collector.api.VerificationStatus;

public interface Observer
{
    public void updateBuildNumber(int build);
    public void updateBuildUrl(URI buildUrl);
	public void update(VerificationStatus status);
}
