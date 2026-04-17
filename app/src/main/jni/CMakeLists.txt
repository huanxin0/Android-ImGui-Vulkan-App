cmake_minimum_required(VERSION 3.10)
project(youxi)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)  # 关键：全局开启 PIC

# 添加静态库预编译导入
add_library(curl STATIC IMPORTED)
set_target_properties(curl PROPERTIES IMPORTED_LOCATION
    ${CMAKE_CURRENT_SOURCE_DIR}/curl/arm64-v8a/lib/libcurl.a)

add_library(ssl STATIC IMPORTED)
set_target_properties(ssl PROPERTIES IMPORTED_LOCATION
    ${CMAKE_CURRENT_SOURCE_DIR}/curl/arm64-v8a/lib/libssl.a)

add_library(crypto STATIC IMPORTED)
set_target_properties(crypto PROPERTIES IMPORTED_LOCATION
    ${CMAKE_CURRENT_SOURCE_DIR}/curl/arm64-v8a/lib/libcrypto.a)

# 收集源文件
file(GLOB_RECURSE YOUMI_SOURCES
    "${CMAKE_CURRENT_SOURCE_DIR}/*.c"
    "${CMAKE_CURRENT_SOURCE_DIR}/*.cpp"
    "${CMAKE_CURRENT_SOURCE_DIR}/*.cxx"
    "${CMAKE_CURRENT_SOURCE_DIR}/*.cc"
)

# 如果需要只包含特定目录（对应原 wildcard 的三个路径），可以用更精确的方式：
# file(GLOB YOUMI_SOURCES
#     "${CMAKE_CURRENT_SOURCE_DIR}/*.c*"
#     "${CMAKE_CURRENT_SOURCE_DIR}/imgui/*.c*"
#     "${CMAKE_CURRENT_SOURCE_DIR}/imgui/backends/*.c*"
# )

# 创建动态库
add_library(youxi SHARED ${YOUMI_SOURCES})

# 头文件路径
target_include_directories(youxi PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/nlohmann
    ${CMAKE_CURRENT_SOURCE_DIR}/imgui
    ${CMAKE_CURRENT_SOURCE_DIR}/imgui/backends
    ${CMAKE_CURRENT_SOURCE_DIR}/curl/arm64-v8a/include
)

# 编译选项
target_compile_options(youxi PRIVATE
    -fvisibility=hidden
    -DVK_NO_PROTOTYPES
)

# 链接静态库
target_link_libraries(youxi
    curl
    ssl
    crypto
    log
    android
    dl
    z
)