package main;

import java.awt.Dimension;
import java.awt.Point;

import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.AgentsConnection;
import es.upv.dsic.gti_ia.core.SingleAgent;

public class MultiBots {

	/**
	 * Program entry point. Connects to broke, build the initial agents and start them.
	 * One of the agents is designated as the initializer. That agent will be in charge of
	 * initializing the subscription to the world
	 * @author James Nolan
	 * @param args not used
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {

		// Connect to broker server
		AgentsConnection.connect("isg2.ugr.es", 6000, "Izar", "Boyero", "Pamuk", false);
		
		// Prepare three maps, one for each bot
		Map map1 = new Map("Bot 1", new Point(0, 0), new Dimension(100, 100));
		Map map2 = new Map("Bot 2", new Point(100, 0), new Dimension(100, 100));
		Map map3 = new Map("Bot 3", new Point(200, 0), new Dimension(100, 100));
		Map map4 = new Map("Bot 4", new Point(300, 0), new Dimension(100, 100));
		Map map5 = new Map("Bot 5", new Point(400, 0), new Dimension(100, 100));
		Map map6 = new Map("Bot 6", new Point(500, 0), new Dimension(100, 100));

        
		// Create and start discovery agents
		SingleAgent bot1 = new DiscoveryBot(new AgentID("bot1"), false, map1),
				bot2 = new DiscoveryBot(new AgentID("bot2"), false, map2),
				bot3 = new DiscoveryBot(new AgentID("bot3"), false, map3),
				bot4 = new DiscoveryBot(new AgentID("bot4"), false, map4),
				bot5 = new DiscoveryBot(new AgentID("bot5"), false, map5),
				bot6 = new DiscoveryBot(new AgentID("bot6"), true, map6); // this one will do the login
		
		// Start all bots
		bot1.start();
		bot2.start();
		bot3.start();
		bot4.start();
		bot5.start();
		bot6.start();
	}
}
