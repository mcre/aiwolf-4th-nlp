package net.mchs_u.mc.aiwolf.nlp.chaser;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
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
			
			List<String> contents = talkToContents(gameInfo, talker, questionTo, key, Clause.createClauses(nl));
			if(contents == null)
				ret.add(Talk.SKIP);
			else
				ret = contents;
		} catch(Exception e) {
			ret.add(Talk.SKIP);
		}
		
		processedTalks.add(key);
		translatedMap.put(key, ret);
		return ret;
	}
	
	// Jumanに直接いれられない特殊な言い回しを置換する
	private static String replaceSpecificWording(String nl) {
		String tmp = nl;
		tmp = tmp.replace("ぼく占", "ぼくは占");
		tmp = tmp.replace("ボク占", "ボクは占");
		tmp = tmp.replace("］の結果", "］の占い結果");
		tmp = tmp.replace("人狼じゃ", "人狼だ");
		tmp = tmp.replace("狂人じゃ", "狂人だ");
		tmp = tmp.replace("人間じゃ", "人間だ");
		tmp = tmp.replace("師じゃ", "師だ");
		return tmp;
	}
	
	private List<String> talkToContents(GameInfo gameInfo, Agent talker, Agent questionTo, String key, List<Clause> clauses) {
		List<Clause> roleClauses   = Clause.findAiwolfTypeClauses(clauses, "役職");
		List<Clause> roleCoClauses = Clause.findAiwolfTypeClauses(clauses, "役職CO");
		List<Clause> actionClauses = Clause.findAiwolfTypeClauses(clauses, "行為");
		List<Clause> playerClauses = Clause.findAiwolfTypeClauses(clauses, "プレイヤー");
		Clause tmp = null;
		
		List<Content> contents = new ArrayList<>();
				
		for(Clause roleCoClause: roleCoClauses) {
			if(!roleCoClause.isNegative()) {
				// ☆役職CO「占い師COします」
				if(roleCoClause.getAttributes().contains("動態述語")) {
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
			if(!roleClause.isNegative()) {				
				// ☆役職CO「私は占い師です」
				tmp = roleClause.getKakuMap().get("ガ");
				if(tmp != null && tmp.getAttributes().contains("一人称") &&
						!roleClause.getModalities().contains("疑問")) {
					switch (roleClause.getAiwolfWordMeaning()) {
					case "占い師":	contents.add(new Content(new ComingoutContentBuilder(talker, Role.SEER))); break;
					case "人狼":		contents.add(new Content(new ComingoutContentBuilder(talker, Role.WEREWOLF))); break;
					case "狂人":		contents.add(new Content(new ComingoutContentBuilder(talker, Role.POSSESSED))); break;
					//case "人間":		contents.add(new Content(new ComingoutContentBuilder(talker, Role.VILLAGER))); break;
					}
				}
				
				// ☆占い結果「Agent[04]さんは人狼です」
				tmp = roleClause.getKakuMap().get("ガ");
				Clause child = roleClause.getChild();
				if(tmp != null && tmp.getAiwolfWordType() != null && 
						tmp.getAiwolfWordType().equals("プレイヤー") && 
						!roleClause.getModalities().contains("疑問") &&
						!(child != null && child.getMain().equals("思う"))) { // 「人狼だと思う」の回避
					Agent target = Agent.getAgent(Integer.parseInt(tmp.getAiwolfWordMeaning()));
					switch (roleClause.getAiwolfWordMeaning()) {
					case "人狼":	contents.add(new Content(new DivinedResultContentBuilder(target, Species.WEREWOLF))); break;
					case "人間":	contents.add(new Content(new DivinedResultContentBuilder(target, Species.HUMAN))); break;
					}
				}
			}
		}
		
		for(Clause playerClause: playerClauses) {
			for(Clause roleClause: roleClauses) {
				for(Clause actionClause: actionClauses) {
					if(
							actionClause.getAiwolfWordMeaning().equals("占い") &&
							roleClause.getAiwolfWordMeaning().startsWith("人") &&
							roleClause.getAttributes().contains("状態述語") &&
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
			
		
		if(questionTo == gameInfo.getAgent()) { // 自分宛て問いかけの場合
			if(
					Clause.findModalityClauses(clauses, "勧誘").size() > 0 || // 一緒に遊ぼうよ。, 今日はAgent[01]さんに投票しましょうよ
					Clause.findModalityClauses(clauses, "意志").size() > 0 || // 今日はAgent[01]さんに投票しましょう
					Clause.findModalityClauses(clauses, "依頼Ａ").size() > 0) { // 今日はAgent[01]さんに投票してください
				qas.put(key, ">>" + talker + " " + talker + "<さん>、うーん、そのお願いはどうしようかな。");
			}
			
			// 2文のときにうまく対応できないかも(そのうちちゃんと調べたい)
			if(Clause.findModalityClauses(clauses, "疑問").size() > 0) {
				for(Clause roleClause: roleClauses) {
					if(roleClause.getAiwolfWordMeaning().equals("人狼")) {
						String main = roleClause.getKakuMap().get("ガ").getMain();
						if(Clause.findMainClauses(clauses, "誰").size() > 0) { // 誰が人狼だと思う？
							qas.put(key, ">>" + talker + " " + talker + "<さん>、<僕>は#<さん>が怪しいと思う<よ>。");
							break;
						} else if(main.equals("君") || main.equals("おまえ") || main.equals("キミ") || main.equals("あなた") || main.equals("御前")) { // あなたが人狼なんでしょう？, あなたが人狼なんですか！？
							qas.put(key, ">>" + talker + " " + talker + "<さん>、<僕>は人狼じゃない<よ>。");
							break;
						} else {
							qas.put(key, ">>" + talker + " " + talker + "<さん>、その質問はちょっとわからない<よ>。");
						}
					} if(roleClause.getAiwolfWordMeaning().equals("狂人")) {
						String main = roleClause.getKakuMap().get("ガ").getMain();
						if(main.equals("君") || main.equals("おまえ") || main.equals("キミ") || main.equals("あなた") || main.equals("御前")) { // あなたが狂人なんでしょう？, あなたが狂人なんですか！？
							qas.put(key, ">>" + talker + " " + talker + "<さん>、<僕>は狂人じゃない<よ>。");
							break;
						} else {
							qas.put(key, ">>" + talker + " " + talker + "<さん>、その質問はちょっとわからない<よ>。");
						}
					} else if(roleClause.getAiwolfWordMeaning().equals("占い師")) { // 占い師はいつCOすべきと思いますか？
						if(Clause.findMainClauses(clauses, "いつ").size() > 0 && Clause.findMainClauses(clauses, "ＣＯ").size() > 0) {
							qas.put(key, ">>" + talker + " " + talker + "<さん>、できるだけ早いほうがいいと思う<よ>。");
							break;
						} else {
							qas.put(key, ">>" + talker + " " + talker + "<さん>、その質問はちょっとわからない<よ>。");
						}
					} else {
						qas.put(key, ">>" + talker + " " + talker + "<さん>、その質問はちょっとわからない<よ>。");
					}
				}
			}
		}
		
		List<String> ret = new ArrayList<>();
		for(Content content: contents) {
			ret.add(content.getText());
		}
		
		return new ArrayList<String>(new LinkedHashSet<>(ret)); // 重複削除
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
