cmake_minimum_required(VERSION 2.8.3)
project(voice_change_filter)

find_package(catkin REQUIRED COMPONENTS
  roscpp
  rospy
  std_msgs
)

catkin_package(
)

include_directories(
  ${catkin_INCLUDE_DIRS}
)

## add_executable(simple src/unreleased/simple.cc)

# target_link_libraries(simple
#   ${catkin_LIBRARIES}
# )

