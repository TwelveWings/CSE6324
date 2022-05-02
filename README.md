# CSE6324
The cloudstorage directory contains 6 subdirectories.

1) client stores the clients and related thread classes that will access the server
2) data stores a class for the file data
3) enums stores enums used in the survey
4) network stores the TCP and UDP helper classes as well as their corresponding thread classes.
5) server contains the server and its related thread classes that will receive data from and respond to the clients.

To run the program, first compile CloudStorage.java in the root directory.

javac -d . CloudStorage.java

Then compile each subdirectory of cloudstorage in the following order

javac cloudstorage\enums\\*.java

javac cloudstorage\data\\*.java

javac cloudstorage\network\\*.java

javac cloudstorage\control\\*.java

javac cloudstorage\server\\*.java

javac cloudstorage\client\\*.java

To run the server program, run: 

java -cp ";sqlite-jdbc-3.36.0.3.jar" cloudstorage.server.Server

Then run:

java cloudstorage.client.Client

1) You will need to specify which directory the client will synchronize with.
2) The client will synchronize with the server on start up, clearing out any existing files.
3) Once the client has started, adding, modifying, or deleting files will trigger synchronization with the server.
4) The client will receive data from the server, and the server will update its database. Then the server will push the data to any other clients currently open.

To reset the tables on the server run:

java -cp ";sqlite-jdbc-3.36.0.3.jar" cloudstorage.server.Server new


To check the tables on the server run:

java -classpath ";sqlite-jdbc-3.36.0.3.jar" cloudstorage.server.TableReader
