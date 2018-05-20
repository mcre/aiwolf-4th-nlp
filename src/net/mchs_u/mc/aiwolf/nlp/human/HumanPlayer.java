package net.mchs_u.mc.aiwolf.nlp.human;

import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Player;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Status;
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.data.Vote;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

import net.mchs_u.mc.aiwolf.nlp.common.Transrater;

public class HumanPlayer implements Player {
	private GameInfo gameInfo = null;
	private int talkListHead, wisperListHead;
	private boolean finished;
	
	public HumanPlayer() {
		finished = false;
	}
	
	public String getName() {
		output("あなたの名前を入力してください");
		return returnText();
	}

	public void update(GameInfo gameInfo) {
		this.gameInfo = gameInfo;
		
		List<Talk> talkList = gameInfo.getTalkList();
		for(int i = talkListHead; i < talkList.size(); i++){
			Talk t = talkList.get(talkListHead);
			if(!t.getText().equals(Talk.SKIP) && !t.getText().equals(Talk.OVER))
				output(t.getAgent() + "「" + t.getText() + "」");
			talkListHead++;
		}
		
		List<Talk> wisperList = gameInfo.getWhisperList();
		for(int i = wisperListHead; i < wisperList.size(); i++){
			Talk t = wisperList.get(wisperListHead);
			if(!t.getText().equals(Talk.SKIP) && !t.getText().equals(Talk.OVER))
				output(t.getAgent() + " （" + t.getText() + "）");
			wisperListHead++;
		}
		
	}

	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		this.gameInfo = gameInfo;
		output("====== Agent一覧");
		for(Agent a: gameInfo.getAgentList())
			output(a.toString());
		output("======");
		output("あなたは " + gameInfo.getAgent() + " です");
	}

	public void dayStart() {
		talkListHead = 0;
		wisperListHead = 0;
		dayStartOrFinish();
	}

	public String talk() {
		if(gameInfo.getDay() <= 0) {
			if(gameInfo.getTalkList().size() <= 0)
				output("挨拶を入力してください");
			else {
				output("あなた(" + gameInfo.getAgent() + ")の役職は【" + Transrater.roleToString(gameInfo.getRole()) + "】です");
				if(gameInfo.getRole() == Role.WEREWOLF) {
					String wolvesText = "他の人狼は";
					int count = 0;
					for(Agent a: gameInfo.getAgentList()) {
						if(gameInfo.getRoleMap().get(a) == Role.WEREWOLF && a != gameInfo.getAgent()) {
							count++;
							wolvesText += "、" + a;
						}
					}
					if(count == 0)
						wolvesText += "いません。";
					else
						wolvesText += "です。";
					output(wolvesText);
				}
				return Talk.OVER;
			}
		} else {
			output("会話を入力してください");
		}
		return returnText();
	}

	public String whisper() {
		output("囁きを入力してください");
		return returnText();
	}

	public Agent vote() {
		List<Vote> vl = gameInfo.getLatestVoteList();
		if(vl.size() > 0) {
			output("====== 投票先一覧");
			for(Vote v: vl)
				output(v.getAgent() + "\t->\t" + v.getTarget());
			output("======");
			output("再投票");
		}
		output("投票先AgentIDを入力してください");
		return selectAgent();
	}

	public Agent attack() {
		if(gameInfo.getLatestExecutedAgent() != null)
			output("今夜追放されるのは " + gameInfo.getLatestExecutedAgent() + " です。");
		output("襲撃先AgentIDを入力してください");
		return selectAgent();
	}

	public Agent divine() {
		if(gameInfo.getLatestExecutedAgent() != null)
			output("今夜追放されるのは " + gameInfo.getLatestExecutedAgent() + " です。");
		output("占い先AgentIDを入力してください");
		return selectAgent();
	}

	public Agent guard() {
		if(gameInfo.getLatestExecutedAgent() != null)
			output("今夜追放されるのは " + gameInfo.getLatestExecutedAgent() + " です。");
		output("護衛先AgentIDを入力してください");
		return selectAgent();
	}

	public void finish() {
		if(finished) //finishが2回呼ばれるぽいので回避
			return;
		dayStartOrFinish();
		Map<Agent, Role>   rm = gameInfo.getRoleMap();
		Map<Agent, Status> sm = gameInfo.getStatusMap();
		
		output("====== 最終結果");
		for(Agent a: gameInfo.getAgentList())
			output(a + "\t" + Transrater.roleToString(rm.get(a)) + "\t" + Transrater.statusToString(sm.get(a)));
		output("======");
		
		for(Agent a: gameInfo.getAliveAgentList()) {
			if(rm.get(a) == Role.WEREWOLF) {
				output("ゲーム終了、人狼陣営の勝利！");
				return;
			}
		}
		output("ゲーム終了、村人陣営の勝利！");
		finished = true;
	}
	
	private void dayStartOrFinish() {
		List<Vote> vl = gameInfo.getVoteList();
		if(vl.size() > 0) {
			output("====== 投票先一覧");
			for(Vote v: vl)
				output(v.getAgent() + "\t->\t" + v.getTarget());
			output("======");
		}
		
		Agent ea = gameInfo.getExecutedAgent();
		if(ea != null)
			output(ea + " が追放されました。");
		
		Agent aa = gameInfo.getAttackedAgent();
		if(aa != null)
			output(aa + " を襲撃しました。");
		
		Agent ga = gameInfo.getGuardedAgent();
		if(ga != null)
			output(ga + " を護衛しました。");
		
		output("+++++++++++++++++++++++++++++ " + gameInfo.getDay() + "日目");
		
		List<Agent> ldal = gameInfo.getLastDeadAgentList();
		for(Agent a: ldal)
			output(a + " が死亡していました。");
		if(ldal.size() <= 0 && gameInfo.getDay() > 1) {
			output("死亡した人はいませんでした。");
		}

		Judge m = gameInfo.getMediumResult();
		if(m != null)
			output(m.getTarget() + " の霊能結果は【" + Transrater.speciesToString(m.getResult()) + "】でした。");
		
		Judge d = gameInfo.getDivineResult();
		if(d != null)
			output(d.getTarget() + " の占い結果は【" + Transrater.speciesToString(d.getResult()) + "】でした。");
		
		if(gameInfo.getDay() > 1) {
			output("====== 生存Agent一覧");
			for(Agent a: gameInfo.getAliveAgentList())
				output(a.toString());
			output("======");
		}
	}

	private Agent selectAgent() {
		Agent agent = null;
		try {
			agent = Agent.getAgent(Integer.parseInt(input()));
		} catch(Exception e) {
			agent = null;
		}
		output("< " + agent);
		return agent;
	}

	private String returnText() {
		String text = Talk.OVER;
		try {
			text = input();
		} catch(Exception e) {
			text = null;
		}
		output("< " + text);
		return text;
	}
	
	private void output(String text) {
		System.out.println("* " + text);
	}
	
	@SuppressWarnings("resource")
	private String input() {
		System.out.print("* > ");
		return (new Scanner(System.in)).nextLine();
	}
}
