package com.rjp.linechartdemo;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Scroller;

import com.alibaba.fastjson.JSONArray;

import java.util.List;

/**
 * @author Gimpo create on 2018/1/12 11:39
 * @email : jimbo922@163.com
 */

public class LineChartView extends View {

    private Context mContext;

    //两种类型 折线 柱状
    public static final int TYPE_LINE = 1;
    public static final int TYPE_BAR = 2;
    private int type = TYPE_BAR;

    String data = "[{\"title\": \"今天\", \"number\": \"42\"}, {\"title\": \"06:00\", \"number\": \"24\"}, {\"title\": \"12:00\", \"number\": \"8\"}, {\"title\": \"18:00\", \"number\": \"42\"}, {\"title\": \"明天\", \"number\": \"12\"}, {\"title\": \"06:00\", \"number\": \"36\"}, {\"title\": \"12:00\", \"number\": \"4\"}, {\"title\": \"18:00\", \"number\": \"16\"}, {\"title\": \"后天\", \"number\": \"50\"}, {\"title\": \"06:00\", \"number\": \"24\"}, {\"title\": \"12:00\", \"number\": \"42\"}, {\"title\": \"18:00\", \"number\": \"25\"}]";

    //图的属性
    private int backColor;      //背景色
    private int barColor;       //柱状图色
    private int lineColor;      //线色
    private int dotColor;       //点色
    private int topHeight;      //顶部偏移
    private int lineHeight;     //行高
    private int lineWidth;      //柱状图宽
    private int lineSpace;      //柱状图之间的缝隙
    private int viewWidth;      //view宽
    private int viewHeight;     //view高
    private int MAX_SCROLL_X;   //最大偏移

    //每一个bar
    private List<Bar> bars;
    private Paint barPaint;
    private Paint dotPaint;
    private Paint linePaint;
    private Paint tipPaint;

    //记录坐标
    private float downX;
    private float downY;
    private Scroller mScroller;
    private int mMinimumVelocity;
    private int mMaximumVelocity;
    private VelocityTracker mVelocityTracker;
    private Path mPath;
    private int dialogX;
    private int dialogY;
    private int touchSlop;

    public LineChartView(Context context) {
        this(context, null);
    }

