# Initialize Android SQLite database from a JSON file in two steps
The sample code is under folder InitSQLiteFromJSON. In the WelcomeActivity, first check if the DB is initialized, 
if not start the one time DB initialization phase.

This initialization will utilize the Gson libary.

First, load the JSON file and convert the JSON array to the corresponding Java object list by using Gson library.
Second, initilize the SQLite database table by inserting each item in the Java object list using Andrid SQLite API.
Each step is done in an AsyncTask and the second task should be launched only when the first task succeeds. 

# An AsyncTaskManager for managing multiple same source AsyncTasks
This CommonAsyncTaskManager is used to store and lookup the launched AsyncTasks in order to determine the most 
recently launched task for the same source. Further more explaination can be found in the sample code under folder 
CommonAsyncTaskManager.
