package io.antmedia.datastore.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DB.TreeMapMaker;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.Vod;

public class MapDBStore implements IDataStore {

	private DB db;
	private BTreeMap<String, String> map;
	private BTreeMap<String, String> vodMap;
	private Gson gson;
	protected static Logger logger = LoggerFactory.getLogger(MapDBStore.class);
	private static final String MAP_NAME = "broadcast";
	private static final String VOD_MAP_NAME = "vod";

	public MapDBStore(String dbName) {

		db = DBMaker.fileDB(dbName).transactionEnable().make();
		map = db.treeMap(MAP_NAME).keySerializer(Serializer.STRING).valueSerializer(Serializer.STRING).counterEnable()
				.createOrOpen();
		vodMap = db.treeMap(VOD_MAP_NAME).keySerializer(Serializer.STRING).valueSerializer(Serializer.STRING)
				.counterEnable().createOrOpen();

		GsonBuilder builder = new GsonBuilder();
		gson = builder.create();

		TreeMapMaker<Integer, String> map = (TreeMapMaker<Integer, String>) db.treeMap("collectionName");

	}

	public BTreeMap<String, String> getVodMap() {
		return vodMap;
	}

	public void setVodMap(BTreeMap<String, String> vodMap) {
		this.vodMap = vodMap;
	}

	public BTreeMap<String, String> getMap() {
		return map;
	}

	public void setMap(BTreeMap<String, String> map) {
		this.map = map;
	}

	@Override
	public String save(Broadcast broadcast) {
		String streamId = null;
		boolean result = false;
		if (broadcast != null) {
			try {

				if (broadcast.getStreamId() == null) {
					streamId = RandomStringUtils.randomNumeric(24);
					broadcast.setStreamId(streamId);
				}
				streamId = broadcast.getStreamId();

				String rtmpURL = broadcast.getRtmpURL();
				if (rtmpURL != null) {
					rtmpURL += streamId;
				}
				broadcast.setRtmpURL(rtmpURL);

				map.put(streamId, gson.toJson(broadcast));
				db.commit();
				result = true;
			} catch (Exception e) {
				e.printStackTrace();
				streamId = null;
			}
		}

		return streamId;
	}

	@Override
	public Broadcast get(String id) {
		if (id != null) {
			String jsonString = map.get(id);
			if (jsonString != null) {
				return gson.fromJson(jsonString, Broadcast.class);
			}
		}
		return null;
	}

	@Override
	public boolean updateName(String id, String name, String description) {
		boolean result = false;
		if (id != null) {
			String jsonString = map.get(id);
			if (jsonString != null) {
				Broadcast broadcast = gson.fromJson(jsonString, Broadcast.class);
				broadcast.setName(name);
				broadcast.setDescription(description);
				map.replace(id, gson.toJson(broadcast));
				db.commit();
				result = true;
			}
		}
		return result;
	}

	@Override
	public boolean updateStatus(String id, String status) {
		boolean result = false;
		if (id != null) {
			String jsonString = map.get(id);
			if (jsonString != null) {
				Broadcast broadcast = gson.fromJson(jsonString, Broadcast.class);
				broadcast.setStatus(status);
				map.replace(id, gson.toJson(broadcast));
				db.commit();
				result = true;
			}
		}
		return result;
	}

	@Override
	public boolean updateDuration(String id, long duration) {
		boolean result = false;
		if (id != null) {
			String jsonString = map.get(id);
			if (jsonString != null) {
				Broadcast broadcast = gson.fromJson(jsonString, Broadcast.class);
				broadcast.setDuration(duration);
				map.replace(id, gson.toJson(broadcast));
				db.commit();
				result = true;
			}
		}
		return result;
	}

	@Override
	public boolean updatePublish(String id, boolean publish) {
		String jsonString = map.get(id);
		boolean result = false;
		if (jsonString != null) {
			Broadcast broadcast = gson.fromJson(jsonString, Broadcast.class);
			broadcast.setPublish(publish);
			map.replace(id, gson.toJson(broadcast));
			db.commit();
			result = true;
		}
		return result;
	}

	@Override
	public boolean addEndpoint(String id, Endpoint endpoint) {
		boolean result = false;
		if (id != null && endpoint != null) {
			String jsonString = map.get(id);
			if (jsonString != null) {
				Broadcast broadcast = gson.fromJson(jsonString, Broadcast.class);
				List<Endpoint> endPointList = broadcast.getEndPointList();
				if (endPointList == null) {
					endPointList = new ArrayList();
				}
				endPointList.add(endpoint);
				broadcast.setEndPointList(endPointList);
				map.replace(id, gson.toJson(broadcast));
				db.commit();
				result = true;
			}
		}
		return result;
	}

