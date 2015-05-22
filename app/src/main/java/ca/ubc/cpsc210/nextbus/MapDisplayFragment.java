package ca.ubc.cpsc210.nextbus;

import java.util.ArrayList;
import java.util.List;

import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedIconOverlay.OnItemGestureListener;
import org.osmdroid.views.overlay.OverlayItem;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import ca.ubc.cpsc210.exception.TranslinkException;
import ca.ubc.cpsc210.nextbus.model.Bus;
import ca.ubc.cpsc210.nextbus.model.BusStop;
import ca.ubc.cpsc210.nextbus.translink.ITranslinkService;
import ca.ubc.cpsc210.nextbus.translink.TranslinkService;
import ca.ubc.cpsc210.nextbus.util.LatLon;
import ca.ubc.cpsc210.nextbus.util.TextOverlay;

/**
 * Fragment holding the map in the UI.
 */
public class MapDisplayFragment extends Fragment {

	/**
	 * Log tag for LogCat messages
	 */
	private final static String LOG_TAG = "MapDisplayFragment";

	/**
	 * Location of Nelson & Granville, downtown Vancouver
	 */
	private final static GeoPoint NELSON_GRANVILLE 
							= new GeoPoint(49.279285, -123.123007);

	/**
	 * Overlay for bus markers.
	 */
	private ItemizedIconOverlay<OverlayItem> busLocnOverlay;

	/**
	 * Overlay for bus stop location
	 */
	private ItemizedIconOverlay<OverlayItem> busStopLocationOverlay;
	
	/**
	 * Overlay for legend
	 */
	private TextOverlay legendOverlay;

	
	/**
	 * View that shows the map
	 */
	private MapView mapView;

	/**
	 * Selected bus stop
	 */
	private BusStop selectedStop;

	/**
	 * Wraps Translink web service
	 */
	private ITranslinkService tlService;

	/**
	 * Map controller for zooming in/out, centering
	 */
	private IMapController mapController;

	/**
	 * True if and only if map should zoom to fit displayed route.
	 */
	private boolean zoomToFit;

	/**
	 * Bus selected by user
	 */
	private OverlayItem selectedBus;

	/**
	 * Set up Translink service
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		Log.d(LOG_TAG, "onActivityCreated");

		setHasOptionsMenu(true);

		tlService = new TranslinkService(getActivity());

		Log.d(LOG_TAG, "Stop number for mapping: " + (selectedStop == null ? "not set" : selectedStop.getStopNum()));
	}

	/**
	 * Set up map view with overlays for buses and selected bus stop.
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		Log.d(LOG_TAG, "onCreateView");

		if (mapView == null) {
			mapView = new MapView(getActivity(), null);

			mapView.setTileSource(TileSourceFactory.MAPNIK);
			mapView.setClickable(true);
			mapView.setBuiltInZoomControls(true);

			// set default view for map (this seems to be important even when
			// it gets overwritten by plotBuses)
			mapController = mapView.getController();
			mapController.setZoom(mapView.getMaxZoomLevel() - 4);
			mapController.setCenter(NELSON_GRANVILLE);


			busLocnOverlay = createBusLocnOverlay();
			busStopLocationOverlay = createBusStopLocnOverlay();
			legendOverlay = createTextOverlay();

			// Order matters: overlays added later are displayed on top of
			// overlays added earlier.
			mapView.getOverlays().add(busStopLocationOverlay);
			mapView.getOverlays().add(busLocnOverlay);
			mapView.getOverlays().add(legendOverlay);
		}

		return mapView;
	}
	

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.fragment_map_refresh, menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.map_refresh) {
			update(false);
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	/**
	 * When view is destroyed, remove map view from its parent so that it can be
	 * added again when view is re-created.
	 */
	@Override
	public void onDestroyView() {
		Log.d(LOG_TAG, "onDestroyView");

		((ViewGroup) mapView.getParent()).removeView(mapView);

		super.onDestroyView();
	}

	@Override
	public void onDestroy() {
		Log.d(LOG_TAG, "onDestroy");

		super.onDestroy();
	}

	/**
	 * Update bus locations.
	 */
	@Override
	public void onResume() {
		Log.d(LOG_TAG, "onResume");

		update(true);

		super.onResume();
	}

	/**
	 * Set selected bus stop
	 * @param selectedStop  the selected stop
	 */
	public void setBusStop(BusStop selectedStop) {
		this.selectedStop = selectedStop;
	}

	/**
	 * Update bus location info for selected stop,
	 * zoomToFit status and repaint.
	 * 
	 * @Param zoomToFit  true if map must be zoomed to fit (when new bus stop has been selected)
	 */
	void update(boolean zoomToFit) {
		Log.d(LOG_TAG, "update - zoomToFit: " + zoomToFit);
		
		this.zoomToFit = zoomToFit;

		if(selectedStop != null) {
			new GetBusInfo().execute(selectedStop);
			selectedBus = null;
		}

		mapView.invalidate();
	}

