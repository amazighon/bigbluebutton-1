package org.bigbluebutton.voiceconf.sip;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.red5.logging.Red5LoggerFactory;

import java.io.IOException;

public class ProcessMonitor implements Runnable {
    private static Logger log = Red5LoggerFactory.getLogger(ProcessMonitor.class, "sip");

    private String[] command;
    private Process process;

    ProcessStream inputStreamMonitor;
    ProcessStream errorStreamMonitor;

    private Thread thread = null;

    public ProcessMonitor(String[] command) {
        this.command = command;
        this.process = null;
        this.inputStreamMonitor = null;
        this.errorStreamMonitor = null;
    }
    
    public String toString() {
        if (this.command == null || this.command.length == 0) { 
            return "";
        }
        
        StringBuffer result = new StringBuffer();
        String delim = "";
        for (String i : this.command) {
        	result.append(delim).append(i);
            delim = " ";
        }
        return result.toString();
    }

    public void run() {
        try {
            log.debug("Creating thread to execute FFmpeg");
            log.debug("Executing: " + this.toString());
            this.process = Runtime.getRuntime().exec(this.command);

            if(this.process == null) {
                log.debug("process is null");
                return;
            }

            InputStream is = this.process.getInputStream();
            InputStream es = this.process.getErrorStream();

            inputStreamMonitor = new ProcessStream(is);
            errorStreamMonitor = new ProcessStream(es);

            inputStreamMonitor.start();
            errorStreamMonitor.start();

            this.process.waitFor();

            int ret = this.process.exitValue();
            log.debug("Exit value: " + ret);

            destroy();
        }
        catch(SecurityException se) {
            log.debug("Security Exception");
        }
        catch(IOException ioe) {
            log.debug("IO Exception");
        }
        catch(NullPointerException npe) {
            log.debug("NullPointer Exception");
        }
        catch(IllegalArgumentException iae) {
            log.debug("IllegalArgument Exception");
        }
        catch(InterruptedException ie) {
            log.debug("Interrupted Excetion");
        }

        if (this.process == null)
            log.debug("Exiting thread that executes FFmpeg");
        else{
            log.debug("FFmpeg VideoTranscoder died unepectedly. Restarting it");
            //TODO
        }
    }

    public void start() {
        this.thread = new Thread(this);
        this.thread.start();
    }

    public void restart(){
        if (this.thread != null)
            this.thread.start();
    }

    public void destroy() {
        if(this.inputStreamMonitor != null 
            && this.errorStreamMonitor != null) {
            this.inputStreamMonitor.close();
            this.errorStreamMonitor.close();
        }

        if(this.process != null) {
            log.debug("Closing FFmpeg process");
            this.process.destroy();
            this.process = null;
        }
    }
}