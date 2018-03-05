/*
 * Copyright (c) 2014, Dustin Lundquist <dustin@null-ptr.net>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>

#include <netdb.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <errno.h>
#include <unistd.h>

#include <ares.h>

#ifdef HAVE_LIBEV_EV_H
#include <libev/ev.h>
#else
#include <ev.h>
#endif

#include <libcork/core.h>

#include "resolv.h"
#include "utils.h"
#include "netutils.h"

/*
 * Implement DNS resolution interface using libc-ares
 */

struct resolv_ctx {
    struct ev_io io;
    struct ev_timer tw;

    ares_channel channel;
    struct ares_options options;
};

struct resolv_query {
    int requests[2];
    size_t response_count;
    struct sockaddr **responses;

    void (*client_cb)(struct sockaddr *, void *);
    void (*free_cb)(void *);

    uint16_t port;

    void *data;

    int is_closed;
};

extern int verbose;

struct resolv_ctx default_ctx;
static struct ev_loop *default_loop;

enum {
    MODE_IPV4_FIRST = 0,
    MODE_IPV6_FIRST = 1
} RESOLV_MODE;

static int resolv_mode = MODE_IPV4_FIRST;

static void resolv_sock_cb(struct ev_loop *, struct ev_io *, int);
static void resolv_timeout_cb(struct ev_loop *, struct ev_timer *, int);
static void resolv_sock_state_cb(void *, int, int, int);

static void dns_query_v4_cb(void *, int, int, struct hostent *);
static void dns_query_v6_cb(void *, int, int, struct hostent *);

static void process_client_callback(struct resolv_query *);
static inline int all_requests_are_null(struct resolv_query *);
static struct sockaddr *choose_ipv4_first(struct resolv_query *);
static struct sockaddr *choose_ipv6_first(struct resolv_query *);
static struct sockaddr *choose_any(struct resolv_query *);

static void reset_timer();

/*
 * DNS UDP socket activity callback
 */
static void
resolv_sock_cb(EV_P_ ev_io *w, int revents)
{
    struct resolv_ctx *ctx = (struct resolv_ctx *)w;

    ares_socket_t rfd = ARES_SOCKET_BAD, wfd = ARES_SOCKET_BAD;

    if (revents & EV_READ)
        rfd = w->fd;
    if (revents & EV_WRITE)
        wfd = w->fd;

    ares_process_fd(ctx->channel, rfd, wfd);

    reset_timer();
}

int
resolv_init(struct ev_loop *loop, char *nameservers, int ipv6first)
{
    int status;

    if (ipv6first)
        resolv_mode = MODE_IPV6_FIRST;
    else
        resolv_mode = MODE_IPV4_FIRST;

    default_loop = loop;

    if ((status = ares_library_init(ARES_LIB_INIT_ALL)) != ARES_SUCCESS) {
        LOGE("c-ares error: %s", ares_strerror(status));
        FATAL("failed to initialize c-ares");
    }

    memset(&default_ctx, 0, sizeof(struct resolv_ctx));

    default_ctx.options.sock_state_cb_data = &default_ctx;
    default_ctx.options.sock_state_cb      = resolv_sock_state_cb;
    default_ctx.options.timeout            = 3000;
    default_ctx.options.tries              = 2;

    status = ares_init_options(&default_ctx.channel, &default_ctx.options,
#if ARES_VERSION_MAJOR >= 1 && ARES_VERSION_MINOR >= 12
                               ARES_OPT_NOROTATE |
#endif
                               ARES_OPT_TIMEOUTMS | ARES_OPT_TRIES | ARES_OPT_SOCK_STATE_CB);

    if (status != ARES_SUCCESS) {
        FATAL("failed to initialize c-ares");
    }

    if (nameservers != NULL) {
#if ARES_VERSION_MAJOR >= 1 && ARES_VERSION_MINOR >= 11
        status = ares_set_servers_ports_csv(default_ctx.channel, nameservers);
#else
        status = ares_set_servers_csv(default_ctx.channel, nameservers);
#endif
    }

    if (status != ARES_SUCCESS) {
        FATAL("failed to set nameservers");
    }

    ev_init(&default_ctx.io, resolv_sock_cb);
    ev_timer_init(&default_ctx.tw, resolv_timeout_cb, 0.0, 0.0);

    return 0;
}

