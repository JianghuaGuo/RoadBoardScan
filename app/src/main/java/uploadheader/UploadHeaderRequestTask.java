package uploadheader;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class UploadHeaderRequestTask extends
		AsyncTask<String, Void, String> {


	private Context context;
	// 请求地址
	private String url="182.92.78.232:8000/upload/";


	public UploadHeaderRequestTask(Context context) {
		this.context = context;
	}

	@Override
	protected String doInBackground(
			String... params) {

		String result = null;

		// 判断网络是否可用
		if (HTTPHelper.checkNetWorkStatus(context)) {
			String fielPath = params[0];
			Log.e("gjh",fielPath);
			File file = new File(fielPath);

			HashMap<String, String> map = new HashMap<String, String>();
			HttpResult hr = HTTPHelper.uploadFile(url, file);
			Log.e("gjh","state:" + hr.getState());
			Log.e("gjh","result:" + hr.getResult());
			switch (hr.getState()) {
			case HttpResult.INTERNET_SUCCESS:
				Toast.makeText(context, "File upload success",
						Toast.LENGTH_SHORT).show();
				break;
			case HttpResult.INTERNET_EXCEPTION:
				Toast.makeText(context, hr.getErrorMsg(),
						Toast.LENGTH_SHORT).show();
				break;
			default:
				break;
			}
			return result;

		} else {

			return result;
		}
	}

	@Override
	protected void onCancelled() {
		 super.onCancelled();
	}

	@Override
	protected void onPostExecute(String result) {
		super.onPostExecute(result);
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
