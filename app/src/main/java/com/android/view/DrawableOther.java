package com.android.view;

import android.util.Log;
import android.view.View;
import java.util.ArrayList;

public class DrawableOther {
	public static int CAPACITY = 10;//容量

	private int count = 0;//计数
	private DrawableSpan drawInitial;
	private ArrayList<DrawableSpan> draw =  new ArrayList<>(CAPACITY);
	private ArrayList<Float> x1 =  new ArrayList<>(CAPACITY);
	private ArrayList<Float> y1 =  new ArrayList<>(CAPACITY);
	private ArrayList<Integer> w1 =  new ArrayList<>(CAPACITY);
	private ArrayList<Integer> h1 =  new ArrayList<>(CAPACITY);
	private ArrayList<View> view =  new ArrayList<>(CAPACITY);
	public DrawableOther(DrawableSpan d) {
		if (d == null) {
			Log.w("DrawableOther", "DrawableSpan...Cannot be empty.");
		}
		drawInitial = d;
	}
	public void add(DrawableSpan d, View v) {
		if (d == null) {
			return;
		} else if (v == null) {
			add(d, d.getCanvasWidth(), d.getCanvasHeight(), 0, 0);
			return;
		}
		view.add(v);
		v.post(new RunnableSpan(count, d));//通过post获取view绘制完成后的宽高，设置画布大小
		count += 1;//关键代码，记录当前调用次数
	}
    public void add(DrawableSpan d, int w, int h, float x, float y) {
		if (d == null || drawInitial == null) {
			return;
		}
		draw.add(d);
		x1.add(x);
		y1.add(y);
		w1.add(w);
		h1.add(h);
		drawInitial.invalidate();
		d.invalidateSpan(new DrawableSpan.Span(){
				@Override
				public void invalidateSpan() {
					//当子画布调用invalidate时调用父invalidate
					drawInitial.invalidate();
				}
			});
	}
	public DrawableSpan getInitial() {
		return drawInitial;
	}
	public ArrayList<DrawableSpan> getDrawableSpan() {
		return draw;
	}
	public float getX(int i) {
		if (i < x1.size()) {
			return x1.get(i);
		}
		return 0f;
	}
	public float getY(int i) {
		if (i < y1.size()) {
			return y1.get(i);
		}
		return 0f;
	}
	public int getWidth(int i) {
		if (i < w1.size()) {
			return w1.get(i);
		}
		return 0;
	}
	public int getHeight(int i) {
		if (i < h1.size()) {
			return h1.get(i);
		}
		return 0;
	}
	public View getView(int i) {
		if (i < view.size()) {
			return view.get(i);
		}
		return null;
	}
	private class RunnableSpan implements Runnable {
		private int coun;
		private DrawableSpan dr;
		public RunnableSpan(int i, DrawableSpan d) {
			coun = i;
			dr = d;
		}
		@Override
		public void run() {
			View vn = getView(coun);//通过键值获取对应的view
			int
				w = vn.getMeasuredWidth(),
				h = vn.getMeasuredHeight();
			float 
				x = vn.getLeft(),//同等于getX(在父布局内,本体左上角的位置)
				y = vn.getTop();//设置偏移位置
			add(dr, w, h, x, y);
		}
	}
}
