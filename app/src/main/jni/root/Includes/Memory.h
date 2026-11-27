/*
 * DeepSeek Coder - process memory primitives (root helper).
 *
 * Opens /proc/<pid>/mem once and exposes typed read/write helpers. The fast
 * path uses process_vm_readv/process_vm_writev so several fields can be
 * fetched with one syscall; if the kernel or SELinux blocks the syscall we
 * transparently fall back to pread64/pwrite64 on /proc/<pid>/mem.
 */
#pragma once
#include <unistd.h>
#include <fcntl.h>
#include <cstdint>
#include <cstdio>
#include <cstddef>
#include <cerrno>
#include <sys/uio.h>
#include <sys/syscall.h>
#include "Logger.h"

#ifndef __NR_process_vm_readv
#  if defined(__aarch64__)
#    define __NR_process_vm_readv 270
#  elif defined(__arm__)
#    define __NR_process_vm_readv 376
#  elif defined(__i386__)
#    define __NR_process_vm_readv 347
#  else
#    define __NR_process_vm_readv 310
#  endif
#endif

#ifndef __NR_process_vm_writev
#  if defined(__aarch64__)
#    define __NR_process_vm_writev 271
#  elif defined(__arm__)
#    define __NR_process_vm_writev 377
#  elif defined(__i386__)
#    define __NR_process_vm_writev 348
#  else
#    define __NR_process_vm_writev 311
#  endif
#endif

inline int mem_fd = -1;
inline pid_t target_pid = -1;
inline bool g_vmReadOk = true;
inline bool g_vmWriteOk = true;

inline int OpenMemory(pid_t pid) {
    target_pid = pid;
    g_vmReadOk = true;
    g_vmWriteOk = true;

    char path[64];
    snprintf(path, sizeof(path), "/proc/%d/mem", pid);
    mem_fd = open(path, O_RDWR);
    if (mem_fd <= 0) {
        LOGW("[DC] /proc/%d/mem open failed (errno=%d), using process_vm_readv", pid, errno);
    }

    // process_vm_readv can still work when /proc/<pid>/mem cannot be opened.
    return pid > 0 ? 1 : -1;
}

inline void CloseMemory() {
    if (mem_fd > 0) {
        close(mem_fd);
        mem_fd = -1;
    }
    target_pid = -1;
}

inline bool vmReadAllowedError(int err) {
    return err == ENOSYS || err == EPERM || err == EACCES || err == EINVAL;
}

inline bool vmWriteAllowedError(int err) {
    return err == ENOSYS || err == EPERM || err == EACCES || err == EINVAL;
}

inline void disableVmRead(int err) {
    if (g_vmReadOk) {
        g_vmReadOk = false;
        LOGW("[DC] process_vm_readv blocked (%d), falling back to pread64", err);
    }
}

inline void disableVmWrite(int err) {
    if (g_vmWriteOk) {
        g_vmWriteOk = false;
        LOGW("[DC] process_vm_writev blocked (%d), falling back to pwrite64", err);
    }
}

inline ssize_t vmReadvRaw(const struct iovec* local, unsigned long localCount,
                          const struct iovec* remote, unsigned long remoteCount) {
    if (target_pid <= 0) return -1;
    return (ssize_t)syscall(__NR_process_vm_readv, target_pid,
                            local, localCount, remote, remoteCount, 0UL);
}

inline ssize_t vmWritevRaw(const struct iovec* local, unsigned long localCount,
                           const struct iovec* remote, unsigned long remoteCount) {
    if (target_pid <= 0) return -1;
    return (ssize_t)syscall(__NR_process_vm_writev, target_pid,
                            local, localCount, remote, remoteCount, 0UL);
}

template <typename T>
T Read(uintptr_t address) {
    T data{};
    if (address == 0) return data;

    if (g_vmReadOk) {
        struct iovec local{ &data, sizeof(T) };
        struct iovec remote{ reinterpret_cast<void*>(address), sizeof(T) };
        ssize_t n = vmReadvRaw(&local, 1, &remote, 1);
        if (n == (ssize_t)sizeof(T)) return data;
        if (n < 0 && vmReadAllowedError(errno)) disableVmRead(errno);
    }

    if (mem_fd > 0) {
        pread64(mem_fd, &data, sizeof(T), address);
    }
    return data;
}

