package edu.ucsb.cs.roots.scheduling;

import org.quartz.DisallowConcurrentExecution;

@DisallowConcurrentExecution
public class SynchronizedRootsJob extends RootsJob {
}
