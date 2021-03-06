package com.trace2learn.TraceLibrary.Database;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.trace2learn.TraceLibrary.Toolbox;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;

public class Lesson extends LessonItem {

    private List<String>					_words; // list of word IDs
    private String							name; // lesson name
    private List<LessonItem>				wordObjects;
    private SortedSet<LessonCategory>		categories;
    private String							narrative;
    private boolean							isUserDefined;

    public Lesson() {
        this(null, true);
    }

    public Lesson(String id) {
        this(id, true);
    }

    public Lesson(boolean isUserDefined) {
        this(null, isUserDefined);
    }

    public Lesson(String id, boolean isUserDefined) {
        this._type         = ItemType.LESSON;
        this._words        = new ArrayList<String>();
        this._stringid     = id;
        this.isUserDefined = isUserDefined;
    }
    
    protected synchronized void initialize() {
        // Not needed unless we ever pull lessons without associated details
        initialized = true;
    }

    public synchronized void addWord(String wordId) {
        _words.add(wordId);
    }
    
    public synchronized void addWord(LessonWord word) {
        if (wordObjects == null) {
            wordObjects = new ArrayList<LessonItem>();
        }
        wordObjects.add(word);
        _words.add(word.getStringId());
    }

    public synchronized List<String> getWordIds() {
        return new ArrayList<String>(_words);
    }

    public synchronized String getWordId(int i) {
        return _words.get(i);
    }

    public synchronized int getNumWords() {
        return _words.size();
    }

    public synchronized List<LessonItem> getWords() {
        if (wordObjects == null) {
            wordObjects = Toolbox.dba.getWordsFromLesson(_stringid);
        }
        return wordObjects;
    }
    
    public synchronized void invalidateWords() {
        wordObjects = null;
    }

    public String getLessonName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public boolean isUserDefined() {
        return isUserDefined;
    }
    
    public void setUserDefined(boolean userDefined) {
        this.isUserDefined = userDefined;
    }
    

    public SortedSet<LessonCategory> getCategories() {
        return categories;
    }

    public void setCategories(SortedSet<LessonCategory> categories) {
        this.categories = categories;
    }

    public void addCategory(LessonCategory category) {
        if (categories == null) {
            categories = new TreeSet<LessonCategory>();
        }
        categories.add(category);
    }
    
    public String getNarrative() {
        return narrative;
    }
    
    public void setNarrative(String narrative) {
        this.narrative = narrative;
    }

    public int length() {
        return _words.size();
    }

    public synchronized boolean removeWord(String word) {
        return _words.remove(word);
    }

    public synchronized String removeWord(int i) {
        return _words.remove(i);
    }

    public synchronized void clearWords() {
        _words.clear();
    }

    /**
     * Draws the item in the canvas provided, using the provided paint brush
     * within the provided bounding box
     * The time is a normalized step from 0 to 1, 0 being not shown at all
     * and 1 being completely drawn.
     * @param canvas - the canvas to draw on
     * @param paint - the drawing settings for the item
     * @param left - the left bound in which the item should be drawn
     * @param top - the top bound in which the item should be drawn
     * @param width - the width of the bounding box in which the item should be drawn
     * @param height - the height of the bounding box in which the item should be drawn
     * @param time - the time in the animation from 0 to 1
     */
    @Override
    public void draw(Canvas canvas, Paint paint, float left, float top,
            float width, float height, float time) {
        draw(canvas, paint, left, top, width, height);
    }

    /**
     * Creates an XML representation of this Lesson.
     * @return an XML string
     */
    public String toXml() {
        StringBuffer sb = new StringBuffer();
        sb.append("<lesson id=\"").append(_stringid).append("\" ");
        sb.append("name=\"").append(Toolbox.xmlEncode(name)).append("\" ");
        sb.append("author=\"").append(isUserDefined ? "user" : "admin").append("\">\n");

        if (narrative != null && narrative.length() > 0) {
            String xmlNarrative = Toolbox.xmlEncode(narrative);
            sb.append("<narrative>").append(xmlNarrative).append("</narrative>\n");
        }
        
        if (categories != null && categories.size() > 0) {
            for (LessonCategory category : categories) {
                sb.append("<category category=\"").append(Toolbox.xmlEncode(category.name)).append("\" />\n");
            }
        }
        
        List<LessonItem> words = getWords();
        int wordPosition = 0;
        for (LessonItem word : words) {
            if (word == null) {
                Log.d("Lesson.toXml()", "word is null");
            } else {
                sb.append(((LessonWord) word).toXml(wordPosition++));
            }
        }
        
        sb.append("</lesson>\n");	    	    

        return sb.toString();
    }	


    /**
     * Converts a parsed XML element to a Lesson
     * @param elem XML DOM element
     * @return the Lesson represented by the XML element, or null if
     * there was an error
     */
    public static Lesson importFromXml(Element elem) {
        try {
            String  id          = elem.getAttribute("id");
            String  name        = Toolbox.xmlDecode(elem.getAttribute("name"));
            boolean userDefined = elem.getAttribute("author").equals("user");

            Log.i("Lesson.importFromXml", "id: " + id);
            Log.i("Lesson.importFromXml", "  name: " + name);
            Log.i("Lesson.importFromXml", "  user-defined: " + userDefined);

            Lesson lesson = new Lesson(id, userDefined);
            lesson.setName(name);
            
            NodeList narr = elem.getElementsByTagName("narrative");
            if (narr.getLength() > 0) {
                String narrative = Parser.getNodeValue(narr.item(0));
                narrative = Toolbox.xmlDecode(narrative);
                lesson.setNarrative(narrative);
            }
            
            NodeList cats = elem.getElementsByTagName("category");
            for (int i = 0; i < cats.getLength(); i++) {
                String str = ((Element) cats.item(i)).getAttribute("category");
                str = Toolbox.xmlDecode(str);
                LessonCategory cat = LessonCategory.lookup(str);
                if (cat != null) {
                    lesson.addCategory(cat);
                }
                Log.i("Lesson.importFromXml", "  category: " + str);
            }

            NodeList words = elem.getElementsByTagName("word");
            for (int i = 0; i < words.getLength(); i++) {
                LessonWord word = LessonWord.importFromXml((Element) words.item(i));
                lesson.addWord(word);
                Log.i("Lesson.importFromXml", "  word: " + word.getStringId());
            }
            Collections.sort(lesson.wordObjects, new Comparator<LessonItem>() {
                @Override
                public int compare(LessonItem l, LessonItem r) {
                    return ((LessonWord) l).getLessonOrder() -
                            ((LessonWord) r).getLessonOrder();
                }
            });

            return lesson;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("Lesson.importFromXml", e.getMessage());
            return null;
        }
    }
    
    public int compareTo(Lesson other) {
        if (this.isUserDefined == other.isUserDefined) {
            return Double.compare(this._sort, other._sort);
        }
        return this.isUserDefined ? -1 : 1;
    }
}