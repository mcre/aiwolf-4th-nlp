package net.mchs_u.mc.aiwolf.nlp.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.mchs_u.mc.aiwolf.nlp.chaser.Ear;

public class TransratedMapChecker {
	public static void main(String[] args) {
		Map<String, List<String>> transratedMap = Ear.load(); 

		List<String> keys = new ArrayList<>(transratedMap.keySet());
		Collections.sort(keys);
		
		for(String k: keys) {
			List<String> values = transratedMap.get(k);
			System.out.print(k + " -> ");
			for(String v: values)
				System.out.print(v + ", ");
			System.out.println();
		}
	}
}
