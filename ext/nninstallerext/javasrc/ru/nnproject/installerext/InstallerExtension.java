package ru.nnproject.installerext;

import java.util.Hashtable;

import com.nokia.mj.impl.installer.utils.PlatformUid;
import com.nokia.mj.impl.rt.midp.InfoInvoker1;
import com.nokia.mj.impl.rt.support.ApplicationInfo;
import com.nokia.mj.impl.rt.support.Jvm;
import com.nokia.mj.impl.storage.StorageAttribute;
import com.nokia.mj.impl.storage.StorageEntry;
import com.nokia.mj.impl.storage.StorageFactory;
import com.nokia.mj.impl.storage.StorageSession;
import com.nokia.mj.impl.utils.Attribute;
import com.nokia.mj.impl.utils.Uid;

public class InstallerExtension {
	
	private static final int VERSION = 1;
	
	private static InstallerExtension instance;

	private static boolean init;
	
	static {
		Jvm.loadSystemLibrary("nninstallerext");
	}
	
	// Public methods
	
	public static int getVersion() {
		return VERSION;
	}

	public static boolean isInstalled(String suiteName, String vendorName, String uid) {
		if(!init) throw new IllegalStateException();
		InstallerExtension inst = getInstance();
		inst.open();
		try {
			if(uid != null) {
				return inst._isNativeAppInstalled(PlatformUid.getIntValue(PlatformUid.createUid(uid)));
			}
			SuiteInfo suiteInfo = new SuiteInfo(suiteName, vendorName);
			return inst.readSuiteInfo(suiteInfo);
		} finally {
			inst.close();
		}
	}

	public static String getVersion(String suiteName, String vendorName, String uid) {
		if(!init) throw new IllegalStateException();
		if (suiteName == null && uid != null) {
			return getSisVersion(uid);
		}
		InstallerExtension inst = getInstance();
		inst.open();
		try {
			SuiteInfo suiteInfo = new SuiteInfo(suiteName, vendorName);
			inst.readSuiteInfo(suiteInfo);
			return suiteInfo.getVersion();
		} finally {
			inst.close();
		}
	}
	
	public static String getSisVersion(String uid) {
		int[] res = new int[3];
		if(getInstance()._getInstalledSisVersion(PlatformUid.getIntValue(PlatformUid.createUid(uid)), res) < 0) {
			return null;
		}
		return res[0] + "." + res[1] + "." + res[2];
	}

	public static String getUid(String suiteName, String vendorName, String uid) {
		if(!init) throw new IllegalStateException();
		SuiteInfo suiteInfo = new SuiteInfo(suiteName, vendorName);
		if (uid != null) {
			suiteInfo.setUid(PlatformUid.createUid(uid));
		}
		InstallerExtension inst = getInstance();
		inst.open();
		try {
			inst.readSuiteInfo(suiteInfo);
			return suiteInfo.getUid().getStringValue();
		} finally {
			inst.close();
		}
	}

	public static int removeApp(String suiteName, String vendorName, String uid) {
		if(!init) throw new IllegalStateException();
		SuiteInfo suiteInfo = new SuiteInfo(suiteName, vendorName);
		if (uid != null) {
			suiteInfo.setUid(PlatformUid.createUid(uid));
		}
		InstallerExtension inst = getInstance();
		inst.open();
		try {
			if (inst.readSuiteInfo(suiteInfo)) {
				return inst._launchJavaInstaller(new String[] { "uninstall", "-uid=" + uid });
			}
		} finally {
			inst.close();
		};
		throw new RuntimeException("app not found");
	}

	public static int installApp(String url) {
		if(!init) throw new IllegalStateException();
		InstallerExtension inst = getInstance();
		return inst._launchJavaInstaller(new String[] { "install", "-ja" + (url.endsWith("jad") ? "d" : "r") + "=" + url });
	}
	
	public static void init() {
		ApplicationInfo app = ApplicationInfo.getInstance();
		if (!"nnhub".equals(app.getSuiteName()) || !"nnproject".equals(app.getVendor()))
			throw new RuntimeException("asd");
		init = true;
		InfoInvoker1.setProtectionDomain("Manufacturer", "MFD");
	}
	
	// Private methods
	
	private static InstallerExtension getInstance() {
		InstallerExtension inst = instance;
		if(inst == null) inst = instance = new InstallerExtension();
		return inst;
	}

	private native boolean _isNativeAppInstalled(int uid);
	private native int _getInstalledSisVersion(int uid, int[] result);
//	private native int _uninstallNativeApp(int uid);
	private native int _launchJavaInstaller(String[] args);
	private native int _launchNativeApp(int uid);

	private InstallerExtension() {
	}
	
	private void open() {
		if(iSession != null) return;
		iSession = StorageFactory.createSession();
		iSession.open();
	}
	
	// StorageHandler

	private StorageSession iSession;

	private void close() {
		if (iSession == null) return;
		iSession.close();
		iSession.destroySession();
		iSession = null;
	}

	private void checkSession() {
		if (iSession == null) {
			throw new RuntimeException("Session does not exist.");
		}
	}

