cmake_minimum_required(VERSION 3.18)
project(cpd_project_1)

set(CMAKE_CXX_STANDARD 14)

add_executable(cpd_project_1 main.cpp)
set(CMAKE_CXX_FLAGS "-O2")

target_link_libraries(${PROJECT_NAME} -lpapi)