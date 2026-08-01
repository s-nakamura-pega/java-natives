package sn.tools.swing.natives.object;

/**
 * ネイティブ構造体と 1 対 1 で対応するオブジェクトの抽象基底クラス。
 *
 * <p>
 * このクラスはコンストラクタ内で {@link #connect()} を呼び出し、 ネイティブ側の構造体を生成してハンドルIDを確定させる。
 * connect() は内部処理であり、外部から呼び出すことは想定していない。
 *
 * <p>
 * サブクラスは connect() を public に公開してはならない。 connect() はネイティブ構造体の生成処理であり、 Java
 * オブジェクト生成時に一度だけ呼ばれるべきである。
 *
 * <p>
 * この設計により、Java オブジェクトとネイティブ構造体の ライフサイクルが完全に同期し、1 対 1 の対応が保証される。
 */
public abstract class AbstractNativeObject implements NativeObject {

	/** ネイティブ構造体のハンドルID */
	protected final long handleId;

	public AbstractNativeObject() {
		handleId = connect();
	}

	@Override
	public long getHandleId() {
		return handleId;
	}

	/**
	 * ネイティブ構造体を生成し、そのハンドルIDを返す。 <br>
	 * サブクラスは native 実装を提供する。
	 *
	 * <p>
	 * 外部から呼び出すことは想定していない。 <br>
	 * コンストラクタで一度だけ呼ばれる。
	 */
	protected abstract long connect();

}
