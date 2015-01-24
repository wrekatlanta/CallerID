all: CallerID
	echo "all built"

CallerID: CallerID.class Calls.class LineMsg.class LineStatus.class WhozzCalling.class WhozzFile.class WhozzTCP.class Queue.class
	javac CallerID.java Calls.java LineMsg.java LineStatus.java WhozzCalling.java WhozzFile.java WhozzTCP.java Queue.java
