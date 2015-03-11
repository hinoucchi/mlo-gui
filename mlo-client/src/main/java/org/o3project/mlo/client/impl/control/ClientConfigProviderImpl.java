/**
 * ClientConfigProviderImpl.java
 * (C) 2014, Hitachi Solutions, Ltd.
 */
package org.o3project.mlo.client.impl.control;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.o3project.mlo.client.control.ClientConfigConstants;
import org.seasar.framework.util.ResourceUtil;

import org.o3project.mlo.server.logic.ConfigProvider;

/**
 * MLO のコンフィグレーション提供インタフェースの実装クラスです。
 *
 */
public class ClientConfigProviderImpl implements ConfigProvider, ClientConfigConstants {
	
	private static final Log LOG = LogFactory.getLog(ClientConfigProviderImpl.class);
	
	/**
	 * デフォルトプロパティファイルのURL
	 */
	static final URL DEFAULT_PROPERTIES_FILE_URL
		= ResourceUtil.getResource("default.mlo-client.properties");
	
	private String propertiesFilePath;
	
	private Map<String, String> configMap = null;
	
	private final Object oMutex = new Object();

	/**
	 * 指定された情報にてインスタンスを生成します。
	 * 
	 * @param propertiesFilePath プロパティファイル (デフォルトを上書きする)
	 */
	public ClientConfigProviderImpl(String propertiesFilePath) {
		this.propertiesFilePath = propertiesFilePath;
		configMap = null;
	}

