package nadav.tasher.airflow;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.view.MotionEvent;
import android.view.View;

public class ColorPicker extends View {
	private static final int CENTER_X=300;
	private static final int CENTER_Y=300;
	private static final int CENTER_RADIUS=100;
	private static final float PI=3.1415926f;
	private final int[] mColors;
	int color;
	private Paint mPaint;
	private Paint mCenterPaint;
	private OnColorChangedListener mListener;
	private boolean mTrackingCenter;
	private boolean mHighlightCenter;
	ColorPicker(Context c, OnColorChangedListener l, int color) {
		super(c);
		mListener=l;
		mColors=new int[]{0xFFFF0000, 0xFFFF00FF, 0xFF0000FF, 0xFF00FFFF, 0xFF00FF00, 0xFFFFFF00, 0xFFFF0000};
		Shader s=new SweepGradient(0, 0, mColors, null);
		mPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
		mPaint.setShader(s);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setStrokeWidth(100);
		mCenterPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
		mCenterPaint.setColor(color);
		mCenterPaint.setStrokeWidth(20);
	}
	@Override
	protected void onDraw(Canvas canvas) {
		float r=CENTER_X - mPaint.getStrokeWidth() * 0.5f;
		canvas.translate(CENTER_X, CENTER_X);
		canvas.drawOval(new RectF(-r, -r, r, r), mPaint);
		canvas.drawCircle(0, 0, CENTER_RADIUS, mCenterPaint);
		if(mTrackingCenter){
			int c=mCenterPaint.getColor();
			mCenterPaint.setStyle(Paint.Style.STROKE);
			if(mHighlightCenter){
				mCenterPaint.setAlpha(0xFF);
			}else{
				mCenterPaint.setAlpha(0x80);
			}
			canvas.drawCircle(0, 0, CENTER_RADIUS + mCenterPaint.getStrokeWidth(), mCenterPaint);
			mCenterPaint.setStyle(Paint.Style.FILL);
			mCenterPaint.setColor(c);
		}
	}
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		setMeasuredDimension(CENTER_X * 2, CENTER_Y * 2);
	}
	private int ave(int s, int d, float p) {
		return s + Math.round(p * (d - s));
	}
	private int interpColor(int colors[], float unit) {
		if(unit<=0){
			return colors[0];
		}
		if(unit>=1){
			return colors[colors.length - 1];
		}
		float p=unit * (colors.length - 1);
		int i=(int) p;
		p-=i;
		int c0=colors[i];
		int c1=colors[i + 1];
		int a=ave(Color.alpha(c0), Color.alpha(c1), p);
		int r=ave(Color.red(c0), Color.red(c1), p);
		int g=ave(Color.green(c0), Color.green(c1), p);
		int b=ave(Color.blue(c0), Color.blue(c1), p);
		return Color.argb(a, r, g, b);
	}
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		float x=event.getX() - CENTER_X;
		float y=event.getY() - CENTER_Y;
		boolean inCenter=Math.sqrt(x * x + y * y)<=CENTER_RADIUS;
		switch(event.getAction()){
			case MotionEvent.ACTION_DOWN:
				mTrackingCenter=inCenter;
				if(inCenter){
					mHighlightCenter=true;
					invalidate();
					break;
				}
			case MotionEvent.ACTION_MOVE:
				if(mTrackingCenter){
					if(mHighlightCenter!=inCenter){
						mHighlightCenter=inCenter;
						invalidate();
					}
				}else{
					float angle=(float) Math.atan2(y, x);
					float unit=angle / (2 * PI);
					if(unit<0){
						unit+=1;
					}
					color=interpColor(mColors, unit);
					mCenterPaint.setColor(interpColor(mColors, unit));
					invalidate();
				}
				break;
			case MotionEvent.ACTION_UP:
				if(mTrackingCenter){
					if(inCenter){
						if(mListener!=null){
							mListener.colorChange(mCenterPaint.getColor());
						}
					}
					mTrackingCenter=false;
					invalidate();
				}
				break;
		}
		return true;
	}
	int getColor() {
		return color;
	}
	interface OnColorChangedListener {
		void colorChange(int color);
	}
}