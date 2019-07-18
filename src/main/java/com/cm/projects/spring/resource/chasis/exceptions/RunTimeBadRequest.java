package com.cm.projects.spring.resource.chasis.exceptions;

/**
 * Used to throw bad requests that occur during runtime
 * @author Cornelius M
 */
public class RunTimeBadRequest extends RuntimeException {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public RunTimeBadRequest(){
        super();
    }
    
    public RunTimeBadRequest(String msg){
        super(msg);
    }
}
