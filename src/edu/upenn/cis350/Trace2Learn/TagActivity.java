package edu.upenn.cis350.Trace2Learn;

import java.util.List;

import edu.upenn.cis350.Trace2Learn.Characters.LessonItem.*;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

public class TagActivity extends Activity {

	//Should be able to take BOTH character and word
	
	private DbAdapter mDbHelper;
	
	//Controls
	private EditText editText;
	private ListView lv;
	private Button addTagButton;
	
	//Variables
	private long id;
	private List<String> currentTags;
	//String itemType;
	ItemType type;
	ArrayAdapter<String> arrAdapter;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        editText = (EditText) findViewById(R.id.edittext);
        lv = (ListView) findViewById(R.id.list);
        addTagButton = (Button) findViewById(R.id.add_tag_button);
        
        mDbHelper = new DbAdapter(this);
        mDbHelper.open();
        
        //Grab the intent/extras. This should be called from CharacterCreation
        id = this.getIntent().getLongExtra("ID", -1); 
       // itemType = this.getIntent().getStringExtra("TYPE");
        type = ItemType.valueOf(this.getIntent().getStringExtra("TYPE"));
        
        //Assuming that it is a character
        currentTags = mDbHelper.getTags(id);
        
        //Populate the ListView
        arrAdapter = new ArrayAdapter<String>(this, 
        		android.R.layout.simple_list_item_1, currentTags);
        
        lv.setAdapter(arrAdapter);
        
        setContentView(R.layout.tag); //tag.xml

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
			Editable input = editText.getText();
			String input2 = input.toString(); //This is the string of the tag you typed in
			
			//if the LessonItem is a character
			if (type == ItemType.CHARACTER)
			{
				mDbHelper.createTags(id, input2); //added it to db
			}
			else if (type == ItemType.WORD)
			{		
				//else if the LessonItem is a word
				mDbHelper.createWordTags(id, input2);	
			}
			
			//update the listview --> update the entire view
			//Refactor this, because refreshing the view is inefficient
			
			//Set edit text back to nothing
			editText.setText("");
		}
    }
	

}
