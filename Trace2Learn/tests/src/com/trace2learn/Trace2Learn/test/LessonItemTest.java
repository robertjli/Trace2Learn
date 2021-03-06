package com.trace2learn.Trace2Learn.test;

import java.util.LinkedHashMap;

import android.test.AndroidTestCase;

import com.trace2learn.TraceLibrary.Database.LessonCharacter;
import com.trace2learn.TraceLibrary.Database.LessonItem;

public class LessonItemTest extends AndroidTestCase {

	static public void compareLessonItem(LessonItem expected, LessonItem actual)
	{
		assertEquals(expected.getStringId(), actual.getStringId());
		assertEquals(expected.getKeyValues(), actual.getKeyValues());
		assertEquals(expected.getSort(), actual.getSort());
		assertEquals(expected.getStringId(), actual.getStringId());
		assertEquals(expected.getTags(), actual.getTags());		
		assertEquals(expected.getItemType(), actual.getItemType());
	}		
	
    public void testCompareTo() {
        LessonItem a = new LessonCharacter();
        LessonItem b = new LessonCharacter();
        LessonItem c = new LessonCharacter();
        a.setSort(1);
        b.setSort(2);
        c.setSort(1);
        
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertTrue(c.compareTo(a) == 0);
    }
    
    public void testSetKeyValues() {
        LessonItem a = new LessonCharacter();
        LinkedHashMap<String, String> kv = new LinkedHashMap<String, String>();
        kv.put("k1", "v1");
        kv.put("k2", "v2");
        
        a.setKeyValues(kv);
        assertEquals(a.getKeyValues(), kv);
    }
}
