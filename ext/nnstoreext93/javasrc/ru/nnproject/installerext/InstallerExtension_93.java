package ru.nnproject.installerext;

import com.nokia.mj.impl.vmport.VmPort;
import com.symbian.j2me.framework.service.app.Container;
import com.symbian.j2me.framework.service.midp.app.MIDPApplication;
import com.symbian.midp.runtime.MIDletSuiteAMS;

public class InstallerExtension_93 {
	
	private static final int VERSION = 1;
	
	private static boolean init;

	static {
		VmPort.getInstance().System_loadLibrary("nnstoreext93");
	}
	
	public static int getVersion() {
		return VERSION;
	}
	
	public static int getUid(String suite, String vendor, String uid) {
		if(uid != null) {
			return getIntUid(uid);
		}
		if(!init) throw new IllegalStateException();
		int[] r = new int[1];
		int err;
		if((err = _getUid(suite, vendor, r)) < 0) {
			if(err == -1) return 0;
			throw new RuntimeException("Native error: " + err);
		}
		return r[0];
	}
	
	public static String getMIDletVersion(String suite, String vendor, String uid) {
		if(!init) throw new IllegalStateException();
		int[] res = new int[3];
		int i = getUid(suite, vendor, null);
		if(i == 0) return null;
		i = _getInstalledVersion(i, res);
		if(i < 0) {
			throw new RuntimeException("Native error: " + i);
		}
		return res[0] + "." + res[1] + "." + res[2];
	}
	
	public static String getSisVersion(String uid) {
		if(!init) throw new IllegalStateException();
		int[] res = new int[3];
		if(_getInstalledSisVersion(getIntUid(uid), res) < 0) {
			return null;
		}
		return res[0] + "." + res[1] + "." + res[2];
	}
	
	public static boolean isInstalled(String suite, String vendor, String uid) {
		if(!init) throw new IllegalStateException();
		if(uid == null) {
			return getUid(suite, vendor, null) != 0;
		}
		return _isInstalled(getIntUid(uid));
	}
	
	public static void removeApp(String suite, String vendor, String uid) {
		throw new IllegalStateException("Not supported");
	}
	
	public static int launchApp(String suite, String vendor, String uid) {
		if(!init) throw new IllegalStateException();
		if(uid == null) {
			return _launchApp(getUid(suite, vendor, null));
		}
		return _launchApp(getIntUid(uid));
	}
	
	public static void init() {
		Container app = MIDletSuiteAMS.getCurrentApplication().getContainer();
		if(!"nnhub".equals(app.getName()) || !"nnproject".equals(app.getVendor())) {
			throw new RuntimeException("asd");
		}
		init = true;
	}
	
	private static int getIntUid(String uid) {
		return (int) Long.parseLong(uid.substring(2), 16);
	}

	private static native int _getUid(String name, String vendor, int[] result);
	private static native boolean _isInstalled(int uid);
	private static native int _getInstalledVersion(int uid, int[] result);
	private static native int _getInstalledSisVersion(int uid, int[] result);
	private static native int _launchApp(int uid);
	
}
