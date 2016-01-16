package main;

import java.util.ArrayList;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.SingleAgent;

public class MasterOfDrones extends SingleAgent {
	
	// The battery, the position of the agents
	private ArrayList<Integer> battery;
	private ArrayList<Integer> x;
	private ArrayList<Integer> y;
	private ArrayList<String> roles;
	
	// The state of each drone, can be: 0 = ready, 1 = objectiveSeen, 2 = objectiveFound.
	private ArrayList<Integer> state;
	
	// The world has a given amount of energy that decrease everytime an agent refuels
	// The total remaining amount is saved in this attribute
	private int totalWorldEnergy;
	
	private ArrayList<String> wantedMoves;
	
	// The internal representation of the map
	Map map;
	
	/**
	 * Constructor
	 * @author Zacarías Romero Sellamitou
	 * @param aid
	 * @throws Exception
	 */
	public MasterOfDrones(AgentID aid, Map map) throws Exception {
		super(aid);
		System.out.println("MASTER!!");
		this.map = map;
		this.battery = new ArrayList<Integer>(5);
		this.x = new ArrayList<Integer>(5);
		this.y = new ArrayList<Integer>(5);
		this.state = new ArrayList<Integer>(5);
		this.wantedMoves = new ArrayList<String>(5);
		this.roles = new ArrayList<String>(5);
		
		for (int i=0;i<5;i++) {
			battery.add(0);
			x.add(0);
			y.add(0);
			state.add(0);
			wantedMoves.add("ayy");
			roles.add("ayy");
		}
	}
	
	/**
	 * Method that gets the perception of the bots
	 * @author Zacarías Romero Sellamitou
	 */
	
	private void getPerception() {
		JSONObject json = new JSONObject();
		ACLMessage msg;
		String botName = null;
		// Send request to master
		// Wait for server answer
		try {
			for (int i=0;i<4;i++) {
				msg = receiveACLMessage();
				botName = msg.getSender().getLocalName();
				//System.out.println(msg);
				json = new JSONObject(msg.getContent());
				
				if (msg.getPerformativeInt() == ACLMessage.INFORM_REF) {
					int index = Character.getNumericValue(botName.charAt(3));
					battery.set(index,json.getInt("battery"));
					x.set(index,json.getInt("x"));
					y.set(index,json.getInt("y"));
					state.set(index,json.getInt("state"));
					totalWorldEnergy = json.getInt("total");
					roles.set(index,json.getString("role"));
					JSONArray jArray = json.getJSONArray("sensor");
					map.update(x.get(index), y.get(index), jArray);
					
					msg = new ACLMessage();
					msg.setSender(getAid());
					msg.setReceiver(new AgentID(botName));
					msg.setPerformative(ACLMessage.INFORM);
					send(msg);
				} else {
					System.err.println(botName + ": Unexpected answer on getPerception : " + msg.getPerformativeInt());
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Method that evaluates the wanted moves of the drones and answers them
	 * @author Zacarías Romero Sellamitou
	 */
	
	private void evaluate() {
		JSONObject json = new JSONObject();
		ACLMessage msg;
		String botName = null;
		// Send request to master
		// Wait for server answer
		
		try {
			for(int i=0;i<4;i++) {
				msg = receiveACLMessage();
				botName = msg.getSender().getLocalName();
				int index = Character.getNumericValue(botName.charAt(3));
				//System.out.println(msg);
				json = new JSONObject(msg.getContent());
				if (msg.getPerformativeInt() == ACLMessage.INFORM) {
					System.out.println(botName + " solicita: "+json.getString("command"));
					wantedMoves.set(index,json.getString("command"));
					
				} else {
					System.err.println(botName + ": Unexpected answer on evaluate : " + msg.getPerformativeInt());
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
			msg = new ACLMessage();
			msg.setSender(getAid());
			msg.addReceiver(new AgentID("bot1"));
			msg.addReceiver(new AgentID("bot2"));
			msg.addReceiver(new AgentID("bot3"));
			msg.addReceiver(new AgentID("bot4"));
			msg.setPerformative(ACLMessage.INFORM);
			send(msg);

	}
	/**
	 * Bot main execution loop. Before entering the loop, it will take care of doing the login
	 * procedure (only for the initializer bot), get the key from the server and check in to the world.
	 * @author James Nolan
	 */
	public void execute() {
		// Loop getting info about all drones and the move they want, evaluating it and send each one if they can
		// procceed or not
		while (true) {
			getPerception();
			evaluate();
		}
	}
}
