import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import javax.microedition.io.DatagramConnection;
import javax.microedition.io.HttpConnection;
import javax.microedition.io.PushRegistry;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Gauge;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.ImageItem;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemCommandListener;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.StringItem;
import javax.microedition.midlet.MIDlet;
import javax.microedition.rms.RecordStore;

import ru.nnproject.installerext.InstallerExtension;

public class CatalogApp extends MIDlet implements CommandListener, ItemCommandListener, Runnable, LangConstants {
	
	private static final String URL = "http://nnp.nnchan.ru/nns/";
	private static final String EXTSIS_URL = "http://nnp.nnchan.ru/nns/nninstallerext.sis";
	
	private static final int RUN_CATALOG = 1;
	private static final int RUN_CATALOG_ICONS = 2;
	private static final int RUN_APP = 3;
	private static final int RUN_INSTALL = 4;
	private static final int RUN_UNINSTALL = 5;
	private static final int RUN_SCREENSHOTS = 6;
	private static final int RUN_REFRESH = 7;
	private static final int RUN_CATEGORIES = 8;
	private static final int RUN_EXIT_TIMEOUT = 9;
	private static final int RUN_CHECK = 10;
	private static final int RUN_CATALOG_APP = 11;
	
	private static final String STAT_INSTALL = "1";
	private static final String STAT_UNINSTALL = "2";
	private static final String STAT_LAUNCH = "3";
	
	private static final String APIV = "&v=1";
	private static final int SETTINGSV = 1;

	private static final String SETTINGS_RECORDNAME = "nnappssets";
	
	private static final String JAVAAPP_PROTOCOL = "localapp://jam/launch?";
	private static final String LOCALAPP_URL = URL + "localapp.php?";

	private static String[] L;

	private static Command exitCmd;
	private static Command aboutCmd;
	private static Command settingsCmd;
	
	private static Command backCmd;
	
	private static Command dlCmd;
	private static Command launchCmd;
	private static Command uninstallCmd;
	private static Command screenshotCmd;
	private static Command hyperlinkCmd;
	
	private static Command cancelCmd;
	private static Command installExtCmd;

	private static CatalogApp midlet;
	private static Display display;
	
	private static boolean started;
	private static boolean warnShown;

	private static Displayable rootScreen;
	private static List categoriesList;
	private static List catalogList;
	private static Form appForm;
	
	private static Form settingsForm;
	private static ChoiceGroup langChoice;

	private static String platform;
	private static boolean symbianJrt;
	private static boolean symbian3;
	private static boolean symbianPatch;
	private static boolean launchSupported;
	private static boolean j2meloader;
	
//	private static Image listPlaceholderImg;
	private static int listImgHeight;
	
	private static JSONArray catalog;
	private static String[] categories;
	private static String category;
	
	private static JSONObject appJson;
	private static ImageItem appImageItem;
	private static String appUrl;
	private static String[] appLaunchInfo;
	private static boolean installing;
	private static int screenshotsIdx;
	
	private static String version;
	private static String statApp;
	private static String statType;
	
	private int run;
	
	private static String lang = "en";

	public CatalogApp() {
		midlet = this;
	}

	protected void destroyApp(boolean unconditional) {
	}

	protected void pauseApp() {
	}

	protected void startApp() {
		if(started) {
			if(checkLaunch()) {
				Hashtable t = parseArgs(getLaunchCommand());
				String s;
				if((s = (String) t.get("app")) != null) {
					appLaunchInfo = new String[] {null, null, null, s};
					final Form f = appForm = new Form(L[0]);
					f.addCommand(backCmd);
					f.setCommandListener(this);
					display(loadingAlert(L[Loading]), f);
					start(RUN_APP);
					return;
				}
			} else {
				start(RUN_REFRESH);
			}
			return;
		}
		started = true;
		display = Display.getDisplay(this);
		version = getAppProperty("MIDlet-Version");
		
		if(!"nnhub".equals(getAppProperty("MIDlet-Name")) ||
				!"nnproject".equals(getAppProperty("MIDlet-Vendor")))
			throw new RuntimeException();
		
		try {
			String s;
			if("ru".equalsIgnoreCase(System.getProperty("user.language")) || (
					(s = System.getProperty("microedition.locale")) != null &&
					s.toLowerCase().indexOf("ru") != -1
					)) {
				lang = "ru";
			}
		} catch (Exception e) {}
		
		try {
			// load settings
			RecordStore r = RecordStore.openRecordStore(SETTINGS_RECORDNAME, false);
			JSONObject j = getObject(new String(r.getRecord(1), "UTF-8"));
			r.closeRecordStore();
			lang = j.getString("lang", lang);
		} catch (Exception e) {}
		
		try {
			(L = new String[100])[0] = "nnhub";
			InputStreamReader r = new InputStreamReader("".getClass().getResourceAsStream("/" + lang), "UTF-8");
			StringBuffer s = new StringBuffer();
			int c;
			int i = 1;
			while((c = r.read()) > 0) {
				if(c == '\r') continue;
				if(c == '\\') {
					s.append((c = r.read()) == 'n' ? '\n' : (char) c);
					continue;
				}
				if(c == '\n') {
					L[i++] = s.toString();
					s.setLength(0);
					continue;
				}
				s.append((char) c);
			}
			r.close();
		} catch (Exception e) {
			throw new RuntimeException(lang);
		}
		
		Form form;
		rootScreen = form = new Form(L[0]);
		form.setCommandListener(this);
		form.addCommand(exitCmd = new Command(L[Exit], Command.EXIT, 1));
		form.append(L[Loading]);
		display(form);

		aboutCmd = new Command(L[About], Command.SCREEN, 3);
		settingsCmd = new Command(L[Settings], Command.SCREEN, 2);
		
		backCmd = new Command(L[Back], Command.BACK, 1);
		
		dlCmd = new Command(L[Download], Command.ITEM, 1);
		launchCmd = new Command(L[LaunchCmd], Command.ITEM, 1);
		uninstallCmd = new Command(L[Uninstall], Command.ITEM, 1);
		screenshotCmd = new Command(L[ScreenshotCmd], Command.ITEM, 1);
		hyperlinkCmd = new Command(L[Open], Command.ITEM, 2);
		
		cancelCmd = new Command(L[Cancel], Command.CANCEL, 1);
		installExtCmd = new Command(L[Install], Command.OK, 1);
		
		try {
			String plat = System.getProperty("microedition.platform");
			if(plat == null) {
				platform = "null";
			} else {
				platform = plat;
				symbianJrt = plat.indexOf("platform=S60") != -1;
				symbian3 = symbianJrt && plat.indexOf("java_build_version=2") != -1; 
				if(!(launchSupported = plat.toLowerCase().startsWith("nokia"))) {
					try {
						Class.forName("javax.microedition.shell.MicroActivity"); // j2me loader check
						j2meloader = launchSupported = true;
					} catch (Exception e) {}
				}
				if(symbianJrt) {
					Class.forName("ru.nnproject.installerext.InstallerExtension");
					System.out.println("ext found");
					InstallerExtension.init();
					symbianPatch = true;
				}
			}
		} catch (Throwable e) {}
		int p;
		if((p = display.getBestImageHeight(Display.LIST_ELEMENT)) <= 0)
			p = 48;
		listImgHeight = p;
		start(RUN_CHECK);
	}

