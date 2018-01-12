package com.rjp.linechartdemo;

/**
 * @author Gimpo create on 2018/1/12 12:26
 * @email : jimbo922@163.com
 */

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

    public int getLeft() {
        return left;
    }

    public void setLeft(int left) {
        this.left = left;
    }

    public int getTop() {
        return top;
    }

    public void setTop(int top) {
        this.top = top;
    }

    public int getRight() {
        return right;
    }

    public void setRight(int right) {
        this.right = right;
    }

    public int getBottom() {
        return bottom;
    }

    public void setBottom(int bottom) {
        this.bottom = bottom;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * 计算bar的中心点
     */
    public void computeMidDot() {
        setMx((right + left) / 2);
        setMy(top);
    }

    public int getMx() {
        return mx;
    }

    public void setMx(int mx) {
        this.mx = mx;
    }

    public int getMy() {
        return my;
    }

    public void setMy(int my) {
        this.my = my;
    }
}
