/*
 * Copyright (C) 2009 University of Washington
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.odk.collect.android.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;

import org.odk.collect.android.utilities.STFileUtils;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Manages the files the application uses.
 *
 * @author Neil Penman
 */
public class FileDbAdapter {

    // file types
    public static final String TYPE_FORM = "form";

    // database columns
    public static final String TYPE_INSTANCE = "instance";
    // status for instances
    public static final String STATUS_INCOMPLETE = "incomplete";
    public static final String STATUS_COMPLETE = "complete";
    public static final String STATUS_SUBMITTED = "submitted";
    // status for forms
    public static final String STATUS_AVAILABLE = "available";
    // status for tasks
    public static final String STATUS_T_NEW = "new";
    public static final String STATUS_T_PENDING = "pending";
    public static final String STATUS_T_ACCEPTED = "accepted";
    //public static final String STATUS_T_OPEN = "open";
    public static final String STATUS_T_REJECTED = "rejected";
    public static final String STATUS_T_DONE = "done";
    public static final String STATUS_T_SUBMITTED = "submitted";
    public static final String STATUS_T_MISSED = "missed";
    public static final String STATUS_T_CANCELLED = "cancelled";
    public static final String STATUS_T_DELETED = "deleted";
    public static final String STATUS_SYNC_YES = "synchronized";
    public static final String STATUS_SYNC_NO = "not synchronized";
    // database columns
    public static final String KEY_T_ID = "_id";
    public static final String KEY_T_TITLE = "name";
    public static final String KEY_T_SOURCE = "source";
    public static final String KEY_T_SOURCETASKID = "sourceTaskId";
    public static final String KEY_T_ASSIGNMENTID = "assignmentId";
    public static final String KEY_T_SCHEDULED_START = "scheduledStart";
    // public static final String KEY_T_START_TEXT = "startAsText";
    public static final String KEY_T_TASKFORM = "taskForm";
    public static final String KEY_T_INSTANCE = "instance";
    public static final String KEY_T_STATUS = "taskStatus";
    public static final String KEY_T_LAT = "lat";
    public static final String KEY_T_LON = "lon";
    public static final String KEY_T_ADDRESS = "address";
    public static final String KEY_T_LOCATION = "location";
    public static final String KEY_T_GEOM_TYPE = "geomtype";
    public static final String KEY_T_LOCNFORM = "locnForm";
    public static final String KEY_T_ASSIGNMENT_MODE = "assignmentmode";
    public static final String KEY_T_PRIORITY = "priority";
    public static final String KEY_T_REPEATS = "repeat";
    public static final String KEY_T_FROM_DATE = "fromdate";
    public static final String KEY_T_DUE_DATE = "duedate";
    public static final String KEY_T_CREATE_DATE = "createdate";
    public static final String KEY_T_CREATOR = "creator";
    public static final String KEY_T_NEED_GPS = "needgps";
    public static final String KEY_T_NEED_RFID = "needrfid";
    public static final String KEY_T_NEED_BARCODE = "needbarcode";
    public static final String KEY_T_NEED_CAM = "needcam";
    public static final String KEY_T_ADHOC_LAT = "alat";
    public static final String KEY_T_ADHOC_LON = "alon";
    public static final String KEY_T_IS_SYNC = "issync";
    private final static String t = "FileDbAdapter";
    private static final String DATABASE_NAME = "data";
    private static final int DATABASE_VERSION = 6;
    private static final String DATABASE_PATH = Environment.getExternalStorageDirectory()
            + "/fieldTask/metadata";
    private static final String TASKS_TABLE = "tasks";
    private static final String TASKS_CREATE =
            "create table tasks (_id integer primary key autoincrement, " +
                    "name text not null, source text not null, sourceTaskId text, formId text, assignmentId text, startAsText text, scheduledStart long, " +
                    "taskForm text, instance text, taskStatus text not null, lat text, lon text, address text, " +
                    "location text, geomtype text, locnForm text, assignmentmode text, priority text, repeat text, fromdate text, duedate text, " +
                    "needgps text, needrfid text, needbarcode text, needcam text, createdate text, creator text, alon text, alat text, issync text);";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;

