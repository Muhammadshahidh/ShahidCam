package com.shahid.cam.forensics.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;

import com.shahid.cam.forensics.data.local.entity.IntegrityLinkLogEntity;

@Dao
public interface IntegrityLinkLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(IntegrityLinkLogEntity entity);
}
