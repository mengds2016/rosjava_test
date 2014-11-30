##############################################################################
# CMake
##############################################################################

cmake_minimum_required(VERSION 2.8.3)
project(rosjava_test)

##############################################################################
# Catkin
##############################################################################

find_package(catkin REQUIRED rosjava_build_tools)

# Set the gradle targets you want catkin's make to run by default, e.g.
#   catkin_rosjava_setup(installApp)
# Note that the catkin_create_rosjava_xxx scripts will usually automatically
# add tasks to this for you when you create subprojects.
catkin_rosjava_setup(installApp publishMavenJavaPublicationToMavenRepository)
## catkin_android_setup(assembleRelease uploadArchives)

catkin_package()

##############################################################################
# Installation
##############################################################################

# Change this to match the maven group name you have specified in the 
# allprojects closure the root build.gradle
install(DIRECTORY ${CATKIN_DEVEL_PREFIX}/${CATKIN_GLOBAL_MAVEN_DESTINATION}/com/github/rosjava/${PROJECT_NAME}/ 
        DESTINATION ${CATKIN_GLOBAL_MAVEN_DESTINATION}/com/github/rosjava/${PROJECT_NAME})

##rosbuild_make_distribution(0.1.0)
##rosbuild_find_ros_stack(android_core)
if(android_core_FOUND_CATKIN_PROJECT)
  set(android_gingerbread_mr1_path "${android_core_SOURCE_PREFIX}/android_gingerbread_mr1")
  set(android_honeycomb_mr2_path "${android_core_SOURCE_PREFIX}/android_honeycomb_mr2")
endif()

EXECUTE_PROCESS(COMMAND ${CMAKE_COMMAND} -E create_symlink
 ${android_gingerbread_mr1_path} "${PROJECT_SOURCE_DIR}/android_gingerbread_mr1"
)
EXECUTE_PROCESS(COMMAND ${CMAKE_COMMAND} -E create_symlink
 ${android_honeycomb_mr2_path} "${PROJECT_SOURCE_DIR}/android_honeycomb_mr2"
)
