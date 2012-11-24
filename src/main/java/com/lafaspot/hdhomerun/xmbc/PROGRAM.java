package com.lafaspot.hdhomerun.xmbc;

import java.util.logging.Level;

import com.lafaspot.hdhomerun.xmbc.SCANNING.Filter;

class PROGRAM extends SCANOBJ {
	private SCANNING mParent;
	public PROGRAM(SCANNING parent, String line) {
		data = line;
		mParent = parent;
	}
	
	public boolean encrypted() {
		return data.contains("(encrypted)") || data.contains("internet");
	}

	public String description() {		
		int pos = data.indexOf(":");
		if (pos == -1) {
			HDHomeRunTunner.log.log(Level.WARNING, "PROGRAM.description missing from line: " + data);
			return "NOT FOUND";
		}
		String description = data.substring(pos+2);
		if (description.trim().equals("0") || description.trim().equals("")) {
			description = mParent.channel("us-cable") + "-" + number();
		}
		return description;
	}

	public String number() {
		int pos = data.indexOf(":");
		if (pos == -1) {
			HDHomeRunTunner.log.log(Level.WARNING, "PROGRAM.number missing from line: " + data);
			return "NOT FOUND";
		}
		return data.substring(8, pos);
	}

	public boolean isDescriptionAvailable() {
		int pos = data.indexOf(":");
		if (pos == -1) return false;
		String description = data.substring(pos+2);
		if (description.trim().equals("0") || description.trim().equals("")
		    || description.trim().equals("0 (control)")
		    || description.trim().equals("0 (no data)")
		    || description.trim().equals("internet")
		    || description.trim().equals("0 (encrypted)")
		    ) {
			return false;
		}
		return true;
	}

	public boolean isFilterTrue(Filter filter) {
		switch (filter) {
			default:
			case NONE : return false;
			case ENCRYPTED : return encrypted();
			case NODESCRIPTION : return encrypted() || !isDescriptionAvailable();			
		}
	}
}