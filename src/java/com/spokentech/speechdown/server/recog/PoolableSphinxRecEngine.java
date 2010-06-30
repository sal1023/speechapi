package com.spokentech.speechdown.server.recog;

import java.io.IOException;

import org.apache.log4j.Logger;

import com.spokentech.speechdown.server.util.pool.PoolableObject;

import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;




public class PoolableSphinxRecEngine extends SphinxRecEngine implements PoolableObject {

    public PoolableSphinxRecEngine(ConfigurationManager cm, GrammarManager grammarManager, String prefixId,
            int id, String recordingFilePath, boolean recordingEnabled) throws IOException,
            PropertyException, InstantiationException {
	    super(cm, grammarManager, prefixId, id, recordingFilePath, recordingEnabled);
	    // TODO Auto-generated constructor stub
    }

	private static Logger _logger = Logger.getLogger(PoolableSphinxRecEngine.class);


    /* (non-Javadoc)
     * @see org.speechforge.cairo.util.pool.PoolableObject#validate()
     */
    public boolean validate() {
        _logger.debug("validate(): returning true");
        return true;
    }

    /* (non-Javadoc)
     * @see org.speechforge.cairo.util.pool.PoolableObject#destroy()
     */
    public void destroy() throws Exception {
        _logger.debug("destroy()");
        return;
    }
    
    /* (non-Javadoc)
     * @see org.speechforge.cairo.util.pool.PoolableObject#activate()
     */
    @Override
    public synchronized void activate() {
        _logger.debug("SphinxRecEngine activating...");
    }

    /* (non-Javadoc)
     * @see org.speechforge.cairo.util.pool.PoolableObject#passivate()
     */
    @Override
    public synchronized void passivate() {
        _logger.debug("SphinxRecEngine passivating...");
        //stopProcessing();
        //_recogListener = null;
    }

}
