package com.spokentech.speechdown.cli;

import java.nio.BufferUnderflowException;

/**
 * Circular buffer of doubles (implemented with an array)
 * 
 * @author Spencer Lord {@literal <}<a href="mailto:spencer@users.spokentech.com">spencer@spokentech.com</a>{@literal >}
 */
public class CircularDArrayBuffer {

  private double[] buffer;
  private int tail;
  private int head;
  private int length = 0;

  /**
 * @return the length
 */
public int getLength() {
	return length;
}

public CircularDArrayBuffer(int n) {
    buffer = new double[n];
    tail = 0;
    head = 0;
    length = 0;
  }

  public void add(double toAdd) {
	
	buffer[head] = toAdd;
    head = head + 1;
	if (head >= buffer.length) 
		head= 0;

	length = length+1;	
	if (length > buffer.length) {
		length = buffer.length;
		tail = head;
	}

    //System.out.println("htl "+head+" "+tail+" "+length);
	//for (double x: buffer) {
	//	System.out.print(x);
	//}
	//System.out.println();

  }

  public double get() {
    double t = 0.0;
    if (length <= 0) {
        throw new BufferUnderflowException();
    } else {
        t =  buffer[tail];
    	length = length-1;
        tail = tail+1;
    	if (tail >= buffer.length) 
    		tail= 0;

    }
    return t;
  }
  
  public double[] getAll() {
	  //System.out.println("Len"+length+"  "+tail+" "+head);

	  if (length == buffer.length ) {
			//for (double x: buffer) {
			//	System.out.print(x);
			//}
			//System.out.println();
		  return buffer;
	  } else if (length == 0) {
		  return null;
	  } else {
		  double[] dd = new double[length];
		  for (int i = 0; i<length; i++) {
			  dd[i] = buffer[(tail+i)%buffer.length];
			  
		  }
			//for (double x: dd) {
			//	System.out.print(x);
			//}
			//System.out.println();
		  return dd;
	  }
  }

  public String toString() {
    return "CircularBuffer(size=" + buffer.length + ", head=" + head + ", tail=" + tail + ")";
  }

 
}