    public FileDbAdapter() {
    }

    public FileDbAdapter open() throws SQLException {
        mDbHelper = new DatabaseHelper();
        mDb = mDbHelper.getWritableDatabase();

        return this;
    }

    public void close() {
        mDbHelper.close();
        mDb.close();
    }

    public void beginTransaction() {
        mDb.beginTransaction();
    }

    public void setTransactionSuccessful() {
        mDb.setTransactionSuccessful();
    }

    public void endTransaction() {
        mDb.endTransaction();
    }

    /**
     * Insert or update task in the database.
     *
     * @param tid if equal to -1 the task will be created, else updated
     * @return id of the new file
     */
    public long createTask(long tid, String source, TaskAssignment ta,
                           String formPath, String instancePath) throws Exception {

        //SimpleDateFormat dFormat = new SimpleDateFormat("EEE d MMM yy");
        DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm");
        ContentValues cv = new ContentValues();
        if (ta.task.title == null) {
            ta.task.title = "local: " + STFileUtils.getName(formPath);
        }
        cv.put(KEY_T_SOURCETASKID, ta.task.id);
        cv.put(KEY_T_TITLE, ta.task.title);
        cv.put(KEY_T_SOURCE, source);

        cv.put(KEY_T_ASSIGNMENTID, ta.assignment.assignment_id);
        cv.put(KEY_T_STATUS, ta.assignment.assignment_status);

        if (ta.task.scheduled_at != null) {
            cv.put(KEY_T_SCHEDULED_START, ta.task.scheduled_at.getTime());
        }
        cv.put(KEY_T_TASKFORM, formPath);
        cv.put(KEY_T_INSTANCE, instancePath);
        cv.put(KEY_T_ADDRESS, ta.task.address);
        if (ta.location != null && ta.location.geometry != null && ta.location.geometry.coordinates != null && ta.location.geometry.coordinates.length >= 1) {
            // Set the location of the task to the first coordinate pair
            String firstCoord = ta.location.geometry.coordinates[0];
            String[] fc = firstCoord.split(" ");
            if (fc.length > 1) {
                cv.put(KEY_T_LON, fc[0]);
                cv.put(KEY_T_LAT, fc[1]);
            }
            StringBuilder builder = new StringBuilder();
            for (String coord : ta.location.geometry.coordinates) {
                builder.append(coord);
                builder.append(",");
            }
            cv.put(KEY_T_LOCATION, builder.toString());    // Save the original location string
            cv.put(KEY_T_GEOM_TYPE, ta.location.geometry.type);
        }


        cv.put(KEY_T_ASSIGNMENT_MODE, ta.task.assignment_mode);
        cv.put(KEY_T_PRIORITY, ta.task.priority);
        cv.put(KEY_T_REPEATS, ta.task.repeats);
        cv.put(KEY_T_NEED_GPS, ta.task.gps ? "yes" : "no");
        cv.put(KEY_T_NEED_CAM, ta.task.camera ? "yes" : "no");
        cv.put(KEY_T_NEED_BARCODE, ta.task.barcode ? "yes" : "no");
        cv.put(KEY_T_NEED_RFID, ta.task.rfid ? "yes" : "no");
        if (ta.task.from_date != null) {
            cv.put(KEY_T_FROM_DATE, ta.task.from_date.getTime());
        }
        if (ta.task.due_date != null) {
            cv.put(KEY_T_DUE_DATE, ta.task.due_date.getTime());
        }
        if (ta.task.created_date != null) {
            cv.put(KEY_T_CREATE_DATE, ta.task.created_date.getTime());
        }
        cv.put(KEY_T_CREATOR, ta.task.created_by);
        cv.put(KEY_T_IS_SYNC, STATUS_SYNC_YES);

        long new_id = -1;
        if (tid == -1) {
            new_id = mDb.insert(TASKS_TABLE, null, cv);
            if (new_id == -1) {
                throw new Exception("SQL Error - Inserting record");
            }
        } else {
            new_id = tid;
            mDb.update(TASKS_TABLE, cv, KEY_T_ID + "=" + tid, null);
        }

        return new_id;
    }