	@Override
	public boolean removeEndpoint(String id, Endpoint endpoint) {
		boolean result = false;

		if (id != null && endpoint != null) {
			String jsonString = map.get(id);
			if (jsonString != null) {
				Broadcast broadcast = gson.fromJson(jsonString, Broadcast.class);
				List<Endpoint> endPointList = broadcast.getEndPointList();
				if (endPointList != null) {
					for (Iterator iterator = endPointList.iterator(); iterator.hasNext();) {
						Endpoint endpointItem = (Endpoint) iterator.next();
						if (endpointItem.rtmpUrl.equals(endpoint.rtmpUrl)) {
							iterator.remove();
							result = true;
							break;
						}
					}

					if (result) {
						broadcast.setEndPointList(endPointList);
						map.replace(id, gson.toJson(broadcast));
						db.commit();
					}
				}
			}
		}
		return result;
	}

	@Override
	public boolean removeAllEndpoints(String id) {
		boolean result = false;
		if (id != null) {
			String jsonString = map.get(id);
			if (jsonString != null) {
				Broadcast broadcast = gson.fromJson(jsonString, Broadcast.class);
				broadcast.setEndPointList(null);
				map.replace(id, gson.toJson(broadcast));
				db.commit();
				result = true;
			}
		}
		return result;
	}

	@Override
	public long getBroadcastCount() {
		return map.getSize();
	}

	@Override
	public boolean delete(String id) {

		boolean result = map.remove(id) != null;
		if (result) {
			db.commit();
		}

		return result;
	}

	@Override
	public List<Broadcast> getBroadcastList(int offset, int size) {
		Collection<String> values = map.values();
		int t = 0;
		int itemCount = 0;
		if (size > 50) {
			size = 50;
		}
		if (offset < 0) {
			offset = 0;
		}
		List<Broadcast> list = new ArrayList();
		for (String broadcastString : values) {
			if (t < offset) {
				t++;
				continue;
			}
			list.add(gson.fromJson(broadcastString, Broadcast.class));
			itemCount++;

			if (itemCount >= size) {
				break;
			}

		}
		return list;
	}

	@Override
	public List<Vod> getVodList(int offset, int size) {
		Collection<String> values = vodMap.values();
		int t = 0;
		int itemCount = 0;
		if (size > 50) {
			size = 50;
		}
		if (offset < 0) {
			offset = 0;
		}
		List<Vod> list = new ArrayList();
		for (String vodString : values) {
			if (t < offset) {
				t++;
				continue;
			}
			list.add(gson.fromJson(vodString, Vod.class));
			itemCount++;

			if (itemCount >= size) {
				break;
			}

		}
		return list;
	}

	@Override
	public List<Broadcast> filterBroadcastList(int offset, int size, String type) {

		int t = 0;
		int itemCount = 0;
		if (size > 50) {
			size = 50;
		}
		if (offset < 0) {
			offset = 0;
		}

		Object[] objectArray = map.getValues().toArray();

		Broadcast[] broadcastArray = new Broadcast[objectArray.length];

		for (int i = 0; i < objectArray.length; i++) {

			broadcastArray[i] = gson.fromJson((String) objectArray[i], Broadcast.class);

		}

		List<Broadcast> filterList = new ArrayList<Broadcast>();
		for (int i = 0; i < broadcastArray.length; i++) {

			if (broadcastArray[i].getType().equals(type)) {

				filterList.add(gson.fromJson((String) objectArray[i], Broadcast.class));

			}

		}

		List<Broadcast> list = new ArrayList();
		for (Broadcast broadcast : filterList) {
			if (t < offset) {
				t++;
				continue;
			}
			list.add(broadcast);
			itemCount++;

			if (itemCount >= size) {
				break;
			}

		}
		return list;

	}

	@Override
	public List<Vod> filterVoDList(int offset, int size, String keyword, long startdate, long endDate) {

		List<Vod> list = new ArrayList();
		int t = 0;
		int itemCount = 0;
		if (size > 50) {
			size = 50;
		}
		if (offset < 0) {
			offset = 0;
		}

		Object[] objectArray = vodMap.getValues().toArray();

		Vod[] vodArray = new Vod[objectArray.length];

		List<Vod> filterList = new ArrayList<Vod>();

		for (int i = 0; i < objectArray.length; i++) {

			vodArray[i] = gson.fromJson((String) objectArray[i], Vod.class);

		}

		if (keyword != null && keyword.length() > 0) {

			for (int i = 0; i < vodArray.length; i++) {

				if (vodArray[i].getStreamName().contains(keyword) && startdate < vodArray[i].getCreationDate()
						&& endDate > vodArray[i].getCreationDate()) {

					filterList.add(gson.fromJson((String) objectArray[i], Vod.class));

				}

			}

		} else if (keyword == null || keyword.length() < 0) {

			for (int i = 0; i < vodArray.length; i++) {

				if (startdate < vodArray[i].getCreationDate() && endDate > vodArray[i].getCreationDate()) {

					filterList.add(gson.fromJson((String) objectArray[i], Vod.class));

				}

			}

		}

		for (Vod broadcast : filterList) {
			if (t < offset) {
				t++;
				continue;
			}
			list.add(broadcast);
			itemCount++;

			if (itemCount >= size) {
				break;
			}

		}

		return list;

	}

