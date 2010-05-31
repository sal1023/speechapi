package com.spokentech.speechdown.client.endpoint;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.spokentech.speechdown.client.sphinx.SpeechDataStreamer;
import com.spokentech.speechdown.common.AFormat;
import com.spokentech.speechdown.common.SpeechEventListener;
import com.spokentech.speechdown.common.sphinx.AudioStreamDataSource;
import com.spokentech.speechdown.common.sphinx.SpeechClassifier;
import com.spokentech.speechdown.common.sphinx.SpeechDataMonitor;
import com.spokentech.speechdown.server.recog.StreamDataSource;

import edu.cmu.sphinx.frontend.DataBlocker;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.endpoint.NonSpeechDataFilter;
import edu.cmu.sphinx.frontend.endpoint.SpeechMarker;
import edu.cmu.sphinx.frontend.feature.BatchCMN;
import edu.cmu.sphinx.frontend.feature.DeltasFeatureExtractor;
import edu.cmu.sphinx.frontend.feature.LiveCMN;
import edu.cmu.sphinx.frontend.filter.Dither;
import edu.cmu.sphinx.frontend.filter.Preemphasizer;
import edu.cmu.sphinx.frontend.frequencywarp.MelFrequencyFilterBank;
import edu.cmu.sphinx.frontend.transform.DiscreteCosineTransform;
import edu.cmu.sphinx.frontend.transform.DiscreteFourierTransform;
import edu.cmu.sphinx.frontend.window.RaisedCosineWindower;

public  class S4EndPointer implements EndPointer {
	private static Logger _logger = Logger.getLogger(S4EndPointer.class);

	protected InputStream astream;
	protected OutputStream ostream;
	protected SpeechEventListener listener;
	protected boolean streamEndReached = false;
	protected boolean speechStarted = false;
	protected boolean speechEnded = false;
	protected DataProcessor processor;

	StreamDataSource dataSource = null;
	
	public S4EndPointer() {
		super();
	}
	
	public  void doEndpointing() {
		
	}

	
	public void start(DataProcessor processor, AFormat format, OutputStream outputStream, SpeechEventListener listener) throws IOException {
        streamEndReached = false;
    	speechStarted = false;
    	speechEnded = false;
    	this.astream = null;
    	this.processor =processor;
    	
        ostream = outputStream;
        this.listener = listener;   
        	
		FrontEnd frontEnd = createFrontend(false, false, processor, listener);
 
		_logger.debug("Starting audio transfer");
		SpeechDataStreamer sds = new SpeechDataStreamer();
		sds.startStreaming(frontEnd, outputStream);	
        
    }
	
	
	public void start(InputStream audioStream, AFormat format, OutputStream outputStream, SpeechEventListener listener) throws IOException {
		
		processor=null;
        astream = audioStream;
        streamEndReached = false;
    	speechStarted = false;
    	speechEnded = false;
    	
        ostream = outputStream;
        this.listener = listener;   
        	
		dataSource = new AudioStreamDataSource();
		 	
		FrontEnd frontEnd = createFrontend(false, false, (DataProcessor) dataSource, listener);
 	
		dataSource.setInputStream((InputStream)audioStream, "ws-audiostream", format);	
	
		_logger.debug("Starting audio transfer");
		SpeechDataStreamer sds = new SpeechDataStreamer();
		sds.startStreaming(frontEnd, outputStream);	
        
    }

	public long triggerStart() {
    	speechStarted=true;
    	if( listener!= null) {
    	    listener.speechStarted();
    	    return -1;
    	} else  {
    	    return 1;
    	}
    }

	public void triggerEnd() {
    	speechEnded=true;
    	if( listener!= null)
    	    listener.speechEnded();
    }



    

    protected  void closeInputDataStream() {
    	_logger.debug("Stopping stream");
    	if (dataSource != null) {
	    	try {
		        dataSource.closeDataStream();
	        } catch (IOException e) {
		        // TODO Auto-generated catch block
		        e.printStackTrace();
	        }
    	}

    
    }

	public void stopRecording() {
        streamEndReached = true;
        //closeInputDataStream();
    }

    
    private FrontEnd createFrontend(boolean featureMode, boolean batchCMN, DataProcessor dataSource, SpeechEventListener listener) {
    	
 	   ArrayList<DataProcessor> components = new ArrayList <DataProcessor>();
 	   components.add(dataSource);
	   components.add (new DataBlocker(10));
	   components.add (new SpeechClassifier(10,0.002,10.0,10.0));
	   //components.add (new SpeechMarker(200,500,100,50,100));
	   components.add (new SpeechMarker(200,250,200,50,200, 15));
	   components.add (new NonSpeechDataFilter());
	   SpeechDataMonitor mon = new SpeechDataMonitor();
	   components.add (mon);
	   mon.setSpeechEventListener(listener);
	   //boolean recordingEnabled = false;
	   //String recordingFilePath = "c:/tmp/";
	   //if (recordingEnabled ) {
		//	boolean isCompletePath = false;
		//	int bitsPerSample = 16;
		//	boolean isSigned = true;
		//	boolean captureUtts = true;
		//	boolean bigEndian = false;
		//	WavWriter recorder = new WavWriter(recordingFilePath,isCompletePath,bitsPerSample,bigEndian,isSigned,captureUtts);
		 //     components.add(recorder);
	   //}
	   if (featureMode) {
		   components.add (new Preemphasizer(0.97));
		   components.add (new Dither());
		   components.add (new RaisedCosineWindower(0.46,(float)25.625,(float)10.0));
		   components.add (new DiscreteFourierTransform(-1,false));
		   components.add (new MelFrequencyFilterBank((double)133.0,(double)3500.0,31));

		   components.add (new DiscreteCosineTransform(40,13));
		   if (batchCMN) {
		      components.add (new BatchCMN());
		   } else {
		      components.add (new LiveCMN(12,500,800));
		   }
		 
		   components.add (new DeltasFeatureExtractor(3));
		   //TODO: Can this be done on client (do we need the acoustic model? or a subset of it?)
		   //components.add (new LDA(_loader));
	   }
	   	   
	   FrontEnd fe = new FrontEnd (components);
	   return fe;   
    }

	@Override
    public boolean requiresServerSideEndPointing() {
	    return false;
    }

	@Override
    public boolean inUse() {
	    // TODO Auto-generated method stub
	    return false;
    }



}