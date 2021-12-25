Embrace Galaxy Brained Logging!

![Galaxy Brain](/Expanding-Loggers.jpg?raw=true "Galaxy Brain")

Building and running examples:
```
javac -g *.java

java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 EnlightenedBrain

java GalaxyLogger 5005
```
