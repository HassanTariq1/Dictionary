package com.itpvt.dictionnaary.persistence;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import com.itpvt.dictionnaary.models.Word;

import java.util.List;

@Dao
public interface WordDao {

    @Query("SELECT * FROM Word")
    List<Word> getAllWords();

    @Insert
    long[] insertWords(Word... words);

    @Delete
    int delete(Word note);

    @Query("UPDATE Word SET title = :title, content = :content, timestamp = :timestamp WHERE uid = :uid")
    int updateWord(String title, String content, String timestamp, int uid);

    @Query("SELECT * FROM Word LIMIT :row, 2 ")
    public List<Word> getSomeWords(int row);

    @Query("SELECT COUNT(*) FROM Word")
    public Integer getNumRows();
}
