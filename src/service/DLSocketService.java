/**
 * wechatdonal
 */
package service;

import java.io.IOException;
import java.net.MalformedURLException;

import org.jivesoftware.smack.packet.Message;
import org.json.JSONObject;

import com.donal.wechat.R;
import com.google.gson.Gson;

import bean.JsonMessage;
import bean.SingleChatEntity;
import bean.StrangerEntity;
import bean.UserEntity;

import tools.AppException;
import tools.Logger;
import ui.Tabbar;

import config.CommonValue;
import config.MessageManager;
import config.NoticeManager;
import im.model.IMMessage;
import im.model.Notice;
import io.socket.IOAcknowledge;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

/**
 * wechat
 *
 * @author donal
 *
 */
public class DLSocketService extends Service implements IOCallback{
	private Context mBus;
	private static SocketIO socket;
	private NotificationManager notificationManager;
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public void onCreate() {
		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mBus = this;
		super.onCreate();
	}
	
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Logger.i("a");
		if (socket == null) {
			try {
				initSocket();
			} catch (MalformedURLException e) {
				Logger.i(e);
			}
		}
		else {
			if (!socket.isConnected()) {
				Logger.i("r");
				try {
					initSocket();
				} catch (MalformedURLException e) {
					Logger.i(e);
				}
			}
		}
		return super.onStartCommand(intent, flags, startId);
	
	}
	
	
	private void initSocket() throws MalformedURLException {
		socket = new SocketIO();
		
		socket.connect(CommonValue.SOCKET_HOST, this);
		
	}
	
	public static void emitEvent(String event, Object... objs) {
		try {
			if (socket.isConnected()) {
				socket.emit(event, objs);
			}
		} catch (Exception e) {
			Logger.i(e);
		}
	}

	@Override
	public void on(String event, IOAcknowledge ack, Object... objs) {
		Intent intent = new Intent();
		if (event.equals(CommonValue.SOCKET_EVENT.LOGIN)) {
			intent.setAction(CommonValue.SOCKET_ACTION.LOGIN);
			try {
				UserEntity entity = UserEntity.parse(objs[0].toString());
				intent.putExtra(CommonValue.SOCKET_EVENT.LOGIN, entity);
				sendBroadcast(intent);
			} catch (Exception e) {
				Logger.i(e);
			}
		}
		else if (event.equals(CommonValue.SOCKET_EVENT.REGISTER)) {
			
		}
		else if (event.equals(CommonValue.SOCKET_EVENT.ALL_USER)) {
			intent.setAction(CommonValue.SOCKET_ACTION.ALL_USER);
			try {
				StrangerEntity entity = StrangerEntity.parse(objs[0].toString());
				intent.putExtra(CommonValue.SOCKET_EVENT.ALL_USER, entity);
				sendBroadcast(intent);
			} catch (Exception e) {
				Logger.i(e);
			}
		}
		else if (event.equals(CommonValue.SOCKET_EVENT.SINGLE_CHAT)) {
			intent.setAction(CommonValue.SOCKET_ACTION.SINGLE_CHAT);
			try {
				ack.ack(true);
				SingleChatEntity entity = SingleChatEntity.parse(objs[0].toString());
				intent.putExtra(CommonValue.SOCKET_EVENT.SINGLE_CHAT, entity);
				handleSingelChatMessage(entity);
			} catch (Exception e) {
				Logger.i(e);
			}
		}
	}

	@Override
	public void onConnect() {
		Logger.i("connect" + socket.getNamespace());
		SharedPreferences sharedPre = mBus.getSharedPreferences(CommonValue.LOGIN_SET, Context.MODE_PRIVATE);
		String USERID = sharedPre.getString(CommonValue.USERID, null);
		emitEvent("active", USERID);
	}

	@Override
	public void onDisconnect() {
		Logger.i("disconnect");
	}

	@Override
	public void onError(SocketIOException e) {
		Logger.i(e);
	}

	@Override
	public void onMessage(String arg0, IOAcknowledge arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMessage(JSONObject arg0, IOAcknowledge arg1) {
		// TODO Auto-generated method stub
		
	}
	
	private void handleSingelChatMessage(SingleChatEntity entity) {
		IMMessage msg = new IMMessage();
		String time = entity.message.timestamp;//(System.currentTimeMillis()/1000)+"";//DateUtil.date2Str(Calendar.getInstance(), Constant.MS_FORMART);
		msg.setTime(time);
		msg.setContent(entity.message.message_content);
//		if (Message.Type.error == message.getType()) {
//			msg.setType(IMMessage.ERROR);
//		} else {
//			msg.setType(IMMessage.SUCCESS);
//		}
		String from = entity.message.message_from;//message.getFrom().split("/")[0];
		msg.setFromSubJid(from);
		NoticeManager noticeManager = NoticeManager
				.getInstance(mBus);
		Notice notice = new Notice();
		notice.setTitle("会话信息");
		notice.setNoticeType(Notice.CHAT_MSG);
		notice.setContent(entity.message.message_content);
		notice.setFrom(from);
		notice.setStatus(Notice.UNREAD);
		notice.setNoticeTime(time);

		IMMessage newMessage = new IMMessage();
		newMessage.setMsgType(0);
		newMessage.setFromSubJid(from);
		newMessage.setContent(entity.message.message_content);
		newMessage.setTime(time);
		MessageManager.getInstance(mBus).saveIMMessage(newMessage);
		long noticeId = -1;

		noticeId = noticeManager.saveNotice(notice);
		if (noticeId != -1) {
			Intent intent = new Intent(CommonValue.NEW_MESSAGE_ACTION);
			intent.putExtra(IMMessage.IMMESSAGE_KEY, msg);
			intent.putExtra("notice", notice);
			sendBroadcast(intent);
			setNotiType(R.drawable.ic_launcher,
					"新消息",
					notice.getContent(), Tabbar.class, from);

		}
	}
	
	private void setNotiType(int iconId, String contentTitle,
			String contentText, Class activity, String from) {
		JsonMessage msg = new JsonMessage();
		Gson gson = new Gson();
		msg = gson.fromJson(contentText, JsonMessage.class);
		Intent notifyIntent = new Intent(this, activity);
		notifyIntent.putExtra("to", from);
		PendingIntent appIntent = PendingIntent.getActivity(this, 0,
				notifyIntent, 0);
		Notification myNoti = new Notification();
		myNoti.flags = Notification.FLAG_AUTO_CANCEL;
		myNoti.icon = iconId;
		myNoti.tickerText = contentTitle;
		myNoti.setLatestEventInfo(this, contentTitle, msg.text, appIntent);
		notificationManager.notify(0, myNoti);
	}
	

}
