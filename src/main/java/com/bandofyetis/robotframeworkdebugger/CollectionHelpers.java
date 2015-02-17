package com.bandofyetis.robotframeworkdebugger;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class CollectionHelpers {

	/**
	 * Standard constructor.  Class contains only static classes, make it impossible to instantiate
	 */
	private CollectionHelpers(){}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static List<Set<Entry<String,Object>>> sortMapByKey(Map<String, ? extends Object> map){
		if (map == null){
			return null;
		}
		List<Set<Entry<String,Object>>> returnlist = new LinkedList(map.entrySet());
	     Collections.sort(returnlist, new Comparator() {
	          public int compare(Object o1, Object o2) {
	               return ((Comparable) ((Map.Entry) (o1)).getKey())
	              .compareTo(((Map.Entry) (o2)).getKey());
	          }
	     });
	     return returnlist;
	}
}