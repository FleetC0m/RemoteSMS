package com.github.fleetc0m.remotesms.server;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.SmsManager;
import android.util.Log;

public class SMSWebSocketServer extends WebSocketServer {

	private static final String TAG = "swss";
	private static final String SMS_INBOX_URI = "content://sms/inbox";
	private static final String SMS_URI = "content://sms";
	private static final String SMS_ADDRESS_COLUMN = "address";
	private static final String SMS_BODY_COLUMN = "body";
	private static final String SMS_TYPE_COLUMN = "type";
	private static final String SMS_DATE_COLUMN = "date";
	private static final String SMS_PERSON_COLUMN = "person";
	private static final String SMS_THREAD_ID_COLUMN = "thread_id";
	public static final String PORT = "port";
	public static final int DEFAULT_PORT = 8889;
	
	public static final String CMD_GET_ALL_MSG_SENDER = "GET ALLNUMBER";
	
	private Context c;
	public SMSWebSocketServer(InetSocketAddress address) {
		super(address);
	}
	
	public SMSWebSocketServer(int port, Context context){
		this(new InetSocketAddress(port));
		this.c = context;
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		Log.d(TAG, "opened");
		/*ArrayList<SMS> list = getAllSMS();
		for(final SMS s : list){
			conn.send("from: " + s.address + " type: " + s.type + " content: " + s.msg);
		}*/
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		Log.d(TAG, "closed: " + reason);
	}

	@Override
	public void onMessage(WebSocket conn, String message) {
		Log.i(TAG, "message: " + message);
		procMessage(conn, message);
	}

	@Override
	public void onError(WebSocket conn, Exception ex) {
		Log.e(TAG, "error: " + ex.getMessage());
	}

	private ArrayList<SMS> getAllSMS(){
		ArrayList<SMS> list = new ArrayList<SMS>();
		Uri uri = Uri.parse(SMS_INBOX_URI);
		Cursor cursor = c.getContentResolver().query(uri, null, null, null, null);
		while(cursor.moveToNext()){
			list.add(new SMS(cursor.getString(cursor.getColumnIndex(SMS_ADDRESS_COLUMN)),
								"",
								cursor.getString(cursor.getColumnIndex(SMS_BODY_COLUMN)),
								cursor.getInt(cursor.getColumnIndex(SMS_TYPE_COLUMN))
								)
			);
		}
		return list;
	}
	
