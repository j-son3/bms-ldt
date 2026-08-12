package com.lmt.lib.bldt.internal.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Frame;
import java.util.function.BiConsumer;
import java.util.function.UnaryOperator;

import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JToolTip;
import javax.swing.text.JTextComponent;

import com.lmt.lib.bldt.ContentDescription;
import com.lmt.lib.bldt.PlayStyle;
import com.lmt.lib.bldt.TableDescription;

/**
 * UI関連ユーティリティ
 */
class UiUtil {
	/**
	 * レベル表記生成
	 * @param desc 難易度表定義
	 * @param content 楽曲情報
	 * @return レベル表記
	 */
	static String makeLdtLevel(TableDescription desc, ContentDescription content) {
		return makeLdtLevel(desc, content.getPlayStyle(), content.getLevelIndex());
	}

	/**
	 * レベル表記生成
	 * @param tableDesc 難易度表定義
	 * @param playStyle プレースタイル
	 * @param levelIndex レベルインデックス
	 * @return レベル表記
	 */
	static String makeLdtLevel(TableDescription tableDesc, PlayStyle playStyle, int levelIndex) {
		var psDesc = tableDesc.getPlayStyleDescription(playStyle);
		var symbol = psDesc.getSymbol();
		var label = psDesc.getLabels().get(levelIndex);
		return symbol + label;
	}

	/**
	 * プレースタイルの短表記生成
	 * @param playStyle プレースタイル
	 * @return プレースタイルの短表記
	 */
	static String makePlayStyleShort(PlayStyle playStyle) {
		return (playStyle == PlayStyle.SINGLE) ? "SP" : "DP";
	}

	/**
	 * コンポーネントのローカライズ
	 * @param component 処理対象コンポーネント
	 * @param getText テキスト取得関数
	 */
	static void localizeComponents(Component component, UnaryOperator<String> getText) {
		// 自身のテキストをローカライズする
		if (component instanceof AbstractButton) {
			var target = (AbstractButton)component;
			localizeText(target, getText, target.getText(), (c, t) -> { c.setText(t); });
		} else if (component instanceof JLabel) {
			var target = (JLabel)component;
			localizeText(target, getText, target.getText(), (c, t) -> { c.setText(t); });
		} else if (component instanceof JTextComponent) {
			var target = (JTextComponent)component;
			localizeText(target, getText, target.getText(), (c, t) -> { c.setText(t); });
		} else if (component instanceof JToolTip) {
			var target = (JToolTip)component;
			localizeText(target, getText, target.getTipText(), (c, t) -> { c.setTipText(t); });
		} else if (component instanceof Dialog) {
			var target = (Dialog)component;
			localizeText(target, getText, target.getTitle(), (c, t) -> { c.setTitle(t); });
		} else if (component instanceof Frame) {
			var target = (Frame)component;
			localizeText(target, getText, target.getTitle(), (c, t) -> { c.setTitle(t); });
		}

		// 子コンポーネントを持っている場合は全てにローカライズを適用する
		var components = (Component[])null;
		if (component instanceof JMenu) {
			components = ((JMenu)component).getMenuComponents();
		} else if (component instanceof Container) {
			components = ((Container)component).getComponents();
		}
		if (components != null) {
			for (var child : components) { localizeComponents(child, getText); }
		}
	}

	/**
	 * コンポーネントのローカライズ(共通処理)
	 * @param <C> コンポーネント型
	 * @param component 処理対象コンポーネント
	 * @param getText テキスト取得関数
	 * @param text 処理対象コンポーネントが持つテキスト
	 * @param setter 処理対象コンポーネントへのテキスト設定関数
	 */
	private static <C extends Component> void localizeText(C component, UnaryOperator<String> getText,
			String text, BiConsumer<C, String> setter) {
		if ((text != null) && text.startsWith("*") && text.endsWith("*")) {
			var key = text.substring(1, text.length() - 1);
			var localizedText = getText.apply(key);
			setter.accept(component, localizedText);
		}
	}
}
