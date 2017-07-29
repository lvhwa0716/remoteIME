package com.lvh.remoteimepc;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.TimeoutException;
import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.RawImage;
import com.android.ddmlib.IShellOutputReceiver;

import java.util.HashMap;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

public class MainPC {

	static final boolean DEBUG = false;
	private AndroidDebugBridge adb;
	private HashMap<String, IDevice> sConnectionDevices = new HashMap<String, IDevice>();
	public static String mSelectedDevice;

	private static final int MOBLIE_PORT = 22222; // must not change this ,same
													// as mobile code
	private static final int PC_PORT = 22222;

	private class ShellOutputReceiver implements IShellOutputReceiver {
		/**
		 * Called every time some new data is available.
		 * 
		 * @param data
		 *            The new data.
		 * @param offset
		 *            The offset at which the new data starts.
		 * @param length
		 *            The length of the new data.
		 */
		public void addOutput(byte[] data, int offset, int length) {

		}

		/**
		 * Called at the end of the process execution (unless the process was
		 * canceled). This allows the receiver to terminate and flush whatever
		 * data was not yet processed.
		 */
		public void flush() {

		}

		/**
		 * Cancel method to stop the execution of the remote shell command.
		 * 
		 * @return true to cancel the execution of the command.
		 */
		public boolean isCancelled() {
			return bExitFlags;
		}
	};

	public void init() {
		AndroidDebugBridge.init(false);
	}

	public void finish() {
		AndroidDebugBridge.terminate();
	}

