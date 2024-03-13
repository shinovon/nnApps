import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
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

import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;
import midletintegration.MIDletIntegration;
import ru.nnproject.installerext.InstallerExtension;

public class CatalogApp extends MIDlet implements CommandListener, ItemCommandListener, Runnable, LangConstants {
	
	private static final String URL = "http://nnm.nnchan.ru/nns/";
	private static final String EXTSIS_URL = "http://nnm.nnchan.ru/nns/nninstallerext.sis";
	
	private static final int RUN_CATALOG = 1;
	private static final int RUN_CATALOG_ICONS = 2;
	private static final int RUN_APP = 3;
	private static final int RUN_INSTALL = 4;
	private static final int RUN_UNINSTALL = 5;
	private static final int RUN_SCREENSHOTS = 6;
	private static final int RUN_REFRESH = 7;
	private static final int RUN_CATEGORIES = 8;
	private static final int RUN_EXIT_TIMEOUT = 9;

	private static final String SETTINGS_RECORDNAME = "nnappssets";

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
	
	private static Display display;
	
	private boolean started;

	private Displayable rootScreen;
	private List categoriesList;
	private List catalogList;
	private Form appForm;
	
	private Form settingsForm;
	private ChoiceGroup langChoice;
	
	private boolean symbian;
	private boolean symbian3;
	private boolean symbianPatch;
	private boolean launchSupported;
	private boolean warnShown;
	
	private Image listPlaceholderImg;
	private int listImgHeight;
	
	private JSONArray catalog;
	private String[] categories;
	private String category;
	
	private JSONObject appJson;
	private ImageItem appImageItem;
	private String appUrl;
	private String[] appLaunchInfo;
	private boolean installing;
	private int screenshotsIdx;
	
	private int run;
	
	private static String lang = "ru";

	public CatalogApp() {
	}

	protected void destroyApp(boolean unconditional) {
	}

	protected void pauseApp() {
	}

	protected void startApp() {
		if(started) {
			start(RUN_REFRESH);
			return;
		}
		started = true;
		display = Display.getDisplay(this);
		
		try {
			// load settings
			RecordStore r = RecordStore.openRecordStore(SETTINGS_RECORDNAME, false);
			JSONObject j = JSON.getObject(new String(r.getRecord(1), "UTF-8"));
			r.closeRecordStore();
			lang = j.getString("lang", lang);
		} catch (Exception e) {}
		
		try {
			loadLang();
			
			Form form;
			rootScreen = form = new Form(L[0]);
			form.append(L[Loading]);
			display(form);

			exitCmd = new Command(L[Exit], Command.EXIT, 1);
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
		} catch (Exception e) {
			display(warningAlert(e.toString()));
			start(RUN_EXIT_TIMEOUT);
			return;
		}
		
		try {
			String platform;
			symbian = (platform = System.getProperty("microedition.platform")).indexOf("platform=S60") != -1;
			symbian3 = symbian && platform.indexOf("java_build_version=2") != -1; 
			if(!(launchSupported = platform.toLowerCase().startsWith("nokia"))) {
				try {
					Class.forName("javax.microedition.shell.MicroActivity"); // j2me loader check
					launchSupported = true;
				} catch (Exception e) {}
			}
			if(symbian) {
				Class.forName("ru.nnproject.installerext.InstallerExtension");
				System.out.println("ext found");
				symbianPatch = true;
				InstallerExtension.init();
			}
		} catch (Throwable e) {} 
		try {
			int p = display.getBestImageHeight(Display.LIST_ELEMENT);
			if(p <= 0) p = 48;
			listPlaceholderImg = resizeAppIcon(Image.createImage("/placeholder.png"), listImgHeight = p);
		} catch (Exception e) {}
		start(RUN_CATEGORIES);
	}

