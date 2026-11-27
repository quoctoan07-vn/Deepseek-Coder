/*
 * DeepSeek Coder - loopback TCP server used by the embedded root helper.
 * The socket is bound to 127.0.0.1 only and protected by a per-launch token.
 */
#pragma once
#include <arpa/inet.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <sys/time.h>
#include <unistd.h>
#include <cstring>
#include <cerrno>
#include <cstdint>

class TcpServer {
public:
    int listenfd = -1;
    int clientfd = -1;

    bool Create() {
        listenfd = socket(AF_INET, SOCK_STREAM | SOCK_CLOEXEC, 0);
        if (listenfd < 0) return false;
        int one = 1;
        setsockopt(listenfd, SOL_SOCKET, SO_REUSEADDR, &one, sizeof(one));
        return true;
    }

    bool Bind(int port) {
        sockaddr_in addr{};
        addr.sin_family = AF_INET;
        addr.sin_port = htons((uint16_t)port);
        addr.sin_addr.s_addr = htonl(INADDR_LOOPBACK);
        return bind(listenfd, (sockaddr*)&addr, sizeof(addr)) == 0;
    }

    bool Listen() {
        return listen(listenfd, 1) == 0;
    }

    bool Accept() {
        clientfd = accept4(listenfd, nullptr, nullptr, SOCK_CLOEXEC);
        return clientfd >= 0;
    }

    void setRecvTimeout(int seconds) {
        timeval tv{};
        tv.tv_sec = seconds;
        tv.tv_usec = 0;
        setsockopt(clientfd, SOL_SOCKET, SO_RCVTIMEO, &tv, sizeof(tv));
    }

    bool sendAll(const void* data, size_t size) {
        const char* p = (const char*)data;
        while (size > 0) {
            ssize_t n = send(clientfd, p, size, MSG_NOSIGNAL);
            if (n == -1 && errno == EINTR) continue;
            if (n <= 0) return false;
            p += n;
            size -= (size_t)n;
        }
        return true;
    }

    bool recvAll(void* data, size_t size) {
        char* p = (char*)data;
        while (size > 0) {
            ssize_t n = recv(clientfd, p, size, 0);
            if (n == -1 && errno == EINTR) continue;
            if (n <= 0) return false;
            p += n;
            size -= (size_t)n;
        }
        return true;
    }

    void Disconnect() {
        if (clientfd >= 0) {
            close(clientfd);
            clientfd = -1;
        }
    }

    void Close() {
        Disconnect();
        if (listenfd >= 0) {
            close(listenfd);
            listenfd = -1;
        }
    }
};
