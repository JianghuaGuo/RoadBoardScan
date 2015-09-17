package network;

import java.io.File;
import java.util.HashMap;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class UploadImageRequestTask extends
		AsyncTask<String, Void, HttpResult> {

	private final String TAG = this.getClass().getSimpleName();
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
		if (HttpHelper.checkNetWorkStatus(context)) {
			String fielPath = params[0];
			Log.i(TAG,fielPath);
			File file = new File(fielPath);

			HashMap<String, String> map = new HashMap<String, String>();
			hr = HttpHelper.uploadFile(url, file, map);
			Log.i(TAG,"state:" + hr.getState());
			Log.i(TAG,"result:" + hr.getResult());
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
	}

	@Override
	protected void onProgressUpdate(Void... values) {
		super.onProgressUpdate(values);
	}

}
