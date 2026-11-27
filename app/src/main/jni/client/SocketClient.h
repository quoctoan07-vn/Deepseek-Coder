/*
 * DeepSeek Coder - app-side socket client.
 * Connects to the embedded root helper on 127.0.0.1 only.
 */
#pragma once
#include <arpa/inet.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <unistd.h>
#include <cerrno>
#include <cstring>
#include <cstdint>
#include "../common/Protocol.h"

class SocketClient {
public:
    int sockfd = -1;

    bool connectTo(const char* host, int port, const char* token) {
        closeSocket();

        sockfd = socket(AF_INET, SOCK_STREAM | SOCK_CLOEXEC, 0);
        if (sockfd < 0) return false;

        timeval tv{};
        tv.tv_sec = 2;
        tv.tv_usec = 0;
        setsockopt(sockfd, SOL_SOCKET, SO_RCVTIMEO, &tv, sizeof(tv));
        setsockopt(sockfd, SOL_SOCKET, SO_SNDTIMEO, &tv, sizeof(tv));

        sockaddr_in addr{};
        addr.sin_family = AF_INET;
        addr.sin_port = htons((uint16_t)port);
        if (inet_pton(AF_INET, host, &addr.sin_addr) != 1) {
            closeSocket();
            return false;
        }

        if (connect(sockfd, (sockaddr*)&addr, sizeof(addr)) != 0) {
            closeSocket();
            return false;
        }

        Handshake hs{};
        hs.magic = DC_MAGIC;
        strncpy(hs.token, token, sizeof(hs.token) - 1);

        if (!sendAll(&hs, sizeof(hs))) {
            closeSocket();
            return false;
        }

        HandshakeResponse hsr{};
        if (!recvAll(&hsr, sizeof(hsr)) || hsr.magic != DC_MAGIC || !hsr.accepted) {
            closeSocket();
            return false;
        }

        return true;
    }

    bool request(const Request& req, Response& resp) {
        if (sockfd < 0) return false;
        if (!sendAll(&req, sizeof(req))) {
            closeSocket();
            return false;
        }
        if (!recvAll(&resp, sizeof(resp))) {
            closeSocket();
            return false;
        }
        return resp.Success != 0;
    }

    bool isConnected() const {
        return sockfd >= 0;
    }

    void closeSocket() {
        if (sockfd >= 0) {
            close(sockfd);
            sockfd = -1;
        }
    }

private:
    bool sendAll(const void* data, size_t size) {
        const char* p = (const char*)data;
        while (size > 0) {
            ssize_t n = send(sockfd, p, size, MSG_NOSIGNAL);
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
            ssize_t n = recv(sockfd, p, size, 0);
            if (n == -1 && errno == EINTR) continue;
            if (n <= 0) return false;
            p += n;
            size -= (size_t)n;
        }
        return true;
    }
};