template <typename T>
void Write(uintptr_t address, T data) {
    if (address == 0) return;

    if (g_vmWriteOk) {
        struct iovec local{ &data, sizeof(T) };
        struct iovec remote{ reinterpret_cast<void*>(address), sizeof(T) };
        ssize_t n = vmWritevRaw(&local, 1, &remote, 1);
        if (n == (ssize_t)sizeof(T)) return;
        if (n < 0 && vmWriteAllowedError(errno)) disableVmWrite(errno);
    }

    if (mem_fd > 0) {
        pwrite64(mem_fd, &data, sizeof(T), address);
    }
}

inline bool ReadArray(uintptr_t address, void* out, size_t size) {
    if (address == 0 || out == nullptr || size == 0) return false;

    if (g_vmReadOk) {
        struct iovec local{ out, size };
        struct iovec remote{ reinterpret_cast<void*>(address), size };
        ssize_t n = vmReadvRaw(&local, 1, &remote, 1);
        if (n == (ssize_t)size) return true;
        if (n < 0 && vmReadAllowedError(errno)) disableVmRead(errno);
    }

    return mem_fd > 0 && pread64(mem_fd, out, size, address) == (ssize_t)size;
}

inline bool WriteArray(uintptr_t address, const void* data, size_t size) {
    if (address == 0 || data == nullptr || size == 0) return false;

    if (g_vmWriteOk) {
        struct iovec local{ const_cast<void*>(data), size };
        struct iovec remote{ reinterpret_cast<void*>(address), size };
        ssize_t n = vmWritevRaw(&local, 1, &remote, 1);
        if (n == (ssize_t)size) return true;
        if (n < 0 && vmWriteAllowedError(errno)) disableVmWrite(errno);
    }

    return mem_fd > 0 && pwrite64(mem_fd, data, size, address) == (ssize_t)size;
}

struct MemRange {
    uintptr_t address;
    void* buffer;
    size_t size;
};

// One process_vm_readv call for up to 16 scattered fields.
inline bool ReadRanges(const MemRange* ranges, size_t count) {
    if (ranges == nullptr || count == 0) return false;

    constexpr size_t kMaxRanges = 16;
    if (count > kMaxRanges) {
        bool ok = true;
        for (size_t i = 0; i < count; i++) {
            if (!ReadArray(ranges[i].address, ranges[i].buffer, ranges[i].size)) ok = false;
        }
        return ok;
    }

    if (g_vmReadOk) {
        struct iovec local[kMaxRanges];
        struct iovec remote[kMaxRanges];
        size_t total = 0;
        for (size_t i = 0; i < count; i++) {
            local[i].iov_base = ranges[i].buffer;
            local[i].iov_len = ranges[i].size;
            remote[i].iov_base = reinterpret_cast<void*>(ranges[i].address);
            remote[i].iov_len = ranges[i].size;
            total += ranges[i].size;
        }
        ssize_t n = vmReadvRaw(local, count, remote, count);
        if (n == (ssize_t)total) return true;
        if (n < 0 && vmReadAllowedError(errno)) disableVmRead(errno);
    }

    bool ok = true;
    for (size_t i = 0; i < count; i++) {
        if (!ReadArray(ranges[i].address, ranges[i].buffer, ranges[i].size)) ok = false;
    }
    return ok;
}

// One process_vm_writev call for up to 16 scattered fields.
inline bool WriteRanges(const MemRange* ranges, size_t count) {
    if (ranges == nullptr || count == 0) return false;

    constexpr size_t kMaxRanges = 16;
    if (count > kMaxRanges) {
        bool ok = true;
        for (size_t i = 0; i < count; i++) {
            if (!WriteArray(ranges[i].address, ranges[i].buffer, ranges[i].size)) ok = false;
        }
        return ok;
    }

    if (g_vmWriteOk) {
        struct iovec local[kMaxRanges];
        struct iovec remote[kMaxRanges];
        size_t total = 0;
        for (size_t i = 0; i < count; i++) {
            local[i].iov_base = ranges[i].buffer;
            local[i].iov_len = ranges[i].size;
            remote[i].iov_base = reinterpret_cast<void*>(ranges[i].address);
            remote[i].iov_len = ranges[i].size;
            total += ranges[i].size;
        }
        ssize_t n = vmWritevRaw(local, count, remote, count);
        if (n == (ssize_t)total) return true;
        if (n < 0 && vmWriteAllowedError(errno)) disableVmWrite(errno);
    }

    bool ok = true;
    for (size_t i = 0; i < count; i++) {
        if (!WriteArray(ranges[i].address, ranges[i].buffer, ranges[i].size)) ok = false;
    }
    return ok;
}
