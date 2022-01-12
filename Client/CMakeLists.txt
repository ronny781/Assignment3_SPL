cmake_minimum_required(VERSION 3.20)
project(Assignment3SPL)


add_executable(Assignment3SPL src/Client.cpp src/connectionHandler.cpp include/connectionHandler.h)

set(BOOST_ROOT "C:Boost")
set(Boost_USE_STATIC_LIBS OFF)
set(Boost_USE_MULTITHREADED_ON)
set(Boost_USE_STATIC_RUNTIME_OFF)
set(CMAKE_CXX_STANDARD 11)
find_package(Boost REQUIRED COMPONENTS system)
find_package(Boost COMPONENTS thread system REQUIRED)


SET(LDFLAGS "-lboost_system -lboost_thread -pthread")
SET(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${LDFLAGS}")

target_include_directories(Assignment3SPL PUBLIC ${Boost_INCLUDE_DIRS})
target_link_libraries(Assignment3SPL ${Boost_LIBRARIES})
