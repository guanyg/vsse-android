package vsse.vsse_and;

import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import vsse.proto.RequestOuterClass.SearchRequest.MsgCase;

public class SearchActivity extends AppCompatActivity {

    private Connection conn;
    private MsgCase searchType;
    private SearchTask searchTask;
    private MyListAdapter resultAdapter;
    private ExpandableListView resultLst;
    private EditText keywordsField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Spinner spinner = findViewById(R.id.searchType);
        this.resultLst = findViewById(R.id.resultLst);
        this.resultAdapter = new MyListAdapter();
        resultLst.setAdapter(resultAdapter);

        String[] typeStrArr = new String[]{"AND", "OR", "*", "?"};
        MsgCase[] typeArr = new MsgCase[]{MsgCase.AND, MsgCase.OR, MsgCase.STAR, MsgCase.Q};
        spinner.setAdapter(
                new ArrayAdapter<>(
                        this, android.R.layout.simple_spinner_item,
                        typeStrArr));

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int pos, long id) {

                searchType = typeArr[pos];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Another interface callback
            }
        });

        this.keywordsField = findViewById(R.id.keywords);

        keywordsField.setOnEditorActionListener((view, actionId, event) -> {
            if(actionId == EditorInfo.IME_ACTION_SEARCH) {
                // this.searchBtn.callOnClick();
                keywordsField.setEnabled(false);
                searchTask = new SearchTask();
                searchTask.execute(keywordsField.getText().toString().toLowerCase().split("[, *?]+"));
            }
            return true;
        });

        Uri uri = getIntent().getData();
        this.conn = new Connection(uri);
        try {
            conn.open();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    private class SearchTask extends AsyncTask<String, Void, List<String>> {

        @Override
        protected List<String> doInBackground(String... strings) {
            try {
                return conn.search(searchType, strings);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
            return Collections.emptyList();
        }

        @Override
        protected void onPostExecute(List<String> strings) {
            resultAdapter.content.clear();
            resultAdapter.content.addAll(strings);
            resultAdapter.notifyDataSetChanged();
            keywordsField.setEnabled(true);
            Log.i("Search", strings.stream().collect(Collectors.joining()));
        }
    }

    private class MyListAdapter extends BaseExpandableListAdapter {
        private List<String> content = new ArrayList<>();

        @Override
        public int getGroupCount() {
            return content.size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return 1;
        }

        @Override
        public String getGroup(int groupPosition) {
            return content.get(groupPosition);
        }

        @Override
        public String getChild(int groupPosition, int childPosition) {
            return content.get(groupPosition);
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return 0;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            TextView textView = getTextView();
            textView.setPadding(100,5,5,5);
            String content = getGroup(groupPosition).split("[\r\n]")[0];
            if (content.length() > 30) {
                content = content.substring(0, 30) + "...";
            }

            textView.setText(content);
            return textView;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            TextView textView = getTextView();
            textView.setPadding(20,20,5,20);
            textView.setBackgroundColor(Color.GRAY);
            textView.setText(getChild(groupPosition, childPosition));
            return textView;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return false;
        }

        private TextView getTextView() {
            AbsListView.LayoutParams lp = new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            TextView textView = new TextView(resultLst.getContext());
            textView.setLayoutParams(lp);
            textView.setPadding(36, 0, 0, 0);
            textView.setTextSize(20);
            return textView;
        }
    }
}
