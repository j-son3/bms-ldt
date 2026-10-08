package com.lmt.lib.bldt.internal.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.font.TextAttribute;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import com.lmt.lib.bldt.ContentDatabase;
import com.lmt.lib.bldt.ContentDescription;
import com.lmt.lib.bldt.DifficultyTables;
import com.lmt.lib.bldt.GuiOption;
import com.lmt.lib.bldt.PlayStyle;
import com.lmt.lib.bldt.TableDescription;
import com.lmt.lib.bldt.internal.Texts;

/**
 * 難易度表ツール画面
 *
 * @since 0.4.0
 */
public class BrowseDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	/** 動作オプション */
	private GuiOption mOption = null;
	/** DBコントローラ */
	private DbController mCtrl = new DbController();
	/** 楽曲情報テーブルのModel */
	private ContentModel mContentModel = new ContentModel();
	/** 難易度表定義リスト */
	private List<TableDescription> mDescs = DifficultyTables.all().collect(Collectors.toUnmodifiableList());

	private JComboBox<DescriptionItem> mDescCombo;
	private JTextField mSearchWordText;
	private JButton mSearchButton;
	private JButton mOfficialSiteButton;
	private JButton mUpdateButton;
	private JTable mContentTable;

	/** DBコントローラ */
	private class DbController {
		/** 処理対象DB */
		private ContentDatabase mDb = null;
		/** 現在選択中の難易度表ID */
		private String mCurId = "";
		/** 現在検索に使用されている検索ワード */
		private String mCurSearchWord = null;

		/**
		 * 処理対象DB設定
		 * @param db 処理対象DB
		 */
		void setDatabase(ContentDatabase db) {
			mDb = db;
		}

		/**
		 * 検索要否判定
		 * @param id 難易度表ID
		 * @param searchWord 検索ワード
		 * @return 検索が必要であればtrue
		 */
		boolean isQueryNecessary(String id, String searchWord) {
			id = Objects.requireNonNullElse(id, "");
			return Objects.isNull(searchWord) || !id.equals(mCurId) || !searchWord.equalsIgnoreCase(mCurSearchWord);
		}

		/**
		 * 検索実行
		 * @param id 難易度表ID
		 * @param searchWord 検索ワード
		 * @return 検索結果リスト
		 */
		List<ContentItem> query(String id, String searchWord) {
			// 難易度表データベースの検索処理
			id = Objects.requireNonNullElse(id, "");
			var result = new ArrayList<ContentItem>();
			var wordLower = searchWord.toLowerCase();
			var matchAll = wordLower.isEmpty();
			var numDesc = mDescs.size();
			for (var i = 0; i < numDesc; i++) {
				// 検索対象の難易度表かどうか確認する
				var curDesc = mDescs.get(i);
				if (!id.isEmpty() && !id.equals(curDesc.getId())) {
					continue;
				}

				// 検索処理メイン
				var col = mDb.get(curDesc.getId());
				for (var ci = 0; ci < col.getCount(); ci++) {
					var content = col.get(ci);
					// 検索ワード未指定の場合は全件ヒットとする
					if (matchAll) {
						result.add(new ContentItem(i, curDesc, content));
						continue;
					}
					// 難易度表記の検索は完全一致(大小文字区別なし)で行う
					var level = UiUtil.makeLdtLevel(curDesc, content);
					if (level.equalsIgnoreCase(searchWord)) {
						result.add(new ContentItem(i, curDesc, content));
						continue;
					}
					// 検索ワードがタイトルに部分マッチする場合はヒットとする
					var title = content.getTitle().toLowerCase();
					if (title.indexOf(wordLower) != -1) {
						result.add(new ContentItem(i, curDesc, content));
						continue;
					}
					// 検索ワードがアーティストに部分マッチする場合はヒットとする
					var artist = content.getArtist().toLowerCase();
					if (artist.indexOf(wordLower) != -1) {
						result.add(new ContentItem(i, curDesc, content));
						continue;
					}
				}
			}

			// 検索結果を「難易度表」「難易度」「タイトル」の昇順でソートする
			result.sort((o1, o2) -> {
				var c = 0;
				if ((c = Integer.compare(o1.order, o2.order)) != 0) {
					return c;
				} else if ((c = o1.content.getPlayStyle().compareTo(o2.content.getPlayStyle())) != 0) {
					return c;
				} else if ((c = Integer.compare(o1.content.getLevelIndex(), o2.content.getLevelIndex())) != 0) {
					return c;
				} else {
					return o1.content.getTitle().compareToIgnoreCase(o2.content.getTitle());
				}
			});

			// 今回指定の検索条件を保存する
			mCurId = id;
			mCurSearchWord = searchWord;

			return result;
		}
	}

	/** ダイアログアダプタ */
	private class DialogAdapter extends WindowAdapter {
		@Override
		public void windowOpened(WindowEvent e) {
			updateList();
			updateUi();
		}
	}

	/** 難易度表選択時処理 */
	private class DescriptionSelection implements ItemListener {
		@Override
		public void itemStateChanged(ItemEvent e) {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				refreshList();
			}
		}
	}

	/** 検索ワード編集時処理 */
	private class SearchWordChanged implements DocumentListener {
		@Override
		public void insertUpdate(DocumentEvent e) {
			updateUi();
		}

		@Override
		public void removeUpdate(DocumentEvent e) {
			updateUi();
		}

		@Override
		public void changedUpdate(DocumentEvent e) {
			updateUi();
		}
	}

	/** 検索ワードのフィルタ */
	private class SearchWordFilter extends DocumentFilter {
		private int mMaxLength;

		SearchWordFilter(int maxLength) {
			mMaxLength = maxLength;
		}

		@Override
		public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr) throws BadLocationException {
	        if (text != null) {
		        if ((fb.getDocument().getLength() + text.length()) <= mMaxLength) {
		        	// 文字数が上限を超えない場合は既定の処理
		            super.insertString(fb, offset, text, attr);
		        } else {
		            // 超過分を切り取る
		            var trimmedString = text.substring(0, mMaxLength - fb.getDocument().getLength());
		            super.insertString(fb, offset, trimmedString, attr);
		        }
			}
		}

		@Override
		public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
				throws BadLocationException {
	        if (text != null) {
		        // 現在の内容と新しい置換後の文字列の合計が最大数を超えないようにする
		        int available = mMaxLength - (fb.getDocument().getLength() - length);
		        if (available > 0) {
		            var trimmedString = text.length() > available ? text.substring(0, available) : text;
		            super.replace(fb, offset, length, trimmedString, attrs);
		        }
	        }
		}
	}

	/** 検索ボタン押下時処理 */
	private class SearchAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (mSearchButton.isEnabled()) {
				refreshList();
			}
		}
	}

	/** 公式サイトボタン押下時処理 */
	private class OfficialSiteAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				var desc = ((DescriptionItem)mDescCombo.getSelectedItem()).desc;
				var url = desc.getOfficialUrl();
				Desktop.getDesktop().browse(url.toURI());
			} catch (Exception ex) {
				// Don't care
			}
		}
	}

	/** 更新ボタン押下時処理 */
	private class UpdateAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			var msgFormat = Texts.get("gui.msg.confirmldtupdate");
			var msg = msgFormat.replace("{0}", mDescCombo.getSelectedItem().toString());
			var title = Texts.get("gui.browse.title");
			var confirm = JOptionPane.showConfirmDialog(BrowseDialog.this, msg, title, JOptionPane.YES_NO_OPTION);
			if (confirm == JOptionPane.YES_OPTION) {
				var desc = ((DescriptionItem)mDescCombo.getSelectedItem()).desc;
				var update = new UpdateDialog();
				update.setAlwaysOnTop(BrowseDialog.this.isAlwaysOnTop());
				update.setLocationRelativeTo(BrowseDialog.this);
				update.setDatabase(mCtrl.mDb);
				update.setUpdateTarget(Objects.isNull(desc) ? mDescs : List.of(desc));
				update.setOption(mOption);
				update.setVisible(true);
				refreshList();
			}
		}
	}

	/** 楽曲情報リストセル選択時処理 */
	private class ContentCellSelection extends MouseAdapter {
		@Override
		public void mouseClicked(MouseEvent e) {
			var point = e.getPoint();
			var row = mContentTable.rowAtPoint(point);
			var col = mContentTable.columnAtPoint(point);

			var content = mContentModel.getItem(row).content;
			if (col == ContentColumn.DL_BODY.ordinal()) {
				// 楽曲本体のダウンロード
				performDownload(content.getTitle(), "gui.browse.str.body", content.getBodyUrl());
			} else if (col == ContentColumn.DL_ADD.ordinal()) {
				// 差分譜面のダウンロード
				performDownload(content.getTitle(), "gui.browse.str.add", content.getAdditionalUrl());
			} else {
				// Do nothing
			}
		}

		/**
		 * ダウンロード処理実行
		 * @param title タイトル
		 * @param urlTypeId URL種別(本体、差分)
		 * @param url アクセス先URL
		 */
		private void performDownload(String title, String urlTypeId, URL url) {
			var urlTypeName = Texts.get(urlTypeId);

			// URLが未設定の場合は何もしない
			if (Objects.isNull(url)) {
				return;
			}

			// アクセス前の確認メッセージを表示する
			var msgFormat = Texts.get("gui.msg.confirmaccesschart");
			var msg = msgFormat.replace("{0}", title).replace("{1}", urlTypeName);
			var puTitle = Texts.get("gui.browse.title");
			var confirm = JOptionPane.showConfirmDialog(BrowseDialog.this, msg, puTitle, JOptionPane.YES_NO_OPTION);
			if (confirm != JOptionPane.YES_OPTION) {
				return;
			}

			// 対象のURLにアクセスする
			try {
				Desktop.getDesktop().browse(url.toURI());
			} catch (Exception ex) {
				// Don't care
			}
		}
	}

	/** 楽曲情報リストセルの動作イベント */
	private class ContentCellMotion extends MouseAdapter {
		@Override
		public void mouseMoved(MouseEvent e) {
			// マウスカーソルがある行・列を特定する
			var point = e.getPoint();
			var row = mContentTable.rowAtPoint(point);
			var col = mContentTable.columnAtPoint(point);

			// 本体・差分のDL列からアクセス先URLを特定する
			var content = mContentModel.getItem(row).content;
			var url = (URL)null;
			if (col == ContentColumn.DL_BODY.ordinal()) {
				url = content.getBodyUrl();
			} else if (col == ContentColumn.DL_ADD.ordinal()) {
				url = content.getAdditionalUrl();
			}

			// 本体・差分列にカーソルがあり、しかもアクセス先URLがある場合はDL用カーソル、それ以外は通常カーソルにする
            if (Objects.nonNull(url)) {
            	mContentTable.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            } else {
            	mContentTable.setCursor(Cursor.getDefaultCursor());
            }
		}
	}

	/** 楽曲情報リストのレンダラ */
	private static class ContentCellRenderer extends DefaultTableCellRenderer {
		/** リンクスタイルかどうか */
		private boolean mIsLinkStyle;

		/**
		 * コンストラクタ
		 * @param isCenter 中央揃えかどうか
		 * @param isLinkStyle リンクスタイルかどうか
		 */
		ContentCellRenderer(boolean isCenter, boolean isLinkStyle) {
			setHorizontalAlignment(isCenter ? SwingConstants.CENTER : SwingConstants.LEADING);
			mIsLinkStyle = isLinkStyle;
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			var label = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if (mIsLinkStyle) {
				// リンクスタイルのセルの場合は青字・下線表示とする
				var font = label.getFont();
				var attr = Map.of(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
				label.setFont(font.deriveFont(attr));
				label.setForeground(Color.BLUE);
			}
			return label;
		}
	}

	/** 楽曲情報リストModel */
	private static class ContentModel extends AbstractTableModel {
		/** 楽曲情報リスト */
		private List<ContentItem> mItems = Collections.emptyList();

		@Override
		public int getColumnCount() {
			return ContentColumn.ALL.size();
		}

		@Override
		public String getColumnName(int column) {
			return ContentColumn.ALL.get(column).getName();
		}

		@Override
		public int getRowCount() {
			return mItems.size();
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			var item = mItems.get(rowIndex);
			return ContentColumn.ALL.get(columnIndex).getValue(item);
		}

		/**
		 * 楽曲情報リスト設定
		 * @param items 楽曲情報リスト
		 */
		void setItems(List<ContentItem> items) {
			mItems = List.copyOf(items);
		}

		/**
		 * 楽曲情報取得
		 * @param index インデックス
		 * @return 楽曲情報
		 */
		ContentItem getItem(int index) {
			return mItems.get(index);
		}
	}

	/** 楽曲情報 */
	private static class ContentItem {
		/** ソート順を決定するための値 */
		final int order;
		/** 難易度表定義 */
		final TableDescription desc;
		/** 楽曲情報 */
		final ContentDescription content;

		/**
		 * コンストラクタ
		 * @param order ソート順を決定するための値
		 * @param desc 難易度表定義
		 * @param content 楽曲情報
		 */
		ContentItem(int order, TableDescription desc, ContentDescription content) {
			this.order = order;
			this.desc = desc;
			this.content = content;
		}
	}

	/** 楽曲情報リストの列定義 */
	private enum ContentColumn {
		/** 難易度表名 */
		TABLE_NAME(
				"gui.browse.list.tablename",
				160,
				false,
				false,
				false,
				i -> i.desc.getName()),
		/** レベル */
		LEVEL(
				"gui.browse.list.level",
				60,
				false,
				true,
				false,
				i -> UiUtil.makeLdtLevel(i.desc, i.content)),
		/** タイトル */
		TITLE(
				"gui.browse.list.title",
				290,
				true,
				false,
				false,
				i -> i.content.getTitle()),
		/** アーティスト */
		ARTIST(
				"gui.browse.list.artist",
				240,
				true,
				false,
				false,
				i -> i.content.getArtist()),
		/** プレースタイル */
		STYLE(
				"gui.browse.list.style",
				50,
				false,
				true,
				false,
				i -> (i.content.getPlayStyle() == PlayStyle.SINGLE) ? "SP" : "DP"),
		/** 本体DL */
		DL_BODY(
				"gui.browse.list.dlbody",
				50,
				false,
				true,
				true,
				i -> Objects.isNull(i.content.getBodyUrl()) ? "" : "DL"),
		/** 差分DL */
		DL_ADD(
				"gui.browse.list.dladd",
				50,
				false,
				true,
				true,
				i -> Objects.isNull(i.content.getAdditionalUrl()) ? "" : "DL");

		/** 全ての列定義リスト */
		static final List<ContentColumn> ALL = Stream.of(values()).collect(Collectors.toUnmodifiableList());

		/** 列名ID */
		private String mNameId;
		/** 初期列幅 */
		private int mDefaultWidth;
		/** 列幅変更可否 */
		private boolean mIsResizeable;
		/** 中央揃えかどうか */
		private boolean mIsCenter;
		/** リンクスタイルかどうか */
		private boolean mIsLinkStyle;
		/** 値取得関数 */
		private Function<ContentItem, Object> mValueGetter;

		/**
		 * コンストラクタ
		 * @param nameId 列名ID
		 * @param defaultWidth 初期列幅
		 * @param isResizeable 列幅変更可否
		 * @param isCenter 中央揃えかどうか
		 * @param isLinkStyle リンクスタイルかどうか
		 * @param valueGetter 値取得関数
		 */
		private ContentColumn(String nameId, int defaultWidth, boolean isResizeable, boolean isCenter, boolean isLinkStyle,
				Function<ContentItem, Object> valueGetter) {
			mNameId = nameId;
			mDefaultWidth = defaultWidth;
			mIsResizeable = isResizeable;
			mIsCenter = isCenter;
			mIsLinkStyle = isLinkStyle;
			mValueGetter = valueGetter;
		}

		/**
		 * 列名取得
		 * @return 列名
		 */
		String getName() {
			return Texts.get(mNameId);
		}

		/**
		 * 初期列幅取得
		 * @return 初期列幅
		 */
		int getDefaultWidth() {
			return mDefaultWidth;
		}

		/**
		 * 列幅変更可否取得
		 * @return 列幅変更可否
		 */
		boolean isResizeable() {
			return mIsResizeable;
		}

		/**
		 * 中央揃えかどうか取得
		 * @return 中央揃えならtrue
		 */
		boolean isCenter() {
			return mIsCenter;
		}

		/**
		 * リンクスタイルかどうか取得
		 * @return リンクスタイルならtrue
		 */
		boolean isLinkStyle() {
			return mIsLinkStyle;
		}

		/**
		 * 列の値取得
		 * @param item 楽曲情報
		 * @return 列の値
		 */
		Object getValue(ContentItem item) {
			return mValueGetter.apply(item);
		}
	}

	/** 難易度表定義項目 */
	private static class DescriptionItem {
		/** 難易度表定義 */
		final TableDescription desc;

		/**
		 * コンストラクタ
		 * @param desc 難易度表定義
		 */
		DescriptionItem(TableDescription desc) {
			this.desc = desc;
		}

		/**
		 * 難易度表ID取得
		 * @return 難易度表ID。「全て」の場合null。
		 */
		String getId() {
			return Objects.isNull(this.desc) ? "" : this.desc.getId();
		}

		@Override
		public String toString() {
			return Objects.isNull(this.desc) ? Texts.get("gui.browse.alldescs") : this.desc.getName();
		}

		/**
		 * 難易度表定義項目の配列取得
		 * @param descs 難易度表定義リスト
		 * @return 難易度表定義項目の配列
		 */
		static DescriptionItem[] values(List<TableDescription> descs) {
			return Stream.concat(Stream.of(new DescriptionItem(null)), descs.stream().map(DescriptionItem::new))
					.toArray(DescriptionItem[]::new);
		}
	}

	/**
	 * 処理対象DB設定
	 * @param db 処理対象DB
	 */
	public void setDatabase(ContentDatabase db) {
		mCtrl.setDatabase(db);
	}

	/**
	 * 動作オプション設定
	 * @param option 動作オプション
	 */
	public void setOption(GuiOption option) {
		mOption = option;
	}

	/**
	 * 楽曲情報リストリフレッシュ
	 */
	private void refreshList() {
		updateList();
		updateUi();
		mSearchWordText.setSelectionStart(0);
		mSearchWordText.setSelectionEnd(mSearchWordText.getText().length());
		if (mContentModel.getRowCount() > 0) {
			SwingUtilities.invokeLater(() -> {
			    var cellRect = mContentTable.getCellRect(0, 0, true);
			    var viewport = (JViewport)mContentTable.getParent();
			    viewport.setViewPosition(new Point(cellRect.x, cellRect.y));
			});
		}
	}

	/**
	 * 楽曲情報リスト更新
	 */
	private void updateList() {
		try {
			// 検索条件に該当する楽曲情報を抽出してリストに反映する
			getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			var descItem = ((DescriptionItem)mDescCombo.getSelectedItem());
			var searchWord = mSearchWordText.getText().stripTrailing();
			var items = mCtrl.query(descItem.getId(), searchWord);
			mContentModel.setItems(items);
			mContentModel.fireTableDataChanged();
		} finally {
			getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
	}

	/**
	 * 全体UI更新
	 */
	private void updateUi() {
		var descItem = ((DescriptionItem)mDescCombo.getSelectedItem());
		var searchWord = mSearchWordText.getText().stripLeading();
		var searchable = mCtrl.isQueryNecessary(descItem.getId(), searchWord);
		mSearchButton.setEnabled(searchable);
		mOfficialSiteButton.setEnabled(Objects.nonNull(descItem.desc));
		mOfficialSiteButton.setVisible(mOption.supportOfficialSite());
		mUpdateButton.setVisible(mOption.supportUpdate());
	}

	/**
	 * UI初期化
	 */
	private void initUi() {
		var header = mContentTable.getTableHeader();
		var cmodel = header.getColumnModel();
		header.setResizingAllowed(true);
		header.setReorderingAllowed(false);
		for (var c : ContentColumn.ALL) {
			var width = c.getDefaultWidth();
			var column = cmodel.getColumn(c.ordinal());
			column.setPreferredWidth(width);
			column.setCellRenderer(new ContentCellRenderer(c.isCenter(), c.isLinkStyle()));
			column.setResizable(c.isResizeable());
		}

		UiUtil.localizeComponents(this, Texts::get);
	}

	/**
	 * Create the dialog.
	 */
	public BrowseDialog() {
		super();
		initComponents();
	}

	public BrowseDialog(Window owner) {
		super(owner);
		initComponents();
	}

	private void initComponents() {
		setTitle("*gui.browse.title*");
		setIconImage(Toolkit.getDefaultToolkit().getImage(BrowseDialog.class.getResource("/com/lmt/lib/bldt/gui_icon.png")));
		setModal(true);
		setMinimumSize(new Dimension(780, 190));
		addWindowListener(new DialogAdapter());
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 950, 600);
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{0, 0};
		gridBagLayout.rowHeights = new int[]{0, 0, 0};
		gridBagLayout.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		getContentPane().setLayout(gridBagLayout);

		JPanel controlPanel = new JPanel();
		GridBagConstraints gbc_controlPanel = new GridBagConstraints();
		gbc_controlPanel.insets = new Insets(5, 5, 5, 5);
		gbc_controlPanel.fill = GridBagConstraints.BOTH;
		gbc_controlPanel.gridx = 0;
		gbc_controlPanel.gridy = 0;
		getContentPane().add(controlPanel, gbc_controlPanel);
		GridBagLayout gbl_controlPanel = new GridBagLayout();
		gbl_controlPanel.columnWidths = new int[]{200, 170, 0, 0, 0, 0, 0};
		gbl_controlPanel.rowHeights = new int[]{0, 0};
		gbl_controlPanel.columnWeights = new double[]{0.0, 0.0, 0.0, 1.0, 0.0, 0.0, Double.MIN_VALUE};
		gbl_controlPanel.rowWeights = new double[]{0.0, Double.MIN_VALUE};
		controlPanel.setLayout(gbl_controlPanel);

		mDescCombo = new JComboBox<>();
		mDescCombo.setMinimumSize(new Dimension(200, 25));
		mDescCombo.setPreferredSize(new Dimension(200, 25));
		mDescCombo.setModel(new DefaultComboBoxModel<>(DescriptionItem.values(mDescs)));
		mDescCombo.addItemListener(new DescriptionSelection());
		GridBagConstraints gbc_mDescCombo = new GridBagConstraints();
		gbc_mDescCombo.insets = new Insets(0, 0, 0, 5);
		gbc_mDescCombo.fill = GridBagConstraints.HORIZONTAL;
		gbc_mDescCombo.gridx = 0;
		gbc_mDescCombo.gridy = 0;
		controlPanel.add(mDescCombo, gbc_mDescCombo);

		mSearchWordText = new JTextField();
		mSearchWordText.setMinimumSize(new Dimension(170, 25));
		mSearchWordText.addActionListener(new SearchAction());
		mSearchWordText.getDocument().addDocumentListener(new SearchWordChanged());
		mSearchWordText.setPreferredSize(new Dimension(170, 25));
		((AbstractDocument)mSearchWordText.getDocument()).setDocumentFilter(new SearchWordFilter(100));
		GridBagConstraints gbc_mSearchWordText = new GridBagConstraints();
		gbc_mSearchWordText.insets = new Insets(0, 0, 0, 5);
		gbc_mSearchWordText.fill = GridBagConstraints.HORIZONTAL;
		gbc_mSearchWordText.gridx = 1;
		gbc_mSearchWordText.gridy = 0;
		controlPanel.add(mSearchWordText, gbc_mSearchWordText);
		mSearchWordText.setColumns(10);

		mSearchButton = new JButton("*gui.browse.button.search*");
		mSearchButton.setMinimumSize(new Dimension(90, 25));
		mSearchButton.addActionListener(new SearchAction());
		mSearchButton.setPreferredSize(new Dimension(90, 25));
		GridBagConstraints gbc_mSearchButton = new GridBagConstraints();
		gbc_mSearchButton.fill = GridBagConstraints.HORIZONTAL;
		gbc_mSearchButton.insets = new Insets(0, 0, 0, 5);
		gbc_mSearchButton.gridx = 2;
		gbc_mSearchButton.gridy = 0;
		controlPanel.add(mSearchButton, gbc_mSearchButton);

		mOfficialSiteButton = new JButton("*gui.browse.button.officialsite*");
		mOfficialSiteButton.addActionListener(new OfficialSiteAction());
		mOfficialSiteButton.setPreferredSize(new Dimension(100, 25));
		GridBagConstraints gbc_mOfficialSiteButton = new GridBagConstraints();
		gbc_mOfficialSiteButton.fill = GridBagConstraints.HORIZONTAL;
		gbc_mOfficialSiteButton.insets = new Insets(0, 0, 0, 5);
		gbc_mOfficialSiteButton.gridx = 4;
		gbc_mOfficialSiteButton.gridy = 0;
		controlPanel.add(mOfficialSiteButton, gbc_mOfficialSiteButton);

		mUpdateButton = new JButton("*gui.browse.button.update*");
		mUpdateButton.setMinimumSize(new Dimension(90, 25));
		mUpdateButton.addActionListener(new UpdateAction());
		mUpdateButton.setPreferredSize(new Dimension(90, 25));
		GridBagConstraints gbc_mUpdateButton = new GridBagConstraints();
		gbc_mUpdateButton.fill = GridBagConstraints.HORIZONTAL;
		gbc_mUpdateButton.gridx = 5;
		gbc_mUpdateButton.gridy = 0;
		controlPanel.add(mUpdateButton, gbc_mUpdateButton);

		JPanel tablePanel = new JPanel();
		GridBagConstraints gbc_tablePanel = new GridBagConstraints();
		gbc_tablePanel.insets = new Insets(0, 5, 5, 5);
		gbc_tablePanel.fill = GridBagConstraints.BOTH;
		gbc_tablePanel.gridx = 0;
		gbc_tablePanel.gridy = 1;
		getContentPane().add(tablePanel, gbc_tablePanel);
		GridBagLayout gbl_tablePanel = new GridBagLayout();
		gbl_tablePanel.columnWidths = new int[]{0, 0};
		gbl_tablePanel.rowHeights = new int[]{0, 0};
		gbl_tablePanel.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gbl_tablePanel.rowWeights = new double[]{1.0, Double.MIN_VALUE};
		tablePanel.setLayout(gbl_tablePanel);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		GridBagConstraints gbc_scrollPane = new GridBagConstraints();
		gbc_scrollPane.fill = GridBagConstraints.BOTH;
		gbc_scrollPane.gridx = 0;
		gbc_scrollPane.gridy = 0;
		tablePanel.add(scrollPane, gbc_scrollPane);

		mContentTable = new JTable();
		mContentTable.addMouseListener(new ContentCellSelection());
		mContentTable.addMouseMotionListener(new ContentCellMotion());
		mContentTable.setRowSelectionAllowed(false);
		mContentTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		mContentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		mContentTable.setModel(mContentModel);
		scrollPane.setViewportView(mContentTable);

		initUi();
	}

}
