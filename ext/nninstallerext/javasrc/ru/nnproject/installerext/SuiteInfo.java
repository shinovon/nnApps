package ru.nnproject.installerext;

import java.util.Hashtable;

import com.nokia.mj.impl.utils.Attribute;
import com.nokia.mj.impl.utils.Uid;

class SuiteInfo {

	private Uid iUid;
	private String iName;
	private String iVendor;
	private String iVersion;
	private boolean iTrusted;
	private Hashtable iAttributes;

	private SuiteInfo() {
		this.iAttributes = new Hashtable();
	}

	public SuiteInfo(Uid aUid) {
		this();
		this.iUid = aUid;
	}

	public SuiteInfo(String aName, String aVendor) {
		this();
		this.iName = aName;
		this.iVendor = aVendor;
	}

	public void setUid(Uid aUid) {
		this.iUid = aUid;
	}

	public void setName(String aName) {
		this.iName = aName;
	}

	public void setVendor(String aVendor) {
		this.iVendor = aVendor;
	}

	public void setVersion(String aVersion) {
		this.iVersion = aVersion;
	}

	public Uid getUid() {
		return this.iUid;
	}

	public String getName() {
		return this.iName;
	}

	public String getVendor() {
		return this.iVendor;
	}

	public String getVersion() {
		return this.iVersion;
	}

	public void setAttributes(Hashtable aAttributes) {
		this.iAttributes = aAttributes;
	}

	public Attribute getAttribute(String aName) {
		return (Attribute) this.iAttributes.get(aName);
	}

	public boolean isTrusted() {
		return this.iTrusted;
	}

	public void setTrusted(boolean aTrusted) {
		this.iTrusted = aTrusted;
	}

}