	public void prepare() {
		try {
			registerChangeListener();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean start() {
		adb = AndroidDebugBridge.createBridge(/* adb path, flags */);
		if (true) { // must set false when release , only for debug
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (!adb.isConnected()) {
			System.out.println("Couldn't connect to ADB server");
			return false;
		}
		return true;
	}

	public void stop() {
		AndroidDebugBridge.disconnectBridge();
	}

	private void registerChangeListener() {
		AndroidDebugBridge
				.addDeviceChangeListener(new AndroidDebugBridge.IDeviceChangeListener() {
					public void deviceConnected(IDevice device) {
						synchronized (sConnectionDevices) {
							sConnectionDevices.put(device.getSerialNumber(),
									device);
							mSelectedDevice = device.getSerialNumber(); // only
																		// for
																		// test
						}
						System.out.println("connected "
								+ device.getSerialNumber());
					}

					public void deviceDisconnected(IDevice device) {
						synchronized (sConnectionDevices) {
							sConnectionDevices.remove(device.getSerialNumber());
						}
						System.out.println("disconnnect "
								+ device.getSerialNumber());
					}

					public void deviceChanged(IDevice device, int changeMask) {
						System.out.println("changed "
								+ device.getSerialNumber() + " /mask="
								+ changeMask);
					}
				});

	}

	private class DeviceSocketInfo {
		public Socket socket;
		public ObjectOutputStream oos;
		public ObjectInputStream ois;
		public int mPort;

		public DeviceSocketInfo(Socket s, ObjectOutputStream oo,
				ObjectInputStream oi, int port) {
			socket = s;
			oos = oo;
			ois = oi;
		}
	}

	private HashMap<String, DeviceSocketInfo> mDeviceSocket = new HashMap<String, DeviceSocketInfo>();

	public boolean openSocket(String deviceId, int pc_port) {
		IDevice device;
		synchronized (sConnectionDevices) {
			device = sConnectionDevices.get(deviceId);
		}
		if (device == null) {
			return false;
		}
		try {
			device.createForward(pc_port, MOBLIE_PORT);
		} catch (TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (AdbCommandRejectedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		// Create socket connection
		try {
			Socket socket = new Socket("localhost", pc_port);
			ObjectOutputStream oos = new ObjectOutputStream(
					socket.getOutputStream());
			// ObjectInputStream ois = new ObjectInputStream(
			// socket.getInputStream()); // these will block
			mDeviceSocket.put(deviceId, new DeviceSocketInfo(socket, oos, null,
					pc_port));
			return true;

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Could not initialize I/O on socket");
			e.printStackTrace();
		}

		return false;
	}

	public boolean writeSocket(String deviceId, String msg) {
		DeviceSocketInfo di = mDeviceSocket.get(deviceId);
		if (di == null) {
			return false;
		}

		try {
			di.oos.writeObject(msg);
			return true;
		} catch (IOException e) {
			System.err.println("writeSocket error");
			e.printStackTrace();
		}
		return false;
	}

	public void closeSocket(String deviceId) {
		DeviceSocketInfo di = mDeviceSocket.get(deviceId);
		if (di == null) {
			return;
		}

		try {
			if (di.ois != null)
				di.ois.close();
			if (di.oos != null)
				di.oos.close();
			if (di.socket != null)
				di.socket.close();
		} catch (IOException e) {
			System.err.println("closeSocket error");
			e.printStackTrace();
		}

		IDevice device;
		synchronized (sConnectionDevices) {
			device = sConnectionDevices.get(deviceId);
		}
		if (device != null) {
			try {
				device.removeForward(di.mPort, MOBLIE_PORT);
			} catch (TimeoutException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (AdbCommandRejectedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		mDeviceSocket.remove(deviceId);
	}

	public boolean InjectKey(String deviceId, String key) {
		IDevice device;
		synchronized (sConnectionDevices) {
			device = sConnectionDevices.get(deviceId);
		}
		if (device == null) {
			return false;
		}
		try {
			device.executeShellCommand("input keyevent " + key, new ShellOutputReceiver());
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public RawImage screenShot(String deviceId) {
		IDevice device;
		synchronized (sConnectionDevices) {
			device = sConnectionDevices.get(deviceId);
		}
		if (device == null) {
			return null;
		}
		try {
			return device.getScreenshot();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private boolean bExitFlags = false;

	private class ExitHandler extends Thread {
		public ExitHandler() {
			super("Exit Handler");
		}

		public void run() {
			System.out.println("Set exit");
			bExitFlags = true;
		}
	}

	public void setHookDispatch() {
		Runtime.getRuntime().addShutdownHook(new ExitHandler());
	}

	public static void main(String[] args) throws Exception {
		MainPC demo = new MainPC();
		String inString = null;
		String mCmd = null;

		switch (args.length) {
		case 0:
			System.err.println("java -jar PCMain.jar cmd params");
			System.err.println("1. Inject Key : key key_name");
			System.err.println("2. Inject String : text string");
			System.err.println("2. ScreenShot : snapshot filepath");
			System.err.println("default Inject String");
			mCmd = "text";
			break;
		case 1:
			mCmd = args[0];
			break;
		case 2:
			mCmd = args[0];
			inString = args[1];
			break;
		default:
			mCmd = "text";
			break;
		}

		demo.init();
		demo.prepare();
		/*** do all jobs here **/
		if (demo.start()) {
			if (mCmd.equals("key")) {
				System.err.println("Inject KeyEvent: " + inString);

				demo.InjectKey(mSelectedDevice, inString);
			} else if (mCmd.equals("text")) {
				System.err.println("Inject Text: " + inString);

				demo.openSocket(mSelectedDevice, PC_PORT);
				if (inString != null) {
					demo.writeSocket(mSelectedDevice, inString);
				} else {
					demo.setHookDispatch();
					while (demo.bExitFlags == false) {
						try {
							BufferedReader strin = new BufferedReader(
									new InputStreamReader(System.in));
							System.out.print("请输入一个字符串：");
							String str = strin.readLine();
							demo.writeSocket(mSelectedDevice, str);
							demo.InjectKey(mSelectedDevice, "ENTER");
						} catch (IOException e) {
							e.printStackTrace();
							break;
						}
					}
				}
				demo.closeSocket(mSelectedDevice);
			} else if (mCmd.equals("snapshot")) {
				RawImage rawImage = demo.screenShot(mSelectedDevice);
				String filepath = inString;
				if (rawImage != null) {
					System.err.println("RawImage : h = " + rawImage.height
							+ " /w=" + rawImage.width + "/ bpp=" + rawImage.bpp
							+ "/ size=" + rawImage.size);
					assert rawImage.bpp == 16;
					BufferedImage image;
					switch (rawImage.bpp) {
					case 16: {
						// convert raw data to an Image
						image = new BufferedImage(rawImage.width,
								rawImage.height, BufferedImage.TYPE_INT_ARGB);
						byte[] buffer = rawImage.data;
						int index = 0;
						for (int y = 0; y < rawImage.height; y++) {
							for (int x = 0; x < rawImage.width; x++) {
								int value = buffer[index++] & 0x00FF;
								value |= (buffer[index++] << 8) & 0x0FF00;
								int r = ((value >> 11) & 0x01F) << 3;
								int g = ((value >> 5) & 0x03F) << 2;
								int b = ((value >> 0) & 0x01F) << 3;
								value = 0xFF << 24 | r << 16 | g << 8 | b;
								image.setRGB(x, y, value);
							}
						}
						break;
					}
					case 32: {
						// convert raw data to an Image
						image = new BufferedImage(rawImage.width,
								rawImage.height, BufferedImage.TYPE_INT_ARGB);
						byte[] buffer = rawImage.data;
						int index = 0;
						for (int y = 0; y < rawImage.height; y++) {
							for (int x = 0; x < rawImage.width; x++) {
								int value = buffer[index++] & 0x00FF;
								value |= (buffer[index++] << 8) & 0x0FF00;
								value |= (buffer[index++] << 16) & 0x0FF0000;
								value |= (buffer[index++] << 24) & 0x0FF000000;

								image.setRGB(x, y, value);
							}
						}
						break;
					}
					default: {
						return;
					}

					}

					if (!ImageIO.write(image, "png", new File(filepath))) {
						throw new IOException("Failed to find png writer");
					}
				}
			} else {
				System.err
						.println("[pls run java -jar PCMain.jar for help]cmd error :"
								+ mCmd);
			}
		}
		/*** done ***/
		demo.stop();
		demo.finish();
	}
}
