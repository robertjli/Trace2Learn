package com.trace2learn.TraceLibrary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.trace2learn.TraceLibrary.Database.DbAdapter;
import com.trace2learn.TraceLibrary.Database.LessonItem;
import com.trace2learn.TraceLibrary.Database.LessonItem.ItemType;
import com.trace2learn.TraceLibrary.Database.LessonWord;

public class TagActivity extends TraceBaseActivity {

    private static final String[] menuItems = { "Move Up",
                                                "Move Down",
                                                "Delete"};
    private static enum menuItemsInd { MoveUp,
                                       MoveDown,
                                       Delete }
    private static final String TagDeleteSuccessMsg = "Deleted the tag successfully.";
    private static final String TagDeleteErrorMsg = "Failed to delete the tag.";

    //Controls
    private EditText tagEntry;
    private EditText keyEntry;
    private EditText valueEntry;
    private ListView listView;
    private Button addTagButton;
    private Button addIdButton;

    //Variables
    private String stringid; //stringID
    private Map<String, String> keyValMap;
    private List<String> currentKeys;
    private List<String> currentKeyVals;
    private List<String> currentTags;
    private List<String> viewSource; // backs the listview 
    private boolean isChanged;

    private ItemType type;
    private ArrayAdapter<String> adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        //Grab the intent/extras. This should be called from CharacterCreation
        stringid = this.getIntent().getStringExtra("ID");
        type = ItemType.valueOf(getIntent().getStringExtra("TYPE"));        

        // assign layout and cache views 
        setContentView(R.layout.id_and_tag);
        tagEntry     = (EditText) findViewById(R.id.edittext);
        addTagButton = (Button)   findViewById(R.id.add_tag_button);
        keyEntry     = (EditText) findViewById(R.id.editkey);
        valueEntry   = (EditText) findViewById(R.id.editvalue);
        addIdButton  = (Button)   findViewById(R.id.add_key_value_pair_button);
        listView     = (ListView) findViewById(R.id.list);
        
        Log.e("ID",stringid);
        Log.e("TYPE",type.toString());
        
        
        // Handle KeyValue pairs
        viewSource = new ArrayList<String>();
        switch(type)
        {
        case CHARACTER:
        case WORD:
            currentKeys = new ArrayList<String>();
            currentKeyVals = new ArrayList<String>();
            keyValMap = Toolbox.dba.getKeyValues(stringid, type);
            for(Map.Entry<String, String> entry : keyValMap.entrySet()) {
                String k = entry.getKey();
                String v = entry.getValue();
                currentKeys.add(k);
                currentKeyVals.add(k + ": " + v);
                
                viewSource.add(k + ": " + v);
            }
            break;
        default:
            Log.e("Tag", "Unsupported Type");
            break;
        }
        
        // Handle Tags
        switch(type)
        {
            case CHARACTER:
                currentTags = Toolbox.dba.getCharacterTags(stringid);
                break;
            case WORD:
                currentTags = Toolbox.dba.getWordTags(stringid);
                break;
            default:
                Log.e("Tag", "Unsupported Type");
        }
        for (String tag : currentTags) {
            viewSource.add(tag);
        }
        
		// always pre-populate key
		keyEntry.setText(Toolbox.PINYIN_KEY);
        // pre-populate pinyin key-value, for words only
        if(type == ItemType.WORD){
    		// retrieve chars that make up word
    		LessonWord currWord = Toolbox.dba.getWordById(stringid);
    		List<String> chars = currWord.getCharacterIds();
    		for(int i = 0; i < chars.size(); i++){
    			Map<String,String> charkeys = Toolbox.dba.getKeyValues(chars.get(i), ItemType.CHARACTER);
    			if(charkeys.containsKey(Toolbox.PINYIN_KEY)){
    				valueEntry.setText( valueEntry.getText().toString() + charkeys.get(Toolbox.PINYIN_KEY) );
    			}
    		}
        }
        // set input focus to description field
        tagEntry.requestFocus();
        
        adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, viewSource);
        adapter.notifyDataSetChanged();
        listView.setAdapter(adapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        registerForContextMenu(listView);

        isChanged = false;
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
    };

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        menu.setHeaderTitle("Options");
        for (int i = 0; i<menuItems.length; i++) {
            menu.add(Menu.NONE, i, i, menuItems[i]);
        }
    }	

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        ListView view = (ListView) info.targetView.getParent();
        int menuItemIndex = item.getItemId(); // the menu item that was selected
        
        int position = info.position; // position of the list item that was selected
        boolean isID = position < currentKeyVals.size(); // true if it's an ID, otherwise it's a tag
        
        if (menuItemIndex == menuItemsInd.Delete.ordinal()) {
            String selectedItem = (String) view.getItemAtPosition(position);
            Log.d("Deleting", selectedItem);
            Log.d("Deleting", " isID = " + isID);
            
            boolean isSqlQuerySuccessful = false;

            switch(type)
            {
                case CHARACTER:
                    if (isID) {
                        isSqlQuerySuccessful = Toolbox.dba.deleteKeyValue(stringid,
                                type, currentKeys.get(position));
                    }
                    else {
                        isSqlQuerySuccessful = Toolbox.dba.deleteCharTag(stringid,
                                selectedItem);
                    }
                    break;
                case WORD:
                    if (isID) {
                        isSqlQuerySuccessful = Toolbox.dba.deleteKeyValue(stringid,
                                type, currentKeys.get(position));
                    }
                    else {
                        isSqlQuerySuccessful = Toolbox.dba.deleteWordTag(stringid,
                                selectedItem);
                    }
                    break;
                default:
                    Log.e("Tag", "Unsupported Type");
                    return false;
            }

            // show pop-up message and update ListView 
            if (isSqlQuerySuccessful) {
                showToast(TagDeleteSuccessMsg);
                if (isID) {
                	keyValMap.remove(currentKeys.get(position));
                	currentKeyVals.remove(position);
                	currentKeys.remove(position);
                	viewSource.remove(position);
                    adapter.notifyDataSetChanged();
                } else {
                    currentTags.remove(position - currentKeyVals.size());
                    viewSource.remove(position);
                    adapter.notifyDataSetChanged();
                }
            } else {
                showToast(TagDeleteErrorMsg);
            }	
        }

        // move
        else if (menuItemIndex == menuItemsInd.MoveUp.ordinal() ||
                menuItemIndex == menuItemsInd.MoveDown.ordinal()) {
            // going to swap sort values with the item above or below

            // need to get other item
            int otherPos;
            if (menuItemIndex == menuItemsInd.MoveUp.ordinal()) {
                otherPos = position - 1;
            } else {
                otherPos = position + 1;
            }

            String table = null;

            switch(type) {
                case CHARACTER:
                    if (isID) {
                        table = DbAdapter.CHARKEYVALUES_TABLE;
                    } else { // it's a tag
                        table = DbAdapter.CHARTAG_TABLE;
                    }
                    break;
                case WORD:
                    if (isID) {
                        table = DbAdapter.WORDKEYVALUES_TABLE;
                    } else { // it's a tag
                        table = DbAdapter.WORDTAG_TABLE;
                    }
                    break;
                default:
                    Log.e("Tag", "Unsupported Type");
                    return false;
            }

            boolean result = false;
            if (isID) {
                // check that item exists
                if (otherPos < 0) {
                    showToast("Cannot move this ID up");
                    return false;
                } else if (otherPos >= currentKeys.size()) {
                    showToast("Cannot move this ID down");
                    return false;
                }
                
                String key   = currentKeys.get(position);
                String other = currentKeys.get(otherPos);
                
                result = Toolbox.dba.swapKeyValues(table, stringid, key, other);
                
                Log.e("Move result", Boolean.toString(result));
                if (!result) {
                    showToast("Move failed");
                    return false;
                }
                
                // success, so update the local copy
                String[] kArr  = new String[currentKeys.size()];
                String[] kvArr = new String[currentKeyVals.size()];
                String[] arr   = new String[currentKeyVals.size()];
                kArr  = currentKeys.toArray(kArr);
                kvArr = currentKeyVals.toArray(kvArr);
                arr   = viewSource.toArray(arr);
                
                int keyIndex   = currentKeys.indexOf(key);
                int otherIndex = currentKeys.indexOf(other);
                String temp      = kArr[keyIndex];
                kArr[keyIndex]   = kArr[otherIndex];
                kArr[otherIndex] = temp;
                
                temp              = kvArr[keyIndex];
                kvArr[keyIndex]   = kvArr[otherIndex];
                kvArr[otherIndex] = temp;
                
                temp            = arr[keyIndex];
                arr[keyIndex]   = arr[otherIndex];
                arr[otherIndex] = temp;
                
                currentKeys    = new ArrayList<String>(Arrays.asList(kArr));
                currentKeyVals = new ArrayList<String>(Arrays.asList(kvArr));
                viewSource     = new ArrayList<String>(Arrays.asList(arr));
                adapter = new ArrayAdapter<String>(this,
                        android.R.layout.simple_list_item_1, viewSource);
                listView.setAdapter(adapter);
            } else { // it's a tag
                position -= currentKeys.size();
                otherPos -= currentKeys.size();
                // check that item exists
                if (otherPos < 0) {
                    showToast("Cannot move this tag up");
                    return false;
                } else if (otherPos >= currentTags.size()) {
                    showToast("Cannot move this tag down");
                    return false;
                }
                
                String tag   = currentTags.get(position);
                String other = currentTags.get(otherPos);
                switch(type){
                	case CHARACTER:
                	case WORD:
                		result = Toolbox.dba.swapTags(table, stringid, tag, other);
                		break;
                	default:
                		Log.e("Tag", "Unsupported Type");
                		return false;
                }
                
                Log.e("Move result", Boolean.toString(result));
                if (!result) {
                    showToast("Move failed");
                    return false;
                }
                
                // success, so update the local copy
                String[] tArr = new String[currentTags.size()];
                String[] arr = new String[currentTags.size()];
                tArr = currentTags.toArray(tArr);
                arr  = viewSource.toArray(arr);

                int tagIndex = currentTags.indexOf(tag);
                int otherIndex = currentTags.indexOf(other);
                String temp      = tArr[tagIndex];
                tArr[tagIndex]   = tArr[otherIndex];
                tArr[otherIndex] = temp;

                tagIndex   += currentKeys.size();
                otherIndex += currentKeys.size();
                temp            = arr[tagIndex];
                arr[tagIndex]   = arr[otherIndex];
                arr[otherIndex] = temp;

                currentTags = new ArrayList<String>(Arrays.asList(tArr));
                viewSource  = new ArrayList<String>(Arrays.asList(arr));
                adapter = new ArrayAdapter<String>(this, 
                        android.R.layout.simple_list_item_1, viewSource);
                listView.setAdapter(adapter);
            }
            
            isChanged = true;
            return true;
        }

        else {
            Log.e("Tag", "Unsupported context menu");
            return false;
        }

        isChanged = true;

        return true;
    }	

    /**
     * When you want to add a tag to a character/word,
     * just add to database and then update the list view
     * to refect that the tag has been added. The tag should 
     * be at the bottom of the list view.
     * @param view
     */
    public void onAddTagButtonClick (View view)
    {
        if (view == addTagButton)
        {
            String input = tagEntry.getText().toString();
            if (input.length() == 0) return;

            // check duplicate tag
            if (currentTags.contains(input)) {
                showToast("Tag already exists");
                return;
            }

            switch(type)
            {
                case CHARACTER:
                    Toolbox.dba.createCharTags(stringid, input);
                    break;
                case WORD:
                    Toolbox.dba.createWordTags(stringid, input);
                    break;
                default:
                    Log.e("Tag", "Unsupported Type");
                    return;
            }
            //update the listview --> update the entire view
            currentTags.add(input);
            viewSource.add(input);
            adapter.notifyDataSetChanged();

            //Set edit text back to nothing
            tagEntry.setText("");			
            isChanged = true;
        }
    }
    
    public void onAddKeyValuePairButtonClick(View view){
        if (view == addIdButton)
        {
            String keyInput = keyEntry.getText().toString(); 
            String valueInput = valueEntry.getText().toString();
            if (keyInput.length() == 0 || valueInput.length() == 0) return;		

            // check duplicate tag
            // TODO RLi: maybe we should prompt overwrite?
            if (keyValMap.get(keyInput) != null) {
                showToast("Key already exists");
                return;
            }

            Toolbox.dba.createKeyValue(stringid, type, keyInput, valueInput);
            // added it to db
            keyValMap.put(keyInput, valueInput);
            currentKeys.add(keyInput);
            currentKeyVals.add(keyInput + ": " + valueInput);
            viewSource.add(currentKeyVals.size() - 1, keyInput + ": " + valueInput);
            adapter.notifyDataSetChanged();
            keyEntry.setText("");			
            valueEntry.setText("");
            isChanged = true;
        }
    }
    
    public void onSaveButtonClick(View view) {
        close();
    }
    
    private final void showToast(String msg){
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        close();
    }

    private void close() {
        if (isChanged) {
            setResult(RESULT_OK);
            if (type == ItemType.CHARACTER) {
                for (LessonItem character : Toolbox.getCachedCharacters()) {
                    if (stringid.equals(character.getStringId())) {
                        character.invalidate();
                        break;
                    }
                }
            }
        } else {
            setResult(RESULT_CANCELED);
        }
        finish();
    }
}
