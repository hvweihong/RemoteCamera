package com.hv.wifi;


import java.lang.reflect.Method;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;

public class HPSetup {

	private static final String SETUP_WIFIAP_METHOD = "setWifiApEnabled";
	Context context = null;
	WifiManager wifiManager = null;
	WifiConfiguration netConfig = new WifiConfiguration();
	
	static HPSetup hPaConnector = null;
	

	public static HPSetup getInstance(Context context) {
		if (hPaConnector == null) {
			hPaConnector = new HPSetup();
			hPaConnector.context = context.getApplicationContext();
			hPaConnector.wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		}
		return hPaConnector;
	}

	public void setupWifiAp(String name, String password,boolean status) throws Exception {
		
		
		
		if (name == null || "".equals(name)) {
			throw new Exception("the name of the wifiap is cannot be null");
		}
		
		//若wifi打开，先关闭wifi
		if(wifiManager.isWifiEnabled()){
			wifiManager.setWifiEnabled(false);
		}
		
		Method setupMethod = wifiManager.getClass().getMethod(SETUP_WIFIAP_METHOD, WifiConfiguration.class, boolean.class);

		// 设置wifi热点名称
		netConfig.SSID = name;

		netConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
		netConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
		netConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
		netConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
		netConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
		netConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
		netConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);

		if (password != null) {
			if (password.length() < 8) {
				throw new Exception("the length of wifi password must be 8 or longer");
			}
			// 设置wifi热点密码
			netConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
			netConfig.preSharedKey = password;
		}

		setupMethod.invoke(wifiManager, netConfig, status);
	}
	
	public int checkWifiState() {
		return wifiManager.getWifiState();
	}
	
}
