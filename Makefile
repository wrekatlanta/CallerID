all: CallerID.jar

CallerID.jar:
	/usr/local/ant/bin/ant -f CallerID.xml dist

clean:
	rm org/wrek/CallerID/*.class
	rm CallerID.jar
