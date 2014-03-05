/**
 * wechatdonal
 */
package bean;

import java.io.Serializable;

/**
 * wechat
 *
 * @author donal
 *
 */
public class SingleChatMessageEntity implements Serializable{
	public String message_from;
	public String message_to;
	public String message_content;
	public String timestamp;
}
