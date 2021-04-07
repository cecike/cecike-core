cmake_minimum_required(VERSION 3.17)
project(ElfLoader)

set(CMAKE_CXX_STANDARD 20)

find_package(verilator HINTS $ENV{VERILATOR_ROOT} ${VERILATOR_ROOT})
if (NOT verilator_FOUND)
    message(FATAL_ERROR "Verilator was not found. Either install it, or set the VERILATOR_ROOT environment variable")
endif()

set(SOURCE_FILES    main.cpp
                    loadelf.cpp
                    sim_memory.cpp
                    simulator.cpp)

add_executable(SimCore ${SOURCE_FILES})
verilate(SimCore
        SOURCES "../SimpleSimCore.v")