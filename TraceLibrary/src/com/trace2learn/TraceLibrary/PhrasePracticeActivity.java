package com.trace2learn.TraceLibrary;

import java.util.ArrayList;
import java.util.List;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.ViewAnimator;

import com.trace2learn.TraceLibrary.Database.DbAdapter;
import com.trace2learn.TraceLibrary.Database.LessonCharacter;
import com.trace2learn.TraceLibrary.Database.LessonWord;

public class PhrasePracticeActivity extends TraceBaseActivity {

    private DbAdapter dba;

    private Mode    currentMode = null;
    private String  lessonID = null;
    private int     phraseIndex; // index of current phrase in collection
    private int     collectionSize;
    private String  lessonName;
    private boolean quizMode;
    private int     currentChar;

    private LessonWord                 word;
    private ArrayList<LessonCharacter> characters;

    private TextView     tagView;
    private TextView     titleView;
    private Button       playButton;
    private Button       traceButton;
    private ToggleButton quizToggle;
    private ImageView    quizIcon;
    private LinearLayout thumbnails;
    private ViewAnimator animator;
    private ImageView    soundIcon;
    private ImageView    prevIcon;
    private ImageView    nextIcon;

    private ArrayList<SquareLayout>          displayLayouts;
    private ArrayList<SquareLayout>          traceLayouts;
    private ArrayList<CharacterPlaybackPane> playbackPanes;
    private ArrayList<CharacterTracePane>    tracePanes;
    
    private SharedPreferences prefs;
    
    private SoundPool soundPool;
    private int       soundId;
    
    private int thumbBg;
    private int thumbBgSelected;

    private enum Mode {
        DISPLAY, TRACE;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.practice_phrase);

        getViews();
        getHandlers();

        characters = new ArrayList<LessonCharacter>();

        displayLayouts = new ArrayList<SquareLayout>();
        traceLayouts   = new ArrayList<SquareLayout>();
        playbackPanes  = new ArrayList<CharacterPlaybackPane>();
        tracePanes     = new ArrayList<CharacterTracePane>();

        dba = new DbAdapter(this);
        dba.open();
        
        prefs = getSharedPreferences(Toolbox.PREFS_FILE, MODE_PRIVATE);
        
        thumbBg         = getResources().getColor(R.color.thumb_background);
        thumbBgSelected = getResources().getColor(R.color.thumb_background_selected);

