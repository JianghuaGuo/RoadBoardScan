package network;
public class HttpResult {

	public static final int INTERNET_EXCEPTION = -1 ;
	public static final int INTERNET_SUCCESS = 1 ;
	
	public static final String CONNECT_TIMEOUT = "Connection Timeout" ;
	public static final String INNTERNET_ERROR = "Network Exception" ;	
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
 