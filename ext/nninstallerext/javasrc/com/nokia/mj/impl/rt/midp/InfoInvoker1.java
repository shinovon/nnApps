package com.nokia.mj.impl.rt.midp;

public class InfoInvoker1 {
	
	public static void setProtectionDomain(String name, String category) {
		MidletInfo info = ApplicationInfoImpl.getMidletInfo();
		info.setProtectionDomain(category);
		info.setProtectionDomainName(name);
	}

}
