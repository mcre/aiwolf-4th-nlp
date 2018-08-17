package net.mchs_u.mc.aiwolf.nlp.chaser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.Topic;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.net.GameInfo;

import net.mchs_u.mc.aiwolf.common.AgentTargetResult;
import net.mchs_u.mc.aiwolf.dokin.Estimate;
import net.mchs_u.mc.aiwolf.dokin.McrePlayer;
import net.mchs_u.mc.aiwolf.nlp.common.Transrater;

public class Mouth {
	private static final double EPS =  0.00001d;

	private Set<String> talkedSet = null;
	private McrePlayer player = null;
	private Map<String, String> characterMap = null;

	private boolean firstVoted = false;
	private Agent targetOfVotingDeclarationToday = null;

	private static int playerNum = 0; // キャラクター変更用

	public Mouth(McrePlayer player) {
		this.player = player;
		characterMap = Character.getCharacterMap(playerNum++);
	}

	public void initialize(GameInfo gameInfo) {
		talkedSet = new HashSet<>();
		firstVoted = false;
	}

	public void dayStart() {
		targetOfVotingDeclarationToday = null;
	}

	public String toNaturalLanguageForTalk(GameInfo gameInfo, String protocol, Collection<String> answers) {
		if(!Content.validate(protocol)) {
			System.err.println("Mouth: 内部エージェントがプロトコル以外を喋ってる -> " + protocol);
			return Talk.SKIP;
		}
		Content content = new Content(protocol);

		if(gameInfo.getDay() == 0) { //　0日目は特殊
			if(!talkedSet.contains("0日目発言")){
				talkedSet.add("0日目発言");
				return r("<こんにちは。>");
			}
			return Talk.OVER;
		}

		String ret = null;
		List<String> tmp = new ArrayList<String>();
		Agent t = content.getTarget();
		switch (content.getTopic()) {
		case OVER:
			return Talk.OVER;
		case SKIP:
			return skipTalk(gameInfo, answers);
		case COMINGOUT:
			if(!content.getTarget().equals(gameInfo.getAgent()))
				return Talk.SKIP;
			if(content.getRole() == Role.WEREWOLF) {
				if(getEstimate().isPowerPlay()) { // もうPPされてる場合
					tmp.clear();
					tmp.add(r("わおーん、<僕>は人狼だ<よ>。"));
					tmp.add(r("ふふふ、<僕>は人狼だ<よ>。"));
					tmp.add(r("ありがとう、<僕>は人狼だ<よ>。"));
					ret = rnd(tmp, 3);
					if(ret != null) return ret;
				} else { // PP発動する場合
					tmp.clear();
					tmp.add(r("もう<僕>たちのほうが多いよう<だね>。わおーん、<僕>は人狼だ<よ>。"));
					tmp.add(r("もう<僕>たちのほうが多いよう<だね>。ふふふ、<僕>は人狼だ<よ>。"));
					ret = rnd(tmp, 2);
					if(ret != null) return ret;
				}
			}
			if(content.getRole() == Role.POSSESSED) {
				tmp.clear();
				tmp.add(r("ふふふふ、<僕>が狂人だ<よ>！"));
				tmp.add(r("あはははは！　<僕>が狂人だ<よ>！"));
				ret = rnd(tmp, 2);
				if(ret != null) return ret;
			}
			return r("<僕>は" + Transrater.roleToString(content.getRole()) + "だ<よ>。");
		case DIVINED:
			String r = Transrater.speciesToString(content.getResult());
			t = content.getTarget();
			tmp.clear();
			tmp.add(r(t + "<さん>の占い結果は、" + r + "だった<よ>。"));
			tmp.add(r(t + "<さん>の占いの結果は、" + r + "だった<よ>。"));
			tmp.add(r(t + "<さん>を占ったら、" + r + "だった<よ>。"));
			tmp.add(r(t + "<さん>を占った結果は、" + r + "だった<よ>。"));
			tmp.add(r("昨日の占い結果だ<よ>、" + t + "<さん>は" + r + "だった<よ>。"));
			ret = rnd(tmp, 5);
			if(ret != null) return ret;
		case IDENTIFIED:
			return r(content.getTarget() + "<さん>の霊能結果は、" + Transrater.speciesToString(content.getResult()) + "だった<よ>。");
		case OPERATOR:
			Content c = content.getContentList().get(0);
			if(c.getTopic() != Topic.VOTE)
				return Talk.SKIP;
			return r(c.getTarget() + "<さん>に投票して<ね>。");
		case VOTE:
			if(getEstimate().isPowerPlay()) {
				if((gameInfo.getRole() == Role.WEREWOLF || gameInfo.getRole() == Role.POSSESSED))
					return r(t + "<さん>に投票する<よ>。");
				else
					return Talk.SKIP;
			}
			// 1回目の投票宣言は何も情報がない中での宣言なのでスルーする
			if(firstVoted) {
				Agent bak = targetOfVotingDeclarationToday;
				targetOfVotingDeclarationToday = t;
				if(bak == null || bak == t) {
					tmp.clear();
					tmp.add(r(t + "<さん>があやしいと思うから投票する<よ>。"));
					tmp.add(r(t + "<さん>が人狼だと思うから投票する<よ>。"));
					ret = rnd(tmp, 2);
					if(ret != null) return ret;
				} else {
					tmp.clear();
					tmp.add(r("さっきと状況が変わったから、やっぱり" + t + "<さん>に投票する<よ>。"));
					tmp.add(r("やっぱり" + t + "<さん>が人狼だと思う。投票する<よ>。"));
					ret = rnd(tmp, 2);
					if(ret != null) return ret;
				}
			}
			firstVoted = true;
		default:
			return Talk.SKIP;
		}
	}

