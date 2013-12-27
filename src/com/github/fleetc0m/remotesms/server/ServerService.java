package com.github.fleetc0m.remotesms.server;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;

import android.app.Service;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.util.Log;

public class ServerService extends Service {
	
	private static final String TAG = "ServerService";
	
	
	private SMSWebSocketServer socketServer;
	private WebServer webServer;
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId){
		super.onStartCommand(intent, flags, startId);
		if(intent == null){
			Log.e(TAG, "intent is null");
		}
		Log.d(TAG, "service started");
		int port = SMSWebSocketServer.DEFAULT_PORT;
		if(intent != null && intent.getExtras() != null){
			port = intent.getExtras().getInt(SMSWebSocketServer.PORT, -1);
			if(port == -1){
				port = SMSWebSocketServer.DEFAULT_PORT;
			}
		}
		socketServer = new SMSWebSocketServer(port, this);
		//The server will start on a new thread so no action necessary here.
		socketServer.start();
		Log.d(TAG, "wifi addr: " + getWifiIpAddress());
		
		webServer = new WebServer(WebServer.PORT, this);
		webServer.setSocketPort(port);
		webServer.setSocketAddress(getWifiIpAddress());
		
		webServer.start();
		return Service.START_FLAG_REDELIVERY;
	}

	private String getWifiIpAddress(){
		ConnectivityManager connMngr = (ConnectivityManager) this.getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo wifiInfo = connMngr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if(!wifiInfo.isConnected()){
			return null;
		}
		WifiManager wifiManager = (WifiManager) this.getSystemService(WIFI_SERVICE);
		int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
		if(ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)){
			ipAddress = Integer.reverseBytes(ipAddress);
		}
		byte[] ipByte = BigInteger.valueOf(ipAddress).toByteArray();
		String ipAddressStr = null;
		try{
			ipAddressStr = InetAddress.getByAddress(ipByte).getHostAddress();
		}catch(UnknownHostException ex){
			Log.e(TAG, "unable to get host address.");
		}
		return ipAddressStr;
	}
}
