/**
 * wechatdonal
 */
package bean;

import java.io.Serializable;

import tools.Logger;

import com.google.gson.Gson;

/**
 * wechat
 *
 * @author donal
 *
 */
public class SingleChatEntity implements Serializable{

	public int status;
	public String msg;
	public SingleChatMessageEntity message;
	
	public static SingleChatEntity parse(String res)  {
		Logger.i(res);
		SingleChatEntity data = new SingleChatEntity();
		Gson gson = new Gson();
		data = gson.fromJson(res, SingleChatEntity.class);
		return data;
	}
}
