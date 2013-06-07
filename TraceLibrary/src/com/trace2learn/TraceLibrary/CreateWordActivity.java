package com.trace2learn.TraceLibrary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.trace2learn.TraceLibrary.Database.LessonCharacter;
import com.trace2learn.TraceLibrary.Database.LessonItem;
import com.trace2learn.TraceLibrary.Database.LessonWord;

public class CreateWordActivity extends TraceBaseActivity {
    
    private LessonWord            newWord;
    private List<LessonItem>      source;  // list of all characters
    private List<LessonItem>      display; // list of items being displayed
    private LessonItemListAdapter charAdapter;
    
    // Activity views
    private LinearLayout thumbnails;
    private ListView     charList;
    private TextView     filterStatus;
    private Button       clearButton;
    private Button       undoButton;
    private Button       cancelButton;
    private Button       saveButton;
    private Button       filterButton;
    
    private LayoutInflater vi;
    
    private int thumbBgColor;
    
    private int     numChars;
    private boolean filtered;
    
    private enum RequestCode {
        EDIT_TAG,
        ADD_TO_COLLECTION;
    }
    
    
    //initializes the list if all characters in the database
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        filtered = false;
        numChars = 0;
        setContentView(R.layout.create_word);
        vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        getViews();
        getHandlers();
        
        filterStatus.setVisibility(View.GONE);

        newWord = new LessonWord();
        
        thumbBgColor = getResources().getColor(R.color.thumb_background);

        getChars();
        displayAllChars();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    
    private void getViews() {
        thumbnails   = (LinearLayout) findViewById(R.id.thumbnail_gallery);
        charList     = (ListView)     findViewById(R.id.charList);
        filterStatus = (TextView)     findViewById(R.id.filterStatus);
        clearButton  = (Button)       findViewById(R.id.clearButton);
        undoButton   = (Button)       findViewById(R.id.undoButton);
        cancelButton = (Button)       findViewById(R.id.cancelButton);
        saveButton   = (Button)       findViewById(R.id.saveButton);
        filterButton = (Button)       findViewById(R.id.filterButton);
    }
    
    private void getHandlers() {
        //when a char is clicked, it is added to the new word and added to the gallery
        charList.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position,long id) {     
                numChars++;
                LessonItem item = (LessonCharacter) charList.getItemAtPosition(position);
                String charId = item.getStringId();
                newWord.addCharacter(charId);
                ImageView iv = new ImageView(getApplicationContext());
                iv.setBackgroundColor(thumbBgColor);
                iv.setImageBitmap(BitmapFactory.buildBitmap(item, 64, 64));
                thumbnails.addView(iv);
            }
        });
        
        // clear button - clears the current word
        clearButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                numChars = 0;
                newWord.clearCharacters();
                thumbnails.removeAllViews();
            }
        });
        
        // delete button - deletes the last character in the word
        undoButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (numChars == 0) {
                    return;
                }
                numChars--;
                newWord.removeLastCharacter();
                thumbnails.removeViewAt(numChars);
            }
        });
        
        // filter button - either shows the filter popup or clears the filter
        filterButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (filtered) {
                    clearFilter();
                } else {
                    showFilterPopup();
                }
            }
        });
        
        // save button - prompts to add word to a lesson
        saveButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = getApplicationContext();

            	// optimization - if word is a single character,
            	// than copy all character tags to the new word
                if(newWord.length() == 1){
                	LessonCharacter lc = Toolbox.dba.getCharacterById(newWord.getCharacterId(0));
                	newWord.setTagList(lc.getTags());
                	newWord.setKeyValues(lc.getKeyValues());
                }
                if(newWord.length() > 0 && Toolbox.dba.addWord(newWord)){
                    addToCollection();
                    return;
                }
                Toolbox.showToast(context, "Word is empty!");
            }
        });
        
        // cancel button - finishes this activity
        cancelButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }
    
    private void getChars() {
        source = Toolbox.characters;
    }

    /**
     * Set display list to source list, thus displaying all characters
     */
    private void displayAllChars() {
        display = source;
        displayChars();      
    }

    /**
     * Display the current display list
     */
    private void displayChars() {
        Collections.sort(display);
        charAdapter = new LessonItemListAdapter(this, display, vi);
        charList.setAdapter(charAdapter);   
    }
    
    private void addToCollection() {
        Intent i = new Intent(getApplicationContext(), AddToCollectionActivity.class);
        i.putExtra("word", newWord.getStringId());
        startActivityForResult(i, RequestCode.ADD_TO_COLLECTION.ordinal());
    }

    private void createTags() {
        String id = newWord.getStringId();
        if (id != null) {
            Log.e("Passing this WordID", id);
            Intent i = new Intent(this, TagActivity.class);

            i.putExtra("ID", id);
            i.putExtra("TYPE", newWord.getItemType().toString());

            startActivityForResult(i, RequestCode.EDIT_TAG.ordinal());
        } else {
            Toolbox.showToast(getApplicationContext(),
                    "Error: Save the word before adding tags");
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RequestCode.EDIT_TAG.ordinal()) {
            finish();
        } else if (requestCode == RequestCode.ADD_TO_COLLECTION.ordinal()) {
            if (resultCode == RESULT_OK) {
                createTags();
            } else {
                Toolbox.dba.deleteWord(newWord.getStringId());
                Toolbox.showToast(getApplicationContext(), "Phrase not saved");
            }
        }
    }
    
    // displays the filter popup
    private void showFilterPopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Apply Filter");
        
        final EditText filterText = new EditText(this);
        builder.setView(filterText);
        
        builder.setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String search = filterText.getText().toString();
                if (search.equals("")) {
                    hideKeyboard(filterText);
                    return;
                }
                
                // Filter action: keep matching items from display list
                // Note that it should be partial match for search terms 3
                // characters or more.
                ArrayList<LessonItem> newList = new ArrayList<LessonItem>();
                topLoop: for (LessonItem item : display) {
                    List<String> tags = item.getTags();
                    for (String tag : tags) {
                        if (Toolbox.containsMatch(1, tag, search)) {
                            newList.add(item);
                            continue topLoop;
                        }
                    }
                    Collection<String> values = item.getKeyValues().values();
                    for (String value : values) {
                        if (Toolbox.containsMatch(1, value, search)) {
                            newList.add(item);
                            continue topLoop;
                        }
                    }
                }
                display = newList;
                displayChars();
                
                // Set state to filtered
                filterButton.setText(R.string.clear_filter);
                filtered = true;
                hideKeyboard(filterText);
                filterStatus.setText("Current filter: " + search);
                filterStatus.setVisibility(View.VISIBLE);
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                hideKeyboard(filterText);
            }
        });
        
        AlertDialog dialog = builder.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        dialog.show();

    }
    
    // clears the filter
    private void clearFilter() {
        displayAllChars();
        filterButton.setText(R.string.filter);
        filtered = false;
        filterStatus.setVisibility(View.GONE);
    }
    
    //for testing purposes
    public LessonWord getWord(){
        return newWord;
    }
    
    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager)getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}