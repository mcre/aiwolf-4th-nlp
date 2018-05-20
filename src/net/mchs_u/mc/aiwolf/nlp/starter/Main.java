package net.mchs_u.mc.aiwolf.nlp.starter;

import java.io.IOException;
import java.net.SocketTimeoutException;

public class Main {
	public static void main(String[] args) throws InstantiationException, IllegalAccessException, SocketTimeoutException, IOException {
		int gameNum = 100; // ローカル用
		int port = 10000;
		
		String type = null;
		type = "ローカル*5";
		//type = "ローカル*4 + 人間*1";
		//type = "大会5人接続";
		//type = "大会1人接続";
				
		switch (type) {
		case "ローカル*5":
			Starter.startServer(port, gameNum, 5000);
			for(int i = 0; i < 5; i++)
				Starter.startAIClient("localhost", port);
			break;
		case "ローカル*4 + 人間*1":
			Starter.startServer(port, gameNum, 300000);
			for(int i = 0; i < 4; i++)
				Starter.startAIClient("localhost", port);
			Starter.startHumanClient("localhost", port);
			break;
		case "大会5人接続":
			for(int i = 0; i < 5; i++)
				Starter.startAIClient("kanolab.net", port);
			break;
		case "大会1人接続":
			Starter.startAIClient("kanolab.net", port);
			break;
		}
		
	}

}
