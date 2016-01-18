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
		Map map1 = new Map("Bot 1", new Point(0, 0), new Dimension(120, 120));
		Map map2 = new Map("Bot 2", new Point(150, 0), new Dimension(120, 120));
		Map map3 = new Map("Bot 3", new Point(300, 0), new Dimension(120, 120));
		Map map4 = new Map("Bot 4", new Point(450, 0), new Dimension(120, 120));
		Map map5 = new Map("Master", new Point(600, 0), new Dimension(200, 200));

        
		// Create and start discovery agents
		SingleAgent bot1 = new DiscoveryBot(new AgentID("bot11"), false, map1, "bot11");
		SingleAgent	bot2 = new DiscoveryBot(new AgentID("bot22"), false, map2, "bot22");
		SingleAgent	bot3 = new DiscoveryBot(new AgentID("bot33"), false, map3, "bot33");
		SingleAgent	bot4 = new DiscoveryBot(new AgentID("bot44"), true, map4, "bot44");
		SingleAgent master = new MasterOfDrones(new AgentID("MasterOfDrones"), map5);
		// Start all bots
		bot1.start();
		bot2.start();
		bot3.start();
		bot4.start();
		master.start();
	}
}
