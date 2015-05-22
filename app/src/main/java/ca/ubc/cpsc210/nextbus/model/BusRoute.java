package ca.ubc.cpsc210.nextbus.model;


/**
 * A bus route
 */
public class BusRoute implements Comparable<BusRoute> {
	private String name;

	/**
	 * Constructor 
	 * 
	 * @param name  the name of this bus route
	 */
	public BusRoute(String name) {
		this.name = name;
	}
	
	/**
	 * Constructor
	 * 
	 * Name of route is set to "unknown"
	 */
	public BusRoute() {
		this("unknown");
	}

	/**
	 * Gets name of route
	 * @return name of route
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets name of route
	 * @param name  name of the route
	 */
	public void setName(String name) {
		this.name = name;
	}

    /**
     * Produces name of route
     */
	@Override
	public String toString() {
		return name;
	}

	/**
	 * Generate hash code based on name of bus route.
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	/**
	 * Test equality of bus routes by name.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BusRoute other = (BusRoute) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	/**
	 * Compare bus routes by name
	 */
    @Override
    public int compareTo(BusRoute other) {
        return name.compareTo(other.name);
    }
	
	
}
