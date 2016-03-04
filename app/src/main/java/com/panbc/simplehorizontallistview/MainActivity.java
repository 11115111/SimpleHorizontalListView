package com.panbc.simplehorizontallistview;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private static final int[] mColors = new int[]{
            Color.YELLOW, Color.BLUE, Color.CYAN, Color.DKGRAY, Color.GRAY, Color.GREEN, Color.RED, Color.WHITE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SimpleHorizontalListView horizontalListView = (SimpleHorizontalListView) findViewById(R.id.horiList);
        horizontalListView.setAdapter(new BaseAdapter() {
                                          @Override
                                          public int getCount() {
                                              return mColors.length;
                                          }

                                          @Override
                                          public Object getItem(int position) {
                                              return null;
                                          }

                                          @Override
                                          public long getItemId(int position) {
                                              return 0;
                                          }

                                          @Override
                                          public View getView(final int position, View convertView, ViewGroup parent) {
                                              if (convertView == null) {
                                                  convertView = new TextView(MainActivity.this);
                                              }
                                              TextView textView = (TextView) convertView;
                                              textView.setGravity(Gravity.CENTER);
                                              textView.setTextSize(20);
                                              textView.setPadding(10, 0, 10, 0);
                                              textView.setText("position " + position);
                                              convertView.setBackgroundColor(mColors[position]);
                                              return convertView;
                                          }
                                      }

        );

    }
}
