/*
 * AuditTrailManager.java
 * This file is part of WifiCellManager.
 * Copyright (C) 2012 Carlos Prados <wifimatic.app@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cprados.wificellmanager.sys;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;

import org.cprados.wificellmanager.BuildConfig;
import org.cprados.wificellmanager.RequestedActionManager.RequestedAction;
import org.cprados.wificellmanager.StateMachine.State;
import org.cprados.wificellmanager.StateMachine.StateAction;

import android.content.Context;
import android.text.format.DateFormat;
import android.util.Log;

/** Manages activity log recording and reading */
public class AuditTrailManager {
           
    /** Tag for logging this class messages */
    private static final String LOGTAG = AuditTrailManager.class.getPackage().getName();
    
    /** Log file name prefix */
    private static final String FILE_NAME_PREFIX = AuditTrailManager.class.getName() + ".";
    
    /** Log file name extension */
    private static final String FILE_NAME_EXTENSION = ".log";
        
    /** Date format for file names */
    private static final String FILENAME_DATEFORMAT = "yyyyMMdd";
    
    /** Date format for file names */
    private static final String FILE_NAME_REGEXP = "\\.";
    
    /** Line separator */
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    
    /** Cache block size */
    private static final int CACHE_PAGE_SIZE = 32;
    
    /** Initial number of pages of the adapter to load on activity creation */
    private static final int INITIAL_NUM_PAGES = 1;
    
    /** Maximum number of dates in history */
    private static final int MAX_FILES = 3;
    
    /** Android context for where reading and writing is done */
    private Context mContext;
        
    /** Cache of activity records */
    private List<ActivityRecord> mCache;
    
    /** Last file loaded into cache */
    private String mLowestFile = null;

    /** Last line of mLowestFile loaded into cache */
    private int mLowestRecord = 0;
    
    /** Indicates if there are older records to load */
    private boolean mOlderRecords = false;
    
    /** Indicates if there are newer records to load */
    private boolean mNewerRecords = false;
    
    /** Latest write date */
    private Date mLatestWrite;
    
    /** The instance of this singleton class */
    private static AuditTrailManager sInstance;
    
    /** Returns existent instance of the class or creates a new one */
    public static AuditTrailManager getInstance (Context context) {
        if (sInstance == null) {
            sInstance = new AuditTrailManager(context);
        }        
        return sInstance;
    }
       
    /** Creates an audit trail manager */
    private AuditTrailManager (Context context) {
        this.mContext = context;
    }

    /** Loads next pages of activity records into cache, starting from oldest record of oldest file 
     * that has not been already loaded. Subsequent calls to this method load a page of older records into the cache.
     * Returns true if there are more records to read */        
    public boolean loadPages (int pages) {        
        return loadRecords (CACHE_PAGE_SIZE * ((pages > 0) ? pages : 0));        
    }
    
    /** Loads a number of records into cache starting from oldest record of oldest file that has not been already loaded.*/
    private boolean loadRecords (int records) {
        
        boolean moreRecords = false;
        boolean moreFiles = false;

        // Creates the cache if it is not created
        if (mCache == null) {
            mCache = new ArrayList<ActivityRecord>(CACHE_PAGE_SIZE);
        }
        
        // Calculates the target maximum size after loading required pages
        int targetCacheSize = mCache.size() + records;

        // Gets the file names to iterate, starting in lowest file with records remaining to be loaded 
        Iterator<String> filesIterator  = getLogFileNames(mLowestFile, (mLowestRecord != 0)).iterator();
           
        // Reads records until all required pages have been loaded or there are no more files
        while ((moreFiles = filesIterator.hasNext()) && mCache.size() < targetCacheSize ) {        

            // Open next file
            String fileName;
            BufferedReader reader = getReader(fileName = filesIterator.next());            
            if (reader != null) {
                                
                try {
                    // Read an parse all the records in the current file skipping unformatted lines 
                    ArrayList<ActivityRecord> fileBlock = new ArrayList<ActivityRecord>(CACHE_PAGE_SIZE);
                    String line;
                    do {
                        line = reader.readLine();
                        ActivityRecord record = ActivityRecord.parseActivityRecord(line);
                        if (record!=null) {    
                            fileBlock.add(record);
                        }                        
                    }
                    while (line != null);
                    
                    // Initializes record pointers to latest of this file +1
                    if (mLowestRecord == 0 ){
                        mLowestRecord = fileBlock.size();
                    }

                    // Adds date separator record
                    if (!fileName.equals(mLowestFile)) {
                        mCache.add(new ActivityRecord(getLogFileDate(fileName)));
                        mLowestFile = fileName;
                    }
                    
                    // Calculates the number of records that still have to be added to cache
                    int remSpace = targetCacheSize - mCache.size();

                    // Extracts the number of records to fill the remaining space
                    moreRecords = mLowestRecord > remSpace;
                    int start = moreRecords  ? mLowestRecord - remSpace : 0;
                    int end = mLowestRecord;
                    List<ActivityRecord> subList = fileBlock.subList(start,end);
                    
                    // Adds the records in reverse order at the end of the cache
                    Collections.reverse(subList);                                                                              
                    mCache.addAll(subList);
                    
                    // Update record pointer
                    mLowestRecord = start;
                }
                catch (IOException e) {
                    Log.e(LOGTAG, Log.getStackTraceString(e));
                }
                finally {
                    closeReader(reader);
                }
            }
        }
        return (mOlderRecords = (moreRecords || moreFiles));
    }
    
