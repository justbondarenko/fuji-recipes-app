cmake_minimum_required(VERSION 3.22.1)
project(fuji_raw LANGUAGES CXX)
set(CMAKE_CXX_STANDARD 17)

# Unmodified upstream source, pinned locally so builds need no extra network fetch.
include(FetchContent)
FetchContent_Declare(libraw
    URL "${CMAKE_CURRENT_SOURCE_DIR}/vendor/LibRaw-0.22.2.tar.gz"
    URL_HASH SHA256=627928088300ecde6ca91ffd202e189203f04ad61ad12f0fe9dc57b9a7a0fb3c)
FetchContent_GetProperties(libraw)
if(NOT libraw_POPULATED)
    FetchContent_Populate(libraw)
endif()
file(GLOB_RECURSE LIBRAW_SOURCES CONFIGURE_DEPENDS "${libraw_SOURCE_DIR}/src/*.cpp")
add_library(raw STATIC ${LIBRAW_SOURCES})
target_include_directories(raw PUBLIC "${libraw_SOURCE_DIR}")
target_compile_definitions(raw PRIVATE LIBRAW_NOTHREADS LIBRAW_NO_IOSTREAMS_DATASTREAM USE_ZLIB)
target_compile_options(raw PRIVATE -O2 -w)
set_target_properties(raw PROPERTIES POSITION_INDEPENDENT_CODE ON)

add_library(fuji_raw SHARED raw_editor.cpp)
target_link_libraries(fuji_raw PRIVATE raw jnigraphics z)
target_compile_options(fuji_raw PRIVATE -O2 -Wall -Wextra)
