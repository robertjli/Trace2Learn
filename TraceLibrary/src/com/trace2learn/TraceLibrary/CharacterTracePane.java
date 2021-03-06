/**
 * 
 */
package com.trace2learn.TraceLibrary;

import com.trace2learn.TraceLibrary.Database.LessonCharacter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.util.Log;

public class CharacterTracePane extends CharacterCreationPane {

	protected LessonCharacter _template;
	
	protected Paint _style;
	private static final float _heightToStroke = 12F/400F;
	
	private float _templateTime = 0;
	private float _timeLimit = 0;
	
	private float _strokeTime = 4000;
	
	private long _lastTick;
	private int _curStroke;
	
	Thread _refreshTimer;
	
	protected Handler _handler;
	private Handler moveToNext;
	
	@SuppressLint("NewApi")
	public CharacterTracePane(Context context) {
		super(context);
				
		_style = new Paint();
		_style.setColor(Color.BLUE);
		_style.setAntiAlias(true);
		_style.setDither(true);
		_style.setStyle(Paint.Style.STROKE);
		_style.setStrokeJoin(Paint.Join.ROUND);
		_style.setStrokeCap(Paint.Cap.ROUND);
		_handler = new Handler();
	}

	public void setTemplate(LessonCharacter template)
	{
		_template = template;
		setCurrentTraceStroke(0);
	}
	
	@Override
	public void clearPane()
	{
		super.clearPane();
		setCurrentTraceStroke(0);
	}
	
	/**
	 * Builds and starts the animation timer
	 */
	public void startTimer()
	{
		if(_refreshTimer == null)
		{
			_refreshTimer = new Thread()
			{
				Runnable _update = new Runnable() {
					public void run() {
						invalidate();
					}
				};
				
				public void run()
				{
					while(_templateTime < _timeLimit)
					{
						_handler.post(_update);
						try {
							Thread.sleep(40);
						} catch (InterruptedException e) {
							break;
						}
					}
					_refreshTimer = null;
				}
			};
			_refreshTimer.start();
		}
	}
	
	public void updateTemplateTime()
	{
		if(_template != null)
		{
			long time = System.currentTimeMillis();
			long dif = time - _lastTick;
			float strokeTime = dif/(_strokeTime*_template.getNumStrokes());
			_templateTime = Math.min(_templateTime + strokeTime, _timeLimit);
		}
	}
	
	protected void setCurrentTraceStroke(int stroke)
	{
		if(_template != null)
		{
			int numStrokes = _template.getNumStrokes();
			if (stroke < 0) stroke = 0;
			if (moveToNext != null && stroke >= numStrokes) {
			    moveToNext.sendEmptyMessageDelayed(0, 500);
			    return;
			}
			float strokeLen = 1F/numStrokes;
			_templateTime = stroke*strokeLen;
			_timeLimit = (stroke+1)*strokeLen;
			_lastTick = System.currentTimeMillis();
			startTimer();
		}
		_curStroke = stroke;
	}
	
	@Override
	protected void beginStroke(float newX, float newY)
	{
		super.beginStroke(newX, newY);
		completeStrokeAnimation();
	}

	@Override
	protected boolean completeStroke(float newX, float newY)
	{
		boolean success = super.completeStroke(newX, newY);

		// proceed to next stroke, but only if 
		if(success) animateNextStroke();
		return success;
	}
	
	protected void animateNextStroke() {
		setCurrentTraceStroke(_curStroke + 1);
	}

	
	protected void completeStrokeAnimation() {
		_templateTime = _timeLimit;
	}
	
	@Override
	public void onDraw(Canvas canvas) {	
		updateTemplateTime();
		Log.i("DRAW", "TIME: " + _strokeTime);

        float dynamicStrokeWidth = canvas.getHeight()*_heightToStroke;
        _style.setStrokeWidth(dynamicStrokeWidth);
				
		canvas.drawColor(_backgroundColor);
		if(_template != null) _template.draw(canvas, _style, _templateTime);
		// Consider using a bitmap buffer so only new strokes are drawn.
		drawCharacter(canvas, _character);
	}
	
	public void addMoveToNextHandler(Handler h) {
	    moveToNext = h;
	}
}
