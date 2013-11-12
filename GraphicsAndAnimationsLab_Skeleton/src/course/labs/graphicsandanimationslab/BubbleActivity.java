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
import android.widget.Toast;
import android.widget.TextView;

public class BubbleActivity extends Activity {

	FrameLayout mFrame;
	Bitmap mBitmap;
	int score = 0;
	boolean canAdd = true;
	int level = 1;
	int lives = 3;

	static final int LEFT = 1;
	static final int RIGHT = 2;
	static final int UP = 3;
	static final int DOWN = 4;
	private GestureDetector mGestureDetector;


	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// Get references to UI elements
		mFrame = (FrameLayout) findViewById(R.id.frame);
		mBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.b64);

		setupGestureDetector();

		// Set up Add and Remove buttons
		final Button addButton = (Button) findViewById(R.id.add_button);

		addButton.setOnClickListener(new OnClickListener() {

			// Create a new BubbleView
			// Add it to the mBubbleViews list
			// Manage RemoveButton

			public void onClick(View v) {
				int randomWidth = (int)(Math.random() * (mFrame.getWidth()/2) + 60);
				int randomHeight = (int)(Math.random() * (mFrame.getHeight()/2) + 60);
				BubbleView b = new BubbleView(BubbleActivity.this, mFrame.getWidth(), mFrame.getHeight(),((int)Math.random()*level + 1));
				mFrame.addView(b);
			}
		});
	}
	public void setupGestureDetector(){
		Log.e("REBOUND", "setting up gesture detector");
		mGestureDetector = new GestureDetector(this,
				new GestureDetector.SimpleOnGestureListener() {


			@Override
			public boolean onSingleTapConfirmed(MotionEvent event) {
				Log.e("REBOUND", "tapped");
				for(int x =0;x < mFrame.getChildCount();x++)
				{
					Log.e("REBOUND", "child index: " + x);
					BubbleView b = (BubbleView)mFrame.getChildAt(x);
					if(b.intersects(event.getX(), event.getY())){
						Log.e("REBOUND", "INTERSECTS: " + x);
						b.stop(true);
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

		private Activity act;

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
		public BubbleView(Context context, int w, int h,int bounces) {

			super(context);

			act = (Activity) context;
			this.bounces = bounces;
			mDisplayWidth = w;
			mDisplayHeight = h;

			Log.i(TAG, "Display Dimensions: x:" + mDisplayWidth + " y:"
					+ mDisplayHeight);

			Random r = new Random();
			mPainter.setColor(Color.RED);
			mPainter.setTextSize(72);

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
						stop(false);
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
		private void stop(final boolean isPopped) {
			if (null != mMoverFuture && mMoverFuture.cancel(true)) {
				mFrame.post(new Runnable() {
					@Override
					public void run() {
						mFrame.removeView(BubbleView.this);
						if (!isPopped)
						{
							lives--;
							TextView liveDisplay = (TextView)act.findViewById(R.id.lives);
							liveDisplay.setText("Lives: " + lives);
							if (lives >= 0)
								Toast.makeText(getApplicationContext(), "Bubble missed!",Toast.LENGTH_SHORT).show();
						}
					}
				});
			} else {
				Log.e(TAG, "failed to cancel mMoverFuture:" + this);
			}
		}

		// moves the BubbleView
		// returns true if the BubbleView has exited the screen
		private boolean moveUntilOffScreen() {                        

			int result = whereBouncing();

			if (bounces > 0)
			{
				if (result > 0)
				{
					switch (result)
					{
					case LEFT:
					case RIGHT:
						mDx *= -1;
						break;
					case UP:
					case DOWN:
						mDy *= -1;
						break;
					}
					bounces--;
				}
			}
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
		private int whereBouncing() {
			if (mX < 0)
				return LEFT;
			else if (mX > mDisplayWidth - mScaledBitmapWidth)
				return RIGHT;
			else if (mY < 0)
				return UP;
			else if (mY > mDisplayHeight - mScaledBitmapWidth)
				return DOWN;
			else return 0;
		}

		// Draws the scaled Bitmap
		@Override
		protected void onDraw(Canvas canvas) {
			canvas.drawBitmap(mScaledBitmap, mX, mY, mPainter);
			canvas.drawText(bounces + "", mX + (mScaledBitmapWidth/4)-1, mY + (mScaledBitmapWidth/4)+1, mPainter);
			//canvas.drawBitmap(mScaledBitmap, getMatrix(), mPainter);

			canvas.drawText(bounces + "", mX + (mScaledBitmapWidth/2)-1, mY + (mScaledBitmapWidth/2)+1, mPainter);
		}
	}
}