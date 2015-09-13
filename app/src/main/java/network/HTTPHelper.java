package network;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.http.conn.ConnectTimeoutException;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;

public class HTTPHelper {


	public static HttpResult uploadFile(String path, File file) {
		HttpResult hr = new HttpResult();
		HttpURLConnection conn = null;
		try {
			String BOUNDARY = "---------------------------7db1c523809b2";// 数据分割线
			// 仿Http协议发送数据方式进行拼接
			StringBuilder sb = new StringBuilder();
			sb.append("--" + BOUNDARY + "\r\n");
			sb.append("Content-Disposition: form-data; name=\"photo\"; filename=\"" + file.getName() + "\"" + "\r\n");
			sb.append("Content-Type: image/jpeg" + "\r\n");
			sb.append("\r\n");

			byte[] before = sb.toString().getBytes("UTF-8");
			byte[] after = ("\r\n--" + BOUNDARY + "--\r\n").getBytes("UTF-8");

			URL url = new URL(path);

			conn = (HttpURLConnection) url.openConnection();

			conn.setRequestMethod("POST");
			conn.setUseCaches(false);
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
			conn.setRequestProperty("Content-Length", String.valueOf(before.length + file.length() + after.length));

			OutputStream out = conn.getOutputStream();

			out.write(before);

			if (!TextUtils.isEmpty(file.getName())) { //用户可能不上传头像
				InputStream in = new FileInputStream(file);
				byte[] buf = new byte[1024];
				int len;
				while ((len = in.read(buf)) != -1)
					out.write(buf, 0, len);
				in.close();
			}
			out.write(after);
			out.close();
			conn.connect();
			InputStream inStream = conn.getInputStream();
			BufferedReader input = new BufferedReader(new InputStreamReader(inStream));
			StringBuilder response = new StringBuilder();
			String oneLine;
			while ((oneLine = input.readLine()) != null) {
				response.append(oneLine);
			}
			hr.setResult(response.toString());
			hr.setState(HttpResult.INTERNET_SUCCESS);
		} catch (ConnectTimeoutException e) {
			hr.setResult(null);
			hr.setState(HttpResult.INTERNET_EXCEPTION);
			hr.setErrorMsg(HttpResult.CONNECT_TIMEOUT);
		} catch (Exception e) {
			hr.setResult(null);
			hr.setState(HttpResult.INTERNET_EXCEPTION);
			hr.setErrorMsg(HttpResult.INNTERNET_ERROR);
			return hr;
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
		return hr;
	}


	/**
	 * 检查是否连接了网络
	 * 
	 * @param context
	 * @return
	 */
	public static boolean checkNetWorkStatus(Context context) {
		boolean result;
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netinfo = cm.getActiveNetworkInfo();
		if (netinfo != null && netinfo.isConnected()) {
			result = true;
		} else {
			result = false;
		}
		return result;
	}

}
