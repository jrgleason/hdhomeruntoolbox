package com.lafaspot.hdhomerun;

import java.util.logging.Logger;

import com.lafaspot.hdhomerun.xmbc.HDHomeRunTunner;
import com.lafaspot.hdhomerun.xmbc.SCANNING.Filter;

public class CreateSTRM {

	private static Logger log = Logger.getLogger("CreateSTRM");
	private static String dir = System.getProperty("user.home") + "/Videos/Live TV";
	
	static public void main(String[] args) {
          int tuner = 1;
          if(args.length > 2 && args[1] != null){
                Integer tunerInt = new Integer(args[1]);
                tuner = tunerInt.intValue();
          }
          HDHomeRunTunner config = new HDHomeRunTunner(tuner);
          if(args[0] != null){
            config = new HDHomeRunTunner(tuner,args[0]);
          }
	  config.createSTRMFile(dir, Filter.ENCRYPTED);
	  log.info("strm files available at '" + dir + "'");
  	  //config.playStream("80","1");
	}
}