	public void commandAction(Command c, Displayable d) {
		if(c == List.SELECT_COMMAND) {
			int i;
			if((i = ((List)d).getSelectedIndex()) == -1) return;
			if(d == catalogList) {
				final JSONObject app = catalog.getObject(i);
				appForm = new Form(app.has("name") ? app.getString("name") : app.getString("suite"));
				appForm.addCommand(backCmd);
				appForm.setCommandListener(this);
				display(loadingAlert(L[Loading]), appForm);
				start(RUN_APP);
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
					RecordStore.deleteRecordStore(SETTINGS_RECORDNAME);
				} catch (Exception e) {
				}
				try {
					JSONObject j = new JSONObject();
					j.put("lang", lang);
					byte[] b = j.toString().getBytes("UTF-8");
					RecordStore r = RecordStore.openRecordStore(SETTINGS_RECORDNAME, true);
					r.addRecord(b, 0, b.length);
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
		if(c == exitCmd) {
			notifyDestroyed();
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
				startApp(app.getString("suite"), app.getString("vendor"), app.getString("uid"));
			} catch (Exception e) {
				e.printStackTrace();
				display(warningAlert(L[AppLaunchError] + ": " + e.toString()));
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
			Form f = new Form(L[About]);
			f.setCommandListener(this);
			f.addCommand(backCmd);
			
			StringItem s;
			s = new StringItem(null, "nnhub v" + this.getAppProperty("MIDlet-Version"));
			s.setFont(Font.getFont(0, 0, Font.SIZE_LARGE));
			s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_VCENTER);
			f.append(s);
			s = new StringItem(null, "что-то\n\n");
			s.setFont(Font.getDefaultFont());
			s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_LEFT);
			f.append(s);
			s = new StringItem("Разработал", "shinovon");
			s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_LEFT);
			f.append(s);
			s = new StringItem("Сайт", "nnp.nnchan.ru", Item.HYPERLINK);
			s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_LEFT);
			s.setDefaultCommand(hyperlinkCmd);
			s.setItemCommandListener(this);
			f.append(s);
			s = new StringItem("Донат", "boosty.to/nnproject/donate", Item.HYPERLINK);
			s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_LEFT);
			s.setDefaultCommand(hyperlinkCmd);
			s.setItemCommandListener(this);
			f.append(s);
			s = new StringItem("Чат", "t.me/nnmidletschat", Item.HYPERLINK);
			s.setLayout(Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_LEFT);
			s.setDefaultCommand(hyperlinkCmd);
			s.setItemCommandListener(this);
			f.append(s);
			s = new StringItem(null, "\n\nвыф тв\n292 labs");
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
				startApp(appLaunchInfo[0], appLaunchInfo[1], appLaunchInfo[2]);
			} catch (Exception e) {
				e.printStackTrace();
				display(warningAlert(L[AppLaunchError] + ": " + e.toString()));
			}
			return;
		}
		if(c == uninstallCmd) {
			if(installing) return;
			start(RUN_UNINSTALL);
			return;
		}
		if(c == screenshotCmd) {
			try {
				// TODO
				if(platformRequest(URL + appLaunchInfo[3] + "/" + ((ImageItem)item).getAltText()))
					notifyDestroyed();
			} catch (Exception e) {}
		}
	}

	public void run() {
		int run;
		synchronized(this) {
			run = this.run;
			notify();
		}
		System.out.println("run " + run);
		switch(run) {
		case RUN_CATALOG: { // load catalog
			try {
				String category = this.category;
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
				
				catalog = JSON.getArray(getUtf(URL + (category == null ? "catalog.php?lang=" + lang : "catalog.php?c=" + category + "&lang=" + lang)));
				
				int l = catalog.size();
				int i = 0;
				while(i < l) {
					JSONObject app = catalog.getObject(i++);
					String name = app.has("name") ? app.getString("name") : app.getString("suite");
					String v;
					if(symbianPatch && (v = getInstalledVersion(app.getString("suite"), app.getString("vendor"), app.getNullableString("uid"))) != null) {
						name += app.has("last") && !app.getString("last").equals(v) ? "\n" + L[updateAvailable] : "\n" + L[installed];
					}
					catalogList.append(name, listPlaceholderImg);
				}
				display(catalogList);
				afterStart();
			} catch (Exception e) {
				e.printStackTrace();
				display(warningAlert(L[CatalogError] + ": " + e.toString()));
				return;
			}
		}
		case RUN_CATALOG_ICONS: { // load catalog icons
			int i = -1;
			int l = catalog.size();
			Image img;
			try {
				while(++i < l) {
					if((img = getAppIcon(catalog.getObject(i).getString("id"), listImgHeight)) == null) continue;
					catalogList.set(i, catalogList.getString(i), img);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;
		}
		case RUN_APP: { // load app form
			try {
				int i;
				JSONObject app = catalog.getObject(i = catalogList.getSelectedIndex());
				
				String id = app.getString("id");

				String suite = app.getString("suite");
				String vendor = app.getString("vendor");
				String uid = app.getNullableString("uid");
				
				appJson = app = JSON.getObject(getUtf(URL + "app.php?id=" + id + "&lang=" + lang));
				
//				int type = app.getInt("type", 0);
				
				appUrl = app.getNullableString("dl");
				appLaunchInfo = new String[] {suite, vendor, uid, id};
				
				appForm.append(appImageItem = new ImageItem(null, resizeAppIcon(catalogList.getImage(i), 58), 
						Item.LAYOUT_2 | Item.LAYOUT_LEFT, null));
				
				StringItem s;
				
				s = new StringItem(null, app.has("name") ? app.getString("name") : app.getString("suite"));
				s.setFont(Font.getFont(0, Font.STYLE_BOLD, Font.SIZE_LARGE));
				s.setLayout(Item.LAYOUT_2 | Item.LAYOUT_LEFT | Item.LAYOUT_TOP);
				appForm.append(s);
				
				s = new StringItem(null, " | " + vendor);
				s.setFont(Font.getFont(0, 0, Font.SIZE_SMALL));
				s.setLayout(Item.LAYOUT_2 | Item.LAYOUT_LEFT | Item.LAYOUT_TOP | Item.LAYOUT_NEWLINE_AFTER);
				appForm.append(s);
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
				s = new StringItem(null, ds + "\n\n");
				s.setFont(Font.getDefaultFont());
				s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				screenshotsIdx = appForm.append(s);
				
				if(app.has("screenshots")) start(RUN_SCREENSHOTS);
				
				String last = app.getString("last");
				if(symbianPatch) {
					boolean installed = isAppInstalled(suite, vendor, uid);
					String ver = installed ? getInstalledVersion(suite, vendor, uid) : null;
					
					
					if(installed) {
						boolean needUpdate = !ver.equals(last);
						
						if(needUpdate) {
							s = new StringItem(null, "\n" + L[LastVersion] + ": " + last + "\n");
							s.setFont(Font.getDefaultFont());
							s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
							appForm.append(s);
							
							s = new StringItem(null, L[InstalledVersion] + ": " + ver + "\n\n");
							s.setFont(Font.getDefaultFont());
							s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
							appForm.append(s);
						} else {
							s = new StringItem(null, "\n" + L[Version] + ": " + ver + "\n\n");
							s.setFont(Font.getDefaultFont());
							s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
							appForm.append(s);
						}
						
						if(appUrl != null && !ver.equals(last)) {
							s = new StringItem(null, L[Update], Item.BUTTON);
							s.setDefaultCommand(dlCmd);
							s.setItemCommandListener(CatalogApp.this);
							s.setFont(Font.getDefaultFont());
							s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
							appForm.append(s);
						}
						
						s = new StringItem(null, L[Launch], Item.BUTTON);
						s.setDefaultCommand(launchCmd);
						s.setItemCommandListener(CatalogApp.this);
						s.setFont(Font.getDefaultFont());
						s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
						appForm.append(s);
						
						s = new StringItem(null, L[Uninstall], Item.BUTTON);
						s.setDefaultCommand(uninstallCmd);
						s.setItemCommandListener(CatalogApp.this);
						s.setFont(Font.getDefaultFont());
						s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
						appForm.append(s);
					} else {
						s = new StringItem(null, "\n" + L[LastVersion] + ": " + last + "\n\n");
						s.setFont(Font.getDefaultFont());
						s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
						appForm.append(s);
						
						if(appUrl != null) {
							s = new StringItem(null, L[Install], Item.BUTTON);
							s.setDefaultCommand(dlCmd);
							s.setItemCommandListener(CatalogApp.this);
							s.setFont(Font.getDefaultFont());
							s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
							appForm.append(s);
						}
					}
				} else {
					s = new StringItem(null, "\n" + L[LastVersion] + ": " + last + "\n\n");
					s.setFont(Font.getDefaultFont());
					s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					appForm.append(s);
					
					if(appUrl != null) {
						s = new StringItem(null, L[Download], Item.BUTTON);
						s.setDefaultCommand(dlCmd);
						s.setItemCommandListener(CatalogApp.this);
						s.setFont(Font.getDefaultFont());
						s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
						appForm.append(s);
					}
					
					if(launchSupported) {
						s = new StringItem(null, L[Launch], Item.BUTTON);
						s.setDefaultCommand(launchCmd);
						s.setItemCommandListener(CatalogApp.this);
						s.setFont(Font.getDefaultFont());
						s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
						appForm.append(s);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				appForm.append(e.toString());
			}
			display(appForm);
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
				display(warningAlert(L[InstallerError] + ": " + e.toString()), appForm);
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
				display(warningAlert(L[InstallerError] + ": " + e.toString()), appForm);
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
				
				JSONArray j = JSON.getArray(getUtf(URL + "categories.json"));

				JSONObject objs = j.getObject(1);
				JSONArray list = j.getArray(0);
				int i = 0;
				int l;
				categories = new String[(l = list.size()) + 1];
				
				categoriesList.append(L[Catalog], null);
				
				while(i < l) {
					String c = list.getString(i++);
					JSONObject o = objs.getObject(c);
					
					if(o.getBoolean("sym", false) && !symbian) // symbian only
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
				display(warningAlert(L[CatalogError] + ": " + e.toString()));
			}
			return;
		}
		case RUN_EXIT_TIMEOUT:
			try {
				Thread.sleep(5000);
			} catch (Exception e) {}
			notifyDestroyed();
			return;
		}
	}
	
	private void afterStart() {
		if(symbian3 && !symbianPatch && !warnShown) {
			warnShown = true;
			Alert a = new Alert("");
			a.setType(AlertType.INFO);
			a.setString(L[SymbianExtensionAlert]);
			a.setCommandListener(this);
			a.addCommand(cancelCmd);
			a.addCommand(installExtCmd);
			display(a, catalogList);
		}
	}
	
	private void refresh() {
		if(catalogList == null || appLaunchInfo == null) return;
		int i = -1;
		int l = catalog.size();
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
				catalogList.set(i, name, catalogList.getImage(i));
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
	
	private void display(Alert a, Displayable d) {
		if(d == null) {
			display.setCurrent(a);
			return;
		}
		display.setCurrent(a, d);
	}
	
	private void display(Displayable d) {
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
	
	private boolean isAppInstalled(String suite, String vendor, String uid) {
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
	
	private String getInstalledVersion(String suite, String vendor, String uid) {
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
	
	private void startApp(String suite, String vendor, String uid) throws Exception {
		String arg = "from=nnstore";
		if(suite == null) { // native app
			if(!symbian) return;
			if(platformRequest("nativeapp:application-uid=" + uid))
				notifyDestroyed();
		} else if(uid != null && symbian) {
			if(MIDletIntegration.startAppWithAppUID(this, uid, arg))
				notifyDestroyed();
		} else {
			if(MIDletIntegration.startApp(this, suite, vendor, arg))
				notifyDestroyed();
		}
	}

	private static Image getAppIcon(String id, int size) throws IOException {
		try {
			return resizeAppIcon(getImage(URL + id + "/default.png"), size);
		} catch (Exception e) {}
		return null;
	}
	
	private static void loadLang() {
		try {
			(L = new String[100])[0] = "nnhub";
			InputStreamReader r = new InputStreamReader("".getClass().getResourceAsStream("/" + lang), "UTF-8");
			StringBuffer s = new StringBuffer();
			int c;
			
			int i = 1;
			for(;;) {
				c = r.read();
				if(c <= 0) break;
				if(c == '\r') continue;
				if(c == '\n') {
					L[i++] = s.toString();
					s.setLength(0);
					continue;
				}
				s.append((char) c);
			}
			r.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	// utils

	private static Image getImage(String url) throws IOException {
		byte[] b = get(url);
		return Image.createImage(b, 0, b.length);
	}
	
	public static String url(String url) {
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
		System.out.println("GET " + url);
		HttpConnection hc = null;
		InputStream in = null;
		try {
			hc = (HttpConnection) Connector.open(url);
			hc.setRequestMethod("GET");
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
		return new String(get(url), "UTF-8");
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

}
