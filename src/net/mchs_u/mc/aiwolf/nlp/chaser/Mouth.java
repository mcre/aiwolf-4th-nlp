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

	public Mouth(McrePlayer player) {
		this.player = player;
	}

	public void initialize(GameInfo gameInfo) {
		talkedSet = new HashSet<>();
		characterMap = Character.getCharacterMap(gameInfo.getAgent().getAgentIdx());
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
				if(getEstimate().isPowerPlay()) // もうPPされてる場合
					return r("わおーん、<僕>は人狼だ<よ>。");
				else // PP発動する場合
					return r("もう<僕>たちのほうが多いよう<だね>。わおーん、<僕>は人狼だ<よ>。");
			}
			if(content.getRole() == Role.POSSESSED) {
				return r("ふふふふ、<僕>が狂人だ<よ>！");
			}
			return r("<僕>は" + Transrater.roleToString(content.getRole()) + "だ<よ>。");
		case DIVINED:
			String r = Transrater.speciesToString(content.getResult());
			t = content.getTarget();
			switch ((int)(Math.random() * 5)) {
			case 0: return r(t + "<さん>の占い結果は、" + r + "だった<よ>。");
			case 1: return r(t + "<さん>の占いの結果は、" + r + "だった<よ>。");
			case 2: return r(t + "<さん>を占ったら、" + r + "だった<よ>。");
			case 3: return r(t + "<さん>を占った結果は、" + r + "だった<よ>。");
			case 4: return r("昨日の占い結果だ<よ>、" + t + "<さん>は" + r + "だった<よ>。");
			}
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
					switch ((int)(Math.random() * 2)) {
					case 0:
						return r(t + "<さん>があやしいと思うから投票する<よ>。");
					case 1:
						return r(t + "<さん>が人狼だと思うから投票する<よ>。");
					}
				} else {
					switch ((int)(Math.random() * 2)) {
					case 0:
						return r("さっきと状況が変わったから、やっぱり" + t + "<さん>に投票する<よ>。");
					case 1:
						return r("やっぱり" + t + "<さん>が人狼だと思う。投票する<よ>。");
					}
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
	
	private static List<Agent> max(Collection<Agent> candidate, Map<Agent, Double> likeness) {
		List<Agent> ret = new ArrayList<>();
		if(likeness == null)
			return ret;
		double max = Collections.max(likeness.values());
		for(Agent agent: candidate)
			if(Math.abs(max - likeness.get(agent)) < EPS)
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
		if(getEstimate().isPowerPlay()) { // PPモード 
			if(!talkedSet.contains("パワープレイ反応")){
				talkedSet.add("パワープレイ反応");
				if(gameInfo.getRole() == Role.WEREWOLF) { // 人狼
					return r("食べちゃう<よ>ー！");
				} else if(gameInfo.getRole() == Role.POSSESSED) { // 狂人
					return "うひゃひゃひゃひゃひゃひゃひゃ！";
				} else { // 村人チーム
					return "え！　助けて！";
				}
			}
			return Talk.SKIP;
		}

		// 共通反応
		if(gameInfo.getLastDeadAgentList().size() > 0 && gameInfo.getDay() == 2) { // 2日目で襲撃死した人がいる場合
			if(!talkedSet.contains("襲撃反応")){
				talkedSet.add("襲撃反応");
				switch ((int)(Math.random() * 5)) {
				case 0: return "本当に襲われるなんて。";
				case 1: return r(gameInfo.getLastDeadAgentList().get(0) + "<さん>が死ん<じゃった>。");
				}
			}
		}

		if(getEstimate().getCoMap().get(gameInfo.getAgent()) == Role.SEER) { // 自分が占い師COしてるとき
			if(getEstimate().getCoSet(Role.SEER).size() == 2) { //二人COしているとき
				if(!talkedSet.contains("対抗占い師反応")){
					talkedSet.add("対抗占い師反応");
					Set<Agent> coSeers = getEstimate().getCoSet(Role.SEER);
					coSeers.remove(gameInfo.getAgent());
					Agent t = (Agent)coSeers.toArray()[0];

					switch ((int)(Math.random() * 6)) {
					case 0: return r(t + "<さん>は嘘をついて<います>！　<僕>が本当の占い師<です>！");
					case 1: return r(t + "<さん>は偽物<です>！　<僕>こそが本当の占い師<です>！");
					case 2: return r(">>" + t + " " + t + "<さん>、<あなた>が人狼<なのですか>！？");
					case 3: return r(">>" + t + " " + t + "<さん>、<あなた>は狂人<なのですか>！？");
					}
				}
			}
		} else {
			if(getEstimate().getCoSet(Role.SEER).size() == 2) { //二人COしているとき
				if(!talkedSet.contains("二人占い師反応")){
					talkedSet.add("二人占い師反応");
					switch ((int)(Math.random() * 5)) {
					case 0: return "どっちが本当の占い師なんだろう。";
					}
				}
			}
		}

		for(String answer: answers) { //Earから渡されたAnswer
			if(!talkedSet.contains("answer:" + answer)){
				talkedSet.add("answer:" + answer);
				
				Agent voteTarget = player.getVoteTarget();
				if(voteTarget != null) {
					if(answer.startsWith(">>" + voteTarget + " "))
						return r(answer.replace("#<さん>", "<あなた>"));
					else
						return r(answer.replace("#", voteTarget.toString()));
				}
			}
		}
		
		// 8ターン目以降(7ターン目を読み込んでいる時)はここより下の発言を抑制
		
		int talkSize = gameInfo.getTalkList().size();
		if(talkSize > 0)
			if(gameInfo.getTalkList().get(talkSize - 1).getTurn() > 6)
				return Talk.SKIP;
		
		List<Agent> candidate = gameInfo.getAgentList();
		candidate.remove(gameInfo.getAgent());
		String st = makeStatusTalk(gameInfo);
		if(!talkedSet.contains("状況発言" + gameInfo.getDay()) && st != null){ // 状況発言
			switch ((int)(Math.random() * 6)) {
			case 0:
				List<Agent> wolves = max(candidate, getEstimate().getWerewolfLikeness());
				if(!wolves.isEmpty() && wolves.size() <= 2) {
					talkedSet.add("状況発言" + gameInfo.getDay());
					return r(st + agentsToTalk(gameInfo, wolves, 'か') + "が人狼だと思う<よ>。");
				}
				break;
			case 1:
				Estimate es = getEstimate();
				List<Agent> possesseds = max(candidate, toPossessedLikeness(es.getWerewolfLikeness(), es.getVillagerTeamLikeness()));
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
			switch ((int)(Math.random() * 10)) {
			case 0: return r(">>" + a + " " + a + "<さん>、<あなた>は誰が人狼だと<思いますか>？");
			//case 1: return r(">>" + a + " " + a + "<さん>、<あなた>は誰が狂人だと<思いますか>？");
			//case 2: return r(">>" + a + " " + a + "<さん>、<あなた>は誰が村人だと<思いますか>？");
			}
		}

		return Talk.SKIP;
	}
	
	private String r(String s) { // replace
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
