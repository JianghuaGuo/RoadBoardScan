package network;

import java.io.File;
import java.util.HashMap;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class UploadHeaderRequestTask extends
		AsyncTask<String, Void, HttpResult> {


	private Context context;
	// 请求地址
	private String url="http://182.92.78.232:8000/upload/";


	public UploadHeaderRequestTask(Context context) {
		this.context = context;
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
			switch (result.getState())
			{
				case HttpResult.INTERNET_SUCCESS:
					Toast.makeText(context, "File upload success.",
							Toast.LENGTH_SHORT).show();
					break;
				case HttpResult.INTERNET_EXCEPTION:
					Toast.makeText(context, result.getErrorMsg(),
							Toast.LENGTH_SHORT).show();
					break;
				default:
					break;
			}
		} else {
			Toast.makeText(context, "File upload failed.",
					Toast.LENGTH_SHORT).show();
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
