package net.mchs_u.mc.aiwolf.nlp.chaser;

import java.util.HashMap;
import java.util.Map;

public class Character {
	public static Map<String, String> getCharacterMap(int number) {
		Map<String, String> c = new HashMap<>();
		switch (number % 5) {
		case 0:
			c.put("こんにちは。", "こんにちは。");
			c.put("僕", "僕");
			c.put("あなた", "きみ");
			c.put("さん", "さん");
			c.put("よ", "よ");
			c.put("ね", "ね");
			c.put("です", "です");
			c.put("います", "いるよ");
			c.put("なのですか", "なんですか");
			c.put("だね", "だね");
			c.put("じゃった", "じゃった");
			c.put("思いますか", "思いますか");
			c.put("かな", "かな");
			break;
		case 1:
			c.put("こんにちは。", "オレが勝つ！");
			c.put("僕", "オレ");
			c.put("あなた", "おまえ");
			c.put("さん", "");
			c.put("よ", "ぜ");
			c.put("ね", "くれ");
			c.put("です", "だ");
			c.put("います", "るぜ");
			c.put("なのですか", "なのか");
			c.put("だね", "だな");
			c.put("じゃった", "でしまった");
			c.put("思いますか", "思う");
			c.put("かな", "か");
			break;
		case 2:
			c.put("こんにちは。", "がんばります！");
			c.put("僕", "わたし");
			c.put("あなた", "あなた");
			c.put("さん", "さん");
			c.put("よ", "わ");
			c.put("ね", "ください");
			c.put("です", "です");
			c.put("います", "います");
			c.put("なのですか", "なのですか");
			c.put("だね", "ですね");
			c.put("じゃった", "じゃいました");
			c.put("思いますか", "思いますか");
			c.put("かな", "でしょうか");
			break;
		case 3:
			c.put("こんにちは。", "よろしくね。");
			c.put("僕", "あたし");
			c.put("あなた", "あなた");
			c.put("さん", "さん");
			c.put("よ", "わ");
			c.put("ね", "ね");
			c.put("です", "だわ");
			c.put("います", "いるわ");
			c.put("なのですか", "なの");
			c.put("だね", "ね");
			c.put("じゃった", "じゃったわ");
			c.put("思いますか", "思いますか");
			c.put("かな", "でしょうか");
			break;
		case 4:
			c.put("こんにちは。", "ぼくが勝つよ！");
			c.put("僕", "ぼく");
			c.put("あなた", "キミ");
			c.put("さん", "さん");
			c.put("よ", "よ");
			c.put("ね", "ね");
			c.put("です", "だよ");
			c.put("います", "いるよ");
			c.put("なのですか", "なの");
			c.put("だね", "だね");
			c.put("じゃった", "じゃった");
			c.put("思いますか", "思いますか");
			c.put("かな", "でしょうか");
			break;
		}
		return c;
	}
}
