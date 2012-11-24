package com.lafaspot.hdhomerun.xmbc;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;


public class SCANNING extends SCANOBJ {
	public enum Filter {NONE, ENCRYPTED, NODESCRIPTION};
	
	public SCANNING(String line) {
		data = line;
	}
	LOCK lock;
	TSID tsid;
	List<PROGRAM> programList = new ArrayList<PROGRAM>();
	
	public String toString() {		
		return String.format("{\n\"data\":\"%s\",\n\"lock\":\"%s\",\n\"tsid\":\"%s\",\n" +
				"\"program.list\":\"%s\"\n}\n", data, lock, tsid, programList);
		
	}
	
	public List<PROGRAM> filterPrograms(Filter filter) {		
		List<PROGRAM> filteredList = new ArrayList<PROGRAM>();
		
		for (PROGRAM program : programList) {
			if(!program.isFilterTrue(filter)) {
				filteredList.add(program);
			}
		}
		return filteredList;
	}

	public String toSTRM(String device, int tuner, PROGRAM program) {
		// hdhomerun://101425DD-1/LIF-ENC 9.6?channel=auto:80&program=3
		return String.format("hdhomerun://%s-%s/tuner%s %s?channel=auto:%s&program=%s",
				device, tuner, tuner, program.description(), channel("us-cable") , program.number());
	}

	public String channel(String type) {
		StringTokenizer tk =  new StringTokenizer( data," (,)");
		while(tk.hasMoreElements()) {
			String elem = tk.nextToken();
			if (elem.startsWith(type)) {
				return elem.substring(type.length() + 1);
			}
		}
		return "CHNOTFOUND";
	} 
}