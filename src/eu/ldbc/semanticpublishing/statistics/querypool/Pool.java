package eu.ldbc.semanticpublishing.statistics.querypool;

import java.util.ArrayList;

import eu.ldbc.semanticpublishing.statistics.Statistics;

/**
 * A class storing collection of items. On each invocation of setItemUnavailable() for a specific item,
 * availability status of all other items is tested, and if all items are 'unavailable', their status is reset to 'available'.
 */
public class Pool {
	private int id;
	private int itemsUnavailable = 0;
	private ArrayList<PoolItem> items = new ArrayList<PoolItem>();
	
	public Pool(int id) {
		this.id = id;
	}
	
	public int getId() {
		return id;
	}
	
	public void addItem(PoolItem item) {
		items.add(item);
	}
	
	public PoolItem getItem(int itemId) {
		for (PoolItem item : items) {
			if (item.getId() == itemId) {
				return item;
			}
		}
		return null;
	}
	
	public boolean hasItem(int id) {
		for (PoolItem item : items) {
			if (item.getId() == id) {
				return true;
			}
		}
		return false;
	}
	
	public boolean itemIsAvailable(int itemId) {
		for (PoolItem item : items) {
			if (item.getId() == itemId) {
				return item.isAvailable();
			}
		}
		return false;
	}

	public synchronized void setItemUnavailable(int itemId) {
		PoolItem item = getItem(itemId);
		if (item != null) {
			item.setUnavailable();
		}
	}	
	
	public synchronized void releaseItemUnavailable(int itemId) {
		itemsUnavailable++;
		checkAndResetAllItems();
	}
	
	private void checkAndResetAllItems() {
		if (itemsUnavailable >= items.size()) {
			setAllItemsAvailable();
			itemsUnavailable = 0;
		}		
	}
		
	private void setAllItemsAvailable() {
		for (PoolItem item : items) {
			item.setAvailable();
		}
	}
	
	public String produceStatistics(long timeSeconds, long timeCorrectionsMS) {
		long totalPoolOperationsCount = 0;
		StringBuilder sb = new StringBuilder();
		
		for (int i = 0; i < items.size(); i++) {
			totalPoolOperationsCount += Statistics.aggregateQueriesArray[items.get(i).getId() - 1].getRunsCount();
		}
		
		double averageQueriesPerSecond = (double)totalPoolOperationsCount / ((double)timeSeconds - (double)(timeCorrectionsMS / 1000));
		if ((double)(Statistics.timeCorrectionsMS.get() / 1000) >= (double)timeSeconds) {
			averageQueriesPerSecond = (double)totalPoolOperationsCount / ((double)timeSeconds);
		}
		
		sb.append(String.format("%.4f average queries per second.", averageQueriesPerSecond));
		
		sb.append(" Pool " + id + ", queries [ ");
		for (PoolItem item : items) {
			sb.append("Q");
			sb.append(item.getId());
			sb.append(" ");
		}
		sb.append("]");		
		
		return sb.toString();
	}
	
	public void showPoolItems() {
		for (PoolItem item : items) {
			System.out.println("\t" + item.toString());
		}
	}
}