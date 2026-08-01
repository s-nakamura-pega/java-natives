package sn.tools.swing.natives.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class NativeLoader {

	private static final Properties NATIVE_PROPERTIES = new Properties();
	private static final Path NATIVE_PROPERTIES_FILE_PATH = Paths.get("config", "properties", "native.properties");

	// ロード済みキーを保持
	private static final Set<String> LOADED_KEYS = new HashSet<>();

	static {
		try (InputStream in = Files.newInputStream(NATIVE_PROPERTIES_FILE_PATH)) {
			NATIVE_PROPERTIES.load(in);
		} catch (IOException e) {
			throw new RuntimeException("Failed to load native.properties: " + NATIVE_PROPERTIES_FILE_PATH, e);
		}
	}

	/**
	 * 指定 key のネイティブライブラリをロードする（重複ロード防止）
	 */
	public static synchronized void load(String key) {

		// add(key) が true のときだけ処理実行（未ロード）
		if (LOADED_KEYS.add(key)) {

			String value = NATIVE_PROPERTIES.getProperty(key);
			if (value == null) {
				throw new IllegalArgumentException("Native library key not found: " + key);
			}

			Path libPath = Paths.get(value).toAbsolutePath();
			if (!Files.exists(libPath)) {
				throw new RuntimeException("Native library not found: " + libPath);
			}

			System.load(libPath.toString());
		}
	}

	/**
	 * native.properties に記載されたすべてのネイティブライブラリをロードする
	 */
	public static void loadAll() {
		for (String key : NATIVE_PROPERTIES.stringPropertyNames()) {
			load(key);
		}
	}

}
