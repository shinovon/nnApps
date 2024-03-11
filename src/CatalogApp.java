import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Choice;
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

import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;
import midletintegration.MIDletIntegration;
import ru.nnproject.installerext.InstallerExtension;

public class CatalogApp extends MIDlet implements CommandListener, ItemCommandListener, Runnable {

	private static final Command exitCmd = new Command("Выход", Command.EXIT, 1);
	private static final Command aboutCmd = new Command("О программе", Command.SCREEN, 2);
	
	private static final Command backCmd = new Command("Назад", Command.BACK, 1);
	
	private static final Command dlCmd = new Command("Скачать", Command.ITEM, 1);
	private static final Command startNoPatchCmd = new Command("Запустить", Command.ITEM, 1);
	private static final Command uninstallCmd = new Command("Удалить", Command.ITEM, 1);
	private static final Command screenshotCmd = new Command("Показать", Command.ITEM, 1);
	
	private static final String URL = "http://nnm.nnchan.ru/nns/";
	
	private Display display;
	
	private boolean started;

	private List list;
	
	private Form appForm;
	
	private boolean symbian;
	private boolean symbianPatch;
	private boolean launchSupported;
	
	private JSONArray catalog;
	private Image listPlaceholderImg;
	private int listImgHeight;
	
	private JSONObject appJson;
	private ImageItem appImageItem;
	private String appUrl;
	private String[] appLaunchInfo;
	private boolean installing;
	private int screenshotsIdx;
	
	private int run;
	

	public CatalogApp() {
	}

	protected void destroyApp(boolean unconditional) {
	}

	protected void pauseApp() {
	}

	protected void startApp() {
		if(started) return;
		started = true;
		display = Display.getDisplay(this);
		
		Form form = new Form("nnApps");
		form.append("Загрузка");
		display(form);
		
		try {
			String platform;
			symbian = (platform = System.getProperty("microedition.platform")).indexOf("platform=S60") != -1;
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
			start(1);
		} catch (Exception e) {}
	}

	public void commandAction(Command c, Displayable d) {
		if(c == List.SELECT_COMMAND) {
			final JSONObject app = catalog.getObject(list.getSelectedIndex());
			appForm = new Form(app.has("display") ? app.getString("display") : app.getString("suite"));
			appForm.addCommand(backCmd);
			appForm.setCommandListener(this);
			display(loadingAlert("Загрузка"), appForm);
			start(3);
			return;
		}
		if(c == backCmd) {
			if(d == appForm) { // dispose app form
				appImageItem = null;
				appUrl = null;
				appLaunchInfo = null;
				appJson = null;
				appForm = null;
			}
			display(list);
			return;
		}
		if(c == exitCmd) {
			notifyDestroyed();
			return;
		}
		if(c == aboutCmd) {
			Form form = new Form("О программе");
			form.setCommandListener(this);
			form.addCommand(backCmd);
			form.append("пока ничего не придумал");
			// TODO
			display(form);
			return;
		}
	}

	public void commandAction(Command c, Item item) {
		if(c == dlCmd) {
			try {
				if(installing) return;
				if(symbianPatch) {
					start(4);
					return;
				}
				if(platformRequest(appUrl))
					notifyDestroyed();
			} catch (Exception e) {}
			return;
		}
		if(c == startNoPatchCmd) {
			try {
				startApp(appLaunchInfo[0], appLaunchInfo[1], appLaunchInfo[2]);
			} catch (Exception e) {
				e.printStackTrace();
				display(warningAlert("Не удалось запустить приложение: " + e.toString()));
			}
			return;
		}
		if(c == uninstallCmd) {
			if(installing) return;
			start(5);
			return;
		}
	}

