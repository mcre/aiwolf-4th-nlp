package net.mchs_u.mc.aiwolf.nlp.chaser;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aiwolf.client.lib.ComingoutContentBuilder;
import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.DivinedResultContentBuilder;
import org.aiwolf.client.lib.RequestContentBuilder;
import org.aiwolf.client.lib.VoteContentBuilder;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.net.GameInfo;

import net.mchs_u.mc.aiwolf.dokin.McrePlayer;

public class Ear{
	private static final String DAT_FILE = "dic/translatedMap.dat";

	private Map<String, List<String>> translatedMap = null;
	private Set<String> processedTalks = null; // このゲームで処理済みのtalk
	private Map<String, String> qas = null; // Mouthに渡すQA集
	private boolean messageOff = false;
	
	@SuppressWarnings("unused")
	private McrePlayer player = null;

	public Ear(McrePlayer player) {
		translatedMap = load();
		this.player = player;
	}
	
	public void initialize() {
		processedTalks = new HashSet<>();
	}
	
	public void dayStart() {
		qas = new HashMap<>();
	}
	
	public List<String> toProtocolsForTalk(GameInfo gameInfo, Agent talker, String naturalLanguage) {
		String key = talker + ":" + naturalLanguage;
		
		List<String> ret = new ArrayList<>();
		try {			
			Agent questionTo = null;
			if(naturalLanguage.startsWith(">>Agent["))
				questionTo = Agent.getAgent(Integer.parseInt(naturalLanguage.substring(8, 10)));
			
			if(translatedMap.containsKey(key)) { // 履歴にある場合
				if(questionTo != gameInfo.getAgent() || processedTalks.contains(key)) { // 自分宛ての問いかけでない場合か、このゲームの処理済み履歴にある場合
					return translatedMap.get(key); // 履歴から返す
				}
			} else if(naturalLanguage.contains(Talk.SKIP)) {
				ret.add(Talk.SKIP);
				return ret;
			} else if(naturalLanguage.contains(Talk.OVER)) {
				ret.add(Talk.OVER);
				return ret;
			}
			
			if(!messageOff)
				System.out.println("　✩Parse("+ gameInfo.getAgent() +")> " + key);
			
			String nl = naturalLanguage;
			
			nl = nl.replaceFirst("^>>Agent\\[..\\] ", "");
			nl = hankakuToZenkaku(nl);
			nl = replaceSpecificWording(nl);
			
			String[] nlSentences = nl.split("(?<=[！？。　]++)"); // 文に分解
			
			for(String nlSentence: nlSentences)
				ret.addAll(talkToContents(gameInfo, talker, questionTo, key, Clause.createClauses(nlSentence), nl));
			
			ret = new ArrayList<String>(new LinkedHashSet<>(ret)); // 重複削除
			Collections.sort(ret); // COMINGOUTを優先にしたいので文字列でソート
			
			if(ret.size() < 1)
				ret.add(Talk.SKIP);
		} catch(Exception e) {
			e.printStackTrace();
			ret.add(Talk.SKIP);
		}
		
		processedTalks.add(key);
		translatedMap.put(key, ret);
		return ret;
	}
	
	// Jumanに直接いれられない特殊な言い回し等を置換する
	private static String replaceSpecificWording(String nl) {
		String tmp = nl;
		tmp = tmp.replace("ぼく占", "ぼくは占");
		tmp = tmp.replace("ぼく人", "ぼくは人");
		tmp = tmp.replace("ぼく狂", "ぼくは狂");
		tmp = tmp.replace("ぼく実", "ぼくは実");
		tmp = tmp.replace("ボク占", "ボクは占");
		tmp = tmp.replace("ボク人", "ボクは人");
		tmp = tmp.replace("ボク狂", "ボクは狂");
		tmp = tmp.replace("ボク実", "ボクは実");
		tmp = tmp.replace("私、", "私は");
		tmp = tmp.replace("ぼく、", "ぼくは");
		tmp = tmp.replace("ボク、", "ボクは");
		tmp = tmp.replace("］の結果", "］の占い結果");
		tmp = tmp.replace("人狼じゃ", "人狼だ");
		tmp = tmp.replace("狂人じゃ", "狂人だ");
		tmp = tmp.replace("人間じゃ", "人間だ");
		tmp = tmp.replace("師じゃ", "師だ");
		tmp = tmp.replace("って言っておく", "です"); // 横着
		tmp = tmp.replace("僕視点では", ""); // 横着
		return tmp;
	}
	
