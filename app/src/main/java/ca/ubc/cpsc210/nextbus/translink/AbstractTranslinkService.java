package ca.ubc.cpsc210.nextbus.translink;

import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import ca.ubc.cpsc210.nextbus.model.Bus;
import ca.ubc.cpsc210.nextbus.model.BusRoute;
import ca.ubc.cpsc210.nextbus.model.BusStop;
import ca.ubc.cpsc210.nextbus.model.BusWaitTime;

public abstract class AbstractTranslinkService implements ITranslinkService {

	public AbstractTranslinkService() {
		super();
	}

	/**
	 * Parses estimated wait times from string received from Translink service and adds them to 
	 * associated bus stop.  Returns without adding estimated wait times if
	 * Translink returns a JSON object that represents an error.
	 * @param input  response from Translink service
	 * @param stop   bus stop associated with bus locations
	 * @throws JSONException when input is not of the expected form
	 */
	protected void parseWaitTimesFromJSON(String input, BusStop stop)
			throws JSONException {

		// did we get a JSONObject containing an error message as a response?
		try {
			JSONObject response = new JSONObject(input);
			response.getString("Code");
			response.getString("Message");
			return;
		} catch (Exception e) {
			// do nothing - this means we didn't get a response in the form of a
			// JSONObject that contains an error message
		}

		JSONArray response = new JSONArray(input);
		int numRoutes = response.length();

		for (int index = 0; index < numRoutes; index++) {
			JSONObject routeSched = response.getJSONObject(index);
			String routeName = routeSched.getString("RouteNo");
			BusRoute busRoute = stop.getRouteNamed(routeName);
			JSONArray schedules = routeSched.getJSONArray("Schedules");

			for (int i = 0; i < schedules.length(); i++) {
				JSONObject shed = schedules.getJSONObject(i);
				int countdown = shed.getInt("ExpectedCountdown");
				boolean cancelledStop = shed.getBoolean("CancelledStop");
				boolean cancelledTrip = shed.getBoolean("CancelledTrip");
				boolean isCancelled = cancelledStop || cancelledTrip;
				BusWaitTime next = new BusWaitTime(busRoute, countdown,
						isCancelled);
				stop.addWaitTime(next);
			}
		}
	}

	/**
	 * Parses buses from string received from Translink service and adds them to 
	 * associated bus stop.  Returns without adding buses to bus stop if
	 * Translink returns a JSON object that represents an error. 
	 * @param input  response from Translink service
	 * @param stop   bus stop associated with bus locations
	 * @throws JSONException when input is not of the expected form
	 */
	protected void parseBusesFromJSON(String input, BusStop stop)
			throws JSONException {

		// did we get a JSONObject containing an error message as a response?
		try {
			JSONObject response = new JSONObject(input);
			response.getString("Code");
			response.getString("Message");
			return;
		} catch (Exception e) {
			// do nothing - this means we didn't get a response in the form of a
			// JSONObject that contains an error message
		}

		JSONArray response = new JSONArray(input);
		int numRoutes = response.length();

		for (int index = 0; index < numRoutes; index++) {
			JSONObject bus = response.getJSONObject(index);
			String routeName = bus.getString("RouteNo");
			BusRoute route = stop.getRouteNamed(routeName);

			Bus b = new Bus(route, bus.getDouble("Latitude"),
					bus.getDouble("Longitude"), bus.getString("Destination"),
					bus.getString("RecordedTime"));
			stop.addBus(b);
		}
	}

	/**
	 * Parse bus stop data from string received from Translink web service
	 * @param input   string received from Translink web service
	 * @return  bus stop parsed from input; null if Translink returns a JSON object
	 * that represents an error
	 * @throws JSONException when input is not of the expected form
	 */
	protected BusStop parseBusStopFromJSON(String input) throws JSONException {
		JSONObject obj = (JSONObject) new JSONTokener(input).nextValue();

		// did we get a JSONObject containing an error code & message as a response?
		try {
			obj.getString("Code");
			obj.getString("Message");
			return null;
		} catch (Exception e) {
			// do nothing - this means we didn't get a response in the form of a
			// JSONObject that contains an error message
		}

		String routes = obj.getString("Routes");
		Set<BusRoute> busRoutes = buildRoutesFromString(routes);

		BusStop busStop = new BusStop(obj.getInt("StopNo"),
				obj.getString("Name"), obj.getDouble("Latitude"),
				obj.getDouble("Longitude"), busRoutes);

		return busStop;
	}

	/**
	 * Parses set of bus routes from comma-delimited string of route names.
	 * 
	 * @param routesAsString
	 *            string of route names, separated by a comma
	 * @return set of bus routes
	 */
	private Set<BusRoute> buildRoutesFromString(String routesAsString) {
		Set<BusRoute> routes = new HashSet<BusRoute>();
		StringTokenizer tokenizer = new StringTokenizer(routesAsString, ",");

		while (tokenizer.hasMoreTokens()) {
			String next = tokenizer.nextToken();
			BusRoute route = new BusRoute(next.trim());
			routes.add(route);
		}

		return routes;
	}

}