	private static String agentsToTalk(GameInfo gameInfo, Collection<Agent> agents, char conj) {
		String ret = "";
		for(Agent agent: agents)
			ret += agent.toString() + "<さん>" + conj;
		return ret.substring(0, ret.length() - 1).replace(gameInfo.getAgent() + "<さん>", "<僕>");
	}

	private static String resultsToTalk(GameInfo gameInfo, Collection<AgentTargetResult> results, Set<Agent> seers) {
		String ret = "";
		for(AgentTargetResult r: results) {
			if(!seers.contains(r.getAgent()))
				continue;
			ret += r.getAgent() + "<さん>が" + r.getTarget() + "<さん>を";
			if(r.getResult() == Species.WEREWOLF)
				ret += "黒、";
			else
				ret += "白、";
		}
		if(ret.length() > 1)
			return ret.substring(0, ret.length() - 1).replace(gameInfo.getAgent() + "<さん>", "<僕>");
		return null;
	}

	private String makeStatusTalk(GameInfo gameInfo) {		
		String s = "";
		Set<Agent> seers = getEstimate().getCoSet(Role.SEER);
		List<AgentTargetResult> divs = getEstimate().getDivinedHistory();
		List<Agent> attackeds = getEstimate().getAttackedAgents();
		if(seers.contains(gameInfo.getAgent()))
			s += "<僕>が占い師で、";		
		seers.remove(gameInfo.getAgent());
		if(!seers.isEmpty())
			s += agentsToTalk(gameInfo, seers, 'と') + "が自称占い師で、";
		if(!divs.isEmpty()) {
			String d = resultsToTalk(gameInfo, divs, seers);
			if(d != null)
				s += resultsToTalk(gameInfo, divs, seers) + "と言っていて、";
		}
		if(!attackeds.isEmpty())
			s += agentsToTalk(gameInfo, attackeds, 'と') + "が襲撃されて、";
		if(s.length() < 1)
			return null;
		if(s.charAt(s.length() - 2) == 'で')
			return s.substring(0, s.length() - 2) + "だから、";
		return s.substring(0, s.length() - 2) + "たから、";
	}

	private static List<Agent> max(Collection<Agent> candidate, Map<Agent, Double> likeness, Agent me) {
		Collection<Agent> candidateCopy = new ArrayList<Agent>(candidate);
		Map<Agent, Double> likenessCopy = new HashMap<Agent, Double>(likeness);
		
		List<Agent> ret = new ArrayList<>();
		candidateCopy.remove(me);
		likenessCopy.remove(me);
		double max = Collections.max(likenessCopy.values());
		for(Agent agent: candidateCopy)
			if(Math.abs(max - likenessCopy.get(agent)) < EPS)
				ret.add(agent);
		return ret;
	}

	private static Map<Agent, Double> toPossessedLikeness(Map<Agent, Double> werewolfLikeness, Map<Agent, Double> villagerTeamLikeness) {
		Map<Agent, Double> ret = new HashMap<>();
		for(Agent agent: werewolfLikeness.keySet())
			ret.put(agent, 1d - werewolfLikeness.get(agent) - villagerTeamLikeness.get(agent));
		return ret;
	}

