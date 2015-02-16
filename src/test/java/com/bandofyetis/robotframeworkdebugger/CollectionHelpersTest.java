package com.bandofyetis.robotframeworkdebugger;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.Map.Entry;

import org.junit.Test;


public class CollectionHelpersTest {
	
	@Test
	public void testSortMapByKeyNullList(){
		Map<String,String> map = null;
		List<Set<Entry<String, Object>>> sortedKeys = CollectionHelpers.sortMapByKey(map);
		assertNull (sortedKeys);		
	}
	
	@Test
	public void testSortMapByKeyZeroElementList(){
		Map<String,String> map = new HashMap<String,String>();
		List<Set<Entry<String, Object>>> sortedKeys = CollectionHelpers.sortMapByKey(map);
		assertTrue (sortedKeys.size() == 0);
	}
	
	@Test public void testSortMapByKey(){
		Map<String,Integer> map = new HashMap<String, Integer>();
		map.put("b", 2);
		map.put("c", 4);
		map.put("a", 111);
		List<Set<Entry<String, Object>>> sortedKeys = CollectionHelpers.sortMapByKey(map);
		assertTrue (sortedKeys.size() == 3);
		for (int i=0; i < sortedKeys.size() -1; i++){
			assertTrue(((String)((Map.Entry)sortedKeys.get(i)).getKey()).compareTo ((String)((Map.Entry)sortedKeys.get(i+1)).getKey()) < 0);
		}
	}
	
	@Test public void testSortMapByKeyAlreadySorted(){
		Map<String,Integer> map = new HashMap<String, Integer>();
		map.put("a", 111);
		map.put("b", 2);
		map.put("c", 4);
		
		List<Set<Entry<String, Object>>> sortedKeys = CollectionHelpers.sortMapByKey(map);
		assertTrue (sortedKeys.size() == 3);
		for (int i=0; i < sortedKeys.size() -1; i++){
			assertTrue(((String)((Map.Entry)sortedKeys.get(i)).getKey()).compareTo ((String)((Map.Entry)sortedKeys.get(i+1)).getKey()) < 0);
		}
	}
}