	public void run() {
		int run;
		synchronized(this) {
			run = this.run;
			notify();
		}
		switch(run) {
		case 1: { // load catalog
			try {
				list = new List("nnApps", Choice.IMPLICIT);
				list.addCommand(aboutCmd);
				list.addCommand(exitCmd);
				list.addCommand(List.SELECT_COMMAND);
				list.setCommandListener(this);
				list.setFitPolicy(Choice.TEXT_WRAP_ON);
				
				catalog = JSON.getArray(getUtf(URL + "catalog.php"));
				
				Enumeration e = catalog.elements();
				while(e.hasMoreElements()) {
					JSONObject app = (JSONObject) e.nextElement();
					String name = app.has("display") ? app.getString("display") : app.getString("suite");
					if(symbianPatch && isAppInstalled(app.getString("suite"), app.getString("vendor"), app.getNullableString("uid"))) {
						name += "\nустановлено";
					}
					list.append(name, listPlaceholderImg);
				}
				display(list);
				start(2);
			} catch (Exception e) {
				e.printStackTrace();
				display(warningAlert("Не удалось загрузить каталог: " + e.toString()));
			}
			return;
		}
		case 2: { // load catalog icons
			int i = -1;
			int l = catalog.size();
			try {
				while(++i < l) {
					list.set(i, list.getString(i),
							resizeAppIcon(getImage(URL + catalog.getObject(i).getString("id") + "/default.png"), listImgHeight));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;
		}
		case 3: { // load app form
			try {
				int i;
				JSONObject app = catalog.getObject(i = list.getSelectedIndex());
				
				String id = app.getString("id");

				String suite = app.getString("suite");
				String vendor = app.getString("vendor");
				String uid = app.getNullableString("uid");
				
				appJson = app = JSON.getObject(getUtf(URL + id + "/app.json"));
				
//				int type = app.getInt("type", 0);
				
				appUrl = app.getNullableString("dl");
				appLaunchInfo = new String[] {suite, vendor, uid, id};
				
				ImageItem img;
				StringItem s;
				
				appImageItem = img = new ImageItem(null, resizeAppIcon(list.getImage(i), 58), 
						Item.LAYOUT_2 | Item.LAYOUT_LEFT, null);
				appForm.append(img);
				
				s = new StringItem(null, app.has("display") ? app.getString("display") : app.getString("suite"));
				s.setFont(Font.getFont(0, Font.STYLE_BOLD, Font.SIZE_LARGE));
				s.setLayout(Item.LAYOUT_2 | Item.LAYOUT_LEFT | Item.LAYOUT_TOP);
				appForm.append(s);
				
				s = new StringItem(null, " | " + vendor);
				s.setFont(Font.getFont(0, 0, Font.SIZE_SMALL));
				s.setLayout(Item.LAYOUT_2 | Item.LAYOUT_LEFT | Item.LAYOUT_TOP | Item.LAYOUT_NEWLINE_AFTER);
				appForm.append(s);
				Object d = app.getNullable("description");
				String ds = "Нет описания";
				if(d != null) {
					if(d instanceof String) ds = (String) d;
					else if(d instanceof JSONObject) {
						if(((JSONObject)d).has("ru"))
							ds = ((JSONObject)d).getString("ru");
						else if(((JSONObject)d).has("en"))
							ds = ((JSONObject)d).getString("en");
					}
				}
				s = new StringItem(null, ds + "\n\n");
				s.setFont(Font.getDefaultFont());
				s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
				screenshotsIdx = appForm.append(s);
				
				if(app.has("screenshots")) start(6);
				
				String last = app.getString("last");
				if(symbianPatch) {
					boolean installed = isAppInstalled(suite, vendor, uid);
					String ver = installed ? getInstalledVersion(suite, vendor, uid) : null;
					
					
					if(installed) {
						boolean needUpdate = !ver.equals(last);
						
						if(needUpdate) {
							s = new StringItem(null, "\nПоследняя версия: " + last + "\n");
							s.setFont(Font.getDefaultFont());
							s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
							appForm.append(s);
							
							s = new StringItem(null, "Установлена: " + ver + "\n\n");
							s.setFont(Font.getDefaultFont());
							s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
							appForm.append(s);
						} else {
							s = new StringItem(null, "\nВерсия: " + ver + "\n\n");
							s.setFont(Font.getDefaultFont());
							s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
							appForm.append(s);
						}
						
						if(appUrl != null && !ver.equals(last)) {
							s = new StringItem(null, "Обновить", Item.BUTTON);
							s.setDefaultCommand(dlCmd);
							s.setItemCommandListener(CatalogApp.this);
							s.setFont(Font.getDefaultFont());
							s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
							appForm.append(s);
						}
						
						s = new StringItem(null, "Открыть", Item.BUTTON);
						s.setDefaultCommand(startNoPatchCmd);
						s.setItemCommandListener(CatalogApp.this);
						s.setFont(Font.getDefaultFont());
						s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
						appForm.append(s);
						
						s = new StringItem(null, "Удалить", Item.BUTTON);
						s.setDefaultCommand(uninstallCmd);
						s.setItemCommandListener(CatalogApp.this);
						s.setFont(Font.getDefaultFont());
						s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
						appForm.append(s);
					} else {
						s = new StringItem(null, "\nПоследняя версия: " + last + "\n\n");
						s.setFont(Font.getDefaultFont());
						s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
						appForm.append(s);
						
						if(appUrl != null) {
							s = new StringItem(null, "Установить", Item.BUTTON);
							s.setDefaultCommand(dlCmd);
							s.setItemCommandListener(CatalogApp.this);
							s.setFont(Font.getDefaultFont());
							s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
							appForm.append(s);
						}
					}
				} else {
					s = new StringItem(null, "\nПоследняя версия: " + last + "\n\n");
					s.setFont(Font.getDefaultFont());
					s.setLayout(Item.LAYOUT_LEFT | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
					appForm.append(s);
					
					if(appUrl != null) {
						s = new StringItem(null, "Скачать", Item.BUTTON);
						s.setDefaultCommand(dlCmd);
						s.setItemCommandListener(CatalogApp.this);
						s.setFont(Font.getDefaultFont());
						s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_BEFORE | Item.LAYOUT_NEWLINE_AFTER);
						appForm.append(s);
					}
					
					if(launchSupported) {
						s = new StringItem(null, "Открыть", Item.BUTTON);
						s.setDefaultCommand(startNoPatchCmd);
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
		case 4: { // install
			installing = true;
			try {
				int r = InstallerExtension.installApp(appUrl);
				System.out.println("installer closed: " + r);
			} catch (Exception e) {
				e.printStackTrace();
				display(warningAlert("Не удалось вызвать установщик: " + e.toString()), appForm);
			}
			installing = false;
			return;
		}
		case 5: { // uninstall
			installing = true;
			try {
				String uid = InstallerExtension.getUid(appLaunchInfo[0], appLaunchInfo[1], null);
				int r = InstallerExtension.removeApp(appLaunchInfo[0], appLaunchInfo[1], uid);
				System.out.println("installer closed: " + r);
			} catch (Exception e) {
				e.printStackTrace();
				display(warningAlert("Не удалось вызвать установщик: " + e.toString()), appForm);
			}
			installing = false;
			return;
		}
		case 6: { // load app screenshots
			JSONArray a = appJson.getArray("screenshots");
			String id = appLaunchInfo[3];
			ImageItem img;
			int i = 0;
			int l = a.size();
			try {
				appImageItem.setImage(resizeAppIcon(getImage(URL + id + "/default.png"), 58));
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
			display.setCurrent((Alert) d, (c = display.getCurrent()) instanceof Alert ? list : c);
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
			return InstallerExtension.getVersion(suite, vendor, null);
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