void
resolv_shutdown(struct ev_loop *loop)
{
    ares_cancel(default_ctx.channel);
    ares_destroy(default_ctx.channel);

    ares_library_cleanup();
}

void
resolv_start(const char *hostname, uint16_t port,
             void (*client_cb)(struct sockaddr *, void *),
             void (*free_cb)(void *), void *data)
{
    /*
     * Wrap c-ares's call back in our own
     */
    struct resolv_query *query = ss_malloc(sizeof(struct resolv_query));

    memset(query, 0, sizeof(struct resolv_query));

    query->port           = port;
    query->client_cb      = client_cb;
    query->response_count = 0;
    query->responses      = NULL;
    query->data           = data;
    query->free_cb        = free_cb;

    query->requests[0] = AF_INET;
    query->requests[1] = AF_INET6;

    ares_gethostbyname(default_ctx.channel, hostname, AF_INET, dns_query_v4_cb, query);
    ares_gethostbyname(default_ctx.channel, hostname, AF_INET6, dns_query_v6_cb, query);

    reset_timer();
}

/*
 * Wrapper for client callback we provide to c-ares
 */
static void
dns_query_v4_cb(void *arg, int status, int timeouts, struct hostent *he)
{
    int i, n;
    struct resolv_query *query = (struct resolv_query *)arg;

    if (status == ARES_EDESTRUCTION) {
        return;
    }

    if (!he || status != ARES_SUCCESS) {
        if (verbose) {
            LOGI("failed to lookup v4 address %s", ares_strerror(status));
        }
        goto CLEANUP;
    }

    if (verbose) {
        LOGI("found address name v4 address %s", he->h_name);
    }

    n = 0;
    while (he->h_addr_list[n])
        n++;

    if (n > 0) {
        struct sockaddr **new_responses = ss_realloc(query->responses,
                                                     (query->response_count + n)
                                                     * sizeof(struct sockaddr *));

        if (new_responses == NULL) {
            LOGE("failed to allocate memory for additional DNS responses");
        } else {
            query->responses = new_responses;

            for (i = 0; i < n; i++) {
                struct sockaddr_in *sa = ss_malloc(sizeof(struct sockaddr_in));
                memset(sa, 0, sizeof(struct sockaddr_in));
                sa->sin_family = AF_INET;
                sa->sin_port   = query->port;
                memcpy(&sa->sin_addr, he->h_addr_list[i], he->h_length);

                query->responses[query->response_count] = (struct sockaddr *)sa;
                if (query->responses[query->response_count] == NULL) {
                    LOGE("failed to allocate memory for DNS query result address");
                } else {
                    query->response_count++;
                }
            }
        }
    }

CLEANUP:

    query->requests[0] = 0; /* mark A query as being completed */

    /* Once all requests have completed, call client callback */
    if (all_requests_are_null(query)) {
        return process_client_callback(query);
    }
}

static void
dns_query_v6_cb(void *arg, int status, int timeouts, struct hostent *he)
{
    int i, n;
    struct resolv_query *query = (struct resolv_query *)arg;

    if (status == ARES_EDESTRUCTION) {
        return;
    }

    if (!he || status != ARES_SUCCESS) {
        if (verbose) {
            LOGI("failed to lookup v6 address %s", ares_strerror(status));
        }
        goto CLEANUP;
    }

    if (verbose) {
        LOGI("found address name v6 address %s", he->h_name);
    }

    n = 0;
    while (he->h_addr_list[n])
        n++;

    if (n > 0) {
        struct sockaddr **new_responses = ss_realloc(query->responses,
                                                     (query->response_count + n)
                                                     * sizeof(struct sockaddr *));

        if (new_responses == NULL) {
            LOGE("failed to allocate memory for additional DNS responses");
        } else {
            query->responses = new_responses;

            for (i = 0; i < n; i++) {
                struct sockaddr_in6 *sa = ss_malloc(sizeof(struct sockaddr_in6));
                memset(sa, 0, sizeof(struct sockaddr_in6));
                sa->sin6_family = AF_INET6;
                sa->sin6_port   = query->port;
                memcpy(&sa->sin6_addr, he->h_addr_list[i], he->h_length);

                query->responses[query->response_count] = (struct sockaddr *)sa;
                if (query->responses[query->response_count] == NULL) {
                    LOGE("failed to allocate memory for DNS query result address");
                } else {
                    query->response_count++;
                }
            }
        }
    }

CLEANUP:

    query->requests[1] = 0; /* mark A query as being completed */

    /* Once all requests have completed, call client callback */
    if (all_requests_are_null(query)) {
        return process_client_callback(query);
    }
}

