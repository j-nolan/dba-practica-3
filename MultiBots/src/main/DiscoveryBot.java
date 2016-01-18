package main;

import java.util.ArrayList;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.SingleAgent;

public class DiscoveryBot extends SingleAgent {
	
	private String botName;
	// A flag that indicates whether the bot should initialize the subscription to the server.
	// Only one discovery bot should take care of that task.
	private boolean initializer;
	
	// Key to the server
	private String key;
	
	// Role of the robot (fly, parrot, falcon...)
	private Role role;
	
	// The battery, the position of the agent
	private int battery, x, y;
	
	// The possition of the goal (-1 at start)
	private int x_goal, y_goal;
	
	// The world has a given amount of energy that decrease everytime an agent refuels
	// The total remaining amount is saved in this attribute
	private int totalWorldEnergy;
	
	// A flag indicating whether the goal has been found
	private boolean goalFound;
	
	// A flag indicating if the drone has the permit to move from MasterOfDrones
	private boolean move;
	
	// The action the drone wants to do on each cycle
	private String wantedMove;
	
	// The internal representation of the map
	Map map;
	
	// Array with the information of the sensor
	private ArrayList<Integer> sensor;
	
	
	// State as: 0 = ready, 1 = objectiveSeen, 2 = objectiveFound
	private int state;
	/**
	 * Constructor
	 * @author James Nolan
	 * @param aid
	 * @param initializer a boolean that indicate if that bot should do the initial login procedure
	 * @throws Exception
	 */
	public DiscoveryBot(AgentID aid, boolean initializer, Map map, String botName) throws Exception {
		super(aid);
		this.initializer = initializer;
		this.map = map;
		this.botName = botName;
		this.state = 0;
		this.x_goal = y_goal = -1;
	}
	
