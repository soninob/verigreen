package com.verigreen.collector.observer;

import java.util.List;
import java.util.Map;

public interface Subject
{
  public void register(Observer o);
  public void unregister(Observer o);
  public void notifyObserver(List<Observer> relevantObservers, Map <String, List<String>> results);
}
