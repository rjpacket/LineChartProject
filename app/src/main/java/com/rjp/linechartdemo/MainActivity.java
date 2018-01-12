package com.rjp.linechartdemo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import static com.rjp.linechartdemo.LineChartView.TYPE_BAR;
import static com.rjp.linechartdemo.LineChartView.TYPE_LINE;

public class MainActivity extends Activity {

    private LineChartView lineChartView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        lineChartView = (LineChartView) findViewById(R.id.line_chart);
    }

    public void switchType(View v) {
        lineChartView.setType(lineChartView.getType() == TYPE_BAR ? TYPE_LINE : TYPE_BAR);
    }
}
