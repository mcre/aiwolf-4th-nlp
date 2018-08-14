package net.mchs_u.mc.aiwolf.nlp.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.net.GameInfo;

import net.mchs_u.mc.aiwolf.nlp.chaser.Ear;

public class ServerLogUtil {
	private static final String SERVER_LOG_DIR = "serverLog/";
	private static final String SERVER_LOG_INVESTIGATION_DIR = "serverLogInvestigation/";
	private static final String ANSWER_FILE = SERVER_LOG_INVESTIGATION_DIR + "answer.tsv";

	private static String downloadString(String url) {
		StringBuilder ret = new StringBuilder();
		try(BufferedReader br = new BufferedReader(new InputStreamReader((new URL(url)).openStream()))) {
			String line = null;
			while ((line = br.readLine()) != null)
				ret.append(line + "\n");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret.toString();
	}

	public static void downloadServerLogs(String url) {
		(new File(SERVER_LOG_DIR)).mkdir();

		String html = downloadString(url);
		Pattern p = Pattern.compile("a href=\"((?!.*_err_).*?\\.log)");
		Matcher m = p.matcher(html);
		while(m.find()) {
			String filename = m.group(1);
			String text = downloadString(url + filename);
			try(BufferedWriter bw = new BufferedWriter(new FileWriter(SERVER_LOG_DIR + filename))) {
				bw.write(text);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static List<String> getTalkList() {
		Set<String> set = new HashSet<String>();
		File[] files = (new File(SERVER_LOG_DIR)).listFiles();
		Pattern p = Pattern.compile("^[0-9]+?,talk,[0-9]+?,[0-9]+?,[0-9]+?,([^%]*?)$");
		
		for(File f: files) {
			try(BufferedReader br = new BufferedReader(new FileReader(f))) {
				String line = null;
				while ((line = br.readLine()) != null) {
					Matcher m = p.matcher(line);
					if(m.find()) {
						String talk = m.group(1);
						talk = talk.replaceAll("Agent\\[[0-9]{2}\\]", "Agent[01]");
						set.add(talk);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		List<String> list = new ArrayList<String>(set);
		Collections.sort(list);
		return list;
	}
	
	public static List<String> naturalLanguageToProtocol(String naturalLanguage) {
		Ear ear = new Ear(new net.mchs_u.mc.aiwolf.dokin.McrePlayer());
		ear.initialize();
		ear.dayStart();
		ear.setMessageOff(true);
		
		GameInfo gameInfo = new GameInfo(){ public Agent getAgent() {return Agent.getAgent(1);}};
		return ear.toProtocolsForTalk(gameInfo, Agent.getAgent(99), naturalLanguage);
	}
		
	private static Map<String, String> loadTsv(String filename) {
		Map<String, String> ret = new HashMap<String, String>();
		
		try(BufferedReader br = new BufferedReader(new FileReader(filename))) {
			String line = null;
			while ((line = br.readLine()) != null) {
				String[] columns = line.split("\t");
				ret.put(columns[0], columns[1].replace("[]", "[Skip]"));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}
	
	public static void test() {
		(new File(SERVER_LOG_INVESTIGATION_DIR)).mkdir();
		Map<String, String> answer = loadTsv(ANSWER_FILE);
		int okCount = 0;
		int ngCount = 0;
		int ukCount = 0; //unknown
		List<String> ngList = new ArrayList<String>();
		List<String> ukList = new ArrayList<String>();
		
		for(String nl: getTalkList()) {
			System.out.println(nl);
			String protocol = naturalLanguageToProtocol(nl).toString();
			if(!answer.containsKey(nl)) {
				ukCount++;
				ukList.add(nl + "\t" + protocol + "\t");
			} else if (answer.get(nl).equals(protocol)) {
				okCount++;
			} else {
				ngCount++;
				ngList.add(nl + "\t" + protocol + "\t" + answer.get(nl));
			}
		}
		
		int allCount = okCount + ngCount + ukCount;
		String rate = String.format("%5.1f%%", ((double)okCount / (double)allCount) * 100.0 );
		String result = okCount + "/" + allCount + " " + rate;
		System.out.println(result);
		
		try(BufferedWriter bw = new BufferedWriter(new FileWriter(SERVER_LOG_INVESTIGATION_DIR + "nglist_" + (new Date()).getTime() + ".tsv"))) {
			bw.write(result);
			bw.newLine();
			for(String s: ngList) {
				bw.write(s);
				bw.newLine();
				bw.flush();
			}
			for(String s: ukList) {
				bw.write(s);
				bw.newLine();
				bw.flush();
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		//downloadServerLogs(args[0]);
		test();
	}
}