	private List<String> talkToContents(GameInfo gameInfo, Agent talker, Agent questionTo, String key, List<Clause> clauses, String fullTalk) throws IOException, InterruptedException {
		List<Clause> roleClauses   = Clause.findAiwolfTypeClauses(clauses, "役職");
		List<Clause> roleCoClauses = Clause.findAiwolfTypeClauses(clauses, "役職CO");
		List<Clause> actionClauses = Clause.findAiwolfTypeClauses(clauses, "行為");
		List<Clause> playerClauses = Clause.findAiwolfTypeClauses(clauses, "プレイヤー");
		Clause tmp = null;
		
		List<Content> contents = new ArrayList<>();
				
		for(Clause roleCoClause: roleCoClauses) {
			if(questionTo != null) // 問いかけの場合はスキップ
				break;
			if(!roleCoClause.isNegative()) {
				// ☆役職CO「占い師COします」
				if(roleCoClause.getAttributes().contains("動態述語") &&
						roleCoClause.getChild() == null && // この単語がなにかに係っているものを回避することで「占い師COしている人は・・・」などを除く
						roleCoClause.getKakuMap().size() < 1) { // ↑と同様
					switch (roleCoClause.getAiwolfWordMeaning()) {
					case "占い師":	contents.add(new Content(new ComingoutContentBuilder(talker, Role.SEER))); break;
					case "人狼":		contents.add(new Content(new ComingoutContentBuilder(talker, Role.WEREWOLF))); break;
					case "狂人":		contents.add(new Content(new ComingoutContentBuilder(talker, Role.POSSESSED))); break;
					case "人間":		contents.add(new Content(new ComingoutContentBuilder(talker, Role.VILLAGER))); break;
					}
				}
			}
		}
		
		for(Clause roleClause: roleClauses) {
			if(questionTo != null) // 問いかけの場合はスキップ
				break;
			
			if(!roleClause.isNegative()) {				
				// ☆役職CO「私は占い師です」
				tmp = roleClause.getKakuMap().get("ガ");
				if(tmp != null && tmp.getAttributes().contains("一人称") &&
						!roleClause.getModalities().contains("疑問") &&
						!roleClause.getAttributes().contains("節機能-理由")) { // 「私が占い師だから、〜」を回避) {
					switch (roleClause.getAiwolfWordMeaning()) {
					case "占い師":	contents.add(new Content(new ComingoutContentBuilder(talker, Role.SEER))); break;
					case "人狼":		contents.add(new Content(new ComingoutContentBuilder(talker, Role.WEREWOLF))); break;
					case "狂人":		contents.add(new Content(new ComingoutContentBuilder(talker, Role.POSSESSED))); break;
					//case "人間":		contents.add(new Content(new ComingoutContentBuilder(talker, Role.VILLAGER))); break; //私は人間です、は村人COというには弱いので消す
					}
				}
				
				// ☆占い結果「Agent[04]さんは人狼です」
				tmp = roleClause.getKakuMap().get("ガ");
				Clause child = roleClause.getChild();
				if(tmp != null && tmp.getAiwolfWordType() != null && 
						roleClause.getKakuMap().get("ニ") == null && // 「ガ」と「ニ」が両方係ってる場合の「○○がXXに人狼判定しました」等をスルーする
						tmp.getAiwolfWordType().equals("プレイヤー") && 
						!roleClause.getModalities().contains("疑問") &&
						!(child != null && child.getMain().equals("思う"))) { // 「人狼だと思う」の回避
					Agent target = Agent.getAgent(Integer.parseInt(tmp.getAiwolfWordMeaning()));
					switch (roleClause.getAiwolfWordMeaning()) {
					case "人狼":	contents.add(new Content(new DivinedResultContentBuilder(target, Species.WEREWOLF))); break;
					case "人間":	contents.add(new Content(new DivinedResultContentBuilder(target, Species.HUMAN))); break;
					}
				}
				
				// ☆占い結果「Agent[04]さんを占いました。結果は人狼です」
				tmp = roleClause.getKakuMap().get("ガ");
				if(tmp != null && tmp.getMain().equals("結果") &&
						!roleClause.getModalities().contains("疑問")) {
					List<Clause> fullTalkClauses = Clause.createClauses(fullTalk); // プレイヤー名は対象文の外にあるのでここで改めて全文にKNPをかける
					List<Clause> fullTalkActionClauses = Clause.findAiwolfTypeClauses(fullTalkClauses, "行為");
					for(Clause ftActionClause: fullTalkActionClauses) {
						if(ftActionClause.isNegative())
							continue;
						if(!ftActionClause.getAiwolfWordMeaning().equals("占い"))
							continue;
						Clause c = ftActionClause.getKakuMap().get("ヲ");
						if(c != null && c.getAiwolfWordType().equals("プレイヤー")) {
							Agent target = Agent.getAgent(Integer.parseInt(c.getAiwolfWordMeaning()));
							switch (roleClause.getAiwolfWordMeaning()) {
							case "人狼":	contents.add(new Content(new DivinedResultContentBuilder(target, Species.WEREWOLF))); break;
							case "人間":	contents.add(new Content(new DivinedResultContentBuilder(target, Species.HUMAN))); break;
							}
							break;
						}
					}
				}
			}
		}
		
		// ☆占い結果「Agent[04]さんを占ったら人狼でした」
		for(Clause playerClause: playerClauses) {
			if(questionTo != null) // 問いかけの場合はスキップ
				break;
			for(Clause roleClause: roleClauses) {
				for(Clause actionClause: actionClauses) {
					if(
							actionClause.getAiwolfWordMeaning().equals("占い") &&
							roleClause.getAiwolfWordMeaning().startsWith("人") &&
							roleClause.getAttributes().contains("状態述語") &&
							!roleClause.getAttributes().contains("節機能-理由") && // 「占いの結果が人狼だったから、〜」を回避
							!actionClause.isNegative() && !roleClause.isNegative()) {
						Agent target = Agent.getAgent(Integer.parseInt(playerClause.getAiwolfWordMeaning()));
						switch (roleClause.getAiwolfWordMeaning()) {
						case "人狼":	contents.add(new Content(new DivinedResultContentBuilder(target, Species.WEREWOLF))); break;
						case "人間":	contents.add(new Content(new DivinedResultContentBuilder(target, Species.HUMAN))); break;
						}
					}
				}
			}
		}
		
		for(Clause actionClause: actionClauses) {
			if(!actionClause.isNegative()) {
				if(actionClause.getAiwolfWordMeaning().equals("投票")) {
					// ☆投票依頼「Agent[04]さんに投票してください」
					Set<String> m = actionClause.getModalities();
					if((m.contains("依頼Ａ") || m.contains("勧誘")) && !m.contains("意志")) {
						int agentId = -1;
						tmp = actionClause.getKakuMap().get("ニ");
						if(tmp != null && tmp.getAiwolfWordType().equals("プレイヤー"))
							agentId = Integer.parseInt(tmp.getAiwolfWordMeaning());
						
						tmp = actionClause.getKakuMap().get("ヲ");
						if(tmp != null && tmp.getAiwolfWordType().equals("プレイヤー"))
							agentId = Integer.parseInt(tmp.getAiwolfWordMeaning());
							
						if(agentId >= 0) {
							Agent target = Agent.getAgent(agentId);
							if(target != null)	
								contents.add(new Content(new RequestContentBuilder(null, new Content(new VoteContentBuilder(target)))));
						}
					}	
				}
			}
		}
			
		String prefix = ">>" + talker + " " + talker + "<さん>、";
		if(questionTo == gameInfo.getAgent()) { // 自分宛て問いかけの場合
			List<Clause> willClauses = Clause.findModalityClauses(clauses, "意志");
			for(Clause willClause: willClauses) {
				if(willClause.getMain().equals("知る")) { // 知りたい
					tmp = willClause.getKakuMap().get("ガ");
					if(tmp != null && tmp.getMain().equals("理由")) // 理由が知りたい
						for(Clause acitonClause: actionClauses)
							if(acitonClause.getAiwolfWordMeaning().equals("占い")) // 占った理由が知りたい
								qas.put(key, prefix + "それは昨日からちょっと様子がおかしいと思ったから……。");
				}
			}
			if(Clause.findModalityClauses(clauses, "疑問").size() > 0) {
				for(Clause suspiciousClause: Clause.findMainClauses(clauses, "疑う")) {
					tmp = suspiciousClause.getKakuMap().get("ヲ");
					Clause tmp2 = suspiciousClause.getKakuMap().get("修飾");
					if(tmp != null) {
						if(tmp.getMain().equals("誰")) // 誰を疑う？
							qas.put(key, prefix + "<僕>は#<さん>が怪しいと思う<よ>。");
						if(tmp2 != null && tmp2.getAttributes().contains("疑問詞")) { // どうして
							if(tmp.getAttributes().contains("一人称")) // どうして僕？
								//qas.put(key, prefix + "&" + String.format("%02d", talker.getAgentIdx()));
								qas.put(key, prefix + "それは昨日からちょっと様子がおかしいと思ったから……。");
							
							String awt = tmp.getAiwolfWordType();
							if(awt != null && awt.equals("プレイヤー")) // どうしてAgent03を疑う？
								//qas.put(key, prefix + "&" + String.format("%02d", Integer.parseInt(tmp.getAiwolfWordMeaning())));
								qas.put(key, prefix + "それは昨日からちょっと様子がおかしいと思ったから……。");
							if(tmp.getAttributes().contains("人称代名詞")) // どうして彼を疑う？
								qas.put(key, prefix + tmp.getMain() + "って誰のこと？");
						}
					}
				}
				for(Clause suspiciousClause: Clause.findMainClauses(clauses, "怪しい")) {
					tmp = suspiciousClause.getKakuMap().get("ガ");
					Clause tmp2 = suspiciousClause.getKakuMap().get("修飾");
					if(tmp != null) {
						if(tmp.getMain().equals("誰")) // 誰が怪しい？
							qas.put(key, prefix + "<僕>は#<さん>が怪しいと思う<よ>。");
						if(tmp2 != null && tmp2.getAttributes().contains("疑問詞")) { // どうして
							if(tmp.getAttributes().contains("一人称")) // どうして僕が怪しい？
								//qas.put(key, prefix + "&" + String.format("%02d", talker.getAgentIdx()));
								qas.put(key, prefix + "それは昨日からちょっと様子がおかしいと思ったから……。");
							
							String awt = tmp.getAiwolfWordType();
							if(awt != null && awt.equals("プレイヤー")) // どうしてAgent03が怪しい？
								//qas.put(key, prefix + "&" + String.format("%02d", Integer.parseInt(tmp.getAiwolfWordMeaning())));
								qas.put(key, prefix + "それは昨日からちょっと様子がおかしいと思ったから……。");
							if(tmp.getAttributes().contains("人称代名詞")) // どうして彼が怪しい？
								qas.put(key, prefix + tmp.getMain() + "って誰のこと？");
						}
					}
				}
				for(Clause goClause: Clause.findMainClauses(clauses, "行く")) { // 行くか？ or 行かないか？
					if(goClause.getAttributes().contains("動態述語"))
						qas.put(key, prefix + "いい<よ>！");
				}
				for(Clause suspiciousClause: Clause.findMainClauses(clauses, "疑う")) {
					tmp = suspiciousClause.getKakuMap().get("ヲ");
					if(tmp != null && tmp.getMain().equals("誰")) { // 誰を疑っている？
						qas.put(key, prefix + "<僕>は#<さん>が怪しいと思う<よ>。");
					}
				}
				for(Clause actionClause: actionClauses) {
					if(actionClause.getAiwolfWordMeaning().equals("占い")) {
						tmp = actionClause.getKakuMap().get("修飾");
						if(tmp != null && tmp.getAttributes().contains("疑問詞")) { // どうして私を占った？
							qas.put(key, prefix + "それは昨日からちょっと様子がおかしいと思ったから……。");
						}
					}
					if(actionClause.getAiwolfWordMeaning().equals("投票")) {
						tmp = actionClause.getKakuMap().get("ニ");
						if(tmp != null && tmp.getMain().equals("誰")) { // 誰に投票する？
							qas.put(key, prefix + "いま時点では<僕>は#<さん>に投票しようと思っている<よ>。");
						} else if (tmp != null && tmp.getAiwolfWordType().equals("プレイヤー")) { // Agent01に投票する？
							qas.put(key, prefix + "いま時点では<僕>は#<さん>に投票しようと思っている<よ>。");
						}
					}
				}
				for(Clause roleClause: roleClauses) {
					if(roleClause.getAiwolfWordMeaning().equals("人間")) {
						if(Clause.findMainClauses(clauses, "誰").size() > 0) { // 誰が村人だと思う？
							qas.put(key, prefix + "<僕>は^<さん>は怪しくないと思う<よ>。");
							break;
						}
					} else if(roleClause.getAiwolfWordMeaning().equals("人狼")) {
						tmp = roleClause.getKakuMap().get("ガ");
						if(Clause.findMainClauses(clauses, "誰").size() > 0) { // 誰が人狼だと思う？
							qas.put(key, prefix + "<僕>は#<さん>が怪しいと思う<よ>。");
							break;
						} else if(tmp != null && tmp.getAttributes().contains("二人称")) { // あなたが人狼なんでしょう？, あなたが人狼なんですか！？
							qas.put(key, prefix + "<僕>は人狼じゃない<よ>。");
							break;
						}
					} else if(roleClause.getAiwolfWordMeaning().equals("狂人")) {
						tmp = roleClause.getKakuMap().get("ガ");
						if(tmp != null && tmp.getAttributes().contains("二人称")) { // あなたが狂人なんでしょう？, あなたが狂人なんですか！？
							qas.put(key, prefix + "<僕>は狂人じゃない<よ>。");
							break;
						}
					} else if(roleClause.getAiwolfWordMeaning().equals("占い師")) { // 占い師はいつCOすべきと思いますか？
						if(Clause.findMainClauses(clauses, "いつ").size() > 0 && Clause.findMainClauses(clauses, "ＣＯ").size() > 0) {
							qas.put(key, prefix + "できるだけ早いほうがいいと思う<よ>。");
							break;
						}
					}
				}
			}
		}
		
		List<String> ret = new ArrayList<>();
		for(Content content: contents) {
			ret.add(content.getText());
		}
		
		return ret;
	}
	