    /*
     * Return all the tasks in the database order by scheduled start
     */
    public Cursor fetchAllTasks() throws Exception {

        Cursor c = null;
        c = mDb.query(false, TASKS_TABLE, new String[]{
                KEY_T_ID, KEY_T_TITLE, KEY_T_SCHEDULED_START, KEY_T_SOURCE, KEY_T_SOURCETASKID, KEY_T_ASSIGNMENTID,
                KEY_T_IS_SYNC,
                KEY_T_TASKFORM, KEY_T_INSTANCE, KEY_T_STATUS, KEY_T_LAT, KEY_T_LON, KEY_T_ADDRESS, KEY_T_ADHOC_LON, KEY_T_ADHOC_LAT
        }, null, null, null, null, KEY_T_SCHEDULED_START + " DESC", null);

        if (c != null) {
            c.moveToFirst();
        }
        return c;
    }

    /*
     * Return tasks for the specified source
     */
    public Cursor fetchTasksForSource(String source, boolean includeLocal) throws Exception {

        String selectClause = null;
        if (includeLocal) {
            selectClause = KEY_T_SOURCE + "='" + source + "' or " + KEY_T_SOURCE + "='local'";
        } else {
            selectClause = KEY_T_SOURCE + "='" + source + "'";
        }
        Cursor c = null;
        c = mDb.query(true, TASKS_TABLE, new String[]{
                        KEY_T_ID, KEY_T_TITLE,
                        KEY_T_SCHEDULED_START,
                        KEY_T_SOURCE,
                        KEY_T_SOURCETASKID,
                        KEY_T_ASSIGNMENTID,
                        KEY_T_IS_SYNC,
                        KEY_T_TASKFORM,
                        KEY_T_INSTANCE,
                        KEY_T_STATUS,
                        KEY_T_LAT,
                        KEY_T_LON,
                        KEY_T_ADDRESS,
                        KEY_T_ADHOC_LON,
                        KEY_T_ADHOC_LAT,
                        KEY_T_LOCATION,
                        KEY_T_GEOM_TYPE
                },
                selectClause,                            // Where
                null,
                null,
                null,
                KEY_T_SCHEDULED_START + " DESC",        // Order by
                null
        );

        if (c != null) {
            c.moveToFirst();
        }
        return c;
    }

    /*
     * Return data for the specified task id
     */
    public Cursor fetchTaskForId(long taskId) throws Exception {

        Cursor c = null;
        c = mDb.query(true, TASKS_TABLE, new String[]{KEY_T_ID, KEY_T_SOURCETASKID, KEY_T_TITLE, KEY_T_STATUS,
                        KEY_T_SOURCE, KEY_T_ASSIGNMENTID, KEY_T_LAT, KEY_T_LON, KEY_T_ADDRESS, KEY_T_SCHEDULED_START,
                        KEY_T_TASKFORM, KEY_T_INSTANCE, KEY_T_LOCATION, KEY_T_LOCNFORM, KEY_T_ASSIGNMENT_MODE,
                        KEY_T_PRIORITY, KEY_T_REPEATS, KEY_T_FROM_DATE, KEY_T_DUE_DATE, KEY_T_CREATE_DATE,
                        KEY_T_CREATOR, KEY_T_NEED_GPS, KEY_T_NEED_RFID, KEY_T_NEED_BARCODE, KEY_T_NEED_CAM,
                        KEY_T_ADHOC_LON, KEY_T_ADHOC_LAT, KEY_T_IS_SYNC},
                KEY_T_ID + "=" + taskId, null,
                null, null, null, null
        );

        if (c != null) {
            c.moveToFirst();
        }
        return c;
    }

