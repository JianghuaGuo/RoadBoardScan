package network;

import java.io.File;
import java.util.HashMap;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class UploadImageRequestTask extends
		AsyncTask<String, Void, HttpResult> {


	private Context context;
	private HttpResultListener listener;
	// 请求地址
	private String url="http://182.92.78.232:8000/upload/";


	public UploadImageRequestTask(Context context, HttpResultListener listener) {
		this.context = context;
		this.listener = listener;
	}

	@Override
	protected HttpResult doInBackground(
			String... params) {

		HttpResult hr = null;
		// 判断网络是否可用
		if (HTTPHelper.checkNetWorkStatus(context)) {
			String fielPath = params[0];
			Log.e("gjh",fielPath);
			File file = new File(fielPath);
			HashMap<String, String> map = new HashMap<String, String>();
			hr = HTTPHelper.uploadFile(url, file);
			Log.e("gjh","state:" + hr.getState());
			Log.e("gjh","result:" + hr.getResult());
			return hr;
		} else {
			return hr;
		}
	}

	@Override
	protected void onCancelled() {
		 super.onCancelled();
	}

	@Override
	protected void onPostExecute(HttpResult result) {
		super.onPostExecute(result);
		if (result != null)
		{
			if (this.listener != null)
			{
				this.listener.onSuccess(result);
			}
		} else {
			if (this.listener != null)
			{
				this.listener.onFailed(null);
			}
		}
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		if (this.listener != null){
			this.listener.onBefore();
		}
	}

	@Override
	protected void onProgressUpdate(Void... values) {
		super.onProgressUpdate(values);
	}

}
