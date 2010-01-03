package com.spokentech.speechdown.client;

import java.util.LinkedList;
import java.util.logging.Logger;

public class WorkQueue {
        
	    private static Logger _logger = Logger.getLogger(AsynchCommand.class.getName());
    
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
	    		  System.out.println(command.getId()+"   "+id);
	    	      if (command.getId().equals(id)) {
	    	    	  _logger.info("Cancel match, removing...");
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