	/**
	 * Bot main execution loop. Before entering the loop, it will take care of doing the login
	 * procedure (only for the initializer bot), get the key from the server and check in to the world.
	 * @author James Nolan, Fernando Suarez
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
		if(role==Role.FLY) sensor = new ArrayList<Integer>(9);
		if(role==Role.PARROT) sensor = new ArrayList<Integer>(25);
		if(role==Role.FALCON) sensor = new ArrayList<Integer>(121);

		// Get into the loop
		Direction nextDirection;
		boolean end = false;
		while (!end) {
			// Before moving, ask for a general perception update to see exactly where the robot is
			this.updatePerception();
			this.sendPerception();
//			wantedMove=sendPerception();
//			if (!wantedMove.equals("idle")) {
//				if (wantedMove.equals("refuel")) refuel();
//				else move(wantedMove);
//			}
			
				
			switch (this.role) {
				case FLY:
					if (!goalFound) {
						Direction d = this.think();
						if (this.requestMove(d))
							this.move(d.toString());
					} else {
						end = true;
					}
					break;
				case PARROT:
	
					break;
				case FALCON:
						Direction d = this.think();
						if (this.requestMove(d))
							this.move(d.toString());
					break;
			}
		}
		
		// Logout (close sessions and asks server to save traces)
		
		/*if (initializer) {
			//logout();
		}*/
	}
	
	/**
	 * This method ask permission to MasterOfDrones if he can refuel
	 * @author Zacar�as Romero
	 */
	
	private boolean askRefuel() {
		JSONObject json = new JSONObject();
		// Send request to master
		try {
			json.put("command", "refuel");
			ACLMessage msg = new ACLMessage();
			msg.setSender(getAid());
			msg.setReceiver(new AgentID("MasterOfDrones"));
			msg.setPerformative(ACLMessage.INFORM);
			msg.setContent(json.toString());
			send(msg);
			
			// Wait for server answer
			try {
				msg = receiveACLMessage();
				//System.out.println(msg);
				if (msg.getPerformativeInt() == ACLMessage.INFORM) {
					System.out.println(botName + ": Refueled");
				} else if(msg.getPerformativeInt() == ACLMessage.REFUSE) {
						System.out.println(botName + ": Denied Refuel");
						return false;
				} else {
					System.err.println(botName + ": Unexpected answer askRefuel : " + msg.getPerformativeInt());
					return false;
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
				return false;
			}
		} catch (JSONException e) {
			e.printStackTrace();
			return false;
		}
		return true;
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
					//msg.addReceiver(new AgentID("bot4"));
					send(msg);
				}
			} else {
				System.err.println("Unexpected answer on getKey " + msg.getPerformativeInt());
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
					//System.out.println("I am " + getName() + " and I decode " + msg.toString());
					role = Role.values()[new JSONObject(msg.getContent()).getInt("rol")];
					System.out.println("I am " + getName() + " and my role is " + role);
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
				JSONArray jArray = result.getJSONArray("sensor");
				if(goalFound) state = 2;
				else {
					sensor.clear();
					for(int i=0;i<jArray.length();i++) {
						sensor.add(i,jArray.getInt(i));
					}
					if(sensor.contains(3)) state = 1;
				}
				//map.update(x, y, result.getJSONArray("sensor"));
				map.update(x, y, jArray);
				
				System.out.println(botName + " --> Battery : " + battery + ", x : " + x + ", y : " + y + " world energy : " + totalWorldEnergy + ", found goal: " + (goalFound ? "yes" : "no"));
				
			} else {
				System.err.println("Server didn't inform us on sensors state, says " + msg.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Send current perception to MasterOfDrones so he can make useful choices
	 * @author Zacar�as Romero Sellamitou
	 * @throws InterruptedException 
	 */
	
	private String sendPerception(){
		JSONObject json = new JSONObject();
		String st = null;
		try {
			json.put("battery", battery);
			json.put("x", x);
			json.put("y", y);
			json.put("state", state);
			json.put("total", totalWorldEnergy);
			json.put("sensor", sensor);
			json.put("role", role.toString());
			
			ACLMessage msg = new ACLMessage();
			msg.setPerformative(ACLMessage.INFORM_REF);
			msg.setSender(getAid());
			msg.setReceiver(new AgentID("MasterOfDrones"));
			msg.setContent(json.toString());
			
			send(msg);
			
			msg = receiveACLMessage();
			//System.out.println(msg);
			if (msg.getPerformativeInt() == ACLMessage.INFORM) {
				JSONObject js = new JSONObject(msg.getContent());
				st = js.getString(botName);
			} else if(msg.getPerformativeInt() == ACLMessage.REFUSE) {
			} else {
				System.err.println(botName + ": Unexpected answer on sendPerception : " + msg.getPerformativeInt());
			}
		} catch (JSONException | InterruptedException e) {
			e.printStackTrace();
		}
		return st;
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
		Direction d = null;
		switch (this.role) {
			case FLY:
				ACLMessage msg;
				try {
					if (state == 0) {
						msg = this.receiveACLMessage();
						// No se mueve hasta que otro bot encuentra el objetivo y recibe sus coordenadas
						if (msg.getPerformativeInt() == ACLMessage.INFORM) {
							JSONObject content = new JSONObject(msg.getContent()).getJSONObject("goal");
							state = 1;
							x_goal = content.getInt("x_goal");
							y_goal = content.getInt("y_goal");
							d = this.getDirectionToGoalFly();
						}
					} else if (state == 1){
						d = this.getDirectionToGoalFly();
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (JSONException e) {
					e.printStackTrace();
				}
				
				break;
			case PARROT:
				// sería similar a FALCON
				break;
			case FALCON:
				if (state == 0) {
					// buscar objetivo
				} else if (state == 1) {
					// ir al objetivo
				}
				break;
		}
		return d;
	}
	
	/**
	 * Send a message to the server to initiate a movement in a given direction
	 * @param direction the direction the robot should go
	 * @return true if server responded as expected, false if an error occurred
	 */
	private boolean move(String direction) {
		JSONObject json = new JSONObject();
		try {
			json.put("command", direction);
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
				//System.out.println(msg);
				if (msg.getPerformativeInt() == ACLMessage.INFORM) {
					System.out.println(botName + ": Ok moved (" + x + ", " + y + ") => " + direction);
				} else {
					System.err.println(botName + ": Unexpected answer on move : " + msg.getPerformativeInt());
					JSONObject js = new JSONObject(msg.getContent());
					System.err.println(js.getString("result") + " " + js.getString("details"));
					return false;
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
				return false;
			}
			
		} catch (JSONException e) {
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
				//System.out.println(msg);
				if (msg.getPerformativeInt() == ACLMessage.INFORM) {
					System.out.println("REFUELING");
				} else {
					System.err.println("Unexpected answer on refuel : " + msg.getPerformativeInt());
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @author Fernando Suarez
	 * @return the next direction to go to the goal (only for fly)
	 */
	private Direction getDirectionToGoalFly () {
		Direction d = null;
		
		int x_aux = x_goal-x;
		int y_aux = y_goal-y;
		
		if (x_aux>0 && y_aux>0) 			d=Direction.SOUTH_EAST;
		else if (x_aux>0 && y_aux<0) 	d=Direction.NORTH_EAST;
		else if (x_aux>0 && y_aux==0) 	d=Direction.EAST;
		else if (x_aux<0 && y_aux>0)	d=Direction.SOUTH_WEST;
		else if (x_aux<0 && y_aux<0)	d=Direction.NORTH_WEST;
		else if (x_aux<0 && y_aux==0)	d=Direction.WEST;
		else if (x_aux==0 && y_aux>0)	d=Direction.SOUTH;
		else if (x_aux==0 && y_aux<0)	d=Direction.NORTH;
		
		return d;
	}
	
	/**
	 * @author Fernando Suarez
	 * @return true if the movement is possible (no crash with other bot)
	 */
	private boolean requestMove (Direction d) {
		boolean move = false;
		int x_aux = this.x+d.getXCoord();
		int y_aux = this.y+d.getYCoord();
		JSONObject json = new JSONObject();
		ACLMessage msg = new ACLMessage();
		
		try {
			json.put("x", x_aux);
			json.put("y", y_aux);
			msg.setPerformative(ACLMessage.QUERY_IF);
			msg.setContent(json.toString());
			msg.setSender(this.getAid());
			msg.setReceiver(new AgentID("MasterOfDrones"));
			this.send(msg);
			
			msg = this.receiveACLMessage();
			if (msg.getPerformativeInt() == ACLMessage.CONFIRM) {
				move = true;
			} else {
				move = false;
			}
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		return move;
	}
}
