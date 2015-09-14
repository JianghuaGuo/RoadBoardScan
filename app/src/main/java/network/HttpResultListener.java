/**
 * 
 */
package network;

/**
 * Description 发起用户请求 实现该结果来监听请求状态、结果
 * @author zhuyg
 * 2014-3-14 上午11:22:00
 */
public interface HttpResultListener
{
	public void onBefore();
	
	/**
	 * 访问成功
	 * @param result 成功数据
	 */
	public void onSuccess(HttpResult result) ;
	/**
	 * 访问失败
	 * @param msg 失败原因
	 */
	public void onFailed(HttpResult result);
	/**
	 * 获取访问进度
	 * @param progress 进度
	 */
	public void updateProgress(int progress);
	
}
