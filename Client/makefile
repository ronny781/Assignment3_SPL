CFLAGS:=-c -Wall -Weffc++ -g -std=c++11 -Iinclude
LDFLAGS:=-lboost_system -pthread -lboost_thread


all: bin/Client

	@echo 'Building target: Client'
	@echo 'Invoking: C++ Linker'

bin/Client: bin/Client.o bin/ConnectionHandler.o
	 g++ -Wall -Weffc++ -o bin/Client bin/ConnectionHandler.o bin/Client.o  $(LDFLAGS)
	@echo 'Finished building target: Client'

bin/ConnectionHandler.o: src/connectionHandler.cpp
	g++ $(CFLAGS) -o bin/ConnectionHandler.o src/connectionHandler.cpp $(LDFLAGS)

bin/Client.o: src/Client.cpp
	g++ $(CFLAGS) -o bin/Client.o src/Client.cpp


.PHONY: clean
clean:
	@echo 'Cleaning'
	rm -f bin/*