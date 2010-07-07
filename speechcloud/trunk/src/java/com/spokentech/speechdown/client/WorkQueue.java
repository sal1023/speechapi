/*
 * Copyright (c) 2009-2010 Spokentech Inc.  All rights reserved.
 *  
 * This file is part of Spokentech speech server
 *  
 */
package com.spokentech.speechdown.client;

import java.util.LinkedList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class WorkQueue {
        
		private static Log _logger =  LogFactory.getLog(AsynchCommand.class.getName());
    
	    private final int nThreads;
	    private final PoolWorker[] threads;
	    private final LinkedList<AsynchCommand> queue;
	    
	    

	    public WorkQueue(int nThreads)  {
	        this.nThreads = nThreads;
	        queue = new LinkedList<AsynchCommand>();
	        threads = new PoolWorker[nThreads];

	        for (int i=0; i<nThreads; i++) {
	            threads[i] = new PoolWorker();
	            threads[i].start();
	        }
	    }

	    public void execute(AsynchCommand r) {
	        synchronized(queue) {
	            queue.addLast(r);
	            queue.notify();
	        }
	    }

	    public synchronized void  cancel(String id) {
	    	boolean match = false;
	       //search queue for id (waiting jobs)
	    	  for (AsynchCommand command : queue) {
	    		  _logger.debug(command.getId()+"   "+id);
	    	      if (command.getId().equals(id)) {
	    	    	  _logger.debug("Cancel match, removing...");
	    	    	  queue.remove(command);
	    	    	  match =true;
	    	    	  break;
	    	      }
	    	    	  
	    	  }
	       if (!match) {
	       //search the thread pool for active jobs
	    	  for (PoolWorker pw : threads)
	    	      pw.cancel(id);
	       }
	    }
	    
	    private class PoolWorker extends Thread {

	    	AsynchCommand r;
            
            public synchronized void cancel(String id) {
            	if (r!= null)
            	    r.cancel(id);
            }
	    	
	    	public void run() {


	            while (true) {
	                synchronized(queue) {
	                    while (queue.isEmpty()) {
	                        try {
	                            queue.wait();
	                        }
	                        catch (InterruptedException ignored) {
	                        }
	                    }

	                    r =  queue.removeFirst();
	                }

	                // If we don't catch RuntimeException, 
	                // the pool could leak threads
	                try {
	                    r.run();
	                }
	                catch (RuntimeException e) {
	                    // You might want to log something here
	                }
	                r=null;
	            }
	        }
	    }
}
