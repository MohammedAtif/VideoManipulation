cmake_minimum_required(VERSION 3.4.1)

find_library( log-lib log )

add_library(video-processing.lib
    SHARED
    video_process/video-process.h
    video_process/video-process.cpp
)

add_library(sample.lib
    SHARED
    sample/native-lib.cpp
)

target_link_libraries(video-processing.lib ${log-lib})
target_link_libraries(sample.lib ${log-lib})