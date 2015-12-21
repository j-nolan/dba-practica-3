package main;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.SingleAgent;

public class DiscoveryBot extends SingleAgent {
	
	// A flag that indicates whether the bot should initialize the subscription to the server.
	// Only one discovery bot should take care of that task.
	private boolean initializer;
	
	// Key to the server
	private String key;
	
	// Role of the robot (fly, parrot, falcon...)
	private Role role;
	
	// The battery, the position of the agent
	private int battery, x, y;
	
	// The world has a given amount of energy that decrease everytime an agent refuels
	// The total remaining amount is saved in this attribute
	private int totalWorldEnergy;
	
	// A flag indicating whether the goal has been found
	private boolean goalFound;
	
	// The internal representation of the map
	Map map;
	
	/**
	 * Constructor
	 * @author James Nolan
	 * @param aid
	 * @param initializer a boolean that indicate if that bot should do the initial login procedure
	 * @throws Exception
	 */
	public DiscoveryBot(AgentID aid, boolean initializer, Map map) throws Exception {
		super(aid);
		this.initializer = initializer;
		this.map = map;
	}
	
	/**
	 * Bot main execution loop. Before entering the loop, it will take care of doing the login
	 * procedure (only for the initializer bot), get the key from the server and check in to the world.
	 * @author James Nolan
	 */
	public void execute() {
		// Is this bot supposed to do the initialization procedure?
		if (initializer) {
			// If so, then send a login subscription to the server
			login();
		}

		// We start by waiting for the key the server sends
		this.key = getKey();
		
		// Once we have the key, the next step is to check in to the world. This will assign
		// a role to the agent
		this.role = checkIn();
		
		// Before moving, ask for a general perception update to see exactly where the robot is
		updatePerception();
		
		// Get into the loop
		Direction nextDirection;
		while (true) {
			// Before moving we need to check if the battery should be refueled
			if (shouldRefuel()) {
				refuel();
			}
			
			// Start by thinking of the next move
			nextDirection = think();
			
			// If a next move has been successfully computed, go in that direction
			if (nextDirection != null) {
				if (!move(nextDirection)) {
					System.out.println("Could not move to next direction. Stop.");
					break;
				}
			} else {
				System.out.println("Could not find next direction. Stop.");
				break;
			}
			
			// Update all the sensors (battery level, position, radar, global energy level,...)
			updatePerception();
		}
		
		// Logout (close sessions and asks server to save traces)
		
		if (initializer) {
			//logout();
		}
	}