        initializeMode();
    }

    @Override
    protected void onPause() {
        super.onPause();
        
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(Toolbox.PREFS_QUIZ_MODE, quizMode);
        editor.commit();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        dba.close();
        if (soundPool != null) {
            soundPool.release();
        }
    };

    private void getViews() {
        titleView   = (TextView)     findViewById(R.id.phrase_title);
        tagView     = (TextView)     findViewById(R.id.tag_list);
        playButton  = (Button)       findViewById(R.id.animate_button);
        traceButton = (Button)       findViewById(R.id.trace_button);
        quizToggle  = (ToggleButton) findViewById(R.id.quiz_toggle);
        quizIcon    = (ImageView)    findViewById(R.id.quiz_icon);
        animator    = (ViewAnimator) findViewById(R.id.view_slot);
        thumbnails  = (LinearLayout) findViewById(R.id.thumbnail_gallery);
        soundIcon   = (ImageView)    findViewById(R.id.sound_button);
        prevIcon	= (ImageView)    findViewById(R.id.go_prev);
        nextIcon	= (ImageView)    findViewById(R.id.go_next);
    }

    private void getHandlers() {
        // Trace Button
        traceButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setCharacterTracePane();
            }
        });

        // Play Button
        playButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setDisplayPane();
            }
        });
        
        // Quiz Mode Toggle
        quizToggle.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setQuizMode(!quizMode);
            }
        });

        // Clicking on the tags while in quiz mode
        tagView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleTagView(false);
            }
        });
        
        // Clicking on the quiz icon while in quiz mode
        quizIcon.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleTagView(true);
            }
        });

        // Clicking on the sound icon
        soundIcon.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                playSound();
            }
        });
        
        nextIcon.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                moveToNextPhrase();
            }
        });    
        
        prevIcon.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                moveToPrevPhrase();
            }
        });
    }
    
    private void moveToNextPhrase() {
        if (phraseIndex < collectionSize) { // not at end of collection yet
            // shutdown and notify parent activity
            Bundle bundle = new Bundle();
            bundle.putInt("next", phraseIndex);
            Intent intent = new Intent();
            intent.putExtras(bundle);
            setResult(RESULT_OK, intent);
            finish();
            
            Vibrator v = (Vibrator) getApplicationContext().getSystemService(Service.VIBRATOR_SERVICE);
            v.vibrate(300);
        }
    }
    
    private void moveToPrevPhrase() {
        if (phraseIndex > 1) { // not at beginning of collection yet
            // shutdown and notify parent activity
            Bundle bundle = new Bundle();
            bundle.putInt("next", phraseIndex - 2);
            Intent intent = new Intent();
            intent.putExtras(bundle);
            setResult(RESULT_OK, intent);
            finish();
            
            Vibrator v = (Vibrator) getApplicationContext().getSystemService(Service.VIBRATOR_SERVICE);
            v.vibrate(300);
        }
    }    
    /**
     * Initialize the display mode, if the activity was started with intent to
     * display a character, that character should be displayed
     */
    private void initializeMode() {
        Bundle bun      = getIntent().getExtras();
        Context context = getApplicationContext();
        if (bun != null && bun.containsKey("wordId")) {
            characters.clear();
            tracePanes.clear();
            playbackPanes.clear();
            traceLayouts.clear();
            displayLayouts.clear();
            
            String wordId = bun.getString("wordId");
            word = dba.getWordById(wordId);
            setWord(word);
            updateTags();

            lessonID = bun.getString("lessonID");
            if (lessonID == null) {
                titleView.setText("");
            } else {
                phraseIndex = bun.getInt("index");
                collectionSize = bun.getInt("collectionSize");
                lessonName = dba.getLessonById(lessonID).getLessonName();
                titleView.setText(lessonName + " - " + phraseIndex + 
                        " of " + collectionSize);
            }

            // Activity Mode
            String mode = prefs.getString(Toolbox.PREFS_PHRASE_MODE, "trace"); 
            if (mode.equals("display")) {
                setDisplayPane();
            } else if (mode.equals("trace")) {
                setCharacterTracePane();
            }
            
            // Quiz Mode
            quizMode = prefs.getBoolean(Toolbox.PREFS_QUIZ_MODE, true);
            setQuizMode(quizMode);
            
            // Sound
            soundIcon.setVisibility(View.GONE);            
            int soundFile = 0;
            if (word.hasKey(Toolbox.SOUND_KEY) || word.hasKey(Toolbox.PINYIN_KEY)) {
                soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
                // use 'sound' tag to locate audio if it exists, otherwise use 'pinyin' tag
                String audioKey = word.hasKey(Toolbox.SOUND_KEY) ? Toolbox.SOUND_KEY : Toolbox.PINYIN_KEY;
                soundFile = getResources().getIdentifier(
                        word.getValue(audioKey), "raw", getPackageName());
	            if (soundFile != 0) { // check to make sure resource exists
	                soundId = soundPool.load(getApplicationContext(), soundFile, 1);
	                soundIcon.setVisibility(View.VISIBLE);
	            }
            }
        } else {
            Toolbox.showToast(context, "No word selected");
            finish();
        }
    }

    private void setSelectedCharacter(int position) {
        thumbnails.getChildAt(currentChar).setBackgroundColor(thumbBg);
        
        currentChar = position;
        animator.setDisplayedChild(position);
        tracePanes.get(position).clearPane();
        thumbnails.getChildAt(position).setBackgroundColor(thumbBgSelected);
    }

    private void setWord(LessonWord word) {
        setCharacterList(word.getCharacterIds());
        setSelectedCharacter(0);
    }

    private void setCharacterList(List<String> ids) {
        Context context = getApplicationContext();
        int index = 0;
        for(String id : ids) {
            LessonCharacter ch = dba.getCharacterById(id);
            ImageView iv = new ImageView(context);
            iv.setBackgroundColor(thumbBg);
            iv.setImageBitmap(BitmapFactory.buildBitmap(ch, 64, 64));
            final int i = index;
            index++;
            iv.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    setSelectedCharacter(i);
                }
            });
            this.characters.add(ch);
            this.thumbnails.addView(iv);
            
            SquareLayout disp = new SquareLayout(animator.getContext());
            CharacterPlaybackPane dispPane = new CharacterPlaybackPane(
                    disp.getContext(), false, 2);
            dispPane.setCharacter(ch);
            disp.addView(dispPane);

            this.displayLayouts.add(disp);
            this.playbackPanes.add(dispPane);

            SquareLayout trace = new SquareLayout(animator.getContext());
            CharacterTracePane tracePane = new CharacterTracePane(
                    disp.getContext());
            tracePane.setTemplate(ch);
            tracePane.addMoveToNextHandler(moveToNext);
            trace.addView(tracePane);

            this.traceLayouts.add(trace);
            this.tracePanes.add(tracePane);
        }
    }

    /**
     * Switches the display mode to display
     */
    private synchronized void setDisplayPane() {
        int child = animator.getDisplayedChild();
        playbackPanes.get(child).setAnimated(true);

        if (currentMode != Mode.DISPLAY) {
            int curInd = animator.getDisplayedChild();
            animator.removeAllViews();
            for(SquareLayout disp : this.displayLayouts)
            {
                animator.addView(disp);
            }
            animator.setDisplayedChild(curInd);
            currentMode = Mode.DISPLAY;
            
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(Toolbox.PREFS_PHRASE_MODE, "display");
            editor.commit();
            
            playButton.setText(Html.fromHtml("<b>" +
                    getString(R.string.animate) + "</b>"));
            traceButton.setText(getString(R.string.practice));
        }
    }

    /**
     * Switches the display mode to display
     */
    private synchronized void setCharacterTracePane() {
        int child = animator.getDisplayedChild();
        this.tracePanes.get(child).clearPane();

        if (currentMode != Mode.TRACE) {
            int curInd = animator.getDisplayedChild();
            animator.removeAllViews();
            for(SquareLayout trace : this.traceLayouts)
            {
                animator.addView(trace);
            }
            animator.setDisplayedChild(curInd);
            currentMode = Mode.TRACE;
            
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(Toolbox.PREFS_PHRASE_MODE, "trace");
            editor.commit();

            
            traceButton.setText(Html.fromHtml("<b>" +
                    getString(R.string.practice) + "</b>"));
            playButton.setText(getString(R.string.animate));
        }
    }

    public void setContentView(View view) {
        super.setContentView(view);
    }

    private void updateTags() {
        if (word != null) {
            StringBuilder sb = new StringBuilder();
            // display any tags
            sb.append(word.getTagsToString());

            // display the pinyin value, if it exists
            if (word.hasKey(Toolbox.PINYIN_KEY)) {
                if (sb.length() > 0) sb.append("\n");
                sb.append("(");
                sb.append(word.getValue(Toolbox.PINYIN_KEY));
                sb.append(")");
            }

            tagView.setText(sb.toString());
        }
    }
    
    private void setQuizMode(boolean state) {
        if (state) {
            quizMode = true;
            tagView.setClickable(true);
            tagView.setVisibility(View.INVISIBLE);
            quizIcon.setVisibility(View.VISIBLE);
        } else {
            quizMode = false;
            tagView.setClickable(false);
            tagView.setVisibility(View.VISIBLE);
            quizIcon.setVisibility(View.INVISIBLE);
        }
        
        quizToggle.setChecked(quizMode);
    }
    
    private void toggleTagView(boolean show) {
        if (!quizMode) { return; }
        
        if (show) {
            tagView.setVisibility(View.VISIBLE);
            quizIcon.setVisibility(View.INVISIBLE);
        } else {
            tagView.setVisibility(View.INVISIBLE);
            quizIcon.setVisibility(View.VISIBLE);
        }
    }
    
    private void playSound() {
        Log.i("SoundPool", "Playing sound");
        soundPool.play(soundId, Toolbox.VOLUME, Toolbox.VOLUME, 1, 0, 1);
    }

    @Override
    public void onRestart() {
        super.onRestart();
        updateTags();
    }

    Handler moveToNext = new Handler() {
        @Override
        public void handleMessage(Message m) {
            int index = animator.getDisplayedChild();
            if (index + 1 < characters.size()) {
                setSelectedCharacter(index + 1);
            } else {
                // this is the end of the word
                if (lessonID != null) {
                    if (phraseIndex < collectionSize) { // still more words
                       
                    	// TODO: animate the 'next' icon, to remind user to proceed to next phrase
                        
                    } else if (phraseIndex == collectionSize) {
                        // the last word in the collection
                        Toolbox.showToast(getApplicationContext(),
                                "Completed " + lessonName);
                    }
                }
            }
        }
    };
}
