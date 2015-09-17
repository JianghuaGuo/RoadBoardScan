
package network;

public interface HttpResultListener
{
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
