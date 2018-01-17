### 一、效果展示

![line_chart.gif](http://upload-images.jianshu.io/upload_images/5994029-039d4f6d80983cc2.gif?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)


### 二、分析
我们可以下载一个墨迹天气app，然后首页下拉到天气走势图这里，可以体验一下他们的折线图，非常的顺滑，那么怎么实现的呢？不用说，肯定是自定义View。
自定义View的核心是什么？那就是计算每一个要绘制的图案的位置。自定义View从什么地方入手？对象。
在我们这个Demo里面，View首先可以看作是一个对象，然后我们可以列出这个对象的属性：
1. backColor;      //背景色
2. barColor;       //柱状图色
3. lineColor;      //线色
4. dotColor;       //点色
5. topHeight;      //顶部偏移
6. lineHeight;     //行高
7. lineWidth;      //柱状图宽
8. lineSpace;      //柱状图之间的缝隙
9. viewWidth;      //view宽
10. viewHeight;     //view高
11. MAX_SCROLL_X;   //最大偏移

当然最开始肯定分析不出这么多属性，慢慢摸索。绘制的每一个柱形也是一个对象，这个对象比较重要，需要保存绘制的所有信息，我们先看这个Bar怎么定义的：
```
public class Bar {
    private int left;
    private int top;
    private int right;
    private int bottom;

    //中心点的位置
    private int mx;
    private int my;

    private String number;
    private String title;
}

```
柱形的位置信息，柱顶中心点的坐标，可以通过位置信息计算得到，然后是number，这个数字是代表柱形的高度，是相对左边刻度的高度，然后title是底部的描述文字，这里我并没有用到，也可以计算之后绘制在图表底部。

### 三、绘制
我们首先绘制柱形，折线图暂时不去管，看代码：
```
String data = "[{\"title\": \"今天\", \"number\": \"42\"}, {\"title\": \"06:00\", \"number\": \"24\"}, {\"title\": \"12:00\", \"number\": \"8\"}, {\"title\": \"18:00\", \"number\": \"42\"}, {\"title\": \"明天\", \"number\": \"12\"}, {\"title\": \"06:00\", \"number\": \"36\"}, {\"title\": \"12:00\", \"number\": \"4\"}, {\"title\": \"18:00\", \"number\": \"16\"}, {\"title\": \"后天\", \"number\": \"50\"}, {\"title\": \"06:00\", \"number\": \"24\"}, {\"title\": \"12:00\", \"number\": \"42\"}, {\"title\": \"18:00\", \"number\": \"25\"}]";

bars = JSONArray.parseArray(data, Bar.class);

```
自定义一个json串，得到柱形的model集合，然后计算每一个柱形的位置：
```
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

```
上下左右的计算方法都很好理解。这个计算我们在什么时候计算呢？最好在 onMeasure() 之后调用：
```
@Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        viewWidth = MeasureSpec.getSize(widthMeasureSpec);
        viewHeight = topHeight + lineHeight * 2;
        setMeasuredDimension(viewWidth, viewHeight);
        
        compute();
        computeY();
    }

```
确定完View的宽高以及每一个柱形图的位置，就是绘制了：
```
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

```
永远记住一条，onDraw() 里面只负责绘制，不要做多余的操作，什么计算了啊，new 一个对象啊，这些都会严重拖慢你的View的顺滑度，内存吃紧，体验特别差。

然后我们还想要View能支持滑动，复写 onTouchEvent() ，这里就比较简单了，很常见：
```
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
                    computeY();
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

```
这里面我们加入了惯性滑动，如果不清楚流程，可以参考上一篇文章。

到这里我们就已经完成了可滑动的柱形图，但是离墨迹天气的折线图还差的很远，不着急，我们希望可以自由切换柱形图和折线图。那么我们需要设置一个类型标记：
```
    //两种类型 折线 柱状
    public static final int TYPE_LINE = 1;
    public static final int TYPE_BAR = 2;
    private int type = TYPE_BAR;

```
通过这个我们去绘制不同的图形，很简单的思路，那么如何绘制折线图呢？很简单，我们之前计算的柱形图的中心点就用上了，只要连接这些中心点就可以了，但是我们能直线连接吗？可以，但是很丑。观察墨迹天气不难看出他们使用的是三阶贝塞尔曲线去柔和的连接这些点：
```
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

```
我们在 onDraw() 里面判断一下，方便我们切换视图。
```
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
```
贝塞尔曲线就完成了。完美了吗？没有。墨迹天气还有一个很神奇的跟着手指在贝塞尔线上滑动的类似dialog的指示器，怎么实现的？我实在是想不到思路。然后百度了一个Demo，了解了思路。指示器的坐标 (x,y) 是计算出来的，x 很好计算，按照比例得出公式：
```
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
```
我这里使用一个简单的蓝点代表，可以是绘制任何的图形：
```
    private void drawDialog(Canvas canvas) {
        canvas.drawCircle(dialogX, dialogY, 20, tipPaint);
    }
```
到这里，整个demo就完成了，边界判断我也没有加。糙了一点，思路倒是没有错。

附上 [简书地址](https://www.jianshu.com/p/4aef57eec972)