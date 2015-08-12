package com.verigreen.collector.observer;

import com.verigreen.collector.buildverification.BuildVerificationResult;

public interface Subject
{
  public void register(Observer o);
  public void unregister(Observer o);
  void notifyObserver();
}