	/**
	 * Create the overlay for bus markers.
	 */
	private ItemizedIconOverlay<OverlayItem> createBusLocnOverlay() {
		ResourceProxy rp = new DefaultResourceProxyImpl(getActivity());

		OnItemGestureListener<OverlayItem> gestureListener = new OnItemGestureListener<OverlayItem>() {
			/**
			 * Display bus information in dialog box when user taps
			 * bus.
			 * 
			 * @param index  index of item tapped
			 * @param oi the OverlayItem that was tapped
			 * @return true to indicate that tap event has been handled
			 */
			@Override
			public boolean onItemSingleTapUp(int index, OverlayItem oi) {
				for (int i = 0; i < busLocnOverlay.size(); i++) {
					busLocnOverlay.getItem(i).setMarker(getResources().getDrawable(R.drawable.bus));
				}
				oi.setMarker(getResources().getDrawable(R.drawable.selected_bus));
				mapView.postInvalidate();
				AlertDialog dlg = createSimpleDialog(oi.getTitle(), oi.getSnippet());
				dlg.show();
				return true;
			}

			@Override
			public boolean onItemLongPress(int index, OverlayItem oi) {
				// do nothing
				return false;
			}
		};
		
		return new ItemizedIconOverlay<OverlayItem>(
				new ArrayList<OverlayItem>(), 
				        getResources().getDrawable(R.drawable.bus), 
				        gestureListener, rp);
	}

	/**
	 * Create the overlay for bus stop marker.
	 */
	private ItemizedIconOverlay<OverlayItem> createBusStopLocnOverlay() {
		ResourceProxy rp = new DefaultResourceProxyImpl(getActivity());

		OnItemGestureListener<OverlayItem> gestureListener = new OnItemGestureListener<OverlayItem>() {
			/**
			 * Display bus stop description in dialog box when user taps
			 * stop.
			 * 
			 * @param index  index of item tapped
			 * @param oi the OverlayItem that was tapped
			 * @return true to indicate that tap event has been handled
			 */
			@Override
			public boolean onItemSingleTapUp(int index, OverlayItem oi) {
				AlertDialog dlg = createSimpleDialog(oi.getTitle(), oi.getSnippet());
				dlg.show();
				return true;
			}

			@Override
			public boolean onItemLongPress(int index, OverlayItem oi) {
				// do nothing
				return false;
			}
		};

		return new ItemizedIconOverlay<OverlayItem>(
				new ArrayList<OverlayItem>(), 
				        getResources().getDrawable(R.drawable.stop), 
				        gestureListener, rp);
	}

	private TextOverlay createTextOverlay() {
		ResourceProxy rp = new DefaultResourceProxyImpl(getActivity());
		Resources res = getResources();
		String legend = res.getString(R.string.legend);
		
		return new TextOverlay(rp, legend);
	}
	
	/**
	 * Plot bus stop
	 */
	private void plotBusStop() {
		LatLon latlon = selectedStop.getLatLon();
		GeoPoint point = new GeoPoint(latlon.getLatitude(),
				latlon.getLongitude());
		OverlayItem overlayItem = new OverlayItem(Integer.valueOf(selectedStop.getStopNum()).toString(), 
				selectedStop.getLocationDesc(), point);
		busStopLocationOverlay.removeAllItems(); // make sure not adding
											     // bus stop more than once
		busStopLocationOverlay.addItem(overlayItem);
	}

	
	/**
	 * Plot buses onto bus location overlay
	 * 
	 * @param zoomToFit  determines if map should be zoomed to bounds of plotted buses
	 */
	private void plotBuses(boolean zoomToFit) {
		// TODO: Phase 2
		List<Bus> buses = selectedStop.getBuses();
		busLocnOverlay.removeAllItems();
		for (int i = 0; i < buses.size(); i++) {
			Bus bus = buses.get(i);
			LatLon latlon = bus.getLatLon();
			GeoPoint point = new GeoPoint(latlon.getLatitude(),
					latlon.getLongitude());
			OverlayItem overlayItem = new OverlayItem(bus.getDestination(), 
					bus.getTime(), point);
			busLocnOverlay.addItem(overlayItem);
		}
	}


	/**
	 * Helper to create simple alert dialog to display message
	 * @param title  the title to be displayed at top of dialog
	 * @param msg  message to display in dialog
	 * @return  the alert dialog
	 */
	private AlertDialog createSimpleDialog(String title, String msg) {
		AlertDialog.Builder dialogBldr = new AlertDialog.Builder(getActivity());
		dialogBldr.setTitle(title);
		dialogBldr.setMessage(msg);
		dialogBldr.setNeutralButton(R.string.ok, null);

		return dialogBldr.create();
	}

	/** 
	 * Asynchronous task to get bus location estimates from Translink service.
	 * Displays progress dialog while running in background.  
	 */
	private class GetBusInfo extends
			AsyncTask<BusStop, Void, Void> {
		private ProgressDialog dialog = new ProgressDialog(getActivity());
		private boolean success = true;

		@Override
		protected void onPreExecute() {
			dialog.setMessage("Retrieving bus info...");
			dialog.show();
		}

		@Override
		protected Void doInBackground(BusStop... selectedStops) {
			BusStop selectedStop = selectedStops[0];

			try {
				tlService.addBusLocationsForStop(selectedStop);
			} catch (TranslinkException e) {
				e.printStackTrace();
				success = false;
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void dummy) {
			dialog.dismiss();

			if (success) {
				plotBuses(zoomToFit);
				plotBusStop();
				mapView.invalidate();
			} else {
				AlertDialog dialog = createSimpleDialog("Error", "Unable to retrieve bus location info...");
				dialog.show();
			}
		}
	}
}
