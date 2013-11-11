package course.labs.graphicsandanimationslab;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

public class BubbleActivity extends Activity {

	FrameLayout mFrame;
	Bitmap mBitmap;
	int score = 0;
	boolean canAdd = true;
	int level = 1;
	private GestureDetector mGestureDetector;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// Get references to UI elements
		mFrame = (FrameLayout) findViewById(R.id.frame);
		mBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.b64);

		// Set up Add and Remove buttons
		final Button addButton = (Button) findViewById(R.id.add_button);

		addButton.setOnClickListener(new OnClickListener() {

			// Create a new BubbleView
			// Add it to the mBubbleViews list
			// Manage RemoveButton

			public void onClick(View v) {
				final BubbleView bubbleView = new BubbleView(
						getApplicationContext(), mFrame.getWidth(), mFrame
						.getHeight());
				int randomWidth = (int)(Math.random() * (mFrame.getWidth()/2) + 60);
				int randomHeight = (int)(Math.random() * (mFrame.getHeight()/2) + 60);
				BubbleView b = new BubbleView(BubbleActivity.this, mFrame.getWidth(), mFrame.getHeight());
				mFrame.addView(b);
			}
		});
	}
	public void setupGestureDetector(){

		mGestureDetector = new GestureDetector(this,

				new GestureDetector.SimpleOnGestureListener() {


			// If a single tap intersects a BubbleView, then pop the BubbleView
			// Otherwise, create a new BubbleView at the tap's location.

			@Override
			public boolean onSingleTapConfirmed(MotionEvent event) {
				for(int x =0;x < mFrame.getChildCount();x++)
				{
					BubbleView b = (BubbleView)mFrame.getChildAt(x);
					if(b.intersects(event.getX(), event.getY())){
						b.stop();
						score += level;
						TextView score = (TextView)findViewById(R.id.score);
						score.setText("Score: " + score);
					}					
				}
				return true;
			}
		});
	}

	private class BubbleView extends View {
		// Base Bitmap Size
		private static final int BITMAP_SIZE = 64;

		// Animation refresh rate
		private static final int REFRESH_RATE = 15;

		// Log TAG
		private static final String TAG = "BubbleActivity";

		// Current top and left coordinates
		private float mX, mY;

		// Direction and speed of movement
		// measured in terms of how much the BubbleView moves
		// in one time step.
		private float mDx, mDy;

		// Height and width of the FrameLayout
		private int mDisplayWidth, mDisplayHeight;

		// Size of the BubbleView
		private int mScaledBitmapWidth;

		// Underlying Bitmap scaled to new size
		private final Bitmap mScaledBitmap;

		private final Paint mPainter = new Paint();

		// Reference to the movement calculation and update code
		private final ScheduledFuture<?> mMoverFuture;

		private int bounces;

		// context and width and height of the FrameLayout
		public BubbleView(Context context, int w, int h) {

			super(context);

			mDisplayWidth = w;
			mDisplayHeight = h;

			Log.i(TAG, "Display Dimensions: x:" + mDisplayWidth + " y:"
					+ mDisplayHeight);

			Random r = new Random();
			mPainter.setColor(Color.RED);

			// Set BubbleView's size

			// mScaledBitmapWidth =
			mScaledBitmapWidth = (int)(mDisplayWidth * (Math.random()*.1 + .15));

			mScaledBitmap = Bitmap.createScaledBitmap(mBitmap,
					mScaledBitmapWidth, mScaledBitmapWidth, false);

			// Set initial location

			mX = (int)(Math.random() * (mDisplayWidth/2) + (mDisplayWidth/4));
			mY = (int)(Math.random() * (mDisplayHeight/2) + (mDisplayWidth/4));

			// Set movement direction and speed
			while(mDx ==0 || mDy == 0){
				mDx = (int)(Math.random() * 3 - 3);
				mDy = (int)(Math.random() * 3 - 3);
			}
			ScheduledExecutorService executor = Executors
					.newScheduledThreadPool(1);

			// The BubbleView's movement calculations & display update
			mMoverFuture = executor.scheduleWithFixedDelay(new Runnable() {
				@Override
				public void run() {
					if(!isOutOfView()){
						moveUntilOffScreen();
						postInvalidate();
					}
					else{
						stop();
					}

				}
			}, 0, REFRESH_RATE, TimeUnit.MILLISECONDS);

			Log.i(TAG, "Bubble created at x:" + mX + " y:" + mY);
			Log.i(TAG, "Bubble direction is dx:" + mDx + " dy:" + mDy);

		}
		// Returns true is the BubbleView intersects position (x,y)
		private synchronized boolean intersects(float x, float y) {
			return x > mX && x < mX + mScaledBitmapWidth && y > mY
					&& y < mY + mScaledBitmapWidth;
		}
		private void stop() {
			if (null != mMoverFuture && mMoverFuture.cancel(true)) {
				mFrame.post(new Runnable() {
					@Override
					public void run() {
						mFrame.removeView(BubbleView.this);
					}
				});
			} else {
				Log.e(TAG, "failed to cancel mMoverFuture:" + this);
			}
		}
		public int getCountOnBubble(){
			return bounces;
		}
		public void incrementScore(int incrementBy){
			score += incrementBy;
		}
		public void decrementCountOnBubble(){
			bounces--;			
		}

		// moves the BubbleView
		// returns true if the BubbleView has exited the screen
		private boolean moveUntilOffScreen() {
			if(isOutOfView())
				return true;
			mX += mDx;
			mY += mDy;

			return false;
		}

		// returns true if the BubbleView has completely left the screen
		private boolean isOutOfView() {
			//TODO decrement score
			return mX < 0 - mScaledBitmapWidth || mX > mDisplayWidth
					|| mY < 0 - mScaledBitmapWidth || mY > mDisplayHeight;
		}

		private boolean isBouncing() {
			return mX < 0 || mX > mDisplayWidth - mScaledBitmapWidth
					|| mY < 0 || mY > mDisplayHeight - mScaledBitmapWidth;
		}

		// Draws the scaled Bitmap
		@Override
		protected void onDraw(Canvas canvas) {
			canvas.drawBitmap(mScaledBitmap, mX, mY, mPainter);
			canvas.drawText("4", mX + (mScaledBitmapWidth/2)-1, mY + (mScaledBitmapWidth/2)+1, mPainter);
			//canvas.drawBitmap(mScaledBitmap, getMatrix(), mPainter);
		}
	}

}