package sn.tools.swing.natives.object;

public interface NativeObject extends AutoCloseable {

	/**
	 * ネイティブハンドルIDを返す
	 */
	long getHandleId();

	/**
	 * close 済みかどうか
	 */
	boolean isClosed(long handleId);

	/**
	 * 接続中かどうか（isClosed の反転）
	 */
	default boolean isConnecting(long handleId) {
		return !isClosed(handleId);
	}

	/**
	 * ハンドルIDを指定して close
	 */
	void close(long handleId);

	/**
	 * AutoCloseable の close() は handleId を使う
	 */
	@Override
	default void close() {
		close(getHandleId());
	}

	public static void close(NativeObject object) {
		if (object.isConnecting(object.getHandleId())) {
			object.close();
		}
	}

}
