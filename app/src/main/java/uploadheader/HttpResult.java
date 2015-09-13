package uploadheader;
/**
 * @author  作者 E-mail: zhuyg
 * @version 创建时间：2013-3-21 上午11:22:53
 * 类说明         Http访问返回结果
 */
public class HttpResult {

	public static final int INTERNET_EXCEPTION = -1 ;
	public static final int INTERNET_SUCCESS = 1 ;
	
	public static final String CONNECT_TIMEOUT = "连接超时" ;
	public static final String INNTERNET_ERROR = "网络异常" ;
	
	private int state ;
	
	private String result ;
	
	private String errorMsg ;

	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	public String getErrorMsg() {
		return errorMsg;
	}

	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}
	
	
}
 