	private synchronized void procMessage(WebSocket conn, String msg){
		if(msg.equals(CMD_GET_ALL_MSG_SENDER)){
			ArrayList<Contact> list = getAllSenders();
			StringBuilder sb = new StringBuilder();
			sb.append("{ \"all_senders\":[");
			for(final Contact c : list){
				if(c.name.equals("")){
					sb.append("{\"name\": \"\", \"number\":\"" +  c.number + "\" },");
				}else{
					sb.append("{\"name\": \"" + c.name + "\", \"number\": \"" + c.number + "\"},");
				}
			}
			sb.setLength(sb.length() - 1);
			sb.append("]}");
			conn.send(sb.toString());
			return;
		}
		if(msg.startsWith("GET ")){
			ArrayList<SMS> msgList = getAllMsgBySender(msg.substring("GET ".length()));
			StringBuilder sb = new StringBuilder();
			sb.append("{\"message_list\":[");
			for(int i = 0; i < msgList.size() && i < 50; i ++){
				sb.append("{\"body\":\"" + msgList.get(i).msg + "\",\"type\":" + msgList.get(i).type + "},");
			}
			sb.setLength(sb.length() - 1);
			sb.append("]}");
			conn.send(sb.toString());
		}
		if(msg.startsWith("SEND")){
			String[] array = msg.split(" ");
			String sender = array[1];
			if(array.length > 3){
				for(int i = 3; i < array.length; i ++){
					array[2] += array[i];
				}
			}
			String body = array[2];
			Log.d(TAG, "Send to " + sender + ": " + body);
			sendSMS(sender, body);
			
			ArrayList<SMS> msgList = getAllMsgBySender(sender);
			StringBuilder sb = new StringBuilder();
			sb.append("{\"message_list\":[");
			for(int i = 0; i < msgList.size() && i < 50; i ++){
				sb.append("{\"body\":\"" + msgList.get(i).msg + "\",\"type\":" + msgList.get(i).type + "},");
			}
			sb.setLength(sb.length() - 1);
			sb.append("]}");
			conn.send(sb.toString());
		}
	}
	private void sendSMS(String phoneNumber, String message)
    {   
		//http://learnandroideasily.blogspot.in/2012/11/how-to-send-sms.html
        String SENT = "SMS_SENT";
        String DELIVERED = "SMS_DELIVERED";
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, message, null, null);        
    }
	
	private ArrayList<SMS> getAllMsgBySender(String sender){
		Log.d(TAG, "getting msg by " + sender);
		ArrayList<SMS> result = new ArrayList<SMS>();
		Uri uri = Uri.parse(SMS_URI);
		Cursor cursor = c.getContentResolver().query(uri, 
													new String[]{SMS_THREAD_ID_COLUMN,
															SMS_ADDRESS_COLUMN,
															SMS_PERSON_COLUMN,
															SMS_BODY_COLUMN, 
															SMS_TYPE_COLUMN}, 
													SMS_ADDRESS_COLUMN + " = ?", 
													new String[]{sender}, 
													SMS_DATE_COLUMN + " DESC");
		if(cursor.getCount() > 0){
			cursor.moveToFirst();
			int threadId = cursor.getInt(cursor.getColumnIndex(SMS_THREAD_ID_COLUMN));
			Uri threadUri = Uri.parse(SMS_URI);
			Cursor threadCursor = c.getContentResolver().query(threadUri, 
					new String[]{SMS_THREAD_ID_COLUMN,
					SMS_ADDRESS_COLUMN,
					SMS_PERSON_COLUMN,
					SMS_BODY_COLUMN,
					SMS_TYPE_COLUMN},
					SMS_THREAD_ID_COLUMN + "= ? ",
					new String[]{Integer.toString(threadId)}, SMS_DATE_COLUMN + " DESC");
			while(threadCursor.moveToNext()){
				Log.d(TAG, "thread id: " + threadCursor.getInt(threadCursor.getColumnIndex(SMS_THREAD_ID_COLUMN))+
						" add: " + threadCursor.getString(threadCursor.getColumnIndex(SMS_ADDRESS_COLUMN)) +
						" person: " + threadCursor.getString(threadCursor.getColumnIndex(SMS_PERSON_COLUMN))+
						" body: " + threadCursor.getString(threadCursor.getColumnIndex(SMS_BODY_COLUMN)) +
						" type: " + threadCursor.getInt(threadCursor.getColumnIndex(SMS_TYPE_COLUMN)));
				result.add(new SMS("", 
						"", 
						threadCursor.getString(threadCursor.getColumnIndex(SMS_BODY_COLUMN)),
						threadCursor.getInt(threadCursor.getColumnIndex(SMS_TYPE_COLUMN))));
			}
			threadCursor.close();
		}
		cursor.close();
		return result;
	}
	
	private ArrayList<Contact> getAllSenders(){
		ArrayList<Contact> list = new ArrayList<Contact>();
		HashMap<String, Boolean> table = new HashMap<String, Boolean>();
		Uri uri = Uri.parse(SMS_INBOX_URI);
		Cursor cursor = c.getContentResolver().query(uri, new String[]{SMS_ADDRESS_COLUMN}, null, null, SMS_DATE_COLUMN + " DESC");
		while(cursor.moveToNext()){
			String sender = cursor.getString(0);
			if(!table.containsKey(sender)){
				table.put(sender, true);
				Uri contactUri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(sender));
				Cursor contactCursor = c.getContentResolver().query(contactUri, new String[]{PhoneLookup.DISPLAY_NAME}, null, null, null);
				if(contactCursor.moveToNext()){
					list.add(new Contact(sender, contactCursor.getString(0)));
				}
				else{
					list.add(new Contact(sender, ""));
				}
				contactCursor.close();
			}
		}
		cursor.close();
		return list;
	}
	
	private static class SMS{
		public String number;
		public String msg;
		public int type;
		public String name;
		public SMS(String number, String name, String msg, int type){
			this.number = number;
			this.msg = msg.replaceAll("\"", "\\\\\"");
			this.type = type;
			this.name = name;
		}
	}
	private static class Contact{
		public String number;
		public String name;
		public Contact(String number, String name){
			this.number = number;
			this.name = name;
		}
	}
}