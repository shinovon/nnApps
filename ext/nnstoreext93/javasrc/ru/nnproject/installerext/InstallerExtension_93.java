package ru.nnproject.installerext;

import com.nokia.mj.impl.vmport.VmPort;

public class InstallerExtension_93 {
	
	private static final int VERSION = 1;

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
		int[] r = new int[1];
		int err;
		if((err = _getUid(suite, vendor, r)) < 0) {
			if(err == -1) return 0;
			throw new RuntimeException("Native error: " + err);
		}
		return r[0];
	}
	
	public static String getMIDletVersion(String suite, String vendor, String uid) {
		int[] res = new int[3];
		if(_getInstalledVersion(getUid(suite, vendor, null), res) < 0) {
			return null;
		}
		return res[0] + "." + res[1] + "." + res[2];
	}
	
	public static String getSisVersion(String uid) {
		int[] res = new int[3];
		if(_getInstalledSisVersion(getIntUid(uid), res) < 0) {
			return null;
		}
		return res[0] + "." + res[1] + "." + res[2];
	}
	
	public static boolean isInstalled(String suite, String vendor, String uid) {
		if(uid == null) {
			return getUid(suite, vendor, null) != 0;
		}
		return _isInstalled(getIntUid(uid));
	}
	
	public static void removeApp(String suite, String vendor, String uid) {
		if(uid == null) {
			throw new IllegalArgumentException("UID has to be set");
		}
		throw new IllegalStateException("Not supported");
	}
	
	public static int launchApp(String suite, String vendor, String uid) {
		if(uid == null) {
			return _launchApp(getUid(suite, vendor, null));
		}
		return _launchApp(getIntUid(uid));
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
