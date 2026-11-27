/*
 * DeepSeek Coder - process discovery (root helper).
 *
 * Scans /proc for the game package name and parses /proc/<pid>/maps to find
 * the load base of libil2cpp.so.
 */

#include "Process.h"
#include <dirent.h>
#include <cstdio>
#include <cstdlib>
#include <cstring>

pid_t FindPid(const char* packageName) {
    DIR* dir = opendir("/proc");
    if (!dir) return -1;

    struct dirent* entry;
    while ((entry = readdir(dir)) != nullptr) {
        int pid = atoi(entry->d_name);
        if (pid <= 0) continue;

        char cmdlinePath[64];
        snprintf(cmdlinePath, sizeof(cmdlinePath), "/proc/%d/cmdline", pid);

        FILE* fp = fopen(cmdlinePath, "r");
        if (!fp) continue;

        char cmdline[256] = {0};
        fgets(cmdline, sizeof(cmdline), fp);
        fclose(fp);

        if (strstr(cmdline, packageName)) {
            closedir(dir);
            return pid;
        }
    }
    closedir(dir);
    return -1;
}

uintptr_t FindLibrary(const char* libName, pid_t pid) {
    char mapsPath[64];
    snprintf(mapsPath, sizeof(mapsPath), "/proc/%d/maps", pid);

    FILE* fp = fopen(mapsPath, "r");
    if (!fp) return 0;

    char line[512];
    while (fgets(line, sizeof(line), fp)) {
        if (strstr(line, libName)) {
            uintptr_t base = strtoull(line, nullptr, 16);
            fclose(fp);
            return base;
        }
    }
    fclose(fp);
    return 0;
}