    /*
     * Return true if the task exists for the specified task id
     */
    public boolean hasTaskWithId(long taskId) throws Exception {

        Cursor c = null;
        boolean hasTask = false;
        c = mDb.query(true, TASKS_TABLE, new String[]{KEY_T_ID, KEY_T_TITLE, KEY_T_STATUS, KEY_T_SOURCE,
                        KEY_T_SCHEDULED_START, KEY_T_TASKFORM, KEY_T_INSTANCE, KEY_T_LOCATION, KEY_T_LOCNFORM},
                KEY_T_ID + "=" + taskId, null,
                null, null, null, null
        );

        if (c != null) {
            hasTask = c.moveToFirst();
        }
        return hasTask;
    }

    /*
     * Update the location of the task after editing the survey
     */
    public int updateAdhocLocation(long taskId, String lon, String lat) throws Exception {

        int count = 0;
        if (taskId >= 0) {
            ContentValues cv = new ContentValues();
            cv.put(KEY_T_ADHOC_LON, lon);
            cv.put(KEY_T_ADHOC_LAT, lat);

            count = mDb.update(TASKS_TABLE, cv, KEY_T_ID + "=" + taskId, null);
        }

        return count;
    }

    /*
     * Update the task after editing the survey
     */
    public int updateTask(TaskAssignment ta) throws Exception {

        ContentValues cv = new ContentValues();
        cv.put(KEY_T_TITLE, ta.task.title);
        cv.put(KEY_T_IS_SYNC, STATUS_SYNC_NO);

        cv.put(KEY_T_SOURCETASKID, ta.assignment.assignment_id);
        cv.put(KEY_T_STATUS, ta.assignment.assignment_status);
        cv.put(KEY_T_IS_SYNC, STATUS_SYNC_NO);   // np

        if (ta.task.scheduled_at != null) {
            cv.put(KEY_T_SCHEDULED_START, ta.task.scheduled_at.getTime());
        }

        if (ta.location != null) {
            if (ta.location.geometry != null && ta.location.geometry.coordinates != null && ta.location.geometry.coordinates.length >= 2) {
                // Set the location of the task to the first coordinate pair
                String firstCoord = ta.location.geometry.coordinates[0];
                String[] fc = firstCoord.split(" ");
                if (fc.length > 1) {
                    cv.put(KEY_T_LON, fc[0]);
                    cv.put(KEY_T_LAT, fc[1]);
                }
                cv.put(KEY_T_LOCATION, ta.location.geometry.coordinates.toString());    // Save the original location string
                cv.put(KEY_T_GEOM_TYPE, ta.location.geometry.type);
            }
        }

        return mDb.update(TASKS_TABLE, cv, KEY_T_ID + "=" + ta.task.id, null);
    }

    /*
     * Update the task details if there is any update
     */
    public int updateTask(long taskId, String instancePath, boolean completed) throws Exception {

        int count = 0;
        String status = completed ? "done" : "accepted";
        if (taskId >= 0) {
            ContentValues cv = new ContentValues();
            cv.put(KEY_T_INSTANCE, instancePath);
            cv.put(KEY_T_STATUS, status);
            cv.put(KEY_T_IS_SYNC, STATUS_SYNC_NO);    // np

            count = mDb.update(TASKS_TABLE, cv, KEY_T_ID + "=" + taskId, null);
        }

        return count;
    }

    public int updateTaskStatus(long taskId, String newStatus) throws Exception {

        int count = 0;
        if (taskId >= 0) {
            ContentValues cv = new ContentValues();
            cv.put(KEY_T_STATUS, newStatus);
            cv.put(KEY_T_IS_SYNC, STATUS_SYNC_NO);

            mDb.update(TASKS_TABLE, cv, KEY_T_ID + "=" + taskId, null);
        }

        return count;
    }