	/* (non-Javadoc)
	 * @see org.o3project.mlo.server.logic.ConfigProvider#getProperties()
	 */
	@Override
	public Map<String, String> getProperties() {
		synchronized (oMutex) {
			if (configMap == null) {
				configMap = Collections.unmodifiableMap(loadConfigMap(propertiesFilePath));
			}
			return configMap;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.o3project.mlo.server.logic.ConfigProvider#getProperty(java.lang.String)
	 */
	@Override
	public String getProperty(String key) {
		synchronized (oMutex) {
			if (configMap == null) {
				configMap = Collections.unmodifiableMap(loadConfigMap(propertiesFilePath));
			}
			return configMap.get(key);
		}
	}

	/* (non-Javadoc)
	 * @see org.o3project.mlo.server.logic.ConfigProvider#getBooleanProperty(java.lang.String)
	 */
	@Override
	public Boolean getBooleanProperty(String key) {
		String prop = getProperty(key);
		return "true".equalsIgnoreCase(prop);
	}
	
	/* (non-Javadoc)
	 * @see org.o3project.mlo.server.logic.ConfigProvider#getIntegerProperty(java.lang.String)
	 */
	@Override
	public Integer getIntegerProperty(String key) {
		String prop = getProperty(key);
		Integer nProp = null;
		try {
			nProp = Integer.parseInt(prop);
		} catch(NumberFormatException e) {
			LOG.warn("Failed to parse Integer.: key=" + key);
		}
		return nProp;
	}

	/* (non-Javadoc)
	 * @see org.o3project.mlo.server.logic.ConfigProvider#getCommaSplitedProperty(java.lang.String)
	 */
	@Override
	public String[] getCommaSplitedProperty(String key) {
		String[] vals = null;
		String prop = getProperty(key);
		if (prop != null) {
			vals = prop.split(",");
		}
		return vals;
	}

	/* (non-Javadoc)
	 * @see org.o3project.mlo.server.logic.ConfigProvider#getSubProperties(java.lang.String)
	 */
	@Override
	public Map<String, String> getSubProperties(String keyPrefix) {
		Map<String, String> subPropMap = new HashMap<String, String>();
		Map<String, String> propMap = getProperties();
		for (Map.Entry<String, String> entry : propMap.entrySet()) {
			if (entry.getKey().startsWith(keyPrefix) && !entry.getKey().equals(keyPrefix)) {
				String newKey = entry.getKey().substring(keyPrefix.length());
				subPropMap.put(newKey, entry.getValue());
			}
		}
		return subPropMap;
	}
	
	private static Map<String, String> loadConfigMap(String propFilePath) {
		Map<String, String> configs = new HashMap<String, String>();
		
		try {
			loadConfigMapFrom(configs, DEFAULT_PROPERTIES_FILE_URL);
		} catch (IOException e) {
			LOG.fatal("Default properties file can not be loaded.", e);
		}
		
		if (propFilePath != null) {
			File propFile = new File(propFilePath);
			if (propFile.exists() && propFile.isFile()) {
				try {
					loadConfigMapFrom(configs, propFilePath);
				} catch (IOException e) {
					LOG.warn("Properties file can not be loaded. : " + propFilePath, e);
				}
			} else {
				LOG.info("Properties file is not specfied. : " + propFilePath);
			}
		} else {
			LOG.warn("Properties file is null.");
		}
		
		return configs;
	}
	
	/**
	 * 指定されたファイルパスからプロパティをロードします。
	 * @param configMap ロード先インスタンス
	 * @param filePath　プロパティファイルパス
	 * @throws IOException 読み込み時異常
	 */
	private static void loadConfigMapFrom(Map<String, String> configMap, String filePath) 
			throws IOException {
		File defaultFile = new File(filePath);
		InputStream istream = null;
		try {
			LOG.info("Loading " + filePath);
			istream = new FileInputStream(defaultFile);
			loadConfigMapFrom(configMap, istream);
		} catch (IOException e) {
			LOG.warn("Failed to load " + defaultFile.getAbsolutePath());
			throw e;
		} finally {
			if (istream != null) {
				istream.close();
				istream = null;
			}
		}
	}
	
	/**
	 * 指定されたURLからプロパティをロードします。
	 * @param configMap ロード先インスタンス
	 * @param url　プロパティファイル URL
	 * @throws IOException 読み込み時異常
	 */
	private static void loadConfigMapFrom(Map<String, String> configMap, URL url) throws IOException {
		InputStream istream = null;
		try {
			LOG.info("Loading " + url);
			istream = url.openStream();
			loadConfigMapFrom(configMap, istream);
		} catch (IOException e) {
			LOG.warn("Failed to load " + url);
			throw e;
		} finally {
			if (istream != null) {
				istream.close();
				istream = null;
			}
		}
	}

	/**
	 * 指定された入力ストリームからプロパティをロードします。
	 * @param configMap ロード先インスタンス
	 * @param istream プロパティファイルの入力ストリーム
	 * @throws IOException 読み込み時異常
	 */
	private static void loadConfigMapFrom(Map<String, String> configMap, InputStream istream) throws IOException {
		Properties props = new Properties();
		props.load(istream);
		for (Object key : props.keySet()) {
			String sKey = (String) key;
			configMap.put(sKey, props.getProperty(sKey));
		}
	}

	/**
	 * コンフィグレーションのデフォルトプロパティファイルを出力するメインメソッドです。 
	 * @param args 引数（未使用）
	 */
	public static void main(String[] args) {
		try {
			String propsFileName = (new File(ClientConfigProviderImpl.DEFAULT_PROPERTIES_FILE_URL.getFile())).getName();
			File propsFile = new File("src/main/resources", propsFileName);
			Map<String, String> props = new LinkedHashMap<String, String>();
			ConfigDefinition.defineProps(props);
			storeProps(propsFile, props);
		} catch (IOException e) {
			LOG.error("Failed to store properties.", e);
		}
	}

	/**
	 * プロパティマップを指定されたファイルに保存します。
	 * @param propsFile プロパティファイル
	 * @param props プロパティマップ
	 * @throws IOException 保存時の異常例外
	 */
	private static void storeProps(File propsFile, Map<String, String> props) throws IOException {
		OutputStreamWriter oswriter = null;
		FileOutputStream fostream = null;
		BufferedWriter bwriter = null;
		
		String[] fileHeaderLines = new String[] {
				"#####################################################################",
				"#       DON'T CHANGE THIS FILE DIRECTLY.",
				"# This file is automatically generated by " + ClientConfigProviderImpl.class.getSimpleName() + " class.",
				"# If you want to change this file, ",
				"# modify the class and generate this file.",
				"#####################################################################",
				"",
		};
		
		try {
			LOG.info("Storing props to " + propsFile.getAbsolutePath());
			fostream = new FileOutputStream(propsFile);
			oswriter = new OutputStreamWriter(fostream, "ISO-8859-1");
			bwriter = new BufferedWriter(oswriter);
			String value = null;
			for (String fileHeaderLine : fileHeaderLines) {
				bwriter.write(fileHeaderLine);
				bwriter.newLine();
			}
			for (String key : props.keySet()) {
				value = props.get(key);
				if (value != null) {
					bwriter.write(String.format("%s=%s", key, value));
					bwriter.newLine();
				}
			}
			bwriter.flush();
			LOG.info("Stored.");
		} finally {
			if (bwriter != null) {
				bwriter.close();
				bwriter = null;
			}
			if (oswriter != null) {
				oswriter.close();
				oswriter = null;
			}
			if (fostream != null) {
				fostream.close();
				fostream = null;
			}
		}
	}
}

/**
 * コンフィグレーションのデフォルト値を定義するクラスです。 
 */
class ConfigDefinition implements ClientConfigConstants {
	
	private ConfigDefinition(){}

	/**
	 * コンフィグレーションのデフォルト値を定義します。
	 * @param props デフォルト値を登録するプロパティマップ
	 */
	static void defineProps(Map<String, String> props) {
		
		/*
		 * 全般
		 */
		props.put(PROP_KEY_DEBUG_CLIENTS, "debugClient");
		/*
		 * MLO-SRV 関連
		 */
		props.put(PROP_KEY_SERVER_BASE_URI, "http://127.0.0.1:8080/DEMO");
		props.put(PROP_KEY_SERVER_CONNECTION_TIMEOUT_SEC, "600");
		props.put(PROP_KEY_SERVER_READ_TIMEOUT_SEC, "600");
		props.put(PROP_KEY_SERVER_SRC_COMPONENT_NAME, CLIENT_TYPE_HITACHI);
		props.put("#" + PROP_KEY_SERVER_SRC_COMPONENT_NAME, CLIENT_TYPE_OTHER);
		
		/*
		 * MLO-SRV 連携用
		 */
		props.put(PROP_KEY_SERVER_DUMMY_INVOKER_SET_FLAG, "false");
		
		props.put(PROP_KEY_SERVER_TOPOLOGY_VIEW_URI, "");
	}
}