	private String skipTalk(GameInfo gameInfo, Collection<String> answers) {
		String ret = null;
		List<String> tmp = new ArrayList<String>();

		if(getEstimate().isPowerPlay()) { // PPモード 
			if(!talkedSet.contains("パワープレイ反応")){
				talkedSet.add("パワープレイ反応");
				if(gameInfo.getRole() == Role.WEREWOLF) { // 人狼
					tmp.clear();
					tmp.add(r("食べちゃう<よ>ー！"));
					tmp.add(r("逃げても無駄だ<よ>！"));
					tmp.add(r("狂人<さん>、うまくやった<よ>！"));
					tmp.add(r("美味しい人間が食べたい<よ>！"));
					ret = rnd(tmp, 4);
					if(ret != null) return ret;
				} else if(gameInfo.getRole() == Role.POSSESSED) { // 狂人
					tmp.clear();
					tmp.add(r("うひゃひゃひゃひゃひゃひゃひゃ！"));
					tmp.add(r("みんな死んじゃえ！"));
					tmp.add(r("人狼様！ <僕>を食べてください！"));
					tmp.add(r("これで人狼様の世界だ<よ>！"));
					tmp.add(r("人間なんて滅びればいいんだ<よ>！"));
					ret = rnd(tmp, 5);
					if(ret != null) return ret;
				} else { // 村人チーム
					tmp.clear();
					tmp.add(r("え！　助けて！"));
					tmp.add(r("もう終わりだ<よ>……。"));
					tmp.add(r("信じられない<よ>……。"));
					tmp.add(r("逃げるしかない！"));
					tmp.add(r("もう手はない<かな>……。"));
					ret = rnd(tmp, 5);
					if(ret != null) return ret;
				}
			}
			return Talk.SKIP;
		}

		// 共通反応
		if(gameInfo.getLastDeadAgentList().size() > 0 && gameInfo.getDay() == 2) { // 2日目で襲撃死した人がいる場合
			if(!talkedSet.contains("襲撃反応")){
				talkedSet.add("襲撃反応");
				tmp.clear();
				tmp.add(r("本当に襲われるなんて。"));
				tmp.add(r(gameInfo.getLastDeadAgentList().get(0) + "<さん>が死ん<じゃった>……。"));
				ret = rnd(tmp, 5);
				if(ret != null) return ret;
			}
		}

		if(getEstimate().getCoMap().get(gameInfo.getAgent()) == Role.SEER) { // 自分が占い師COしてるとき
			if(getEstimate().getCoSet(Role.SEER).size() == 2) { //二人COしているとき
				if(!talkedSet.contains("対抗占い師反応")){
					talkedSet.add("対抗占い師反応");
					Set<Agent> coSeers = getEstimate().getCoSet(Role.SEER);
					coSeers.remove(gameInfo.getAgent());
					Agent t = (Agent)coSeers.toArray()[0];

					tmp.clear();
					tmp.add(r(t + "<さん>は嘘をついて<います>！　<僕>が本当の占い師<です>！"));
					tmp.add(r(t + "<さん>は偽物<です>！　<僕>こそが本当の占い師<です>！"));
					tmp.add(r(">>" + t + " " + t + "<さん>、<僕>が占い師<です>！　<あなた>が人狼<なのですか>！？"));
					tmp.add(r(">>" + t + " " + t + "<さん>、<僕>が占い師<です>！　<あなた>は狂人<なのですか>！？"));
					ret = rnd(tmp, 6);
					if(ret != null) return ret;
				}
			}
		} else {
			if(getEstimate().getCoSet(Role.SEER).size() == 2) { //二人COしているとき
				if(!talkedSet.contains("二人占い師反応")){
					talkedSet.add("二人占い師反応");
					tmp.clear();
					tmp.add("どっちが本当の占い師なんだろう。");
					ret = rnd(tmp, 5);
					if(ret != null) return ret;
				}
			}
		}

		for(String answer: answers) { //Earから渡されたAnswer
			if(!talkedSet.contains("answer:" + answer)){
				talkedSet.add("answer:" + answer);
				Agent voteTarget = player.getVoteTarget();
				
				Estimate es = getEstimate();
				List<Agent> estimatedVillagers = max(gameInfo.getAgentList(), es.getVillagerTeamLikeness(), gameInfo.getAgent());
				Agent estimatedVillager = null;
				if(estimatedVillagers.size() > 0)
					estimatedVillager = estimatedVillagers.get(0);
				
				List<Agent> estimatedPossesseds = max(gameInfo.getAgentList(), toPossessedLikeness(es.getWerewolfLikeness(), es.getVillagerTeamLikeness()), gameInfo.getAgent());
				Agent estimatedPossessed = null;
				if(estimatedPossesseds.size() > 0)
					estimatedPossessed = estimatedPossesseds.get(0);
				
				if(voteTarget != null && answer.contains("#")) {
					if(answer.startsWith(">>" + voteTarget + " "))
						return r(answer.replace("#<さん>", "<あなた>"));
					else
						return r(answer.replace("#", voteTarget.toString()));
				}
				if(estimatedPossessed != null && answer.contains("*")) {
					if(answer.startsWith(">>" + estimatedPossessed + " "))
						return r(answer.replace("*<さん>", "<あなた>"));
					else
						return r(answer.replace("*", estimatedPossessed.toString()));
				}
				if(estimatedVillager != null && answer.contains("^")) {
					if(answer.startsWith(">>" + estimatedVillager + " "))
						return r(answer.replace("^<さん>", "<あなた>"));
					else
						return r(answer.replace("^", estimatedVillager.toString()));
				}
				return r(answer);
			}
		}

		// 8ターン目以降(7ターン目を読み込んでいる時)はここより下の発言を抑制 -------------

		int talkSize = gameInfo.getTalkList().size();
		if(talkSize > 0)
			if(gameInfo.getTalkList().get(talkSize - 1).getTurn() > 6)
				return Talk.SKIP;

		String st = makeStatusTalk(gameInfo);
		if(!talkedSet.contains("状況発言" + gameInfo.getDay()) && st != null){ // 状況発言
			switch ((int)(Math.random() * 6)) {
			case 0:
				List<Agent> wolves = max(gameInfo.getAgentList(), getEstimate().getWerewolfLikeness(), gameInfo.getAgent());
				if(!wolves.isEmpty() && wolves.size() <= 2) {
					talkedSet.add("状況発言" + gameInfo.getDay());
					return r(st + agentsToTalk(gameInfo, wolves, 'か') + "が人狼だと思う<よ>。");
				}
				break;
			case 1:
				Estimate es = getEstimate();
				List<Agent> possesseds = max(gameInfo.getAgentList(), toPossessedLikeness(es.getWerewolfLikeness(), es.getVillagerTeamLikeness()), gameInfo.getAgent());
				if(!possesseds.isEmpty() && possesseds.size() <= 2) {
					talkedSet.add("状況発言" + gameInfo.getDay());
					return r(st + agentsToTalk(gameInfo, possesseds, 'か') + "が狂人だと思う<よ>。");
				}
				break;
			}
		}

		if(!talkedSet.contains("who")){
			talkedSet.add("who");
			List<Agent> as = new ArrayList<>(gameInfo.getAliveAgentList());
			as.remove(gameInfo.getAgent());
			Collections.shuffle(as);
			Agent a = as.get(0);
			tmp.clear();
			tmp.add(r(">>" + a + " " + a + "<さん>、<あなた>は誰が人狼だと<思いますか>？"));
			tmp.add(r(">>" + a + " " + a + "<さん>、<あなた>は誰が狂人だと<思いますか>？"));
			tmp.add(r(">>" + a + " " + a + "<さん>、<あなた>は誰が村人だと<思いますか>？"));
			ret = rnd(tmp, 10);
			if(ret != null) return ret;
		}

		return Talk.SKIP;
	}