/*
 * Called once all requests have been completed
 */
static void
process_client_callback(struct resolv_query *query)
{
    struct sockaddr *best_address = NULL;

    if (resolv_mode == MODE_IPV4_FIRST) {
        best_address = choose_ipv4_first(query);
    } else if (resolv_mode == MODE_IPV6_FIRST) {
        best_address = choose_ipv6_first(query);
    } else {
        best_address = choose_any(query);
    }

    query->client_cb(best_address, query->data);

    for (int i = 0; i < query->response_count; i++)
        ss_free(query->responses[i]);

    ss_free(query->responses);

    if (query->free_cb != NULL)
        query->free_cb(query->data);
    else
        ss_free(query->data);

    ss_free(query);
}

static struct sockaddr *
choose_ipv4_first(struct resolv_query *query)
{
    for (int i = 0; i < query->response_count; i++)
        if (query->responses[i]->sa_family == AF_INET) {
            return query->responses[i];
        }

    return choose_any(query);
}

static struct sockaddr *
choose_ipv6_first(struct resolv_query *query)
{
    for (int i = 0; i < query->response_count; i++)
        if (query->responses[i]->sa_family == AF_INET6) {
            return query->responses[i];
        }

    return choose_any(query);
}

static struct sockaddr *
choose_any(struct resolv_query *query)
{
    if (query->response_count >= 1) {
        return query->responses[0];
    }

    return NULL;
}

static inline int
all_requests_are_null(struct resolv_query *query)
{
    int result = 1;

    for (int i = 0; i < sizeof(query->requests) / sizeof(query->requests[0]);
         i++)
        result = result && query->requests[i] == 0;

    return result;
}

/*
 *  DNS timeout callback
 */
static void
resolv_timeout_cb(struct ev_loop *loop, struct ev_timer *w, int revents)
{
    struct resolv_ctx *ctx = cork_container_of(w, struct resolv_ctx, tw);

    ares_process_fd(ctx->channel, ARES_SOCKET_BAD, ARES_SOCKET_BAD);

    reset_timer();
}

static void
reset_timer()
{
    struct timeval tvout;
    struct timeval *tv = ares_timeout(default_ctx.channel, NULL, &tvout);
    if (tv == NULL) {
        return;
    }
    float repeat = tv->tv_sec + tv->tv_usec / 1000000. + 1e-9;
    ev_timer_set(&default_ctx.tw, repeat, repeat);
    ev_timer_again(default_loop, &default_ctx.tw);
}

/*
 * Handle c-ares events
 */
static void
resolv_sock_state_cb(void *data, int s, int read, int write)
{
    struct resolv_ctx *ctx = (struct resolv_ctx *)data;
    int io_active          = ev_is_active(&ctx->io);

    if (read || write) {
        if (io_active && ctx->io.fd != s) {
            ev_io_stop(default_loop, &ctx->io);
        }
        ev_io_set(&ctx->io, s, (read ? EV_READ : 0) | (write ? EV_WRITE : 0));
        ev_io_start(default_loop, &ctx->io);
    } else {
        ev_io_stop(default_loop, &ctx->io);
        ev_io_set(&ctx->io, -1, 0);
    }
}
