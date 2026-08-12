package com.lmt.lib.bldt.internal.gui;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;

import com.lmt.lib.bldt.ContentDatabase;
import com.lmt.lib.bldt.ContentDescription;
import com.lmt.lib.bldt.GuiOption;
import com.lmt.lib.bldt.PlayStyle;
import com.lmt.lib.bldt.TableDescription;
import com.lmt.lib.bldt.UpdateProgress;
import com.lmt.lib.bldt.UpdateResult;
import com.lmt.lib.bldt.internal.Texts;

/**
 * 難易度表更新画面
 *
 * @since 0.4.0
 */
public class UpdateDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	/** 難易度表データへのアクセスタイムアウト */
	private static final Duration TIMEOUT = Duration.ofSeconds(60L);

	/** 差分種別：追加 */
	private static final int ADD = 0;
	/** 差分種別：更新 */
	private static final int MODIFY = 1;
	/** 差分種別：削除 */
	private static final int REMOVE = 2;

	/** 動作オプション */
	private GuiOption mOption = null;
	/** 処理対象DB */
	private ContentDatabase mDb;
	/** 処理対象難易度表定義リスト */
	private List<TableDescription> mTargetDescs;
	/** 更新結果ログ出力ストリーム */
	private OutputStream mLogStream;
	/** 更新結果ログWriter */
	private BufferedWriter mLogWriter;
	/** 難易度表更新処理スレッド */
	private Thread mUpdateThread = null;
	/** 更新処理完了フラグ */
	private boolean mFinished = false;

	private JTextArea mUpdateLogText;
	private JButton mShowLogButton;
	private JButton mAbortCloseButton;

	/** ダイアログアダプタ */
	private class DialogAdapter extends WindowAdapter {
		@Override
		public void windowOpened(WindowEvent e) {
			// タイトルを更新する
			var titleFmt = String.format("%s - %%s", Texts.get("gui.update.title"));
			if (mTargetDescs.size() > 1) {
				setTitle(String.format(titleFmt, Texts.get("gui.browse.alldescs")));
			} else {
				var name = mTargetDescs.get(0).getName();
				setTitle(String.format(titleFmt, name));
			}

			// 更新結果ログWriterを開く
			try {
				mLogStream = mOption.openLog();
				mLogWriter = null;
				if (Objects.nonNull(mLogStream)) {
					mLogWriter = new BufferedWriter(new OutputStreamWriter(mLogStream, StandardCharsets.UTF_8));
				}
			} catch (IOException ex) {
				// Don't care
			}

			mShowLogButton.setVisible(mOption.supportShowLog());
			mShowLogButton.setEnabled(false);
			mAbortCloseButton.setEnabled(true);
			mUpdateThread = new Thread(UpdateDialog.this::updateMain, "ldtupdate");
			mUpdateThread.start();
		}

		@Override
		public void windowClosing(WindowEvent e) {
			// 更新処理が完了するまでは×ボタンでのクローズは許可しない
			if (mFinished) {
				UpdateDialog.this.dispose();
			}
		}
	}

	/** ログ表示ボタン押下時処理 */
	private class ShowLogAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				mOption.showLog(UpdateDialog.this);
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}

	/** 中止・閉じるボタン押下時処理 */
	private class AbortCloseAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (mFinished) {
				closeAction();
			} else {
				abortAction();
			}
		}

		/**
		 * 中止処理
		 */
		private void abortAction() {
			mAbortCloseButton.setEnabled(false);
			mUpdateThread.interrupt();
		}

		/**
		 * 閉じる処理
		 */
		private void closeAction() {
			UpdateDialog.this.dispose();
		}
	}

	/** 難易度表更新処理プログレス */
	private class Progress implements UpdateProgress {
		@Override
		public void publish(TableDescription desc, PlayStyle playStyle, int iDesc, int numDesc, Status status) {
			switch (status) {
			case START:
				ui(() -> printf("%s (%s) ... ", desc.getName(), UiUtil.makePlayStyleShort(playStyle)));
				break;
			case DONE:
				ui(() -> printf("%s\n", Texts.get("gui.update.str.updatedone")));
				break;
			case UNNECESSARY:
				ui(() -> printf("%s\n", Texts.get("gui.update.str.updateunnecessary")));
				break;
			case ERROR:
				ui(() -> printf("%s\n", Texts.get("gui.update.str.updateerror")));
				break;
			default:
				break;  // Don't care
			}
		}
	}

	/** 更新差分内容 */
	private static class DeltaContext {
		/** 差分種別 */
		int type;
		/** 楽曲情報 */
		ContentDescription content;
		/** 更新前のレベルインデックス */
		int oldLevelIndex;

		/**
		 * コンストラクタ
		 * @param type 差分種別
		 * @param content 楽曲情報
		 * @param oldLevelIndex 更新前のレベルインデックス
		 */
		DeltaContext(int type, ContentDescription content, int oldLevelIndex) {
			this.type = type;
			this.content = content;
			this.oldLevelIndex = oldLevelIndex;
		}
	}

	/**
	 * 処理対象DB設定
	 * @param db 処理対象DB
	 */
	public void setDatabase(ContentDatabase db) {
		mDb = db;
	}

	/**
	 * 更新対象難易度表定義リスト設定
	 * @param descs 更新対象難易度表定義リスト
	 */
	public void setUpdateTarget(List<TableDescription> descs) {
		mTargetDescs = descs;
	}

	/**
	 * 動作オプション設定
	 * @param option 動作オプション
	 */
	public void setOption(GuiOption option) {
		mOption = option;
	}

	/**
	 * UI処理
	 * @param proc 処理関数
	 */
	private void ui(Runnable proc) {
		EventQueue.invokeLater(proc);
	}

	/**
	 * ログ出力
	 * @param format 出力書式
	 * @param args 出力引数
	 */
	private void printf(String format, Object...args) {
		var msg = String.format(format, args);
		printLog(msg);
		writeLog(msg);
	}

	/**
	 * 難易度表更新画面へのログ出力
	 * @param msg メッセージ内容
	 */
	private void printLog(String msg) {
		mUpdateLogText.append(msg);
	}

	/**
	 * 更新結果ログへのログ出力
	 * @param msg メッセージ内容
	 */
	private void writeLog(String msg) {
		try {
			if (Objects.nonNull(mLogWriter)) {
				mLogWriter.write(msg);
				mLogWriter.flush();
			}
		} catch (IOException e) {
			// Don't care
		}
	}

	/**
	 * 難易度表更新処理メイン
	 */
	private void updateMain() {
		var before = new LinkedHashMap<String, List<ContentDescription>>();
		var results = new LinkedHashMap<String, UpdateResult>();
		try {
			// 更新前のデータを退避しておく
			// 更新が終わった後で更新結果を表示するために、更新前のデータが必要になる
			mTargetDescs.stream().map(TableDescription::getId).forEach(id -> {
				before.put(id, mDb.get(id).all().collect(Collectors.toList()));
			});

			// 難易度表の更新を開始する
			ui(() -> printf("%s\n", Texts.get("gui.update.str.updatestart")));
			var client = HttpClient.newBuilder()
					.connectTimeout(TIMEOUT)
					.followRedirects(HttpClient.Redirect.NORMAL)
					.proxy(ProxySelector.getDefault())
					.build();
			if (mTargetDescs.size() > 1) {
				// 全ての難易度表を更新する
				mDb.update(client, TIMEOUT, new Progress(), results);
			} else {
				// 指定された難易度表を更新する
				var id = mTargetDescs.get(0).getId();
				var result = mDb.update(client, id, TIMEOUT, new Progress());
				results.put(id, result);
			}
		} catch (InterruptedException e) {
			// 中止ボタンによって処理が中断された
			ui(() -> printf("%s\n", Texts.get("gui.update.str.abort")));
		} catch (Exception e) {
			// エラーが発生した場合は処理を中断する
			ui(() -> printf("%s\n", Texts.get("gui.update.str.unexpect")));
		} finally {
			ui(() -> {
				mOption.updateResult(results);
				dumpResult(before);
				mAbortCloseButton.setText(Texts.get("gui.update.close"));
				mAbortCloseButton.setEnabled(true);
				mShowLogButton.setEnabled(mOption.supportShowLog());
				mFinished = true;

				// ログWriterの後始末
				if (Objects.nonNull(mLogStream)) {
					try {
						mOption.closeLog(mLogStream);
					} catch (IOException e) {
						e.printStackTrace();
					} finally {
						mLogStream = null;
						mLogWriter = null;
					}
				}
			});
		}
	}

	/**
	 * 難易度表更新結果出力
	 * @param before 更新前の各難易度表楽曲情報リストマップ
	 */
	private void dumpResult(Map<String, List<ContentDescription>> before) {
		// ヘッダ部を出力する
		printf("\n%s\n", Texts.get("gui.update.str.updateresult"));

		// 難易度表ごとに更新差分をチェックする
		var strAdd = Texts.get("gui.update.str.add");
		var strMod = Texts.get("gui.update.str.mod");
		var strRm = Texts.get("gui.update.str.remove");
		var updated = false;
		var added = new ArrayList<DeltaContext>();
		var modified = new ArrayList<DeltaContext>();
		var removed = new ArrayList<DeltaContext>();
		for (var desc : mTargetDescs) {
			// 更新対象外の難易度表は飛ばす
			var id = desc.getId();
			if (!before.containsKey(id)) {
				continue;
			}

			// 追加・変更・削除された楽曲を抽出する
			var name = desc.getName();
			var beforeList = before.get(id);
			var afterList = mDb.get(id).all().collect(Collectors.toList());
			makeDeltaList(beforeList, afterList, added, modified, removed);

			// 差分がある場合は結果を表示する
			if (!added.isEmpty() || !modified.isEmpty() || !removed.isEmpty()) {
				updated = true;
				printf("%s - %s:%d  %s:%d  %s:%d\n", name, strAdd, added.size(), strMod, modified.size(),
						strRm, removed.size());
				dumpDeltaList(desc, added, strAdd);
				dumpDeltaList(desc, modified, strMod);
				dumpDeltaList(desc, removed, strRm);
			}
		}

		// 更新が全くなかった場合の表示
		if (!updated) {
			printf("%s\n", Texts.get("gui.update.str.updatenone"));
		}
	}

	/**
	 * 更新差分出力
	 * @param desc 難易度表定義
	 * @param list 更新差分リスト
	 * @param prefix 更新内容を表す文字列
	 */
	private void dumpDeltaList(TableDescription desc, List<DeltaContext> list, String prefix) {
		if (!list.isEmpty()) {
			if (list.size() > 50) {
				// 更新差分が多い場合は画面上のログでは省略表示とする
				printLog(String.format("  %s: %s\n", prefix, Texts.get("gui.update.str.omit")));
				list.forEach(dc -> writeLog(makeDeltaText(desc, prefix, dc)));
			} else {
				// 更新差分を全件表示する
				list.forEach(dc -> printf("%s", makeDeltaText(desc, prefix, dc)));
			}
		}
	}

	/**
	 * 差分内容の文字列生成
	 * @param desc 難易度表定義
	 * @param prefix 更新内容を表す文字列
	 * @param dc 更新差分
	 * @return 差分内容の文字列
	 */
	private static String makeDeltaText(TableDescription desc, String prefix, DeltaContext dc) {
		if (dc.type == MODIFY) {
			var curLdtLevel = UiUtil.makeLdtLevel(desc, dc.content);
			return String.format("  %s: %s (%s) %s --- %s --- %s -> %s\n",
					prefix,
					curLdtLevel,
					UiUtil.makePlayStyleShort(dc.content.getPlayStyle()),
					dc.content.getTitle(),
					dc.content.getArtist(),
					UiUtil.makeLdtLevel(desc, dc.content.getPlayStyle(), dc.oldLevelIndex),
					curLdtLevel);
		} else {
			return String.format("  %s: %s (%s) %s --- %s\n",
					prefix,
					UiUtil.makeLdtLevel(desc, dc.content),
					UiUtil.makePlayStyleShort(dc.content.getPlayStyle()),
					dc.content.getTitle(),
					dc.content.getArtist());
		}
	}

	/**
	 * 追加・更新・削除の更新差分生成
	 * @param befores 更新前の楽曲情報リスト(IN)
	 * @param afters 更新後の楽曲情報リスト(IN)
	 * @param added 追加差分リスト(OUT)
	 * @param modified 更新差分リスト(OUT)
	 * @param deleted 削除差分リスト(OUT)
	 */
	private static void makeDeltaList(List<ContentDescription> befores, List<ContentDescription> afters,
			List<DeltaContext> added, List<DeltaContext> modified, List<DeltaContext> deleted) {
		// リスト初期化
		added.clear();
		modified.clear();
		deleted.clear();

		// 追加・変更を検出する
		for (var after : afters) {
			var before = befores.stream().filter(c -> equalsContent(after, c)).findFirst();
			if (before.isPresent()) {
				// 更新前から存在する楽曲(タイトル、アーティスト、プレースタイルが一致する楽曲)
				// 更新の検出はSHA-256またはMD5が登録されている難易度表のみとする。
				// 理由：タイトル・アーティスト等のマッチングでは、同一楽曲の他難易度が誤ってマッチングしてしまうため
				if ((Objects.nonNull(after.getSha256()) || Objects.nonNull(after.getMd5())) &&
						(after.getLevelIndex() != before.get().getLevelIndex())) {
					// 難易度が変更された楽曲
					modified.add(new DeltaContext(MODIFY, after, before.get().getLevelIndex()));
				}
			} else {
				// 更新後リストに新しく登場した楽曲(追加)
				added.add(new DeltaContext(ADD, after, 0));
			}
		}

		// 削除を検出する
		for (var before : befores) {
			if (afters.stream().noneMatch(c -> equalsContent(before, c))) {
				// 更新前は存在したが、更新後に存在しない楽曲(削除)
				deleted.add(new DeltaContext(REMOVE, before, 0));
			}
		}

		// 検出した差分をソートする
		Comparator<DeltaContext> comparator = (d1, d2) -> {
			var cd1 = d1.content;
			var cd2 = d2.content;
			var c = 0;
			if ((c = Integer.compare(cd1.getPlayStyle().ordinal(), cd2.getPlayStyle().ordinal())) != 0) {
				return c;
			} else if ((c = Integer.compare(cd1.getLevelIndex(), cd2.getLevelIndex())) != 0) {
				return c;
			} else if ((c = cd1.getTitle().compareTo(cd2.getTitle())) != 0) {
				return c;
			} else {
				return cd1.getArtist().compareTo(cd2.getArtist());
			}
		};
		added.sort(comparator);
		modified.sort(comparator);
		deleted.sort(comparator);
	}

	/**
	 * 楽曲情報が同一かどうかチェック
	 * @param cd1 楽曲情報1
	 * @param cd2 楽曲情報2
	 * @return 同一であればtrue
	 */
	private static boolean equalsContent(ContentDescription cd1, ContentDescription cd2) {
		var sha256 = cd1.getSha256();
		var md5 = cd1.getMd5();
		if (Objects.nonNull(sha256)) {
			return sha256.equalsIgnoreCase(cd2.getSha256());
		} else if (Objects.nonNull(md5)) {
			return md5.equalsIgnoreCase(cd2.getMd5());
		} else {
			return cd1.getTitle().equals(cd2.getTitle()) &&
					cd1.getArtist().equals(cd2.getArtist()) &&
					(cd1.getPlayStyle() == cd2.getPlayStyle());
		}
	}

	/**
	 * Create the dialog.
	 */
	public UpdateDialog() {
		super();
		initComponents();
	}

	public UpdateDialog(Window owner) {
		super(owner);
		initComponents();
	}

	private void initComponents() {
		setTitle("*gui.update.title*");
		setIconImage(Toolkit.getDefaultToolkit().getImage(BrowseDialog.class.getResource("/com/lmt/lib/bldt/gui_icon.png")));
		setModal(true);
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		setBounds(100, 100, 750, 350);
		setMinimumSize(new Dimension(400, 200));
		addWindowListener(new DialogAdapter());
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{0, 0};
		gridBagLayout.rowHeights = new int[]{0, 0, 0};
		gridBagLayout.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{1.0, 0.0, Double.MIN_VALUE};
		getContentPane().setLayout(gridBagLayout);

		JScrollPane updateLogContainer = new JScrollPane();
		updateLogContainer.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		updateLogContainer.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		GridBagConstraints gbc_updateLogContainer = new GridBagConstraints();
		gbc_updateLogContainer.insets = new Insets(5, 5, 5, 5);
		gbc_updateLogContainer.fill = GridBagConstraints.BOTH;
		gbc_updateLogContainer.gridx = 0;
		gbc_updateLogContainer.gridy = 0;
		getContentPane().add(updateLogContainer, gbc_updateLogContainer);

		mUpdateLogText = new JTextArea();
		mUpdateLogText.setEditable(false);
		updateLogContainer.setViewportView(mUpdateLogText);

		JPanel controlContainer = new JPanel();
		GridBagConstraints gbc_controlContainer = new GridBagConstraints();
		gbc_controlContainer.insets = new Insets(0, 5, 5, 5);
		gbc_controlContainer.fill = GridBagConstraints.BOTH;
		gbc_controlContainer.gridx = 0;
		gbc_controlContainer.gridy = 1;
		getContentPane().add(controlContainer, gbc_controlContainer);
		GridBagLayout gbl_controlContainer = new GridBagLayout();
		gbl_controlContainer.columnWidths = new int[]{0, 0, 0, 0};
		gbl_controlContainer.rowHeights = new int[]{0, 0};
		gbl_controlContainer.columnWeights = new double[]{1.0, 0.0, 0.0, Double.MIN_VALUE};
		gbl_controlContainer.rowWeights = new double[]{0.0, Double.MIN_VALUE};
		controlContainer.setLayout(gbl_controlContainer);

		mShowLogButton = new JButton("*gui.update.showlog*");
		mShowLogButton.addActionListener(new ShowLogAction());
		mShowLogButton.setPreferredSize(new Dimension(100, 25));
		GridBagConstraints gbc_mShowLogButton = new GridBagConstraints();
		gbc_mShowLogButton.insets = new Insets(0, 0, 0, 5);
		gbc_mShowLogButton.gridx = 1;
		gbc_mShowLogButton.gridy = 0;
		controlContainer.add(mShowLogButton, gbc_mShowLogButton);

		mAbortCloseButton = new JButton("*gui.update.abort*");
		mAbortCloseButton.addActionListener(new AbortCloseAction());
		mAbortCloseButton.setPreferredSize(new Dimension(100, 25));
		GridBagConstraints gbc_mAbortCloseButton = new GridBagConstraints();
		gbc_mAbortCloseButton.gridx = 2;
		gbc_mAbortCloseButton.gridy = 0;
		controlContainer.add(mAbortCloseButton, gbc_mAbortCloseButton);

		UiUtil.localizeComponents(this, Texts::get);
	}

}
