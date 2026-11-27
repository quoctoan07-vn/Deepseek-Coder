/*
 * DeepSeek Coder - process discovery declarations (root helper).
 */

#pragma once
#include <cstdint>
#include <sys/types.h>

pid_t FindPid(const char* packageName);
uintptr_t FindLibrary(const char* libName, pid_t pid);
