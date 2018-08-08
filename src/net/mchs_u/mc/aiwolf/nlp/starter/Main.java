package net.mchs_u.mc.aiwolf.nlp.starter;

import java.io.IOException;
import java.net.SocketTimeoutException;

public class Main {
	public static void main(String[] args) throws InstantiationException, IllegalAccessException, SocketTimeoutException, IOException {
		String type = "local5";
		String server = "";
		int port = 10000;
		int gameNum = 100; // ローカル用
		
		for(String arg: args) {
			if(arg.startsWith("type="))
				type = arg.replaceFirst("type=", "");
			if(arg.startsWith("server="))
				server = arg.replaceFirst("server=", "");
			if(arg.startsWith("port="))
				port = Integer.parseInt(arg.replaceFirst("port=", ""));
			if(arg.startsWith("gameNum="))
				gameNum = Integer.parseInt(arg.replaceFirst("gameNum=", ""));
		}
				
		switch (type) {
		case "local5":
			Starter.startServer(port, gameNum, 5000);
			for(int i = 0; i < 5; i++)
				Starter.startAIClient("localhost", port);
			break;
		case "local4human1":
			Starter.startServer(port, gameNum, 300000);
			for(int i = 0; i < 4; i++)
				Starter.startAIClient("localhost", port);
			Starter.startHumanClient("localhost", port);
			break;
		case "remote5":
			for(int i = 0; i < 5; i++)
				Starter.startAIClient(server, port);
			break;
		case "remote1":
			Starter.startAIClient(server, port);
			break;
		}
		
	}

}
