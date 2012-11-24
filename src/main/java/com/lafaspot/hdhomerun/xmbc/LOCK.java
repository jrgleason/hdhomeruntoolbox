package com.lafaspot.hdhomerun.xmbc;

class LOCK extends SCANOBJ {
	public LOCK(SCANNING scanning, String line) {
		data = line;
	}
	
	public boolean none() {
		return data.startsWith("LOCK: none");
	}
}