    /** Clears the activity records cache */
    public boolean releaseCache () {
        if (mCache != null) {
            mCache = null;
            mLowestFile = null;
            mLowestRecord = 0;
            mOlderRecords = false;
            mNewerRecords = false;
            return true;
        }
        return false;        
    }

    /** Reloads the cache from file. Tries to get the same number of records it has */
    public boolean refreshCache () {
        boolean result = false;
        if (mCache != null) {
            int numRecords = getNumRecords();            
            releaseCache();
            loadRecords(numRecords > CACHE_PAGE_SIZE * INITIAL_NUM_PAGES ? numRecords : CACHE_PAGE_SIZE * INITIAL_NUM_PAGES);
            result = true;
        }
        return result;
    }
    
    /** Writes an activity record at the end of todays log file. */
    public void writeRecord (ActivityRecord record) {        
        
        // Opens todays log file for writing
        Date date  = new Date();
        FileOutputStream outStream = getOutStream(date);
        
        if (outStream != null && record != null) {
            String line = record + LINE_SEPARATOR;
            try {
                outStream.write(line.getBytes());
                mNewerRecords = true;
                
                // Delete old files if this is the first record of the day
                if (isNewDay(date)) {
                    deleteOldFiles(date); 
                }
                
                // Updates latest write date
                mLatestWrite = date;
            }
            catch (Exception e) {
                Log.e(LOGTAG, Log.getStackTraceString(e));
            }
            finally {
                closeOutStream(outStream);
            }
        }
    }

    /** Returns an activity record from an specific position or null if not found */
    public ActivityRecord readRecord (int position) {              
        if (mCache == null ) {
            loadPages(INITIAL_NUM_PAGES);
        }
        return (position >= 0 && position < mCache.size()) ? mCache.get(position) : null;        
    }
    
    /** Checks if date is a day after of latest write date */
    private boolean isNewDay (Date date) {
        
        // Checks if last write was a day from a day before given date
        boolean dayBefore = false;
        if (mLatestWrite != null) {
            Calendar given = Calendar.getInstance();
            given.setTime(date);        
            Calendar last = Calendar.getInstance();
            last.setTime(mLatestWrite); // given date        
            dayBefore = (given.get(Calendar.YEAR) > last.get(Calendar.YEAR) || 
                    given.get(Calendar.DAY_OF_YEAR) > last.get(Calendar.DAY_OF_YEAR));
        }

        return (mLatestWrite == null || dayBefore);
    }

    /** Deletes files that are maximum number of days older than given date*/
    private boolean deleteOldFiles(Date date) {
        
        // Gets time of the most recent date to delete
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_YEAR, -1 * MAX_FILES);
        Date deleteDate = cal.getTime();
        
        // Gets the set of files to delete
        SortedSet<String> deleteFiles = getLogFileNames(getLogFileName(deleteDate),true);
        