    public LineChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context, attrs);
    }

    private void initView(Context context, AttributeSet attrs) {
        mContext = context;
        lineHeight = dp2px(mContext, 60);
        lineWidth = dp2px(mContext, 60);
        topHeight = dp2px(mContext, 30);
        lineSpace = dp2px(mContext, 4);
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.LineChartView);
            backColor = a.getColor(R.styleable.LineChartView_back_color, Color.parseColor("#eb1c42"));
            setBackgroundColor(backColor);
            barColor = a.getColor(R.styleable.LineChartView_bar_color, Color.parseColor("#999999"));
            lineColor = a.getColor(R.styleable.LineChartView_line_color, Color.parseColor("#ffffff"));
            dotColor = a.getColor(R.styleable.LineChartView_dot_color, Color.parseColor("#ffffff"));
        }

        barPaint = new Paint();
        barPaint.setAntiAlias(true);
        barPaint.setColor(barColor);

        dotPaint = new Paint();
        dotPaint.setAntiAlias(true);
        dotPaint.setColor(dotColor);

        tipPaint = new Paint();
        tipPaint.setAntiAlias(true);
        tipPaint.setColor(Color.BLUE);

        linePaint = new Paint();
        linePaint.setAntiAlias(true);
        linePaint.setColor(lineColor);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(10f);

        mPath = new Path();

        bars = JSONArray.parseArray(data, Bar.class);

        //惯性滑动部分
        ViewConfiguration viewConfiguration = ViewConfiguration.get(mContext);
        touchSlop = viewConfiguration.getScaledTouchSlop();
        mScroller = new Scroller(mContext);
        // 惯性滑动最低速度要求 低于这个速度认为是触摸
        mMinimumVelocity = viewConfiguration.getScaledMinimumFlingVelocity();
        // 惯性滑动的最大速度  触摸速度不会超过这个值
        mMaximumVelocity = viewConfiguration.getScaledMaximumFlingVelocity();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        obtainVelocityTracker();
        float currentX = event.getX();
        float currentY = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downX = currentX;
                downY = currentY;
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = currentX - downX;
                float dy = currentY - downY;
                if(Math.abs(dx) > Math.abs(dy)){
                    getParent().requestDisallowInterceptTouchEvent(true);
                    scrollTo((int) (getScrollX() - dx), 0);
                    computeXY();
                    invalidate();
                    downX = currentX;
                    downY = currentY;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                int initialVelocity = (int) mVelocityTracker.getXVelocity();
                if ((Math.abs(initialVelocity) > mMinimumVelocity)) {
                    flingX(-initialVelocity);
                }
                releaseVelocityTracker();
                break;
        }
        if (mVelocityTracker != null) {
            mVelocityTracker.addMovement(event);
        }
        return true;
    }

    /**
     * 计算指示器的位置
     */
    private void computeXY() {
        int scrollX = getScrollX();
        dialogX = (int) (scrollX * 1.0 / MAX_SCROLL_X * (viewWidth - lineWidth)) + scrollX + lineWidth / 2;
        int size = bars.size();
        int currentIndex = 0;
        for (int i = 0; i < size; i++) {
            if(i != size - 1){
                Bar currentBar = bars.get(i);
                Bar nextBar = bars.get(i + 1);
                if (currentBar.getMx() < dialogX && nextBar.getMx() > dialogX) {
                    currentIndex = i;
                    break;
                }
            }
        }
        if (currentIndex < size - 1) {
            Bar currentBar = bars.get(currentIndex);
            Bar nextBar = bars.get(currentIndex + 1);
            dialogY = (int) ((dialogX - currentBar.getMx()) * (nextBar.getMy() - currentBar.getMy()) * 1.0 / (nextBar.getMx() - currentBar.getMx()) + currentBar.getMy());
        }
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            int x = mScroller.getCurrX();
            scrollTo(x, 0);
            computeXY();
            postInvalidate();
        }
    }

    /**
     * 惯性滑动
     *
     * @param velocityX
     */
    public void flingX(int velocityX) {
        mScroller.fling(getScrollX(), getScrollY(), velocityX, 0, 0, MAX_SCROLL_X, 0, 0);
        awakenScrollBars(mScroller.getDuration());
        invalidate();
    }

    /**
     * 初始化 速度追踪器
     */
    private void obtainVelocityTracker() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    /**
     * 释放 速度追踪器
     */
    private void releaseVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    /**
     * 得到数据计算每一个柱状图的位置
     */
    private void compute() {
        if (bars == null || bars.size() == 0) {
            return;
        }
        int size = bars.size();
        for (int i = 0; i < size; i++) {
            Bar bar = bars.get(i);
            bar.setLeft(i * (lineWidth + lineSpace));
            bar.setTop((int) (viewHeight - (lineHeight * Integer.parseInt(bar.getNumber())) * 1.0 / 25));
            bar.setRight((i + 1) * lineWidth + i * lineSpace);
            bar.setBottom(viewHeight);
            bar.computeMidDot();
            if (i == size - 1) {
                MAX_SCROLL_X = bar.getRight() - viewWidth;
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getType() == TYPE_BAR) {
            drawBar(canvas);
        } else if (getType() == TYPE_LINE) {
            drawLine(canvas);
            drawDialog(canvas);
        }
    }

    private void drawDialog(Canvas canvas) {
        canvas.drawCircle(dialogX, dialogY, 20, tipPaint);
    }

    /**
     * 绘制折线图
     *
     * @param canvas
     */
    private void drawLine(Canvas canvas) {
        int size = bars.size();
        for (int i = 0; i < size; i++) {
            Bar bar = bars.get(i);
            canvas.drawCircle(bar.getMx(), bar.getMy(), dp2px(mContext, 4), dotPaint);
            if (i != size - 1) {
                mPath.reset();
                Bar nextBar = bars.get(i + 1);
                boolean isUp = Integer.parseInt(nextBar.getNumber()) > Integer.parseInt(bar.getNumber());
                mPath.moveTo(bar.getMx(), bar.getMy());
                int midX = (bar.getMx() + nextBar.getMx()) / 2;
                int midY = (bar.getMy() + nextBar.getMy()) / 2;

                int p1x = (bar.getMx() + midX) / 2;
                int p1y = (bar.getMy() + midY) / 2 + (isUp ? 50 : -50);

                int p2x = (midX + nextBar.getMx()) / 2;
                int p2y = (midY + nextBar.getMy()) / 2 + (isUp ? -50 : 50);

                mPath.cubicTo(p1x, p1y, p2x, p2y, nextBar.getMx(), nextBar.getMy());
                canvas.drawPath(mPath, linePaint);
            }
        }

    }

    /**
     * 绘制柱状图
     *
     * @param canvas
     */
    private void drawBar(Canvas canvas) {
        for (Bar bar : bars) {
            canvas.drawRect(new Rect(bar.getLeft(), bar.getTop(), bar.getRight(), bar.getBottom()), barPaint);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        viewWidth = MeasureSpec.getSize(widthMeasureSpec);
        viewHeight = topHeight + lineHeight * 2;
        setMeasuredDimension(viewWidth, viewHeight);

        compute();
        computeXY();
    }

    public int dp2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public void setType(int type) {
        this.type = type;
        invalidate();
    }

    public int getType() {
        return type;
    }
}