	/**
	 * This method asks the agent to perform the initial login procedure. It sends a subscribe
	 * message to the broker. This method should only be called by one of the bots. The bot in charge
	 * of performing the initial login is set by a booolean in the constructor
	 * @author James Nolan
	 */
	private void login() {
		
		// First step is to connect to the server. We connect with the subscription command
		// specifing which world we want to be in
		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject.put("world", "map1");
			
			// Create the ACL message that will send the first command to the server
			ACLMessage msg = new ACLMessage();
			msg.setPerformative(ACLMessage.SUBSCRIBE);
			msg.setSender(this.getAid());
			msg.setReceiver(new AgentID("Izar"));
			msg.setContent(jsonObject.toString());
			
			// Send message
			send(msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Wait for the server to send the key. Once the key is received, the agent that sent
	 * the initial request (the "initializer") to the server will broadcast that key to the
	 * other bots so that they can also checkin to the server
	 * @author James Nolan
	 * @return the key that the server sent for this session
	 */
	private String getKey() {
		String key = null;
		try {
			ACLMessage msg = receiveACLMessage();
			if (msg.getPerformativeInt() == ACLMessage.INFORM) {
				// Check that message contains key
				key = new JSONObject(msg.getContent()).getString("result");
				
				// The initializer broadcasts the key to the other bots
				if (initializer) {
					// Broadcast the key to other bots
					msg.clearAllReceiver();
					msg.addReceiver(new AgentID("bot1"));
					msg.addReceiver(new AgentID("bot2"));
					msg.addReceiver(new AgentID("bot3"));
					msg.addReceiver(new AgentID("bot4"));
					msg.addReceiver(new AgentID("bot5"));
					send(msg);
				}
			} else {
				System.err.println("Unexpected answer " + msg.getPerformativeInt());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return key;
	}
	
	/**
	 * Check in to the world. This method should be executed as soon as the agent recieves the key.
	 * The checkin procedure will give the agent a role in the world (fly, parrot, falcon,..).
	 * @author James Nolan
	 * @return the role that is assigned to the agent
	 */
	private Role checkIn() {
		Role role = null;
		try {
			// Prepare the JSON with the command and key
			JSONObject json = new JSONObject();
			json.put("command", "checkin");
			json.put("key", key);
			
			// Prepare the ACL Message
			ACLMessage msg = new ACLMessage();
			msg.setSender(getAid());
			msg.setReceiver(new AgentID("Izar"));
			msg.setPerformative(ACLMessage.REQUEST);
			msg.setContent(json.toString());
			
			// Send message
			send(msg);
			
			// Wait for answer
			try {
				msg = receiveACLMessage();
				if (msg.getPerformativeInt() == ACLMessage.INFORM) {
					// Get assigned role from server
					System.out.println("I am " + getAid() + " and I decode " + msg.toString());
					role = Role.values()[new JSONObject(msg.getContent()).getInt("rol")];
				} else {
					System.err.println("Server did not assign a role for checkin. Says " + msg.toString());
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		// Return the assigned role
		return role;
	}
	


	/**
	 * Send a request to the server to get the latest sensor values. The method waits for the server's answer.
	 * When it recieves the answer, it updates all the agent's attribute and returns
	 * @author James Nolan
	 */
	private void updatePerception() {
		try {
			// Prepare the key in a json object
			JSONObject json = new JSONObject();
			json.put("key", key);
			
			// Prepare the ACL message for the QUERY_REF
			ACLMessage msg = new ACLMessage();
			msg.setPerformative(ACLMessage.QUERY_REF);
			msg.setSender(getAid());
			msg.setReceiver(new AgentID("Izar"));
			msg.setContent(json.toString());
			
			// send perception message
			send(msg);
			
			// Wait for server answer
			msg = receiveACLMessage();
			
			if (msg.getPerformativeInt() == ACLMessage.INFORM) {
				// Read sensors values
				json = new JSONObject(msg.getContent());
				JSONObject result = json.getJSONObject("result");
				
				// Update attributes
				battery = result.getInt("battery");
				x = result.getInt("x");
				y = result.getInt("y");
				totalWorldEnergy = result.getInt("energy");
				goalFound = result.getBoolean("goal");
				map.update(x, y, result.getJSONArray("sensor"));
				
				System.out.println("Battery : " + battery + ", x : " + x + ", y : " + y + " world energy : " + totalWorldEnergy + ", found goal" + (goalFound ? "yes" : "no"));
				
			} else {
				System.err.println("Server didn't inform us on sensors state, says " + msg.toString());
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Compute the next direction in which the agent should go. This will depend on what strategy we defined
	 * for the agent's role.
	 * @author James Nolan
	 * @return the direction if a next step has been found, or null if the algorithm doesn't find a next step (if stuck for example)
	 */
	private Direction think() {
		// SPecific behaviour for each role. This must be coded further according to our strategy (not finished)
		// For now, the strategy is very basic: each role juste loop over all the directions and goes in the first direction
		// that doesn't crash the robot
		Direction[] directions = Direction.values(); // All available directions
		switch (this.role) {
		case FLY:
			for (Direction direction : directions) {
				if (map.validMove(x, y, direction)) {
					return direction;
				}
			}
			break;
		case PARROT:
			for (Direction direction : directions) {
				if (map.validMove(x, y, direction)) {
					return direction;
				}
			}
			break;
		case FALCON:
			for (Direction direction : directions) {
				if (map.validMove(x, y, direction)) {
					return direction;
				}
			}
			break;
		}
		return null;
	}
	
	/**
	 * Send a message to the server to initiate a movement in a given direction
	 * @param direction the direction the robot should go
	 * @return true if server responded as expected, false if an error occurred
	 */
	private boolean move(Direction direction) {
		JSONObject json = new JSONObject();
		try {
			json.put("command", direction.toString());
			json.put("key", key);
			ACLMessage msg = new ACLMessage();
			msg.setSender(getAid());
			msg.setReceiver(new AgentID("Izar"));
			msg.setPerformative(ACLMessage.REQUEST);
			msg.setContent(json.toString());
			send(msg);
			
			// Wait for server answer
			try {
				msg = receiveACLMessage();
				System.out.println(msg);
				if (msg.getPerformativeInt() == ACLMessage.INFORM) {
					System.out.println("Ok moved (" + x + ", " + y + " => " + direction);
				} else {
					System.err.println("Unexpected answer : " + msg.getPerformativeInt());
					return false;
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
				return false;
			}
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	/**
	 * A simple method that estimates whether the robot has to refuel or not, according to the latest perception update
	 * @return true if it should (because low battery), false otherwise
	 */
	private boolean shouldRefuel() {
		return battery < 5;
	}
	
	/**
	 * Send a "refuel" command to the server
	 */
	private void refuel() {
		System.out.println("REFUELING");
		JSONObject json = new JSONObject();
		try {
			json.put("command", "refuel");
			json.put("key", key);
			ACLMessage msg = new ACLMessage();
			msg.setSender(getAid());
			msg.setReceiver(new AgentID("Izar"));
			msg.setPerformative(ACLMessage.REQUEST);
			msg.setContent(json.toString());
			send(msg);
			
			// Wait for server answer
			try {
				msg = receiveACLMessage();
				System.out.println(msg);
				if (msg.getPerformativeInt() == ACLMessage.INFORM) {
					System.out.println(msg);
				} else {
					System.err.println("Unexpected answer : " + msg.getPerformativeInt());
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
