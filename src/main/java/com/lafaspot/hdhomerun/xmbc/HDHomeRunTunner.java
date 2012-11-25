package com.lafaspot.hdhomerun.xmbc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.lafaspot.hdhomerun.xmbc.SCANNING.Filter;

public class HDHomeRunTunner {
	public transient static Logger log = Logger.getLogger("HDHomeRunTunner");
	private final String mId;
	private final int mTuner;
	private List<SCANNING> scanList = new ArrayList<SCANNING>();
	private String dir = System.getProperty("user.home") + "/.hdhomerun";
	private String scanFile = "scan_tuner%s.txt";
	private static final String CMD_PREREQ = "sudo apt-get install hdhomerun-config";
	private static final String CMD_DISCOVER = "hdhomerun_config discover";
        private static final String CMD_DISCOVER_EXT = "hdhomerun_config discover %s";
	private static final String CMD_MKFIFO = "mkfifo %s";
	private static final String CMD_SETCHANNEL = "hdhomerun_config %s set /tuner%s/channel auto:%s";
	private static final String CMD_SAVESTREAM = "hdhomerun_config %s save /tuner%s %s";
	private static final String CMD_SETPROGRAM = "hdhomerun_config %s set /tuner%s/program %s";
	private static final String CMD_PLAY = "mplayer %s";
	private static final String CMD_SCAN = "hdhomerun_config %s scan %s %s";

	public HDHomeRunTunner(int tuner) {
		File file = new File(dir);
		file.mkdirs();		
		mTuner = tuner;
		mId = readDeviceId();
		readScan();
	}

        public HDHomeRunTunner(int tuner, String ip) {
                System.out.println("In here");
                File file = new File(dir);
                file.mkdirs();
                mTuner = tuner;
                mId = readDeviceId(ip);
                readScan();
        }

	public void playStream(String channel, String program){
		File file = new File(dir, filterFileName("stream_tuner" + mTuner + ".ts"));
		if (file.exists()) {
                        System.out.println("File Exisists");
			file.delete();
			String output = exec(String.format(CMD_MKFIFO, file.toString()));
			log.info(output);
		}
		String output = exec(String.format(CMD_SETCHANNEL, mId, mTuner, channel));
		log.info(output);
		output = exec(String.format(CMD_SETPROGRAM, mId, mTuner, program));
		log.info(output);
		Process discard1 = exec(String.format(CMD_SAVESTREAM, mId, mTuner, file.toString()), false);
		Process discard2 = exec(String.format(CMD_PLAY, file.toString()), false);		
		try {
			discard2.waitFor();
		} catch (InterruptedException e) {
			// Ignore
		}
		try {
			discard1.waitFor();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			// Ignore
		}
	}
	
	private String readDeviceId() {
		String[] temp = null;
		temp = exec(CMD_DISCOVER).split(" ");
		return temp[2];
	}
       
        private String readDeviceId(String ip){
                return ip;        
        }

	public void createSTRMFile(String directory, Filter filter) {
		File dir = new File(directory);
		dir.mkdirs();
		
		for ( SCANNING scanning : channels(filter)) {	
			if(!scanning.filterPrograms(filter).isEmpty()){
				for ( PROGRAM program : scanning.filterPrograms(filter)) {
					String fileName = program.description().trim() + " tuner" + mTuner + ".strm";
					
					File file = new File(dir, filterFileName(fileName));
					int index = 0;
					while (file.exists()) {
						index++;
						fileName = program.description().trim() + " tuner" + mTuner + "_" + index + ".strm";
						file = new File(dir, filterFileName(fileName));
					}
					writeFile(file, scanning.toSTRM(mId, mTuner, program));
				}
			}
		}
	}

	private String filterFileName(String fileName) {
		return fileName.replaceAll("[/\\&;:]", "-");
	}
	
	public List<SCANNING> channels(Filter filter) {
		List<SCANNING> scanFiltered = new ArrayList<SCANNING>();
		for (SCANNING scanning: scanList) {
			if(!scanning.lock.none() && !scanning.filterPrograms(filter).isEmpty()) {
				scanFiltered.add(scanning);
			}
		}

		return scanFiltered;
	} 
	
	private void readScan() {
		try {
                        System.out.println("Filename is "+dir + File.separator
                                        + String.format(scanFile, mTuner));
			File file = new File(dir + File.separator
					+ String.format(scanFile, mTuner));
			String scan = null;
			if (file.exists()) {
                                System.out.println("File Exists");
				scan = readFile(file);
			} else {
                                System.out.println("Not created");
                                file.createNewFile();
				String output = exec(String.format(CMD_SCAN, mId, mTuner, file.toString()));
				log.info(output);
				scan = readFile(file);
			}
			BufferedReader scanReader = new BufferedReader(new StringReader(
					scan));
			String line = null;
			SCANNING scanning = null;

			while ((line = scanReader.readLine()) != null) {
				if (line.startsWith("SCANNING")) {
					scanning = new SCANNING(line);
					scanList.add(scanning);
				} else if (line.startsWith("TSID")) {
					scanning.tsid = new TSID(scanning, line);
				} else if (line.startsWith("PROGRAM: tsid=")) {
					// support for version 20080430
					scanning.tsid = new TSID(scanning, line);
				} else if (line.startsWith("LOCK")) {
					scanning.lock = new LOCK(scanning, line);
				} else if (line.startsWith("PROGRAM: ")) {
					// support for version 20080430
					String newline = line.replace("PROGRAM: ", "PROGRAM ");
					scanning.programList.add(new PROGRAM(scanning, newline));
				} else if (line.startsWith("PROGRAM")) {
					scanning.programList.add(new PROGRAM(scanning, line));
				}else {
					log.info("Scanning line ignored: " + line);
				}

			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void writeFile(File file, String data) {
		PrintWriter fos;

		try {
			fos = new PrintWriter(file);
			fos.write(data);
			fos.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private String readFile(File file) {
		FileReader fis = null;
		BufferedReader bis = null;
		StringBuffer buff = new StringBuffer();

		try {
                        
			fis = new FileReader(file);

			// Here BufferedInputStream is added for fast reading.
			bis = new BufferedReader(fis);

			// dis.available() returns 0 if the file does not have more lines.
			String line = null;
			while ((line = bis.readLine()) != null) {
				buff.append(line).append("\n");
			}

			// dispose all the resources after using them.
			fis.close();
			bis.close();

			return buff.toString();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private String exec(String cmd) {
		try {
			log.log(Level.INFO, cmd);
			Process proc = Runtime.getRuntime().exec(cmd);
			proc.waitFor();
			return convertStreamToString(proc.getInputStream());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private Process exec(String cmd, boolean wait) {
		try {
			log.log(Level.INFO, cmd);
			Process proc = Runtime.getRuntime().exec(cmd);
			if(wait) proc.waitFor();
			
			return proc;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static String convertStreamToString(InputStream is)
			throws Exception {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();
		String line = null;
		while ((line = reader.readLine()) != null) {
			sb.append(line + "\n");
		}
		is.close();
		return sb.toString();
	}

}