        // Delete each file
        boolean deleted = true;
        for (String fileName : deleteFiles) {
            deleted &= deleteFile (fileName);            
        }
        return deleted;      
    }
    
    /** Delete a file */
    private boolean deleteFile(String fileName) {
        boolean deleted = false;
        if (fileName != null && fileName.startsWith(FILE_NAME_PREFIX) && fileName.endsWith(FILE_NAME_EXTENSION)) {
            try {
                File file = new File(mContext.getFilesDir(), fileName);
                deleted = file.delete();
                if (BuildConfig.DEBUG) {
                    Log.d(LOGTAG, "File deleted: " + fileName);
                }
            }
            catch (Exception e) {
                Log.e(LOGTAG, Log.getStackTraceString(e));
            }
        }
        return deleted;
    }
    
    /** Returns the number of records */ 
    public int getNumRecords () {
        if (mCache == null ) {
            loadPages(INITIAL_NUM_PAGES);
        }
        return mCache.size();
    }
    
    /** Returns if there are older records to be loaded */
    public boolean getOlderRecords () {
        return mOlderRecords;
    }

    /** Returns if there are older records to be loaded */
    public boolean getNewerRecords () {
        return mNewerRecords;
    }
    
    /** Returns day of older record loaded */
    public Date getOldestRecordDate () {
        Date date = null;
        if (mLowestFile != null) {
            date = getLogFileDate(mLowestFile);
        }
        return date;
    }
    
    /** Returns a reader to retrieve activity records from given log file */
    private BufferedReader getReader(String fileName) {
        BufferedReader result = null;

        // Validates file name
        if (fileName != null && fileName.startsWith(FILE_NAME_PREFIX) && fileName.endsWith(FILE_NAME_EXTENSION)) {

            try {
                FileInputStream inStream = mContext.openFileInput(fileName);
                if (inStream != null) {
                    result = new BufferedReader(new InputStreamReader(inStream));
                }
            }
            catch (FileNotFoundException e) {
                Log.e(LOGTAG, Log.getStackTraceString(e));
            }
        }
        return result;
    }
    
    /** Open file output stream */
    private FileOutputStream getOutStream(Date date) {
        FileOutputStream outStream = null;
        try {
            outStream = mContext.openFileOutput(getLogFileName(date), Context.MODE_APPEND);
        }
        catch (FileNotFoundException e) {
            Log.e(LOGTAG, Log.getStackTraceString(e));                
        }
        return outStream;
    }

    /** Returns log file name for a given date records */
    private String getLogFileName (Date date) {
                
        String fileNameDate = DateFormat.format(FILENAME_DATEFORMAT, date).toString();
        String fileName = FILE_NAME_PREFIX + fileNameDate + FILE_NAME_EXTENSION;
        return fileName;
    }
    
    /** Returns the set of all existent log file names in descending order by name. Returns an empty set if there aren't any files */
    private SortedSet<String> getLogFileNames(String lowerFile, boolean include) {
        
        // Get the list of files
        List<String> list = Arrays.asList(mContext.fileList());
                
        // Reverses the order and stores into a sorted set
        SortedSet<String> result = new TreeSet<String>(Collections.reverseOrder());                
        if (list != null) {
            result.addAll(list);
        }
        
        // Retrieves the subset after the given file name including or excluding given name
        if (lowerFile != null) {
            result = result.tailSet(lowerFile);        
            if (!include) {
                result.remove(lowerFile);
            }
        }       
        
        return result;
    }                   
    
    /** Returns the date of a log file from its name */
    private Date getLogFileDate(String fileName) {

        Date result = null;
        if (fileName != null) {
            String fields[] = fileName.split(FILE_NAME_REGEXP);
            if (fields != null && fields.length > 1) {
                String dateString = fields[fields.length-2];
                try {
                    SimpleDateFormat dateFormat = new SimpleDateFormat (FILENAME_DATEFORMAT, Locale.US);
                    result = dateFormat.parse(dateString);
                }
                catch (Exception e) {
                    Log.e(LOGTAG, Log.getStackTraceString(e));                    
                }
            }
        }
        
        return result;
    }
    
    /** Closes file reader */
    private void closeReader(BufferedReader reader) {
        try {
            if (reader != null) {
                reader.close();
            }
        }
        catch (Exception e) {
            Log.e(LOGTAG, Log.getStackTraceString(e));
        }
    }
        
    /** Closes file output stream */
    private void closeOutStream (FileOutputStream outStream) {
        try {
            if (outStream != null) {
                outStream.close();
            }
        }
        catch (Exception e) {
            Log.e(LOGTAG, Log.getStackTraceString(e));
        }        
    }
    
    /** Class that represents activity records managed by this class */
    public static class ActivityRecord {
        
        /** Field separator of activity records */
        private static final String FIELD_SEPARATOR = "|";
        
        /** Regular expression to split fields */
        private static final String FIELDS_REGEXP = "\\|";
        
        /** String that cannot exist on any activity record parameter */
        private static final String REPLACEMENT_STRING = "$";
        
        /** Null string */
        private static final String NULL_STRING = "";
        
        /** State recorded */
        public State state;

        /** State action recorded */
        public StateAction action;
        
        /** Requested action recorded */
        public RequestedAction requestedAction;
        
        /** Date of the record */
        public Date date;
        
        /** Payload to get the description of action, state and requestedAction */
        public Object[] payload;
        
        /** Creates an activity record */
        public ActivityRecord(State state, StateAction action, RequestedAction requestedAction, Date date, Object...payload) {
            this.state = state;
            this.action = action;
            this.requestedAction = requestedAction;
            this.date = date;            
            this.payload = payload;
        }
        
        /** Creates a date separator activity record */
        public ActivityRecord (Date date) {
            this.date = date;
        }
        
        /** Creates an empty activity record */
        private ActivityRecord () {            
        }
        
        @Override
        public String toString() {       
            
            // Fixed length part: fields 0 to 3.
            String result = (date != null ? date.getTime() : NULL_STRING) +  FIELD_SEPARATOR + 
                    (state != null ? state.name() : NULL_STRING ) + FIELD_SEPARATOR +
                    (action != null ? action.name() : NULL_STRING ) + FIELD_SEPARATOR +
                    (requestedAction != null ? requestedAction.name() : NULL_STRING ) + FIELD_SEPARATOR;
                   
            // Variable length part: fields 4 and following.
            if (payload != null) {
                for (int i=0; i < payload.length; i++) {                    
                    result += (payload[i] != null ? manageFieldSeparator(payload[i].toString(), true) : NULL_STRING) + FIELD_SEPARATOR;
                }
            }            
            return result;      
        }
        
        /** Creates an activity record from a text line created by toString() */
        public static ActivityRecord parseActivityRecord(String textLine) {

            ActivityRecord result = null;
            String[] fields = null;
            
            if (textLine != null) {
                try {
                    // Splits the string
                    fields = textLine.split(FIELDS_REGEXP);
                    
                    // Parses fixed lenght part
                    if (fields.length > 3) {
                        result = new ActivityRecord();
                        
                        // Parses the date                        
                        result.date = !fields[0].equals(NULL_STRING) ? new Date(Long.parseLong(fields[0])) : null;

                        // Parses the state
                        result.state = !fields[1].equals(NULL_STRING) ? State.valueOf(fields[1]) : null;
                       
                        // Parses the action
                        result.action = !fields[2].equals(NULL_STRING) ? StateAction.valueOf(fields[2]) : null;

                        // Parses the requested action
                        result.requestedAction = !fields[3].equals(NULL_STRING) ? RequestedAction.valueOf(fields[3]) : null;
                    }

                    // Parses variable length part                                
                    if (fields.length > 4) {
                        result.payload = new Object[fields.length - 4];
                        System.arraycopy(fields, 4, result.payload, 0, fields.length - 4);
                        parsePayload(result.payload);
                    }

                }
                catch (Exception e) {
                    Log.e(LOGTAG, "Error parsing activity record: " + textLine);
                    Log.e(LOGTAG, Log.getStackTraceString(e));
                    result = null;
                }
            }

            return result;
        }

        /** Parses the payload */
        private static void parsePayload (Object[] payload) {
            
            for (int i=0; i < payload.length; i++) {
                if ((payload[i] != null) && payload[i] instanceof String) {                    
                    if (!payload[i].equals(NULL_STRING)) {
                        // Tries to get an integer
                        try {
                            payload[i] = Integer.parseInt((String)(payload[i]));
                        }
                        // Otherwise leave it as an String
                        catch (NumberFormatException e) {
                            payload[i] = manageFieldSeparator(payload[i].toString(), false);
                        }
                    }                    
                    else {
                        // Replace with a null
                        payload[i] = null;                        
                    }
                }
            }
        }
        
        /** Replaces or restores field separator character from field */
        private static String manageFieldSeparator(String field, boolean replace) {
            return (replace ? field.replace(FIELD_SEPARATOR, REPLACEMENT_STRING) : field.replace(REPLACEMENT_STRING, FIELD_SEPARATOR));            
        }
    }
}