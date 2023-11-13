package com.android.view;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import java.util.List;

import static android.graphics.Paint.ANTI_ALIAS_FLAG;
/**
 @桔子
 2022.3
 务必使用invalidate刷新画布，否则不会更新画布参数
 **/
public class DrawableSpan extends Drawable {
	private float[] angle = new float[8];
	private Draw draw;
	private Span invaliSpan;
	private boolean pattern;
	private int width,height,color = 0xffeeeeee,color_right = 0xffeeeeee,color_bottom = 0xffeeeeee,shadowX = 40,shadowY = 40;
	private float xx = 0,yy = 0;
	private Paint paint,paint_right,paint_bottom;
	private Path path,path_right,path_bottom,isPath;
	private Style style = Style.CIRCLE;
	private Model model = Model.FLAT;
	public DrawableSpan(Style s , int w, int h) {
		init(s, w, h);
	}
	public DrawableSpan(int w, int h) {
		init(Style.CIRCLE, w, h);
	}
	public DrawableSpan(DrawableSpan d) {
		this(d, d.getStyle(), d.getModel());
	}
	public DrawableSpan(DrawableSpan d, Model m) {
		this(d, d.getStyle(), m);
	}
	public DrawableSpan(DrawableSpan d, Style s, Model m) {
		init(s, d.getIntrinsicWidth(), d.getIntrinsicHeight());

		int[] con = d.getContainerdeltaLength();
		setContainerdeltaLength(con[0], con[1]);
		setRound(d.getRound());
		setColor(d.getColor());
		setModel(m);
		setPattern(d.getPattern());


		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			setColorFilter(d.getColorFilter());
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			setAlpha(d.getAlpha());
		}
	}
	private void init(Style s, int w, int h) {
		style = s;
		width = w;
		height = h;
		super.setBounds(0, 0, w , h);

		paint = new Paint(ANTI_ALIAS_FLAG);
		paint_right = new Paint(ANTI_ALIAS_FLAG);
		paint_bottom = new Paint(ANTI_ALIAS_FLAG);

		path = new Path();
        path_right = new Path();
        path_bottom = new Path();
	}
	@Override
	public void draw(Canvas c) {
		if (c.isHardwareAccelerated()) {
			Log.w("DrawableSpan", "DrawableSpan.onDraw(Canvas) Accelerated by hardware!");
		}
		c.save();
		c.translate(xx, yy);//设置画布位置偏移
		if (pattern) {
			c.translate(shadowX + 2, shadowY + 2);
		}//这是当使用内阴影偏移，方便绘制阴影
		if (model == Model.CONCAVE) {
			c.clipPath(path);
			c.drawPath(path, paint);
			c.drawPath(path_bottom, paint_bottom);
			c.drawPath(path_right, paint_right);
		} else {
			c.drawPath(path_bottom, paint_bottom);
			c.drawPath(path_right, paint_right);
			c.drawPath(path, paint);
		}
		c.restore();
		try {
			if (draw != null) {
				draw.onDraw(this, c);
			}
		} catch (Exception e) {
			Log.w("DrawableSpan", "DrawableSpan.onDraw(Canvas)" + e.toString());
		}
	}
	public void setCanvasX(float f) {
		xx = f;
	}//画布x轴偏移
	public void setCanvasY(float f) {
		yy = f;
	}//画布y轴偏移
	public void setContainerdeltaLength(int x, int y) {
		shadowX = (int) (x / 1.25);
		shadowY = (int) (y / 1.25);
	}//阴影扩散范围
	public void setContainerdeltaLength(int i) {
		setContainerdeltaLength(i, i);
	}
	public void setStyle(Style s) {
		if (s != null) {
			style = s;
		}
	}//图形样式
	public void setPattern(boolean b) {
		/**设置阴影扩散状态
		 阴影状态分为内阴影(true)以及外阴影(false)
		 内阴影通过
		 阴影范围缩放主画大小在画布边缘绘制阴影
		 外阴影通过
		 setClipChildren让父布局不限制子控件绘制边界
		 所以阴影的绘制是超出view自身宽高限制的
		 外阴影更好看，但可能会显示不出阴影效果

		 注意
		 此方法设置后必须使用
		 方法invalidate(宽,高)以此生效
		 宽高等于-1则是铺满
		 **/
		pattern = b;
	}
	public void setCanvasWidth(int i) {
		width = i;
	}
	public void setCanvasHeight(int i) {
		width = i;
	}
	public void setModel(Model m) {
		if (m != null) {
			model = m;
		}
	}//阴影样式
	public void postPath(Path ph) {
		isPath = ph;
	}//自定义路径样式
	public void setColor(int i) {
		setColor(i, i);
	}//背景色
	public void setColor(int v, int i) {//设置背景色，阴影色
		color = v;
		color_right = i;
		color_bottom = i;
	}//背景+阴影色
	public void setRound(float...r) {//设置圆角
		if (r == null) {
			return;
		}
		switch (r.length) {
			case 1:
				for (int i=0;i < 8;i++) {
					angle[i] = r[0];
				}
				break;
			case 4:
				for (int i = 0,v = 0;i < 8;i++) {
					if (i % 2 != 0) {
						angle[i - 1] = r[v];
						angle[i] = r[v];
						v++;
					}
				}
				break;
			case 8:
				for (int i=0;i < 8;i++) {
					angle[i] = r[i];
				}
				break;
			default:
				for (int i=0;i < 8;i++) {
					if (i < r.length) {
						angle[i] = r[i];
					} else {
						angle[i] = 0;
					}
				}
				break;
		}
	}//圆角1~8
	public DrawableSpan invalidate(int w, int h) {
		return invalidate(w, h, true);
	}
	public DrawableSpan invalidate() {
		return invalidate(true);
	}
	private DrawableSpan invalidate(int w, int h, boolean b) {//重载画布以及设置画布范围
		if (w > 0) {
			width = w;
		} else if (w == -1) {
			width = getIntrinsicWidth();
		}
		if (h > 0) {
			height = h;
		} else if (h == -1) {
			height = getIntrinsicHeight();
		}
		if (pattern) {
			width = (width - (shadowX * 2 + 4));
			height = (height - (shadowY * 2 + 4));
		}
		return invalidate(b);
	}//重绘并且限制画布大小
	private DrawableSpan invalidate(boolean b) {//重载画笔以及路径并刷新画布
		postColor(color, color_right, color_bottom);
		postPath(width, height);
		if (invaliSpan != null && b) {
			invaliSpan.invalidateSpan();
		}
		super.invalidateSelf();
		return this;
	}//重绘
	public void setDraw(Draw d) {
		draw = d;
	}
	public void invalidateSpan(Span s) {
		invaliSpan = s;
	}
	@Override
	public void setAlpha(int i) {
		paint.setAlpha(i);
		paint_right.setAlpha(i);
		paint_bottom.setAlpha(i);
	}
	@Override
	public void setColorFilter(ColorFilter f) {
		paint.setColorFilter(f);
		paint_right.setColorFilter(f);
		paint_bottom.setColorFilter(f);
	}
	public Paint getPaint(PaintOS p) {
		if (p == null) {
			return null;
		}
		switch (p) {
			case PAINT_SUBJECT:
				return paint;
			case PAINT_RIGHT:
				return paint_right;
			case PAINT_BOTTOM:
				return paint_bottom;
		}
		return null;
	}//获取画笔
	public float getCanvasX() {
		return xx;
	}//获取画布偏移xy
	public float getCanvasY() {
		return yy;
	}
	public int getCanvasWidth() {
		return width;
	}//获取画布大小
	public int getCanvasHeight() {
		return height;
	}
	public Model getModel() {
		return model;
	}
	public Style getStyle() {
		return style;
	}
	public Draw getDraw() {
		return draw;
	}
	public int[] getShadowColor() {
		return new int[]{color_right,color_bottom};
	}//获取上下两个阴影色
	public int getColor() {
		return color;
	}//获取背景色
	public float[] getRound() {
		return angle;
	}
	public boolean getPattern() {
		return pattern;
	}
	public int[] getContainerdeltaLength() {
		return new int[]{(int)(shadowX * 1.25),(int)(shadowY * 1.25)};
	}//获取阴影范围x and y
	@Override  
	public int getIntrinsicWidth() {  
		return super.getBounds().width();  
	}
	@Override  
	public int getIntrinsicHeight() {  
		return super.getBounds().height();  
	}
	@Override
	public int getOpacity() {
		return PixelFormat.TRANSLUCENT;
	}//argb8888
	public enum PaintOS {  //画笔
		PAINT_SUBJECT,//主体
		PAINT_RIGHT,  //亮色
		PAINT_BOTTOM; //暗色
	}
	public enum Style {    //样式
		RECTANGLE //矩形
		, CIRCLE;  //圆形
	}
	public enum Model {    //阴影
        FLAT       //平
		, CONCAVE  //凹
		, CONVEX   //凸
		, PRESSED  //合并
		, NULL;    //空
	}
	public interface Draw {
		void onDraw(DrawableSpan drawable, Canvas canvas);
	}//虚类，可在画布继续绘制
	public interface Span {
		void invalidateSpan();
	}//调用invalidate时的回调，使用invalidate(false)则不回调
	private void postColor(int i, int r, int b) {
		//首先重置渐变以及阴影
		paint.setShader(null);
		paint_right.setShadowLayer(0, 0, 0, 0);
		paint_bottom.setShadowLayer(0, 0, 0, 0);
        //交换颜色，增亮减暗
		int rc = manipulateColor(r, 1.1f);//亮色
		int bc = manipulateColor(b, 0.9f);//暗色

		paint.setColor(i);
		paint_right.setColor(rc);
		paint_bottom.setColor(bc);
		if (model == Model.NULL) {
			return;//空
		}
		if (model == Model.CONCAVE || model == Model.PRESSED) {
			LinearGradient linearGradient = new LinearGradient(0f, 0f
															   , width, height
															   , bc, rc
															   , Shader.TileMode.CLAMP);
			paint.setShader(linearGradient);
		} else if (model == Model.CONVEX) {
			LinearGradient linearGradient = new LinearGradient(0f, 0f
															   , width, height
															   , rc, bc
															   , Shader.TileMode.CLAMP);
			paint.setShader(linearGradient);
		}
		if (model != Model.CONCAVE || model != Model.CONVEX) {
			paint_right.setShadowLayer(shadowX, -shadowX / 2, -shadowX / 2, rc);
			paint_bottom.setShadowLayer(shadowY, shadowY / 2, shadowY / 2, bc);
		}
	}//设置画笔颜色以及阴影，渐变(渐变为内阴影，模拟光照欺骗眼睛让感官以为它是凹陷的)
	private void postPath(int w, int h) {
		if (path_right.isInverseFillType()) {//判断是否启用反转模式
			path_right.toggleInverseFillType();//反转成正常模式
		}
		if (path_bottom.isInverseFillType()) {
			path_bottom.toggleInverseFillType();
		}
		path.reset();//正常
        path_right.reset();//上(亮色)阴影
        path_bottom.reset();//下(暗色)阴影
		if (style == Style.RECTANGLE) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				path.addRoundRect(0, 0, w  , h , angle, Path.Direction.CW);
			}
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				path_right.addRoundRect(0, 0, w  , h , angle, Path.Direction.CCW);
			}
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				path_bottom.addRoundRect(0, 0, w  , h, angle, Path.Direction.CCW);
			}
		} else {
			float radius = h < w ? h / 2 : w / 2;
            path.addCircle(w / 2, h / 2, radius, Path.Direction.CW);
            path_right.addCircle(w / 2, h / 2, radius, Path.Direction.CW);
            path_bottom.addCircle(w / 2, h / 2, radius, Path.Direction.CW);
		}
		if (isPath != null) {
			path.set(isPath);
			path_right.set(isPath);
			path_bottom.set(isPath);
		}
		path.close();
        path_right.close();
        path_bottom.close();
		if (isPath == null && model == Model.CONCAVE) {
            if (!path_right.isInverseFillType()) {
				path_right.toggleInverseFillType();//反转至剪裁模式
            }
            if (!path_bottom.isInverseFillType()) {
				path_bottom.toggleInverseFillType();
            }
        }
	}//设置路径以及路径属性，不建议在draw内设置因为背景可能重绘会导致多余性能消耗
	public static int manipulateColor(int color, float factor) {
        int a = Color.alpha(color);
        int r = Math.round(Color.red(color) * factor);
        int g = Math.round(Color.green(color) * factor);
        int b = Math.round(Color.blue(color) * factor);
        return Color.argb(a,
						  Math.min(r, 255),
						  Math.min(g, 255),
						  Math.min(b, 255));
    }//颜色计算
	public static void postDrawableSpan(final DrawableOther other) {
		/**在第1个DrawableSpan上继续绘制其它DrawableSpan
		 此方法只是重叠，第一DrawableSpan还是需要禁用硬件加速或使用内阴影
		 动态刷新不推荐，耗费性能

		 注意
		 子画布刷新时会刷新主画布
		 主画布刷新时不刷新子画布
		 **/
		if (other == null) {
			return;
		}
		DrawableSpan drawInitial = other.getInitial();
		final List<DrawableSpan> draw = other.getDrawableSpan();
		if (drawInitial == null || draw == null) {
			return;
		}
		drawInitial.setDraw(new Draw(){
				@Override
				public void onDraw(DrawableSpan drawable, Canvas canvas) {
					int size = draw.size();
					if (!draw.isEmpty() && size < 1) {
						Log.w("DrawableSpan", "postCanvas DrawableSpan...Greater Than 0");
						return;
					}
					for (int i = size - 1;i > -1;i--) {
						DrawableSpan d = draw.get(i);
						if (d == null) {
							return;
						}
						int 
							w = other.getWidth(i),
							h = other.getHeight(i);
						float 
							x = other.getX(i),
							y = other.getY(i);
						d.invalidate(w, h, false);
						canvas.save();
						canvas.translate(x, y);
						d.draw(canvas);
						canvas.restore();
					}
				}
			});
	}
	public static void invalidateView(DrawableSpan d, View v) {
		DrawableSpan.invalidateView(d, v, -1, -1);
	}
	public static void invalidateView(final DrawableSpan d, final View v, final int w, final int h) {
		View view = (View) v.getParent();
		if ((!d.getPattern()) && (view instanceof ViewGroup)) {
			ViewGroup ew  = ((ViewGroup)view);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
				if (ew.getClipChildren()) {
					ew.setClipChildren(false);
				}
			}
		}
		view.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
		/**
		 禁用硬件加速(必须);
		 根布局设置ClipChildren=false(必须);

		 如本体设置禁用硬件加速会导致ClipChildren失效
		 所以把禁用硬件加速设置到它的父布局

		 注意:如view重叠后阴影是不生效的
		 两个必须的条件要满足
		 如果需要叠加需要在它的后面放个父布局
		 这个父布局的宽高一定要大于阴影辐射范围
		 暂时以此为准
		 **/
		if (view.isHardwareAccelerated()) {
			Log.w("DrawableSpan", "invalidateView Accelerated by hardware!");
		}
		v.setBackground(d);
		v.post(new Runnable(){
				@Override
				public void run() {
					d.setBounds(0, 0, v.getMeasuredWidth(), v.getMeasuredHeight());
					d.invalidate(w, h);
					//刷新drawble宽高，drawble宽高太小会让图像产生锯齿
					//以及设置画布大小
				}
			});
	}
}

