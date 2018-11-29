package com.itpvt.dictionnaary;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


import com.itpvt.dictionnaary.adapters.WordsRecyclerAdapter;
import com.itpvt.dictionnaary.models.Word;
import com.itpvt.dictionnaary.threading.MyThread;
import com.itpvt.dictionnaary.util.Constants;
import com.itpvt.dictionnaary.util.VerticalSpacingItemDecorator;

import java.util.ArrayList;

public class DictionaryActivity extends AppCompatActivity implements
        WordsRecyclerAdapter.OnWordListener,
        Handler.Callback,
        View.OnClickListener,
        SwipeRefreshLayout.OnRefreshListener
{

    private static final String TAG = "NotesListActivity";

    //ui components
    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout mSwipeRefresh;

    //vars
    private ArrayList<Word> mWords = new ArrayList<>();
    private WordsRecyclerAdapter mWordRecyclerAdapter;
    private FloatingActionButton mFab;
    private Handler mMainThreadHandler = null;
    private MyThread mBackgroundThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dictionary);
        mRecyclerView = findViewById(R.id.recyclerView);
        mFab = findViewById(R.id.fab);
        mSwipeRefresh = findViewById(R.id.swipe_refresh);

        mFab.setOnClickListener(this);
        mSwipeRefresh.setOnRefreshListener(this);
        mMainThreadHandler = new Handler(this);

        setupRecyclerView();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(mBackgroundThread == null){
            mBackgroundThread = new MyThread(this, mMainThreadHandler);
            mBackgroundThread.start();
        }
        if(mWords.size() == 0){
            retrieveWords();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mBackgroundThread != null){
            mBackgroundThread.quitThread();
        }
    }


    private void retrieveWords() {
        Log.d(TAG, "retrieveWords: called.");
        Message message = Message.obtain(null, Constants.WORDS_RETRIEVE);
        mBackgroundThread.sendMessageToBackgroundThread(message);
    }


    public void deleteWord(Word word) {
        Log.d(TAG, "deleteWord: called.");
        mWords.remove(word);
        mWordRecyclerAdapter.getFilteredWords().remove(word);
        mWordRecyclerAdapter.notifyDataSetChanged();

        Message message = Message.obtain(null, Constants.WORD_DELETE);
        Bundle bundle = new Bundle();
        bundle.putParcelable("word_delete", word);
        message.setData(bundle);
        mBackgroundThread.sendMessageToBackgroundThread(message);
    }

    private void setupRecyclerView(){
        Log.d(TAG, "setupRecyclerView: called.");
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        VerticalSpacingItemDecorator itemDecorator = new VerticalSpacingItemDecorator(10);
        mRecyclerView.addItemDecoration(itemDecorator);
        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(mRecyclerView);
        mWordRecyclerAdapter = new WordsRecyclerAdapter(mWords, this);
        mRecyclerView.setAdapter(mWordRecyclerAdapter);
    }

    @Override
    public void onWordClick(int position) {
        Intent intent = new Intent(this, EditWordActivity.class);
        intent.putExtra("selected_word", mWords.get(position));
        startActivity(intent);
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()){

            case R.id.fab:{
                Intent intent = new Intent(this, EditWordActivity.class);
                startActivity(intent);
                break;
            }

        }
    }


    ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            deleteWord(mWords.get(mWords.indexOf(mWordRecyclerAdapter.getFilteredWords().get(viewHolder.getAdapterPosition()))));
        }
    };

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what){

            case Constants.WORDS_RETRIEVE_SUCCESS:{
                Log.d(TAG, "handleMessage: successfully retrieved notes. This is from thread: " + Thread.currentThread().getName());

                if(mWords != null){
                    if(mWords.size() > 0){
                        mWords.clear();
                    }
                }

                ArrayList<Word> words = new ArrayList<>(msg.getData().<Word>getParcelableArrayList("words_retrieve"));
                mWords.addAll(words);
                mWordRecyclerAdapter.notifyDataSetChanged();
                break;
            }

            case Constants.WORDS_RETRIEVE_FAIL:{
                Log.d(TAG, "handleMessage: unable to retrieve words. This is from thread: " + Thread.currentThread().getName());

                break;
            }

            case Constants.WORD_INSERT_SUCCESS:{
                Log.d(TAG, "handleMessage: successfully inserted new word. This is from thread: " + Thread.currentThread().getName());

                break;
            }

            case Constants.WORD_INSERT_FAIL:{
                Log.d(TAG, "handleMessage: unable to insert new word. This is from thread: " + Thread.currentThread().getName());

                break;
            }

            case Constants.WORD_DELETE_SUCCESS:{
                Log.d(TAG, "handleMessage: successfully deleted a word. This is from thread: " + Thread.currentThread().getName());

                break;
            }

            case Constants.WORD_DELETE_FAIL:{
                Log.d(TAG, "handleMessage: unable to delete word. This is from thread: " + Thread.currentThread().getName());

                break;
            }

        }
        return true;
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.dictionary_activity_actions, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView =
                (SearchView) searchItem.getActionView();

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setMaxWidth(Integer.MAX_VALUE);

        // listening to search query text change
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // filter recycler view when query submitted
                mWordRecyclerAdapter.getFilter().filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                // filter recycler view when text is changed
                mWordRecyclerAdapter.getFilter().filter(query);
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public void onRefresh() {
        retrieveWords();
        mSwipeRefresh.setRefreshing(false);
    }
}