	private boolean readSuiteInfo(SuiteInfo aSuiteInfo) {
		checkSession();
		if (aSuiteInfo == null) {
			return false;
		}
		boolean result = false;
		if (aSuiteInfo.getUid() == null) {
			if ((aSuiteInfo.getName() == null) || (aSuiteInfo.getVendor() == null)) {
				return result;
			}
			StorageEntry query = new StorageEntry();
			query.addAttribute(new StorageAttribute("PACKAGE_NAME", aSuiteInfo.getName()));

			query.addAttribute(new StorageAttribute("VENDOR", aSuiteInfo.getVendor()));

			query.addAttribute(new StorageAttribute("ID", ""));
			StorageEntry[] entries = iSession.search("APPLICATION_PACKAGE", query);
			if ((entries != null) && (entries.length > 0)) {
				aSuiteInfo.setUid(PlatformUid.createUid(entries[0].getAttribute("ID").getValue()));
			}
		}
		if (aSuiteInfo.getUid() != null) {
		}
		return readSuiteInfoByUid(aSuiteInfo);
	}

	private boolean readSuiteInfoByUid(SuiteInfo aSuiteInfo) {
		if (aSuiteInfo == null) {
			return false;
		}
		boolean result = false;

		StorageEntry query = new StorageEntry();
		query.addAttribute(new StorageAttribute("ID", aSuiteInfo.getUid().getStringValue()));

		StorageEntry[] entries = iSession.search("APPLICATION_PACKAGE", query);
		if ((entries != null) && (entries.length > 0)) {
			result = true;

			StorageEntry entry = entries[0];
			aSuiteInfo.setName(getAttributeValue(entry, "PACKAGE_NAME"));
			aSuiteInfo.setVendor(getAttributeValue(entry, "VENDOR"));
			aSuiteInfo.setVersion(getAttributeValue(entry, "VERSION"));

//			aSuiteInfo.setRootDir(getAttributeValue(entry, "ROOT_PATH"));
//			aSuiteInfo.setMediaId(Integer.parseInt(getAttributeValue(entry, "MEDIA_ID")));

//			aSuiteInfo.setInitialSize(Integer.parseInt(getAttributeValue(entry, "INITIAL_SIZE")));

//			aSuiteInfo.setJadPath(getAttributeValue(entry, "JAD_PATH"));
//			aSuiteInfo.setJarPath(getAttributeValue(entry, "JAR_PATH"));
//			aSuiteInfo.setJadUrl(getAttributeValue(entry, "JAD_URL"));
//			aSuiteInfo.setJarUrl(getAttributeValue(entry, "JAR_URL"));
//			aSuiteInfo.setAccessPoint(getAttributeValue(entry, "ACCESS_POINT"));
//			aSuiteInfo.setContentInfo(Integer.parseInt(getAttributeValue(entry, "CONTENT_INFO")));

//			aSuiteInfo.setContentId(getAttributeValue(entry, "CONTENT_ID"));

			aSuiteInfo.setAttributes(readAttributes(aSuiteInfo.getUid()));
			if (aSuiteInfo.getAttribute("MIDlet-Jar-RSA-SHA1") != null) {
				aSuiteInfo.setTrusted(true);
			}
		}
		if (result) {
			query = new StorageEntry();
			query.addAttribute(new StorageAttribute("ID", aSuiteInfo.getUid().getStringValue()));

			entries = iSession.search("MIDP_PACKAGE", query);
			result = (entries != null) && (entries.length > 0);
		}
		return result;
	}
	
	private boolean uidInUse(Uid aUid) {
		checkSession();
		StorageEntry query = new StorageEntry();
		query.addAttribute(new StorageAttribute("ID", aUid.getStringValue()));

		StorageEntry[] entries = iSession.search("APPLICATION_PACKAGE", query);
		if ((entries != null) && (entries.length > 0)) {
			return true;
		}
		entries = iSession.search("APPLICATION", query);
		if ((entries != null) && (entries.length > 0)) {
			return true;
		}
		return false;
	}

	private Hashtable readAttributes(Uid aUid) {
		if (aUid == null) {
			return null;
		}
		Hashtable attrs = null;
		StorageEntry query = new StorageEntry();
		query.addAttribute(new StorageAttribute("ID", aUid.getStringValue()));

		StorageEntry[] entries = iSession.search("APPLICATION_PACKAGE_ATTRIBUTES", query);
		if (entries != null) {
			attrs = new Hashtable();
			for (int i = 0; i < entries.length; i++) {
				StorageEntry entry = entries[i];
				String name = getAttributeValue(entry, "NAME");
				String value = getAttributeValue(entry, "VALUE");
				String trustedStr = getAttributeValue(entry, "TRUSTED");
				boolean trusted = Integer.parseInt(trustedStr) > 0;
				if (value == null) {
					value = "";
				}
				attrs.put(name, new Attribute(name, value, trusted));
			}
		}
		return attrs;
	}

	private static String getAttributeValue(StorageEntry aEntry, String aName) {
		if (aEntry == null) {
			return null;
		}
		StorageAttribute attr = aEntry.getAttribute(aName);
		if (attr != null) {
			return attr.getValue();
		}
		return null;
	}

}
