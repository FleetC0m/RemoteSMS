package com.github.fleetc0m.remotesms.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import android.content.Context;
import android.util.Log;

public class WebServer implements Runnable {
	public static final int PORT = 8888;
	private ServerSocket serverSocket;
	private int port;
	private Thread serverThread;
	private volatile boolean serverRunning;
	private Context c;
	private int socketPort;
	private String socketAddress;
	
	
	private static final String TAG = "ws";
	
	public WebServer(int port, Context c){
		this.port = port;
		this.c = c;
	}
	
	public WebServer(Context c){
		this(PORT, c);
	}
	public void setSocketPort(int port){
		this.socketPort = port;
	}
	public void setSocketAddress(String add){
		this.socketAddress = add;
	}
	
	public synchronized void start(){
		if(serverRunning){
			throw new IllegalStateException("Server is already running.");
		}
		serverThread = new Thread(this);
		serverThread.start();
		serverRunning = true;
	}
	
	public synchronized void stop(){
		if(!serverRunning){
			throw new IllegalStateException("Server is already stopped");
		}
		serverThread.interrupt();
		serverRunning = false;
	}
	
	public void restart(){
		stop();
		start();
	}
	
	@Override
	public void run() {
		Log.d(TAG, "server thread started");
		try {
			serverSocket = new ServerSocket(port);
		} catch (IOException e) {
			e.printStackTrace();
		}
		while(!Thread.currentThread().isInterrupted()){
			try {
				Socket clientSocket = serverSocket.accept();
				Log.d(TAG, "client accepted");
				new Thread(new RequestHandler(clientSocket)).start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		Log.d(TAG, "server thread stopped");
	}
	
	class RequestHandler implements Runnable{
		private Socket clientSocket;
		public RequestHandler(Socket clientSocket){
			this.clientSocket = clientSocket;
		}
		@Override
		public void run(){
			PrintWriter out = null;
			BufferedReader in = null;
			try {
				out = new PrintWriter(clientSocket.getOutputStream(), true);
				in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			} catch (IOException e) {
				e.printStackTrace();
			}
			String inputLine = null;
			String request = null;
			StringBuilder inputStrBuilder = new StringBuilder();
			try {
				while((inputLine = in.readLine()) != null){
					Log.v(TAG, "incoming: " + inputLine + " (" + inputLine.length() + ")");
					if(inputLine.startsWith("GET ")){
						request = inputLine.substring("GET ".length(), inputLine.lastIndexOf(" "));
						//Log.d(TAG, "request: " + request);
					}
					if(inputLine.length() == 0){
						break;
					}
					inputStrBuilder.append(inputLine);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			handleRequest(request, out);
		}
		private final String HEADER_200 = "HTTP/1.0 200 OK\r\n" +
										"Server: android-RemoteSMS\r\n" +
										"Content-type: text/html\r\n\r\n";
		
		private final String HEADER_404 = "HTTP/1.0 404 Not found\r\n" + 
										"Server: android:RemoteSMS\r\n" + 
										"Content-type: text/html\r\n\r\n";
		private String getHeader200(String content_type){
			return "HTTP/1.0 200 OK\r\n" +
					"Server: android-RemoteSMS\r\n" +
					"Content-type: " + content_type + "\r\n\r\n";
		}
		
		
		private void handleRequest(String request, PrintWriter out){
			Log.d(TAG, "Handling: " + request);
			if(request == null){
				Log.w(TAG, "request is null");
				out.close();
				return;
			}
			if(request.equals("/favicon")){
				Log.v(TAG, request + ": 404");
				out.print(HEADER_404);
				out.close();
				return;
			}
			if(request.equals("/")){
				Log.v(TAG, request + ": 200");
				out.print(HEADER_200);
				sendRawResource(out, 
						com.github.fleetc0m.remotesms.R.raw.index);
				out.close();
				return;
			}
			if(request.equals("/index.js")){
				Log.v(TAG, request + ": 200");
				out.print(getHeader200("text/javascript"));
				out.print(generateIndexJs());
				out.close();
				return;
			}
			if(request.equals("/jquery_2_0_3_dev.js")){
				Log.v(TAG, request + ": 200");
				out.print(getHeader200("text/javascript"));
				sendRawResource(out, 
						com.github.fleetc0m.remotesms.R.raw.jquery_2_0_3_dev);
				out.close();
				return;
			}
			if(request.equals("/jquery_2_0_3_min.js")){
				Log.v(TAG, request + ": 200");
				out.print(getHeader200("text/javascript"));
				sendRawResource(out,
						com.github.fleetc0m.remotesms.R.raw.jquery_2_0_3_min);
				out.close();
				return;
			}
			
			
			Log.d(TAG, request + ": 404");
			out.print(HEADER_404);
			out.close();
		}
		
		private String generateIndexJs(){
			String js = "var last_cmd;" +
						"var connection = new WebSocket('ws://" + socketAddress + ":" + socketPort + "');" + 
						"connection.onopen=function(){connection.send('"+SMSWebSocketServer.CMD_GET_ALL_MSG_SENDER+"');"+
														"last_cmd=\""+SMSWebSocketServer.CMD_GET_ALL_MSG_SENDER + "\";};"+
						"connection.onerror=function(e){console.log('websocket error: + e')};";
			return js;
		}
		
		private void sendRawResource(PrintWriter out, int resourceID){
			Log.v(TAG, "sending reply");
			BufferedReader htmlIn = new BufferedReader(
					new InputStreamReader(c.getResources().openRawResource(
							resourceID)));
			String outputLine = null;
			try {
				while((outputLine = htmlIn.readLine()) != null){
					Log.v(TAG, "outbound: " + outputLine);
					out.println(outputLine);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			out.print("\r\n\r\n");
		}
	}
}
