package main;

/**
 * An enumeration that list all the available role types. Each role has a radar size (how far can it see),
 * a battery consumption level (how much battery it looses at each step) and a fantasma flag (that tells us
 * whether the bot can fly over obstacles 
 * @author James Nolan
 *
 */
public enum Role {
	// The three available roles and their properties
	FLY(3, 2, true),
	PARROT(5, 1,false),
	FALCON(11, 4, false);
	
	// Constructor
	private Role(int radarSize, int batteryConsumption, boolean fantasma) {
		this.radarSize = radarSize;
		this.batteryConsumption = batteryConsumption;
		this.fantasma = fantasma;
	}
	
	// how far can this role see?
	// for example, the fly can see within a 3x3 matrix so its radar size is 3s
	int radarSize;
	
	// The battery that this role consumes at each movement
	int batteryConsumption;
	
	// Whether the role can fly over obstacles or not
	boolean fantasma;

	public String toString() {
		String st = null;
		if (radarSize==3) st = "fly";
		if (radarSize==5) st = "parrot";
		if (radarSize==11) st = "falcon";
		return st;
	}
}