	@Override
	public boolean addVod(String id, Vod vod) {
		String vodId = null;
		boolean result = false;

		if (vod != null) {
			try {
				vodId = RandomStringUtils.randomNumeric(24);
				vod.setVodId(vodId);

				vodMap.put(vodId, gson.toJson(vod));
				db.commit();

				result = true;
				logger.warn(Long.toString(vod.getCreationDate()));

			} catch (Exception e) {
				e.printStackTrace();
				id = null;
			}
		}
		return result;
	}

	/*
	 * IP Camera Operations
	 */

	@Override
	public boolean addCamera(Broadcast camera) {
		boolean result = false;
		String streamId = null;

		// StreamID Address is primary key

		if (camera != null) {
			try {
				streamId = RandomStringUtils.randomNumeric(24);
				camera.setStreamId(streamId);

				map.put(streamId, gson.toJson(camera));
				db.commit();
				result = true;
			} catch (Exception e) {
				e.printStackTrace();
				streamId = null;
			}
		}

		return result;
	}

	@Override
	public boolean editCameraInfo(Broadcast camera) {
		boolean result = false;
		try {

			logger.warn("inside of editCameraInfo");

			logger.warn("gelen camera:  " + camera.getStreamId());

			Broadcast oldCam = get(camera.getStreamId());

			oldCam.setName(camera.getName());
			oldCam.setUsername(camera.getUsername());
			oldCam.setPassword(camera.getPassword());
			oldCam.setIpAddr(camera.getIpAddr());

			logger.warn(oldCam.getName());

			getMap().replace(oldCam.getStreamId(), gson.toJson(oldCam));

			db.commit();
			result = true;
		} catch (Exception e) {
			result = false;
		}

		logger.warn("result inside edit camera: " + result);
		return result;
	}

	/**
	 * Delete camera from camera store
	 * 
	 * @returns true if camera exists, otherwise return false
	 */
	@Override
	public boolean deleteCamera(String id) {
		boolean result = false;
		try {

			if (map.containsKey(id)) {
				logger.warn("inside of deleteCamera");
				map.remove(id);
				db.commit();
				result = true;
			}

		} catch (Exception e) {
			result = false;
		}
		return result;
	}

	@Override
	public Broadcast getCamera(String ipAddr) {

		Object[] objectArray = map.getValues().toArray();

		Broadcast[] broadcastArray = new Broadcast[objectArray.length];

		for (int i = 0; i < objectArray.length; i++) {

			broadcastArray[i] = gson.fromJson((String) objectArray[i], Broadcast.class);

		}

		Broadcast camera = new Broadcast();

		for (int i = 0; i < broadcastArray.length; i++) {

			if (broadcastArray[i].getType() == "ipCamera") {

				if (broadcastArray[i].getIpAddr().equals(ipAddr)) {

					camera = broadcastArray[i];

					break;

				}
			}

		}

		return camera;
	}

	@Override
	public List<Broadcast> getCameraList() {

		Object[] objectArray = map.getValues().toArray();

		Broadcast[] broadcastArray = new Broadcast[objectArray.length];

		List<Broadcast> cameraList = new ArrayList<Broadcast>();

		for (int i = 0; i < objectArray.length; i++) {

			broadcastArray[i] = gson.fromJson((String) objectArray[i], Broadcast.class);

		}

		for (int i = 0; i < broadcastArray.length; i++) {

			if (broadcastArray[i].getType().equals("ipCamera")) {

				cameraList.add(gson.fromJson((String) objectArray[i], Broadcast.class));

			}

		}

		return cameraList;
	}

	@Override
	public void close() {
		db.close();
	}

	@Override
	public boolean deleteVod(String id) {
		boolean result = vodMap.remove(id) != null;
		if (result) {
			db.commit();
		}

		return result;
	}

	@Override
	public boolean resetBroadcastStatus() {

		Object[] objectArray = map.getValues().toArray();

		Broadcast[] broadcastArray = new Broadcast[objectArray.length];

		for (int i = 0; i < objectArray.length; i++) {

			broadcastArray[i] = gson.fromJson((String) objectArray[i], Broadcast.class);

		}

		for (int i = 0; i < broadcastArray.length; i++) {
			if (broadcastArray[i].getStatus().equals("broadcasting")) {
				broadcastArray[i].setStatus("created");
				map.replace(broadcastArray[i].getStreamId(), gson.toJson(broadcastArray[i]));
			}

		}

		return false;
	}

	@Override
	public long getTotalVodNumber() {

		return getVodMap().size();
	}

}
