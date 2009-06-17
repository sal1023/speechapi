package com.spokentech.speechdown.cli;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;

public class MyDataSource implements DataSource{
	
	InputStream stream;
	
	public MyDataSource(InputStream stream) {
	    super();
	    this.stream = stream;
    }

	public String getContentType() {
	    // TODO Auto-generated method stub
	    return null;
    }

	public InputStream getInputStream() throws IOException {
	    return stream;
    }

	public String getName() {
	    // TODO Auto-generated method stub
	    return null;
    }

	public OutputStream getOutputStream() throws IOException {
		throw new IOException("Outputstream not supported on my data source");
    }

}
