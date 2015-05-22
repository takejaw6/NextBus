package ca.ubc.cpsc210.nextbus.translink;

import ca.ubc.cpsc210.exception.TranslinkException;
import ca.ubc.cpsc210.nextbus.model.BusStop;

public interface ITranslinkService {

    /**
     * Add current wait time estimates for a bus to particular bus stop
     * @param stop  the bus stop to which wait time estimates must be added
     * @return wait time estimates
     * @throws TranslinkException when an exception occurs obtaining or parsing data from Translink service
     */
    public abstract void addWaitTimeEstimatesToStop(BusStop stop)
            throws TranslinkException;

    /**
     * Add bus location information for buses currently serving a particular stop
     * (replaces current list of bus locations for stop).
     * @param stop  the bus stop to which bus location information is to be added
     * @return location information for buses serving stop number stopNum
     * @throws TranslinkException when an exception occurs obtaining or parsing data from Translink service
     */
    public abstract void addBusLocationsForStop(BusStop stop)
            throws TranslinkException;

    /**
     * Gets description and location information for a bus stop
     * @param stopNum  the bus stop number
     * @return bus stop description and location information
     * @throws TranslinkException when an exception occurs obtaining or parsing data from Translink service
     */
    public abstract BusStop getBusStop(String stopNum)
            throws TranslinkException;
    

}