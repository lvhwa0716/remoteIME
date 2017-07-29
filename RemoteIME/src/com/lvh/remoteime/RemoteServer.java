

package com.lvh.remoteime;

import android.content.Context;
import android.os.IBinder;
import java.util.ArrayList;
import java.util.List;
import android.util.Log;
import android.os.Handler;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.net.ServerSocket;

public class RemoteServer extends Thread {
	static final String TAG = "RemoteIME-RemoteServer";
	private SoftKeyboard mSkb;
	private int mPort;
	private ServerSocket mServer = null;

	public RemoteServer(SoftKeyboard skb, int port) {
		mSkb = skb;
		mPort = port;
	}
	@Override
	public void run() {
		
		Socket client = null; 
		ObjectInputStream ois = null;
		if(com.lvh.remoteime.Debug.DebugFlags) {
			Log.d(TAG, "RemoteServer Running ");
		}
		try {
			mServer = new ServerSocket(mPort);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		while (true) {  
			try {  
				// attempt to accept a connection  
				client = mServer.accept();
				if(com.lvh.remoteime.Debug.DebugFlags) {
					Log.d(TAG, "Connect From= "  + client.getRemoteSocketAddress().toString());
				}

				ois = new ObjectInputStream(  client.getInputStream());
				while (true) {
					String somewords = (String) ois.readObject(); 
					if((somewords != null) && !somewords.isEmpty()) {
						if(com.lvh.remoteime.Debug.DebugFlags) {
							Log.d(TAG, "Recv=" + somewords);  
						}
						mSkb.injectString(somewords);
					}
				}
			} catch (IOException e) {  
				Log.e(TAG, "" + e);  
			} catch (ClassNotFoundException e) {  
				// TODO Auto-generated catch block  
				e.printStackTrace();  
			} finally {
				if(ois != null)
					try {
						ois.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				if(client != null)
					try {
						client.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			}
		}  
	}

}
