package midletintegration;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Hashtable;

import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import javax.microedition.io.DatagramConnection;
import javax.microedition.io.PushRegistry;
import javax.microedition.midlet.MIDlet;

import CatalogApp;

/**
 * MIDlet Integration library
 * 
 * @author Shinovon
 * @author curoviyxru (Mathew)
 * @version 1.2
 * 
 */
public class MIDletIntegration implements Runnable {
	
	private static final String JAVAAPP_PROTOCOL = "localapp://jam/launch?";
	private static final boolean s60 = checkSymbian();
	
	private static int instances;
	private static DatagramConnection dataConnection;
	private static boolean receiving;
	
	private int pushPort;
	private String cmd;
	private Object lock;
	
	private MIDletIntegration(int port, String cmd, Object lock) {
		this.pushPort = port;
		this.cmd = cmd;
		this.lock = lock;
	}
	
	/**
	 * Checks if a MIDlet has received a new start request from another MIDlet<br>
	 * Recommended to use in startApp() with "Nokia-MIDlet-Background-Event: pause" property in MANIFEST.MF<br>
	 * After receiving a request, you should receive arguments from getLaunchCommand()
	 * @see {@link #getLaunchCommand()}
	 * @return true if new arguments have been received since the last check
	 */
	public static boolean checkLaunch() {
		if(receiving) return false;
		try {
			if(PushRegistry.listConnections(true).length > 0) {
				return true;
			}
		} catch (Throwable e) {
		}
		if(System.getProperty("com.nokia.mid.cmdline.instance") == null) {
			return false;
		}
		try {
			int i = Integer.parseInt(System.getProperty("com.nokia.mid.cmdline.instance"));
			if(i > instances) {
				instances = i;
				String cmd = System.getProperty("com.nokia.mid.cmdline");
				return cmd != null && cmd.length() > 0;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	/**
	 * Gets received command
	 * 
	 * @see {@link #checkLaunch()}
	 * @see {@link java.lang.System#getProperty(String)}
	 * @return Received command
	 */
	public static String getLaunchCommand() {
		receiving = true;
		String args = null;
		String[] arr = null;
		try {
			arr = PushRegistry.listConnections(true);
		} catch (Throwable e) {
		}
		if(arr != null && arr.length > 0) {
			try {
				DatagramConnection conn = (DatagramConnection) Connector.open(arr[0]);
				Datagram data = conn.newDatagram(conn.getMaximumLength());
				conn.receive(data);
				args = data.readUTF();
				conn.close();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
			}
		} else {
			args = System.getProperty("com.nokia.mid.cmdline");
		}
		if("empty=1".equals(args)) {
			args = "";
		}
		receiving = false;
		return args;
	}
	
	/**
	 * Converts string to arguments table
	 * 
	 * @see {@link java.util.Hashtable}
	 * @see {@link #getLaunchCommand()}
	 * @see {@link midletintegration.Util#parseArgs(String)}
	 * @return Arguments table or null
	 */
	public static Hashtable getArguments(String s) {
		return parseArgs(s);
	}
	
	/**
	 * Runs a MIDlet by Name/Vendor
	 * @see {@link #startApp(MIDlet, String, String, String)}
	 * @see {@link javax.microedition.midlet.MIDlet#platformRequest(String)}
	 * @param midlet Current MIDlet instance
	 * @param name MIDlet-Name
	 * @param vendor MIDlet-Vendor
	 * @return true if the MIDlet suite MUST exit
	 * @throws MIDletNotFoundException if MIDlet was not found
	 * @throws ProtocolNotSupportedException if MIDlet launch protocol not supported
	 */
	public static boolean startApp(MIDlet midlet, String name, String vendor) throws MIDletNotFoundException, ProtocolNotSupportedException, ConnectionNotFoundException {
		return startApp(midlet, name, vendor, null);
	}
	
	/**
	 * Runs a MIDlet by Name/Vendor with arguments
	 * @see {@link javax.microedition.midlet.MIDlet#platformRequest(String)}
	 * @param midlet Current MIDlet instance
	 * @param name MIDlet-Name
	 * @param vendor MIDlet-Vendor
	 * @param cmd Command
	 * @return true if the MIDlet suite MUST exit
	 * @throws MIDletNotFoundException if MIDlet was not found
	 * @throws ProtocolNotSupportedException if MIDlet launch protocol not supported
	 */
	public static boolean startApp(MIDlet midlet, String name, String vendor, String cmd) throws MIDletNotFoundException, ProtocolNotSupportedException, ConnectionNotFoundException {
		return startApp(midlet, "midlet-name=" + CatalogApp.url(name) + ";midlet-vendor=" + CatalogApp.url(vendor) + ";" + (cmd != null && cmd.length() > 0 ? CatalogApp.url(cmd) : "empty=1"));
	}

	
	/**
	 * Starts a MIDlet by UID
	 * @see {@link #startAppWithAppUID(MIDlet, String, String)}
	 * @see {@link javax.microedition.midlet.MIDlet#platformRequest(String)}
	 * @param midlet Current MIDlet instance
	 * @param uid App's "Nokia-MIDlet-UID-1" value
	 * @return true if the MIDlet suite MUST exit
	 * @throws MIDletNotFoundException if MIDlet was not found
	 * @throws ProtocolNotSupportedException if MIDlet launch protocol not supported
	 */
	public static boolean startAppWithAppUID(MIDlet midlet, String uid) throws MIDletNotFoundException, ProtocolNotSupportedException, ConnectionNotFoundException {
		return startAppWithAppUID(midlet, uid, null);
	}
	
	/**
	 * Runs a MIDlet by UID with arguments
	 * @see {@link #startAppWithAppUID(MIDlet, String, String)}
	 * @see {@link javax.microedition.midlet.MIDlet#platformRequest(String)}
	 * @param midlet Current MIDlet instance
	 * @param uid App's "Nokia-MIDlet-UID-1" value
	 * @param cmd Command
	 * @return true if the MIDlet suite MUST exit
	 * @throws MIDletNotFoundException if MIDlet was not found
	 * @throws ProtocolNotSupportedException if MIDlet launch protocol not supported
	 */
	public static boolean startAppWithAppUID(MIDlet midlet, String uid, String cmd) throws MIDletNotFoundException, ProtocolNotSupportedException, ConnectionNotFoundException {
		return startApp(midlet, "midlet-uid=" + CatalogApp.url(uid) + ";" + (cmd != null && cmd.length() > 0 ? CatalogApp.url(cmd) : "empty=1"));
	}
	
	private static boolean startApp(MIDlet midlet, String cmd) throws MIDletNotFoundException, ProtocolNotSupportedException, ConnectionNotFoundException {
		try {
			return midlet.platformRequest(JAVAAPP_PROTOCOL + cmd);
		} catch (ConnectionNotFoundException e) {
			if(e.getMessage() != null) {
				if(e.getMessage().startsWith("Cannot start Java application") ||
						e.getMessage().indexOf("following error: -12") != -1) {
					throw new MIDletNotFoundException(e.getMessage());
				} else if(e.getMessage().indexOf("Invalid localapp URL") != -1 ||
						e.getMessage().indexOf("Invalid URL") != -1) {
					throw new ProtocolNotSupportedException(e.getMessage());
				}
			}
			throw e;
		}
	}
	
	/**
	 * Runs a MIDlet by Name/Vendor, UID or Push port with arguments
	 * @param midlet Current MIDlet instance
	 * @param name MIDlet-Name
	 * @param vendor MIDlet-Vendor
	 * @param pushPort Push port
	 * @param uid App's "Nokia-MIDlet-UID-1" value
	 * @param cmd Command
	 * @return true if the MIDlet suite MUST exit
	 * @throws MIDletNotFoundException if MIDlet was not found
	 * @throws ProtocolNotSupportedException if MIDlet launch protocol not supported
	 */
	public static boolean startApp(MIDlet midlet, String name, String vendor, String uid, int pushPort, String cmd) throws MIDletNotFoundException, ProtocolNotSupportedException, IOException {
		try {
			if(System.getProperty("com.nokia.mid.cmdline.instance") != null) {
				if(uid != null) {
					return MIDletIntegration.startAppWithAppUID(midlet, uid, cmd);
				}
				return startApp(midlet, name, vendor, cmd);
			}
		} catch (IOException e) {
		}
		return startApp(midlet, pushPort, cmd);
	}
	
	/**
	 * Runs a MIDlet by Name/Vendor or Push port with arguments
	 * @param midlet Current MIDlet instance
	 * @param name MIDlet-Name
	 * @param vendor MIDlet-Vendor
	 * @param pushPort Push port
	 * @param cmd Command
	 * @return true if the MIDlet suite MUST exit
	 * @throws MIDletNotFoundException if MIDlet was not found
	 * @throws ProtocolNotSupportedException if MIDlet launch protocol not supported
	 */
	public static boolean startApp(MIDlet midlet, String name, String vendor, int pushPort, String cmd) throws MIDletNotFoundException, ProtocolNotSupportedException, IOException {
		try {
			if(System.getProperty("com.nokia.mid.cmdline.instance") != null) {
				return startApp(midlet, name, vendor, cmd);
			}
		} catch (IOException e) {
		}
		return startApp(midlet, pushPort, cmd);
	}

	private static Exception exception;
	
	/**
	 * Runs a MIDlet by Push port
	 * @param midlet Current MIDlet instance
	 * @param pushPort Push port
	 * @param cmd Command
	 * @return true if the MIDlet suite MUST exit
	 * @throws MIDletNotFoundException if MIDlet was not found
	 * @throws ProtocolNotSupportedException if MIDlet launch protocol not supported
	 * @throws RuntimeException if connection thread is still running
	 */
	public static boolean startApp(MIDlet midlet, final int pushPort, String cmd) throws MIDletNotFoundException, ProtocolNotSupportedException, IOException {
		if(dataConnection != null) {
			throw new RuntimeException("busy");
		}
		exception = null;
		try {
			if(cmd == null) {
				cmd = "empty=1";
			}
			Object lock = new Object();
			Thread thread = new Thread(new MIDletIntegration(pushPort, cmd, lock));
			thread.start();
			try {
				synchronized(lock) {
					lock.wait(4000);
				}
			} catch (Exception e) {
			}
			if(dataConnection != null) {
				thread.interrupt();
				try {
					dataConnection.close();
				} catch (Exception e) {
				}
				throw new MIDletNotFoundException();
			}
			if(exception != null) {
				IOException e = (IOException) exception;
				exception = null;
				throw e;
			}
			return s60;
		} catch (Error e) {
			throw new ProtocolNotSupportedException(e.toString());
		}
	}
	
	public void run() {
		try {
			dataConnection = (DatagramConnection) Connector.open("datagram://127.0.0.1:" + pushPort);
			Datagram data = dataConnection.newDatagram(dataConnection.getMaximumLength());
			data.reset();
			data.writeUTF(cmd);
			dataConnection.send(data);
			if(s60) {
				try {
					dataConnection.send(data);
				} catch (Exception e) {
				}
			}
			dataConnection.close();
		} catch (IOException e) {
			exception = e;
		} catch (Exception e) {
		}
        dataConnection = null;
        synchronized(lock) {
        	lock.notify();
        }
	}
	
	public static void registerPush(MIDlet midlet, int port) throws ClassNotFoundException, IOException {
		if(PushRegistry.listConnections(false).length == 0) {
			PushRegistry.registerConnection("datagram://:" + port, midlet.getClass().getName(), "*");
		}
	}
	
	public static void unregisterPush(int port) {
		try {
			if(PushRegistry.listConnections(false).length > 0) {
				PushRegistry.unregisterConnection("datagram://:" + port);
			}
		} catch (Throwable e) {
		}
	}
	
	static Hashtable parseArgs(String str) {
		if(str == null) {
			return null;
		}
		Hashtable table = new Hashtable();
		int idx = str.indexOf(';');
		while (idx != -1) {
			String arg = str.substring(0, idx);
			int idx2 = arg.indexOf("=");
			if(idx2 != -1) {
				table.put(arg.substring(0, idx2).trim(), arg.substring(idx2 + 1));
			} else {
				table.put(arg.trim(), "");
			}
			str = str.substring(idx + 1);
			idx = str.indexOf(';');
		}
		if(str.length() > 0) {
			int idx2 = str.indexOf("=");
			if(idx2 != -1) {
				table.put(str.substring(0, idx2).trim(), str.substring(idx2 + 1));
			} else {
				table.put(str.trim(), "");
			}
		}
		return table;
	}
	
	static String decodeURL(String s) {
		if(s == null) {
			return null;
		}
		boolean needToChange = false;
		int numChars = s.length();
		StringBuffer sb = new StringBuffer(numChars > 500 ? numChars / 2 : numChars);
		int i = 0;
		char c;
		byte[] bytes = null;
		while (i < numChars) {
			c = s.charAt(i);
			switch (c) {
			case '%':
				try {
					if (bytes == null)
						bytes = new byte[(numChars - i) / 3];
					int pos = 0;
					while (((i + 2) < numChars) && (c == '%')) {
						int v = Integer.parseInt(s.substring(i + 1, i + 3), 16);
						if (v < 0)
							throw new IllegalArgumentException();
						bytes[pos++] = (byte) v;
						i += 3;
						if (i < numChars)
							c = s.charAt(i);
					}
					if ((i < numChars) && (c == '%'))
						throw new IllegalArgumentException();
					sb.append(new String(bytes, 0, pos, "UTF-8"));
				} catch (UnsupportedEncodingException e) {
					throw new IllegalArgumentException();
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException();
				}
				needToChange = true;
				break;
			default:
				sb.append(c);
				i++;
				break;
			}
		}
		return (needToChange ? sb.toString() : s);
	}

	static boolean checkSymbian() {
		String platform = System.getProperty("microedition.platform");
		if(platform == null) platform = "";
		return platform.indexOf("platform=S60") != -1 ||
				System.getProperty("com.symbian.midp.serversocket.support") != null ||
				System.getProperty("com.symbian.default.to.suite.icon") != null;
	}
	
}