    /*
     * Update task status for the task that has the passed in assignment id and source
     */
    public int updateTaskStatusForAssignment(long assId, String newStatus, String source) throws Exception {

        int count = 0;
        if (assId >= 0) {
            ContentValues cv = new ContentValues();
            cv.put(KEY_T_STATUS, newStatus);
            cv.put(KEY_T_IS_SYNC, STATUS_SYNC_NO);

            mDb.update(TASKS_TABLE, cv,
                    KEY_T_ASSIGNMENTID + "=" + assId + " and " + KEY_T_SOURCE + "='" + source + "'",
                    null);
        }

        return count;
    }

    /*
     * Update the status of a task when passed the unique reference to an instance in the odk instance provider
     */
    public int updateTaskStatus(String instanceFilePath, String newStatus) {

    	/*
         * The task database uniquely references an instance using the path to the instance
    	 */

        int count = 0;
        if (instanceFilePath != null) {
            ContentValues cv = new ContentValues();
            cv.put(KEY_T_STATUS, newStatus);
            cv.put(KEY_T_IS_SYNC, STATUS_SYNC_NO);

            mDb.update(TASKS_TABLE, cv, KEY_T_INSTANCE + "='" + instanceFilePath + "'", null);
        }

        return count;
    }

    /*
     * set the issync field to STATUS_SYNC_YES to indicate that the task data has been synchronized with server
     */
    public int setTaskSynchronized(long taskId) {
        int count = 0;
        if (taskId >= 0) {
            ContentValues cv = new ContentValues();
            cv.put(KEY_T_IS_SYNC, STATUS_SYNC_YES);

            mDb.update(TASKS_TABLE, cv, KEY_T_ID + "=" + taskId, null);
        }

        return count;
    }

    /*
     * Delete the tasks from the source that have the specified status
     */
    public int deleteTasksFromSource(String source, String status) throws Exception {
        return mDb.delete(TASKS_TABLE, KEY_T_SOURCE + "='" + source +
                "' AND " + KEY_T_STATUS + " = '" + status + "'", null);
    }

    public int deleteTask(long tid) throws Exception {
        return mDb.delete(TASKS_TABLE, KEY_T_ID + "=" + tid, null);
    }

    public int deleteNonOpenTasksFromSource(String source) throws Exception {
        return mDb.delete(TASKS_TABLE, KEY_T_SOURCE + "='" + source +
                "' AND " + KEY_T_STATUS + " != 'open'", null);
    }

    /*
     * Return true if the current task status allows it to be rejected
     */
    public boolean canReject(String currentStatus) {
        boolean valid = false;
        if (currentStatus.equals(STATUS_T_PENDING) ||
                currentStatus.equals(STATUS_T_NEW) ||
                currentStatus.equals(STATUS_T_ACCEPTED)) {
            valid = true;
        }
        return valid;
    }

    /*
     * Return true if the current task status allows it to be completed
     */
    public boolean canComplete(String currentStatus) {
        boolean valid = false;
        if (currentStatus.equals(STATUS_T_ACCEPTED)) {
            valid = true;
        }

        return valid;
    }

    /*
     * Return true if the current task status allows it to be completed
     */
    public boolean canAccept(String currentStatus) {
        boolean valid = false;
        if (currentStatus.equals(STATUS_T_PENDING) ||
                currentStatus.equals(STATUS_T_NEW) ||
                currentStatus.equals(STATUS_T_REJECTED)) {
            valid = true;
        }

        return valid;
    }

    private static class DatabaseHelper extends ODKSQLiteOpenHelper {

        DatabaseHelper() {
            super(DATABASE_PATH, DATABASE_NAME, null, DATABASE_VERSION);

            // Create database storage directory if it doesn't not already exist.
            try {
                File f = new File(DATABASE_PATH);
                f.mkdirs();
            } catch (Exception e) {
                // TODO add error message
            }
        }


        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(TASKS_CREATE);
        }


        @Override
        // upgrading will destroy all old data
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TASKS_TABLE);
            onCreate(db);
        }
    }

}