	public List<String> toProtocolsForWhisper(GameInfo gameInfo, Agent talker, String naturalLanguage) {
		List<String> ret = new ArrayList<>();
		ret.add(Talk.SKIP);
		return ret;
	}
	
	// juman辞書に半角文字登録できなさそうなので
	private static String hankakuToZenkaku(String value) {
		StringBuilder sb = new StringBuilder(value);
		for (int i = 0; i < sb.length(); i++) {
			int c = (int) sb.charAt(i);
			if ((c >= 0x30 && c <= 0x39) || (c >= 0x41 && c <= 0x5A) || (c >= 0x61 && c <= 0x7A))
				sb.setCharAt(i, (char) (c + 0xFEE0));
			if (c == '[')
				sb.setCharAt(i, '［');
			if (c == ']')
				sb.setCharAt(i, '］');
			if (c == ' ')
				sb.setCharAt(i, '、');
			if (c == '　')
				sb.setCharAt(i, '、');
			if (c == '?')
				sb.setCharAt(i, '？');
			if (c == '!')
				sb.setCharAt(i, '！');
		}
		value = sb.toString();
		return value;
	}
	
	public Collection<String> getAnswers() {
		return qas.values();
	}

	@SuppressWarnings("unchecked")
	public static Map<String, List<String>> load() {
		Map<String, List<String>> ret = null;
		ObjectInputStream ois = null;
		try {
			ois = new ObjectInputStream(new FileInputStream(DAT_FILE));
			ret = (Map<String, List<String>>)ois.readObject();
		} catch (IOException | ClassNotFoundException e) {
			ret = new HashMap<>();
		} finally {
			try { ois.close(); } catch (Exception e) {}
		}
		return ret;
	}

	private static void save(Map<String, List<String>> map) {
		ObjectOutputStream oos = null;
		try {
			oos = new ObjectOutputStream(new FileOutputStream(DAT_FILE));
			oos.writeObject(map);
		} catch (IOException e) {
		} finally {
			try { oos.close(); } catch (Exception e) {}
		}
	}

	public void save() {
		save(translatedMap);
	}
	
	public void setMessageOff(boolean messageOff) {
		this.messageOff = messageOff;
	}
}
