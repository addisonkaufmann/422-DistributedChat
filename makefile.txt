JFLAGS = -g
JC = javac
.SUFFIXES: .java .class

all: classes 

.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
	User.java \
	ChatClient.java \
	ChatServer.java

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class