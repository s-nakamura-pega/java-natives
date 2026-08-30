package sn.tools.natives.object;

public interface NativeObject extends AutoCloseable {

	/**
	 * ネイティブハンドルIDを返す
	 */
	long getHandleId();

	/**
	 * ネイティブハンドルIDを返す
	 */
	void setHandleId(long handleId);

	/**
	 * close 済みかどうか
	 */
	default boolean isClosed() {
		return getHandleId() == 0;
	}

	/**
	 * 接続中かどうか（isClosed の反転）
	 */
	default boolean isConnected() {
		return !isClosed();
	}

	/**
	 * AutoCloseable の close() は handleId を使う
	 */
	@Override
	void close();

	public static void close(NativeObject object) {
		if (object.isConnected()) {
			object.close();
		}
	}

}
