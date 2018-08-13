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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServerLogUtil {
	private static final String DIR = "serverLog/";

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
		(new File(DIR)).mkdir();

		String html = downloadString(url);
		Pattern p = Pattern.compile("a href=\"((?!.*_err_).*?\\.log)");
		Matcher m = p.matcher(html);
		while(m.find()) {
			String filename = m.group(1);
			String text = downloadString(url + filename);
			try(BufferedWriter bw = new BufferedWriter(new FileWriter(DIR + filename))) {
				bw.write(text);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void getTalkList() {
		Set<String> set = new HashSet<String>();
		File[] files = (new File(DIR)).listFiles();
		Pattern p = Pattern.compile("^[0-9]+?,talk,[0-9]+?,[0-9]+?,[0-9]+?,([^%]*?)$");
		
		for(File f: files) {
			try(BufferedReader br = new BufferedReader(new FileReader(f))) {
				String line = null;
				while ((line = br.readLine()) != null) {
					Matcher m = p.matcher(line);
					if(m.find()) {
						String talk = m.group(1);
						talk = talk.replaceAll("Agent\\[[0-9]{2}\\]", "Agent[99]");
						set.add(talk);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		List<String> list = new ArrayList<String>(set);
		Collections.sort(list);
		for(String s: list)
			System.out.println(s);
		System.out.println(set.size()); // 4637
	}

	public static void main(String[] args) {
		//downloadServerLogs(args[0]);
		getTalkList();
	}
}
