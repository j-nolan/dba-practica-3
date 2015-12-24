package main;

import java.util.ArrayList;

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
	
	// The state of each drone, can be: ready, objectiveSeen, objectiveFound.
	private ArrayList<String> state;
	
	// The world has a given amount of energy that decrease everytime an agent refuels
	// The total remaining amount is saved in this attribute
	private int totalWorldEnergy;
	
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
		this.map = map;
		this.battery = new ArrayList<Integer>(4);
		this.x = new ArrayList<Integer>(4);
		this.y = new ArrayList<Integer>(4);
		this.state = new ArrayList<String>(4);
	}
	
	/**
	 * Method that evaluates the wanted moves of the drones and answers them
	 * @author Zacarías Romero Sellamitou
	 */
	
	private void evaluate(String botName) {
		JSONObject json = new JSONObject();
		ACLMessage msg;
		// Send request to master
		// Wait for server answer
		try {
			msg = receiveACLMessage();
			//System.out.println(msg);
			json = new JSONObject(msg.getContent());
			if (msg.getPerformativeInt() == ACLMessage.INFORM) {
				System.out.println(msg.getSender().getLocalName() + " solicita: "+json.getString("command"));
			} else {
				System.err.println(botName + ": Unexpected answer : " + msg.getPerformativeInt());
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			json.put("command", "OK");
			msg = new ACLMessage();
			msg.setSender(getAid());
			msg.setReceiver(new AgentID(botName));
			if (botName == "bot3") msg.setPerformative(ACLMessage.REFUSE);else msg.setPerformative(ACLMessage.INFORM);
			msg.setContent(json.toString());
			send(msg);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
			evaluate("bot1");
			evaluate("bot2");
			evaluate("bot3");
			evaluate("bot4");
		}
	}
}
