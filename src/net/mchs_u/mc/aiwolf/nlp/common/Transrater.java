package net.mchs_u.mc.aiwolf.nlp.common;

import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.data.Status;

public class Transrater {
	public static String roleToString(Role role) {
		switch (role) {
		case BODYGUARD:
			return "狩人";
		case MEDIUM:
			return "霊媒師";
		case POSSESSED:
			return "狂人";
		case SEER:
			return "占い師";
		case VILLAGER:
			return "村人";
		case WEREWOLF:
			return "人狼";
		default:
			return null;
		}
	}

	public static String speciesToString(Species species) {
		switch (species) {
		case HUMAN:
			return "人間";
		case WEREWOLF:
			return "人狼";
		default:
			return null;
		}
	}
	
	public static String statusToString(Status status) {
		switch (status) {
		case ALIVE:
			return "生存";
		case DEAD:
			return "死亡";
		default:
			return null;
		}
	}
}