	// ランダムで発言を返す。乱数10, 発言パターン3の場合、7割の確率でnullを返すので、確率で発言させることもできる
	private String rnd(List<String> talkCandidates, int numOfRandomPatterns) {
		int random = (int)(Math.random() * numOfRandomPatterns);
		if(random >= talkCandidates.size())
			return null;
		return talkCandidates.get(random);
	}

	private String r(String s) { // replaceの略, キャラクターごとの発言に置換するだけ
		String ret = s;
		for(String key: characterMap.keySet())
			ret = ret.replace("<" + key + ">", characterMap.get(key));
		return ret;
	}

	public String toNaturalLanguageForWhisper(GameInfo gameInfo, String protocol) {		
		if(!Content.validate(protocol)) {
			System.err.println("Mouth: 内部エージェントがプロトコル以外を喋ってる -> " + protocol);
			return Talk.SKIP;
		}
		Content content = new Content(protocol);

		switch (content.getTopic()) {
		case OVER:
			return Talk.OVER;
		case SKIP:
			return Talk.SKIP;
		case COMINGOUT:
			if(content.getTarget().equals(gameInfo.getAgent()) && content.getRole() == Role.VILLAGER)
				return r("<僕>は潜伏する<よ>。");
			return Talk.SKIP;
		case ATTACK:
			return r(content.getTarget() + "を襲撃する<よ>。");
		default:
			return Talk.SKIP;
		}
	}

	private Estimate getEstimate() {
		return (Estimate)player.getPretendVillagerEstimate();
	}
}