	public void commandAction(Command c, Displayable d) {
		if(c == exitCmd) {
			notifyDestroyed();
			return;
		}
		if(c == List.SELECT_COMMAND) {
			int i;
			if((i = ((List)d).getSelectedIndex()) == -1) return;
			if(d == catalogList) {
				final JSONObject app = catalog.getObject(i);
				final Form f = appForm = new Form(app.has("name") ? app.getString("name") : app.getString("suite"));
				f.addCommand(backCmd);
				f.setCommandListener(this);
				display(loadingAlert(L[Loading]), f);
				start(RUN_CATALOG_APP);
				return;
			}
			if(d == categoriesList) {
				category = categories[i];
				display(loadingAlert(L[Loading]), appForm);
				start(RUN_CATALOG);
				return;
			}
		}
		if(c == backCmd) {
			if(d == settingsForm) { // save settings
				lang = langChoice.getSelectedIndex() == 1 ? "en" : "ru";
				try {
					JSONObject j = new JSONObject();
					j.put("lang", lang);
					j.put("v", SETTINGSV);
					byte[] b = j.toString().getBytes("UTF-8");
					RecordStore r = RecordStore.openRecordStore(SETTINGS_RECORDNAME, true);
					if(r.getNumRecords() > 0) {
						r.setRecord(1, b, 0, b.length);
					} else {
						r.addRecord(b, 0, b.length);
					}
					r.closeRecordStore();
				} catch (Exception e) {
				}
				display(rootScreen);
				settingsForm = null;
				langChoice = null;
				return;
			}
			if(d == appForm) { // dispose app form
				appImageItem = null;
				appUrl = null;
				appJson = null;
				appForm = null;
				display(catalogList);
				return;
			}
			if(d == catalogList) {
				catalogList = null;
			}
			display(rootScreen);
			return;
		}
		if(c == cancelCmd) {
			display(rootScreen);
			return;
		}
		if(c == installExtCmd) {
			try {
				display(rootScreen);
				if(platformRequest(EXTSIS_URL))
					notifyDestroyed();
			} catch (Exception e) {}
			return;
		}
		if(c == launchCmd) {
			try {
				int i;
				if((i = ((List)d).getSelectedIndex()) == -1) return;
				final JSONObject app = catalog.getObject(i);
				appLaunchInfo = new String[] {app.getString("suite"), app.getString("vendor"), app.getString("uid"), app.getString("id")};
				launchApp();
			} catch (Exception e) {
				e.printStackTrace();
				display(warningAlert(L[AppLaunchError].concat(": ").concat(e.toString())));
			}
			return;
		}
		if(c == settingsCmd) {
			settingsForm = new Form(L[Settings]);
			settingsForm.setCommandListener(this);
			settingsForm.addCommand(backCmd);
			langChoice = new ChoiceGroup(L[Language], Choice.POPUP, new String[] {"ru", "en"}, null);
			langChoice.setSelectedIndex(lang.equals("en") ? 1 : 0, true);
			settingsForm.append(langChoice);
			display(settingsForm);
			return;
		}
		if(c == aboutCmd) {
			final Form f = new Form(L[About]);
			f.setCommandListener(this);
			f.addCommand(backCmd);
			
			StringItem s;
			s = new StringItem(null, L[0] + " v" + version);
			s.setFont(Font.getFont(0, 0, Font.SIZE_LARGE));
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_VCENTER);
			f.append(s);
			s = new StringItem(null, L[AboutText] + "\n\n");
			s.setFont(Font.getDefaultFont());
			s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_LEFT);
			f.append(s);
			s = new StringItem(L[DevelopedBy], "shinovon");
			s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_LEFT);
			f.append(s);
			s = new StringItem(L[Web], "nnp.nnchan.ru", Item.HYPERLINK);
			s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_LEFT);
			s.setDefaultCommand(hyperlinkCmd);
			s.setItemCommandListener(this);
			f.append(s);
			s = new StringItem(L[Donate], "boosty.to/nnproject/donate", Item.HYPERLINK);
			s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_LEFT);
			s.setDefaultCommand(hyperlinkCmd);
			s.setItemCommandListener(this);
			f.append(s);
			s = new StringItem(L[Chat], "t.me/nnmidletschat", Item.HYPERLINK);
			s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_LEFT);
			s.setDefaultCommand(hyperlinkCmd);
			s.setItemCommandListener(this);
			f.append(s);
			s = new StringItem(null, "\n\n292 labs");
			s.setFont(Font.getDefaultFont());
			s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_LEFT);
			f.append(s);
			display(f);
			return;
		}
	}

	public void commandAction(Command c, Item item) {
		if(c == dlCmd) {
			try {
				if(installing) return;
				if(appLaunchInfo != null)
					stat(STAT_INSTALL, appLaunchInfo[3]);
				if(symbianPatch) {
					start(RUN_INSTALL);
					return;
				}
				if(platformRequest(appUrl))
					notifyDestroyed();
			} catch (Exception e) {}
			return;
		}
		if(c == launchCmd) {
			try {
				launchApp();
			} catch (Exception e) {
				e.printStackTrace();
				display(warningAlert(L[AppLaunchError].concat(": ").concat(e.toString())));
			}
			return;
		}
		if(c == uninstallCmd) {
			if(installing) return;
			stat(STAT_UNINSTALL, appLaunchInfo[3]);
			start(RUN_UNINSTALL);
			return;
		}
		if(c == screenshotCmd) {
			try {
				// TODO
				if(platformRequest(URL + appLaunchInfo[3] + "/" + ((ImageItem)item).getAltText()))
					notifyDestroyed();
			} catch (Exception e) {}
			return;
		}
		if(c == hyperlinkCmd) {
			try {
				if(platformRequest("http://".concat(((StringItem) item).getText())))
					notifyDestroyed();
			} catch (Exception e) {}
			return;
		}
	}

	public void run() {
		int run;
		synchronized(this) {
			run = this.run;
			notify();
		}
		System.out.println("run " + run);
		String app_id = null;
		Image app_img = null;
		switch(run) {
		case RUN_CATALOG: { // load catalog
			try {
				String c = category;
				catalogList = new List(L[0] + " - " + categoriesList.getString(categoriesList.getSelectedIndex()), Choice.IMPLICIT);
				if(rootScreen == null) {
					rootScreen = catalogList;
					catalogList.addCommand(aboutCmd);
					catalogList.addCommand(exitCmd);
				} else catalogList.addCommand(backCmd);
				catalogList.addCommand(launchCmd);
				catalogList.addCommand(List.SELECT_COMMAND);
				catalogList.setCommandListener(this);
				catalogList.setFitPolicy(Choice.TEXT_WRAP_ON);
				
				catalog = getArray(getUtf(URL + (c == null ? "catalog.php?lang=" + lang + APIV : "catalog.php?c=" + c + "&lang=" + lang + "&p=" + url(platform) + APIV)));
				
				int l = catalog.size();
				int i = 0;
				while(i < l) {
					JSONObject app = catalog.getObject(i++);
					String name = app.has("name") ? app.getString("name") : app.getString("suite");
					String v;
					if(symbianPatch && (v = getInstalledVersion(app.getString("suite"), app.getString("vendor"), app.getNullableString("uid"))) != null) {
						name += app.has("last") && !app.getString("last").equals(v) ? "\n" + L[updateAvailable] : "\n" + L[installed];
					}
					catalogList.append(name, null);
				}
				display(catalogList);
				afterStart();
			} catch (Exception e) {
				e.printStackTrace();
				display(warningAlert(L[CatalogError].concat(": ").concat(e.toString())));
				return;
			}
		}
		case RUN_CATALOG_ICONS: { // load catalog icons
			int i = -1;
			JSONArray a = catalog;
			List list = catalogList;
			int l = a.size();
			Image img;
			try {
				while(++i < l) {
					if((img = getAppIcon(a.getObject(i).getString("id"), listImgHeight)) == null) continue;
					list.set(i, list.getString(i), img);
				}
			} catch (Exception e) {
			}
			return;
		}
		case RUN_CATALOG_APP: {
			int i;
			app_id = catalog.getObject(i = catalogList.getSelectedIndex()).getString("id");
			app_img = catalogList.getImage(i);
		}
		case RUN_APP: { // load app form
			final Form f = appForm;
			try {
				if(app_id == null) app_id = appLaunchInfo[3];
				JSONObject app = appJson = getObject(getUtf(URL + "app.php?id=" + app_id + "&lang=" + lang + APIV));

				String suite = app.getString("suite");
				String vendor = app.getString("vendor");
				String uid = app.getNullableString("uid");
				
//				int type = app.getInt("type", 0);
				
				appUrl = app.getNullableString("dl");
				appLaunchInfo = new String[] {suite, vendor, uid, app_id};
				
				f.setTitle(app.has("name") ? app.getString("name") : app.getString("suite"));
				
				if(app_img != null)
					f.append(appImageItem = new ImageItem(null, resizeAppIcon(app_img, 58), 
						Item.LAYOUT_2 | Item.LAYOUT_LEFT, null));
				
				StringItem s;
				
				s = new StringItem(null, app.has("name") ? app.getString("name") : app.getString("suite"));
				s.setFont(Font.getFont(0, Font.STYLE_BOLD, Font.SIZE_LARGE));
				s.setLayout(Item.LAYOUT_2 | Item.LAYOUT_LEFT | Item.LAYOUT_TOP);
				f.append(s);
				
				s = new StringItem(null, " | " + vendor);
				s.setFont(Font.getFont(0, 0, Font.SIZE_SMALL));
				s.setLayout(Item.LAYOUT_2 | Item.LAYOUT_LEFT | Item.LAYOUT_TOP | Item.LAYOUT_NEWLINE_AFTER);
				f.append(s);
				Object d = app.getNullable("description");
				String ds = L[NoDescription];
				if(d != null) {
					if(d instanceof String) ds = (String) d;
					else if(d instanceof JSONObject) {
						if(((JSONObject)d).has(lang))
							ds = ((JSONObject)d).getString(lang);
						else if(((JSONObject)d).has("en"))
							ds = ((JSONObject)d).getString("en");
					}
				}
				s = new StringItem(null, ds + "\n");
				s.setFont(Font.getDefaultFont());
				s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				screenshotsIdx = f.append(s);
				
				if(app.has("screenshots")) start(RUN_SCREENSHOTS);
				
				String last = app.getString("last");
				
				boolean supported = true;
				int idx;
				String c;
				if((c = app.getString("c", null)) != null) {
					while(supported && (idx = c.indexOf(';')) != -1) {
						supported &= compatibility(c.substring(0, idx));
						c = c.substring(idx + 1);
					}
					if(c.length() > 0) supported &= compatibility(c);
				}
				if(!supported) {
					s = new StringItem(null, "\n" + L[AppNotSupported] + "\n\n" + L[LastVersion] + ": " + last + "\n\n");
					s.setFont(Font.getDefaultFont());
					s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					f.append(s);
					
					if(appUrl != null) {
						s = new StringItem(null, L[Download], Item.BUTTON);
						s.setDefaultCommand(dlCmd);
						s.setItemCommandListener(CatalogApp.this);
						s.setFont(Font.getDefaultFont());
						s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
						f.append(s);
					}
				} else {
					if(symbianPatch) {
						boolean installed = isAppInstalled(suite, vendor, uid);
						String ver = installed ? getInstalledVersion(suite, vendor, uid) : null;
						
						if(installed) {
							boolean needUpdate = !ver.equals(last);
							
							if(needUpdate) {
								s = new StringItem(null, "\n" + L[LastVersion] + ": " + last + "\n");
								s.setFont(Font.getDefaultFont());
								s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
								f.append(s);
								
								s = new StringItem(null, L[InstalledVersion] + ": " + ver + "\n\n");
								s.setFont(Font.getDefaultFont());
								s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
								f.append(s);
							} else {
								s = new StringItem(null, "\n" + L[Version] + ": " + ver + "\n\n");
								s.setFont(Font.getDefaultFont());
								s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
								f.append(s);
							}
							
							if(appUrl != null && !ver.equals(last)) {
								s = new StringItem(null, L[Update], Item.BUTTON);
								s.setDefaultCommand(dlCmd);
								s.setItemCommandListener(CatalogApp.this);
								s.setFont(Font.getDefaultFont());
								s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
								f.append(s);
							}
							
							s = new StringItem(null, L[Launch], Item.BUTTON);
							s.setDefaultCommand(launchCmd);
							s.setItemCommandListener(CatalogApp.this);
							s.setFont(Font.getDefaultFont());
							s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
							f.append(s);
							
							s = new StringItem(null, L[Uninstall], Item.BUTTON);
							s.setDefaultCommand(uninstallCmd);
							s.setItemCommandListener(CatalogApp.this);
							s.setFont(Font.getDefaultFont());
							s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
							f.append(s);
						} else {
							s = new StringItem(null, "\n" + L[LastVersion] + ": " + last + "\n\n");
							s.setFont(Font.getDefaultFont());
							s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
							f.append(s);
							
							if(appUrl != null) {
								s = new StringItem(null, L[Install], Item.BUTTON);
								s.setDefaultCommand(dlCmd);
								s.setItemCommandListener(CatalogApp.this);
								s.setFont(Font.getDefaultFont());
								s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
								f.append(s);
							}
						}
					} else {
						s = new StringItem(null, "\n" + L[LastVersion] + ": " + last + "\n\n");
						s.setFont(Font.getDefaultFont());
						s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
						f.append(s);
						
						if(appUrl != null) {
							s = new StringItem(null, L[Download], Item.BUTTON);
							s.setDefaultCommand(dlCmd);
							s.setItemCommandListener(CatalogApp.this);
							s.setFont(Font.getDefaultFont());
							s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
							f.append(s);
						}
						
						if(launchSupported) {
							s = new StringItem(null, L[Launch], Item.BUTTON);
							s.setDefaultCommand(launchCmd);
							s.setItemCommandListener(CatalogApp.this);
							s.setFont(Font.getDefaultFont());
							s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
							f.append(s);
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				f.append(e.toString());
			}
			display(f);
			return;
		}
		case RUN_INSTALL: { // install
			installing = true;
			try {
				int r = InstallerExtension.installApp(appUrl);
				System.out.println("installer closed: " + r);
				Thread.sleep(3000);
				refresh();
			} catch (Exception e) {
				e.printStackTrace();
				display(warningAlert(L[InstallerError].concat(": ").concat(e.toString())), appForm);
			}
			installing = false;
			return;
		}
		case RUN_UNINSTALL: { // uninstall
			installing = true;
			try {
				String uid = InstallerExtension.getUid(appLaunchInfo[0], appLaunchInfo[1], null);
				int r = InstallerExtension.removeApp(appLaunchInfo[0], appLaunchInfo[1], uid);
				System.out.println("installer closed: " + r);
				Thread.sleep(3000);
				refresh();
			} catch (Exception e) {
				e.printStackTrace();
				display(warningAlert(L[InstallerError].concat(": ").concat(e.toString())), appForm);
			}
			installing = false;
			return;
		}
		case RUN_SCREENSHOTS: { // load app screenshots
			JSONArray a = appJson.getArray("screenshots");
			String id = appLaunchInfo[3];
			ImageItem img;
			int i = 0;
			int l = a.size();
			try {
				appImageItem.setImage(getAppIcon(id, 58));
				while(i < l) {
					JSONArray o = a.getArray(i++);
					img = new ImageItem(null, getImage(URL + id + "/" + o.getString(o.size() > 1 ? 1 : 0)), Item.LAYOUT_LEFT, o.getString(0));
					img.setItemCommandListener(this);
					img.addCommand(screenshotCmd);
					appForm.insert(screenshotsIdx + i, img);
				}
			} catch (Exception e) {}
			return;
		}
		case RUN_REFRESH:
			refresh();
			return;
		case RUN_CATEGORIES: { // load categories
			try {
				rootScreen = categoriesList = new List(L[0], Choice.IMPLICIT);
				categoriesList.addCommand(aboutCmd);
				categoriesList.addCommand(settingsCmd);
				categoriesList.addCommand(exitCmd);
				categoriesList.addCommand(List.SELECT_COMMAND);
				categoriesList.setCommandListener(this);
				
				JSONArray j = getArray(getUtf(URL + "categories.php?lang=" + lang + "&p=" + url(platform) + APIV));

				JSONObject objs = j.getObject(1);
				JSONArray list = j.getArray(0);
				int i = 0;
				int l;
				categories = new String[(l = list.size()) + 1];
				
				categoriesList.append(L[Catalog], null);
				
				while(i < l) {
					String c = list.getString(i++);
					JSONObject o = objs.getObject(c);
					
					if(o.getBoolean("sym", false) && !symbianJrt) // symbian only
						continue;
					
					Object n = o.get("name");
					if(n instanceof JSONObject) {
						n = ((JSONObject)n).has(lang) ? ((JSONObject)n).getString(lang) : ((JSONObject)n).getString("en");
					}
					categories[categoriesList.append((String) n, null)] = c;
				}
				display(categoriesList);
				afterStart();
			} catch (Exception e) {
				e.printStackTrace();
				display(warningAlert(L[CatalogError].concat(": ").concat(e.toString())));
			}
			return;
		}
		case RUN_EXIT_TIMEOUT:
			try {
				Thread.sleep(5000);
			} catch (Exception e) {}
			notifyDestroyed();
			return;
		case RUN_CHECK: {
			if(statType == null) {
				try {
					JSONObject j = getObject(getUtf(URL + "check.php?t=0&lang=" + lang + "&v=" + version + "&p=" + url(platform)));
					if(j.getBoolean("update_available", false)) {
						String url = j.getString("download_url");
						String msg = j.getString("message", L[UpdateAvailable]);
						Alert a = new Alert(L[0]);
						a.setType(AlertType.INFO);
						a.setString(msg);
						a.setTimeout(3000);
						display(a);
						Thread.sleep(2000);
						if(symbianPatch) {
							InstallerExtension.installApp(url);
						} else {
							platformRequest(url);
						}
						Thread.sleep(1000);
						notifyDestroyed();
					} else {
						start(RUN_CATEGORIES);
					}
				} catch (Exception e) {
					display(warningAlert(L[NetworkError].concat(" \n\ndetails: ").concat(e.toString())));
					start(RUN_EXIT_TIMEOUT);
				}
				return;
			}
			try {
				getUtf(URL + "check.php?t=" + url(statType) + "&a=" + statApp + "&lang=" + lang + "&v=" + version + "&p=" + url(platform));
			} catch (Exception e) {}
			return;
		}
		}
	}

	private static void afterStart() {
		if(!symbian3 || symbianPatch || warnShown) return;
		warnShown = true;
		Alert a = new Alert("");
		a.setType(AlertType.INFO);
		a.setString(L[SymbianExtensionAlert]);
		a.setCommandListener(midlet);
		a.addCommand(cancelCmd);
		a.addCommand(installExtCmd);
		display(a, catalogList);
	}
	
	private static void refresh() {
		if(catalogList == null || appLaunchInfo == null || !symbianPatch) return;
		int i = -1;
		int l = catalog.size();
		List list = catalogList;
		try {
			while(++i < l) {
				final JSONObject app = catalog.getObject(i);
				if(!app.getString("id").equals(appLaunchInfo[3]))
					continue;
				String name = app.has("name") ? app.getString("name") : app.getString("suite");
				String v;
				if(symbianPatch && (v = getInstalledVersion(app.getString("suite"), app.getString("vendor"), app.getNullableString("uid"))) != null) {
					name += app.has("last") && !app.getString("last").equals(v) ? "\n" + L[updateAvailable] : "\n" + L[installed];
				}
				list.set(i, name, list.getImage(i));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void start(int i) {
		try {
			synchronized(this) {
				run = i;
				new Thread(this).start();
				wait();
			}
		} catch (Exception e) {}
	}
	
	private static void display(Alert a, Displayable d) {
		if(d == null) {
			display.setCurrent(a);
			return;
		}
		display.setCurrent(a, d);
	}
	
	private static void display(Displayable d) {
		if(d instanceof Alert) {
			Displayable c;
			display.setCurrent((Alert) d, (c = display.getCurrent()) instanceof Alert ? rootScreen : c);
			return;
		}
		display.setCurrent(d);
	}
	
	private static Alert loadingAlert(String text) {
		Alert a = new Alert("");
		a.setString(text);
		a.setIndicator(new Gauge(null, false, Gauge.INDEFINITE, Gauge.CONTINUOUS_RUNNING));
		a.setTimeout(30000);
		return a;
	}
	
	private static Alert warningAlert(String text) {
		Alert a = new Alert("");
		a.setType(AlertType.ERROR);
		a.setString(text);
		a.setTimeout(3000);
		return a;
	}

	private static Image resizeAppIcon(Image img, int p) {
		if(img == null) return null;
		return resize(img, p, p);
	}
	
	private static boolean isAppInstalled(String suite, String vendor, String uid) {
		try {
			if(suite == null) { // native app TODO
				return false;
//				return InstallerExtension.isInstalled(null, vendor, uid);
			}
			return InstallerExtension.isInstalled(suite, vendor, null);
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return false;
	}
	
	private static String getInstalledVersion(String suite, String vendor, String uid) {
		try {
			if(suite == null) { // native app TODO
				return "1.0.0";
//				return InstallerExtension.getVersion(null, vendor, uid);
			}
			String s = InstallerExtension.getVersion(suite, vendor, null);
			if(s != null && s.indexOf('.') == s.lastIndexOf('.'))
				s = s.concat(".0");
			return s;
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private void launchApp() throws Exception {
		stat(STAT_LAUNCH, appLaunchInfo[3]);
		if(appLaunchInfo[0] == null) { // native app
			if(!symbianJrt) return;
			if(platformRequest("nativeapp:application-uid=" + appLaunchInfo[2]))
				notifyDestroyed();
		} else if(appLaunchInfo[2] != null && symbian3) {
			if(platformRequest(JAVAAPP_PROTOCOL + "midlet-uid=" + appLaunchInfo[2] + ";launchfrom=nnstore"))
				notifyDestroyed();
		} else {
			if(s40()) {
				platformRequest(LOCALAPP_URL +
						"name=" + url(appLaunchInfo[0]) +
						"&vendor=" + url(appLaunchInfo[1]) +
						"&id=" + url(appLaunchInfo[3]) +
						"&id=" + url(appLaunchInfo[3]) +
						"&from=nnstore"
						);
				notifyDestroyed();
				return;
			}
			if(platformRequest(JAVAAPP_PROTOCOL +
					"midlet-vendor=" + url(appLaunchInfo[1]) +
					";midlet-name=" + url(appLaunchInfo[0]) +
					";launchfrom=nnstore"))
				notifyDestroyed();
		}
	}

	private static Image getAppIcon(String id, int size) throws IOException {
		try {
			return resizeAppIcon(getImage(URL + id + "/default.png"), size);
		} catch (Exception e) {}
		return null;
	}
	
	private static void stat(String type, String id) {
		statType = type;
		statApp = id;
		midlet.start(RUN_CHECK);
	}
	
	// utils

	private static Image getImage(String url) throws IOException {
		byte[] b = get(url);
		return Image.createImage(b, 0, b.length);
	}
	
	static String url(String url) {
		StringBuffer sb = new StringBuffer();
		char[] chars = url.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			int c = chars[i];
			if (65 <= c && c <= 90) {
				sb.append((char) c);
			} else if (97 <= c && c <= 122) {
				sb.append((char) c);
			} else if (48 <= c && c <= 57) {
				sb.append((char) c);
			} else if (c == 32) {
				sb.append("%20");
			} else if (c == 45 || c == 95 || c == 46 || c == 33 || c == 126 || c == 42 || c == 39 || c == 40
					|| c == 41) {
				sb.append((char) c);
			} else if (c <= 127) {
				sb.append(hex(c));
			} else if (c <= 2047) {
				sb.append(hex(0xC0 | c >> 6));
				sb.append(hex(0x80 | c & 0x3F));
			} else {
				sb.append(hex(0xE0 | c >> 12));
				sb.append(hex(0x80 | c >> 6 & 0x3F));
				sb.append(hex(0x80 | c & 0x3F));
			}
		}
		return sb.toString();
	}

	private static String hex(int i) {
		String s = Integer.toHexString(i);
		return "%".concat(s.length() < 2 ? "0" : "").concat(s);
	}
	
	private static byte[] readBytes(InputStream inputStream, int initialSize, int bufferSize, int expandSize) throws IOException {
		if (initialSize <= 0) initialSize = bufferSize;
		byte[] buf = new byte[initialSize];
		int count = 0;
		byte[] readBuf = new byte[bufferSize];
		int readLen;
		while ((readLen = inputStream.read(readBuf)) != -1) {
			if(count + readLen > buf.length) {
				byte[] newbuf = new byte[count + expandSize];
				System.arraycopy(buf, 0, newbuf, 0, count);
				buf = newbuf;
			}
			System.arraycopy(readBuf, 0, buf, count, readLen);
			count += readLen;
		}
		if(buf.length == count) {
			return buf;
		}
		byte[] res = new byte[count];
		System.arraycopy(buf, 0, res, 0, count);
		return res;
	}
	
	private static byte[] get(String url) throws IOException {
		HttpConnection hc = null;
		InputStream in = null;
		try {
			hc = open(url);
			int r;
			if((r = hc.getResponseCode()) >= 400) {
				throw new IOException("HTTP " + r);
			}
			in = hc.openInputStream();
			return readBytes(in, (int) hc.getLength(), 1024, 2048);
		} finally {
			try {
				if (in != null) in.close();
			} catch (IOException e) {
			}
			try {
				if (hc != null) hc.close();
			} catch (IOException e) {
			}
		}
	}

	private static String getUtf(String url) throws IOException {
		HttpConnection hc = null;
		InputStream in = null;
		try {
			hc = open(url);
			int i, j, k = 0;
			if((i = hc.getResponseCode()) >= 400) {
				throw new IOException("HTTP " + i);
			}
			String r;
			while(i >= 300) {
				if(++k > 3) {
					throw new IOException("Too many redirects!");
				}
				if((r = hc.getHeaderField("Location")).startsWith("/")) {
					r = url.substring(0, (j = url.indexOf("//") + 2)) + url.substring(j, url.indexOf("/", j)) + r;
				}
				hc.close();
				hc = open(r);
				if((i = hc.getResponseCode()) >= 400) {
					throw new IOException("HTTP " + i);
				}
			}
			in = hc.openInputStream();
			byte[] buf = new byte[(i = (int) hc.getLength()) <= 0 ? 1024 : i];
			i = 0;
			while((j = in.read(buf, i, buf.length - i)) != -1) {
				if((i += j) == buf.length) {
					System.arraycopy(buf, 0, buf = new byte[i + 2048], 0, i);
				}
			}
			return new String(buf, 0, i, "UTF-8");
		} finally {
			try {
				if (in != null) in.close();
			} catch (IOException e) {}
			try {
				if (hc != null) hc.close();
			} catch (IOException e) {}
		}
	}
	
	// platform & compatibility
	
	private static HttpConnection open(String url) throws IOException {
		HttpConnection hc = (HttpConnection) Connector.open(url);
		hc.setRequestMethod("GET");
		hc.setRequestProperty("X-User-Agent", "nnstore/" + version);
		return hc;
	}

	private static boolean compatibility(String c) {
		int idx = c.indexOf('|');
		boolean r = false;
		if(idx == -1) {
			boolean not = c.charAt(0) == '!';
			switch(c.length()) {
			case 1:
				r = "1".equals(c);
				break;
			case 2:
				if("sj".equals(c)) {
					r = symbianJrt;
					break;
				}
				if("s3".equals(c)) {
					r = symbian3;
					break;
				}
				if("sg".equals(c)) {
					r = platform.toLowerCase().startsWith("samsung");
					break;
				}
				if("se".equals(c)) {
					r = platform.toLowerCase().startsWith("sonyericsson");
					break;
				}
				if("nk".equals(c)) {
					r = platform.toLowerCase().startsWith("nokia");
					break;
				}
				if("jl".equals(c)) {
					r = j2meloader;
					break;
				}
			case 3:
				if("s60".equals(c)) {
					r = symbianJrt || s60();
					break;
				}
				if("s94".equals(c)) {
					r = symbianJrt && platform.indexOf("version=5.0") != -1;
					break;
				}
				if("s93".equals(c)) {
					r = symbianJrt && platform.indexOf("version=3.2") != -1;
					break;
				}
				if("s92".equals(c)) {
					r = !symbianJrt && s60() &&
							System.getProperty("microedition.amms.version") != null;
					break;
				}
				if("s91".equals(c)) {
					r = !symbianJrt && s60() &&
							System.getProperty("microedition.amms.version") == null &&
							(
									System.getProperty("microedition.sip.version") != null ||
									checkClass("javax.crypto.Cipher")
							);
					break;
				}
				if("s40".equals(c)) {
					r = s40();
					break;
				}
				if("sjp".equals(c)) {
					r = System.getProperty("com.sonyericsson.java.platform") != null;
					break;
				}
				if("m3g".equals(c)) {
					r = System.getProperty("microedition.m3g.version") != null;
					break;
				}
				if("gps".equals(c)) {
					r = System.getProperty("microedition.location.version") != null;
					break;
				}
				if("swt".equals(c)) {
					r = checkClass("org.eclipse.ercp.swt.mobile.MobileShell");
					break;
				}
			default:
				if(c.startsWith("s40")) {
					if(!s40()) break;
					boolean plus = c.endsWith("+");
					if(c.startsWith("s40v3")) {
						r = checkClass("com.nokia.mid.pri.PriAccess") ||
							checkClass("javax.microedition.m2g.ScalableGraphics");
						if(!plus)
							r &= System.getProperty("microedition.amms.version") == null;
						break;
					}
					if(c.startsWith("s40v5")) {
						r = System.getProperty("microedition.amms.version") != null;
						if(!plus)
							r &= System.getProperty("microedition.location.version") == null;
						break;
					}
					if(c.startsWith("s40v6")) {
						r = System.getProperty("microedition.location.version") != null ||
								checkClass("com.arm.cldc.mas.GlobalLock");
						if(!plus)
							r &= platform.indexOf("java") == -1 && System.getProperty("com.nokia.mid.ui.version") == null;
						break;
					}
					if(c.startsWith("s40a")) {
						String s = System.getProperty("com.nokia.mid.ui.version");
						// FIXME: s40 nokia ui version
						r = !symbian3 && s != null && s.length() == 3 && s.charAt(0) == '1' && s.charAt(2) > '2' && s.charAt(2) < '7';
						break;
					}
				}
				if(c.startsWith("s60v3")) {
					if(!s60()) break;
					r = System.getProperty("microedition.sip.version") != null ||
							checkClass("javax.crypto.Cipher");
					if(!c.endsWith("+"))
						r &= !symbianJrt || platform.indexOf("version=5.") == -1;
					break;
				}
				if(c.startsWith("s60v5")) {
					r = symbianJrt && c.endsWith("+") ? platform.indexOf("version=5.") != -1 : platform.indexOf("version=5.0") != -1;
					break;
				}
				if("belle".equals(c)) {
					r = symbian3 && platform.charAt(platform.indexOf("version=5.") + 10) > '2';
					break;
				}
				if("asha".equals(c)) {
					String s = System.getProperty("com.nokia.mid.ui.version");
					r = !symbian3 && s != null && s.length() == 3 && (s.charAt(0) == '2' || s.charAt(2) > '6');
					break;
				}
				System.out.println("Undefined compatibility flag: " + c);
				return false;
			}
			return not?!r:r;
		}
		do {
			r = compatibility(c.substring(0, idx));
			c = c.substring(idx + 1);
		} while(!r && (idx = c.indexOf('|')) != -1);
		if(c.length() > 0) r |= compatibility(c);
		return r;
	}
	
	private static boolean s40() {
		return !checkClass("com.sun.midp.Main") && 
				(platform.startsWith("Nokia") ||
				platform.startsWith("Vertu")) &&
				(
						checkClass("javax.microedition.midlet.MIDletProxy") || 
						checkClass("com.nokia.mid.impl.isa.jam.Jam")
				);
	}
	
	private static boolean s60() { // symbian ..-9.2 check
		return System.getProperty("com.symbian.midp.serversocket.support") != null ||
				System.getProperty("com.symbian.default.to.suite.icon") != null ||
				checkClass("com.symbian.midp.io.protocol.http.Protocol") ||
				checkClass("com.symbian.lcdjava.io.File");
	}
	
	private static boolean checkClass(String s) {
		try {
			Class.forName(s);
			return true;
		} catch (Exception e) {
		}
		return false;
	}
	
	// image utils

	private static Image resize(Image src_i, int size_w, int size_h) {
		// set source size
		int w = src_i.getWidth();
		int h = src_i.getHeight();

		// no change??
		if (size_w == w && size_h == h)
			return src_i;

		int[] dst = new int[size_w * size_h];

		resize_rgb_filtered(src_i, dst, w, h, size_w, size_h);

		// not needed anymore
		src_i = null;

		return Image.createRGBImage(dst, size_w, size_h, true);
	}

	private static final void resize_rgb_filtered(Image src_i, int[] dst, int w0, int h0, int w1, int h1) {
		int[] buffer1 = new int[w0];
		int[] buffer2 = new int[w0];

		// UNOPTIMIZED bilinear filtering:               
		//         
		// The pixel position is defined by y_a and y_b,
		// which are 24.8 fixed point numbers
		// 
		// for bilinear interpolation, we use y_a1 <= y_a <= y_b1
		// and x_a1 <= x_a <= x_b1, with y_d and x_d defining how long
		// from x/y_b1 we are.
		//
		// since we are resizing one line at a time, we will at most 
		// need two lines from the source image (y_a1 and y_b1).
		// this will save us some memory but will make the algorithm 
		// noticeably slower

		for (int index1 = 0, y = 0; y < h1; y++) {

			final int y_a = ((y * h0) << 8) / h1;
			final int y_a1 = y_a >> 8;
			int y_d = y_a & 0xFF;

			int y_b1 = y_a1 + 1;
			if (y_b1 >= h0) {
				y_b1 = h0 - 1;
				y_d = 0;
			}

			// get the two affected lines:
			src_i.getRGB(buffer1, 0, w0, 0, y_a1, w0, 1);
			if (y_d != 0)
				src_i.getRGB(buffer2, 0, w0, 0, y_b1, w0, 1);

			for (int x = 0; x < w1; x++) {
				// get this and the next point
				int x_a = ((x * w0) << 8) / w1;
				int x_a1 = x_a >> 8;
				int x_d = x_a & 0xFF;

				int x_b1 = x_a1 + 1;
				if (x_b1 >= w0) {
					x_b1 = w0 - 1;
					x_d = 0;
				}

				// interpolate in x
				int c12, c34;
				int c1 = buffer1[x_a1];
				int c3 = buffer1[x_b1];

				// interpolate in y:
				if (y_d == 0) {
					c12 = c1;
					c34 = c3;
				} else {
					int c2 = buffer2[x_a1];
					int c4 = buffer2[x_b1];

					final int v1 = y_d & 0xFF;
					final int a_c2_RB = c1 & 0x00FF00FF;
					final int a_c2_AG_org = c1 & 0xFF00FF00;

					final int b_c2_RB = c3 & 0x00FF00FF;
					final int b_c2_AG_org = c3 & 0xFF00FF00;

					c12 = (a_c2_AG_org + ((((c2 >>> 8) & 0x00FF00FF) - (a_c2_AG_org >>> 8)) * v1)) & 0xFF00FF00
							| (a_c2_RB + ((((c2 & 0x00FF00FF) - a_c2_RB) * v1) >> 8)) & 0x00FF00FF;
					c34 = (b_c2_AG_org + ((((c4 >>> 8) & 0x00FF00FF) - (b_c2_AG_org >>> 8)) * v1)) & 0xFF00FF00
							| (b_c2_RB + ((((c4 & 0x00FF00FF) - b_c2_RB) * v1) >> 8)) & 0x00FF00FF;
				}

				// final result

				final int v1 = x_d & 0xFF;
				final int c2_RB = c12 & 0x00FF00FF;

				final int c2_AG_org = c12 & 0xFF00FF00;
				dst[index1++] = (c2_AG_org + ((((c34 >>> 8) & 0x00FF00FF) - (c2_AG_org >>> 8)) * v1)) & 0xFF00FF00
						| (c2_RB + ((((c34 & 0x00FF00FF) - c2_RB) * v1) >> 8)) & 0x00FF00FF;
			}
		}
	}
	
	// midletintegration
	
	private static int instances;
	private static boolean receiving;
	
	/**
	 * Checks if a MIDlet has received a new start request from another MIDlet<br>
	 * Recommended to use in startApp() with "Nokia-MIDlet-Background-Event: pause" property in MANIFEST.MF<br>
	 * After receiving a request, you should receive arguments from getLaunchCommand()
	 * @see {@link #getLaunchCommand()}
	 * @return true if new arguments have been received since the last check
	 */
	static boolean checkLaunch() {
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
	static String getLaunchCommand() {
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
		try {
			while (i < numChars) {
				c = s.charAt(i);
				switch (c) {
				case '%':
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
					needToChange = true;
					break;
				default:
					sb.append(c);
					i++;
					break;
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e.toString());
		}
		return (needToChange ? sb.toString() : s);
	}
	
	// nnjson

	// parse all nested elements once
	static final boolean parse_members = false;
	
	// identation for formatting
	static final String FORMAT_TAB = "  ";
	
	// used for storing nulls, get methods must return real null
	static final Object json_null = new Object();
	
	static final Boolean TRUE = new Boolean(true);
	static final Boolean FALSE = new Boolean(false);

	static JSONObject getObject(String text) {
		if (text == null || text.length() <= 1)
			throw new RuntimeException("JSON: Empty text");
		if (text.charAt(0) != '{')
			throw new RuntimeException("JSON: Not JSON object");
		return (JSONObject) parseJSON(text);
	}

	static JSONArray getArray(String text) {
		if (text == null || text.length() <= 1)
			throw new RuntimeException("JSON: Empty text");
		if (text.charAt(0) != '[')
			throw new RuntimeException("JSON: Not JSON array");
		return (JSONArray) parseJSON(text);
	}

	static Object getJSON(Object obj) {
		if (obj instanceof Hashtable) {
			return new JSONObject((Hashtable) obj);
		}
		if (obj instanceof Vector) {
			return new JSONArray((Vector) obj);
		}
		if (obj == null) {
			return json_null;
		}
		return obj;
	}

	static Object parseJSON(String str) {
		char first = str.charAt(0);
		int length = str.length() - 1;
		char last = str.charAt(length);
		switch(first) {
		case '"': { // string
			if (last != '"')
				throw new RuntimeException("JSON: Unexpected end of text");
			if(str.indexOf('\\') != -1) {
				char[] chars = str.substring(1, length).toCharArray();
				str = null;
				int l = chars.length;
				StringBuffer sb = new StringBuffer();
				int i = 0;
				// parse escaped chars in string
				loop: {
					while (i < l) {
						char c = chars[i];
						switch (c) {
						case '\\': {
							next: {
								replace: {
									if (l < i + 1) {
										sb.append(c);
										break loop;
									}
									char c1 = chars[i + 1];
									switch (c1) {
									case 'u':
										i+=2;
										sb.append((char) Integer.parseInt(
												new String(new char[] {chars[i++], chars[i++], chars[i++], chars[i++]}),
												16));
										break replace;
									case 'x':
										i+=2;
										sb.append((char) Integer.parseInt(
												new String(new char[] {chars[i++], chars[i++]}),
												16));
										break replace;
									case 'n':
										sb.append('\n');
										i+=2;
										break replace;
									case 'r':
										sb.append('\r');
										i+=2;
										break replace;
									case 't':
										sb.append('\t');
										i+=2;
										break replace;
									case 'f':
										sb.append('\f');
										i+=2;
										break replace;
									case 'b':
										sb.append('\b');
										i+=2;
										break replace;
									case '\"':
									case '\'':
									case '\\':
									case '/':
										i+=2;
										sb.append((char) c1);
										break replace;
									default:
										break next;
									}
								}
								break;
							}
							sb.append(c);
							i++;
							break;
						}
						default:
							sb.append(c);
							i++;
						}
					}
				}
				str = sb.toString();
				sb = null;
				return str;
			}
			return str.substring(1, length);
		}
		case '{': // JSON object or array
		case '[': {
			boolean object = first == '{';
			if (object ? last != '}' : last != ']')
				throw new RuntimeException("JSON: Unexpected end of text");
			int brackets = 0;
			int i = 1;
			char nextDelimiter = object ? ':' : ',';
			boolean escape = false;
			String key = null;
			Object res = object ? (Object) new JSONObject() : (Object) new JSONArray();
			
			for (int splIndex; i < length; i = splIndex + 1) {
				// skip all spaces
				for (; i < length - 1 && str.charAt(i) <= ' '; i++);

				splIndex = i;
				boolean quote = false;
				for (; splIndex < length && (quote || brackets > 0 || str.charAt(splIndex) != nextDelimiter); splIndex++) {
					char c = str.charAt(splIndex);
					if (!escape) {
						if (c == '\\') {
							escape = true;
						} else if (c == '"') {
							quote = !quote;
						}
					} else escape = false;
	
					if (!quote) {
						if (c == '{' || c == '[') {
							brackets++;
						} else if (c == '}' || c == ']') {
							brackets--;
						}
					}
				}

				// fail if unclosed quotes or brackets left
				if (quote || brackets > 0) {
					throw new RuntimeException("JSON: Corrupted JSON");
				}

				if (object && key == null) {
					key = str.substring(i, splIndex);
					key = key.substring(1, key.length() - 1);
					nextDelimiter = ',';
				} else {
					Object value = str.substring(i, splIndex).trim();
					// check if value is empty
					if(((String) value).length() == 0) continue;
					// don't check length because if value is empty, then exception is going to be thrown anyway
					char c = ((String) value).charAt(0);
					// leave JSONString as value to parse it later, if its object or array and nested parsing is disabled
					value = parse_members || (c != '{' && c != '[') ?
							parseJSON((String) value) : new String[] {(String) value};
					if (object) {
						((JSONObject) res)._put(key, value);
						key = null;
						nextDelimiter = ':';
					} else if (splIndex > i) {
						((JSONArray) res).addElement(value);
					}
				}
			}
			return res;
		}
		case 'n': // null
			return json_null;
		case 't': // true
			return TRUE;
		case 'f': // false
			return FALSE;
		default: // number
			if ((first >= '0' && first <= '9') || first == '-') {
				try {
					// hex
					if (length > 1 && first == '0' && str.charAt(1) == 'x') {
						if (length > 9) // str.length() > 10
							return new Long(Long.parseLong(str.substring(2), 16));
						return new Integer(Integer.parseInt(str.substring(2), 16));
					}
					// decimal
					if (str.indexOf('.') != -1 || str.indexOf('E') != -1 || "-0".equals(str))
						return new Double(Double.parseDouble(str));
					if (first == '-') length--;
					if (length > 8) // (str.length() - (str.charAt(0) == '-' ? 1 : 0)) >= 10
						return new Long(Long.parseLong(str));
					return new Integer(Integer.parseInt(str));
				} catch (Exception e) {}
			}
			throw new RuntimeException("JSON: Couldn't be parsed: " + str);
//			return new JSONString(str);
		}
	}

	// transforms string for exporting
	static String escape_utf8(String s) {
		int len = s.length();
		StringBuffer sb = new StringBuffer();
		int i = 0;
		while (i < len) {
			char c = s.charAt(i);
			switch (c) {
			case '"':
			case '\\':
				sb.append("\\").append(c);
				break;
			case '\b':
				sb.append("\\b");
				break;
			case '\f':
				sb.append("\\f");
				break;
			case '\n':
				sb.append("\\n");
				break;
			case '\r':
				sb.append("\\r");
				break;
			case '\t':
				sb.append("\\t");
				break;
			default:
				if (c < 32 || c > 1103) {
					String u = Integer.toHexString(c);
					sb.append("\\u");
					for (int z = u.length(); z < 4; z++) {
						sb.append('0');
					}
					sb.append(u);
				} else {
					sb.append(c);
				}
			}
			i++;
		}
		return sb.toString();
	}

	static double getDouble(Object o) {
		try {
			if (o instanceof String[])
				return Double.parseDouble(((String[]) o)[0]);
			if (o instanceof Integer)
				return ((Integer) o).intValue();
			if (o instanceof Long)
				return ((Long) o).longValue();
			if (o instanceof Double)
				return ((Double) o).doubleValue();
		} catch (Throwable e) {}
		throw new RuntimeException("JSON: Cast to double failed: " + o);
	}

	static int getInt(Object o) {
		try {
			if (o instanceof String[])
				return Integer.parseInt(((String[]) o)[0]);
			if (o instanceof Integer)
				return ((Integer) o).intValue();
			if (o instanceof Long)
				return (int) ((Long) o).longValue();
			if (o instanceof Double)
				return ((Double) o).intValue();
		} catch (Throwable e) {}
		throw new RuntimeException("JSON: Cast to int failed: " + o);
	}

	static long getLong(Object o) {
		try {
			if (o instanceof String[])
				return Long.parseLong(((String[]) o)[0]);
			if (o instanceof Integer)
				return ((Integer) o).longValue();
			if (o instanceof Long)
				return ((Long) o).longValue();
			if (o instanceof Double)
				return ((Double) o).longValue();
		} catch (Throwable e) {}
		throw new RuntimeException("JSON: Cast to long failed: " + o);
	}


}
