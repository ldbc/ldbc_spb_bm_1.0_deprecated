package eu.ldbc.semanticpublishing.statistics.querypool;

import java.util.ArrayList;
import java.util.HashMap;

public class PoolManager {
	
	private ArrayList<Pool> pools = new ArrayList<Pool>();
	
	private HashMap<String, Integer> statusMap = new HashMap<String, Integer>();
	
	public void initialize(String s) throws IllegalArgumentException, NumberFormatException {
		if (s.trim().isEmpty()) {
			System.out.println("No query pools have been detected, continuing with default behaviour...");
			return;
		}
		
		if (!validateInitString(s)) {
			throw new IllegalArgumentException(s + ", check definitions.properties parameter : queryPools...");
		}
		
		String[] tokens = s.split("\\}");
		
		int poolId = 0;
		Pool pool = null;
		
		for (int i = 0; i < tokens.length; i++) {
			String token = tokens[i].trim();
			
			if (token.length() == 0) {
				continue;
			}
			
			pool = new Pool(poolId++);
			
			token = token.replace("{", "");

			String[] tokens2 = token.split(",");
			for (int j = 0; j < tokens2.length; j++) {
				int itemId = Integer.parseInt(tokens2[j].trim());
				pool.addItem(new PoolItem(itemId));
			}
			pools.add(pool);
		}	
	}
	
	private boolean validateInitString(String s) {
		int left = 0;
		int right = 0;
		int digits = 0;
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) == '{') {
				left++;
			}
			if (s.charAt(i) == '}') {
				right++;
			}
			
			if (left > right) {
				if (s.charAt(i) == ' ' || s.charAt(i) == ',' || s.charAt(i) == '{' || s.charAt(i) == '}') {
					continue;
				}
				if (!isNumeric(s.charAt(i))) {
					return false;
				} else {
					digits++;
				}
			}
		}
		
		if ((left == 0) || (right == 0)) {
			return false;
		}
		
		//empty pools are not allowed
		if (digits == 0) {
			return false;
		}
		
		return (left == right);
	}
	
	private boolean isNumeric(char ch) {
		if (ch != '0' && ch != '1' && ch != '2' && 
			ch != '3' && ch != '4' && ch != '5' && 
			ch != '6' && ch != '7' && ch != '8' && ch != '9') {
			return false;
		}
		return true;
	}
	
	private Pool getPoolByItemId(int itemId) {
		for (Pool pool : pools) {
			if (pool.hasItem(itemId)) {
				return pool;
			}
		}
		return null;
	}
	
	public Pool getPool(int poolId) {
		for (Pool pool : pools) {
			if (pool.getId() == poolId) {
				return pool;
			}
		}
		return null;
	}
	
	/**
	 * @param itemId - id of the item to be set unavailable
	 * @return - true if operation succeeded. false - if item was not available 
	 */
	public synchronized boolean checkAndSetItemUnavailable(int itemId) {
		Pool pool = getPoolByItemId(itemId);
		if (pool != null) {
			if (pool.itemIsAvailable(itemId)) {
				pool.setItemUnavailable(itemId);
				return true;
			}
		}
		
		if (pools.size() == 0) {
			//assuming no pools are configured, skipping query pools management
			return true;
		} else {
			return false;
		}
	}
	
	public synchronized boolean checkAndSetItemUnavailable2(String threadId, int itemId) {
		Pool pool = getPoolByItemId(itemId);
		if (pool != null) {
			if (pool.itemIsAvailable(itemId)) {
				pool.setItemUnavailable(itemId);
				if (statusMap.containsKey(threadId)) {
					statusMap.remove(threadId);
				}
				statusMap.put(threadId, new Integer(itemId));
				return true;
			}
		}
		
		if (pools.size() == 0) {
			//assuming no pools are configured, skipping query pools management
			return true;
		} else {
			return false;
		}
	}
	
	public synchronized void releaseItemUnavailable(int itemId) {
		Pool pool = getPoolByItemId(itemId);
		if (pool != null) {
			pool.releaseItemUnavailable(itemId);
		}		
	}

	public int getPoolsCount() {
		return pools.size(); 
	}
	
	public String getExecutionStatus() {
		if (statusMap.isEmpty()) {
			System.out.println("no PoolManager status available");
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (String threadId : statusMap.keySet()) {
			sb.append("t:");
			sb.append(threadId);
			sb.append("q:");
			sb.append(statusMap.get(threadId));
			sb.append("  ");
		}
		return sb.toString();
	}
}