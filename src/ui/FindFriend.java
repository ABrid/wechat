/**
 * wechatdonal
 */
package ui;

import im.Chating;

import java.util.ArrayList;
import java.util.List;

import service.DLSocketService;
import tools.AppManager;
import tools.Logger;
import tools.UIHelper;
import ui.adapter.FriendCardAdapter;
import ui.adapter.StrangerAdapter;
import xlistview.XListView;
import xlistview.XListView.IXListViewListener;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;
import bean.StrangerEntity;
import bean.UserEntity;
import bean.UserInfo;

import com.donal.wechat.R;

import config.ApiClent;
import config.AppActivity;
import config.CommonValue;
import config.ApiClent.ClientCallback;

/**
 * wechat
 *
 * @author donal
 *
 */
public class FindFriend extends AppActivity implements IXListViewListener{
	private int lvAction;
	private int lvDataState;
	private int currentPage;
	
	private XListView xlistView;
	private List<UserInfo> datas;
	private StrangerAdapter mAdapter;
	
	@Override
	protected void onDestroy() {
		unregisterReceiver(receiver);
		super.onDestroy();
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.findfriend);
		IntentFilter filter = new IntentFilter();
		filter.addAction(CommonValue.SOCKET_ACTION.ALL_USER);
		registerReceiver(receiver, filter);
		initUI();
		Handler jumpHandler = new Handler();
		jumpHandler.postDelayed(new Runnable() {
			public void run() {
				getFriendCardFromCache();
			}
		}, 100);
	}
	
	private void initUI() {
		xlistView = (XListView)findViewById(R.id.xlistview);
		xlistView.setXListViewListener(this, 0);
        xlistView.setRefreshTime();
        xlistView.setPullLoadEnable(false);
        xlistView.setDividerHeight(0);
        datas = new ArrayList<UserInfo>();
		mAdapter = new StrangerAdapter(this, datas);
		xlistView.setAdapter(mAdapter);
	}
	
	private void getFriendCardFromCache() {
		currentPage = 1;
		findFriend(currentPage, "", UIHelper.LISTVIEW_ACTION_REFRESH);
	}
	
	private void findFriend(int page, String nickName, final int action) {
//		String apiKey = appContext.getLoginApiKey();
//		ApiClent.findFriend(appContext, apiKey, page+"", UIHelper.LISTVIEW_COUNT+"", nickName, new ClientCallback() {
//			@Override
//			public void onSuccess(Object data) {
//				StrangerEntity entity = (StrangerEntity)data;
//				switch (entity.status) {
//				case 1:
//					handleFriends(entity, action);
//					break;
//				default:
//					showToast(entity.msg);
//					break;
//				}
//			}
//			
//			@Override
//			public void onFailure(String message) {
//				xlistView.stopLoadMore();
//				xlistView.stopRefresh();
//				showToast(message);
//			}
//			
//			@Override
//			public void onError(Exception e) {
//				xlistView.stopLoadMore();
//				xlistView.stopRefresh();
//			}
//		});
		lvAction = action;
		DLSocketService.emitEvent("alluser", page, UIHelper.LISTVIEW_COUNT);
	}
	
	private void handleFriends(StrangerEntity entity, int action) {
		xlistView.stopLoadMore();
		xlistView.stopRefresh();
		switch (action) {
		case UIHelper.LISTVIEW_ACTION_INIT:
		case UIHelper.LISTVIEW_ACTION_REFRESH:
			datas.clear();
			datas.addAll(entity.userList);
			break;
		case UIHelper.LISTVIEW_ACTION_SCROLL:
			datas.addAll(entity.userList);
			break;
		}
		if(entity.userList.size() == UIHelper.LISTVIEW_COUNT){					
			lvDataState = UIHelper.LISTVIEW_DATA_MORE;
			xlistView.setPullLoadEnable(true);
			mAdapter.notifyDataSetChanged();
		}
		else {
			lvDataState = UIHelper.LISTVIEW_DATA_FULL;
			xlistView.setPullLoadEnable(false);
			mAdapter.notifyDataSetChanged();
		}
		if(datas.isEmpty()){
			lvDataState = UIHelper.LISTVIEW_DATA_EMPTY;
			xlistView.setPullLoadEnable(false);
		}
	}

	@Override
	public void onRefresh(int id) {
		currentPage = 1;
		findFriend(currentPage, "", UIHelper.LISTVIEW_ACTION_REFRESH);
	}

	@Override
	public void onLoadMore(int id) {
		if (lvDataState == UIHelper.LISTVIEW_DATA_EMPTY) {
			findFriend(currentPage, "", UIHelper.LISTVIEW_ACTION_INIT);
		}
		if (lvDataState == UIHelper.LISTVIEW_DATA_MORE) {
			currentPage ++;
			findFriend(currentPage, "", UIHelper.LISTVIEW_ACTION_SCROLL);
		}
	}
	
	@Override
	public void onBackPressed() {
		isExit();
	}
	
	public void show2OptionsDialog(final String[] arg ,final UserInfo model){
//		new AlertDialog.Builder(context).setTitle(null).setItems(arg,
//				new DialogInterface.OnClickListener(){
//			public void onClick(DialogInterface dialog, int which){
//				switch(which){
//				case 0:
//					addFriend(model);
//					break;
//				}
//			}
//		}).show();
		Intent intent = new Intent(context, Chating.class);
		intent.putExtra("to", model.mobile);
		startActivity(intent);
	}
	
	private void addFriend(UserInfo user) {
		ApiClent.addFriend(appContext, appContext.getLoginApiKey(), user.userId, new ClientCallback() {
			
			@Override
			public void onSuccess(Object data) {
				showToast((String)data);
			}
			
			@Override
			public void onFailure(String message) {
				showToast(message);
			}
			
			@Override
			public void onError(Exception e) {
				
			}
		});
	}
	

	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			StrangerEntity entity = (StrangerEntity) intent.getSerializableExtra(CommonValue.SOCKET_EVENT.ALL_USER);
			if (entity.status == 1) {
				handleFriends(entity, lvAction);
			}
			else {
				Logger.i(entity.msg);
			}
		}

	};
}
