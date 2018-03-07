# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_VERBOSE_MAKEFILE ON)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

include_directories(src/main/jni/shadowsocks-libev/libcork/include)
include_directories(src/main/jni/shadowsocks-libev/libipset/include)
include_directories(src/main/jni/shadowsocks-libev/libbloom/murmur2)
include_directories(src/main/jni/shadowsocks-libev/libbloom)

add_library( # Sets the name of the library.
             libev
             # Sets the library as a shared library.
             STATIC
             # Provides a relative path to your source file(s).
             src/main/jni/libev/ev.c
             src/main/jni/libev/event.c )

set_target_properties( libev
                       PROPERTIES COMPILE_FLAGS
                       "-Wno-unused-function \
                        -Wno-unused-variable \
                        -Wno-unused-value " )

add_library( libsodium
             STATIC
             src/main/jni/libsodium/src/libsodium/crypto_aead/chacha20poly1305/sodium/aead_chacha20poly1305.c
             src/main/jni/libsodium/src/libsodium/crypto_aead/xchacha20poly1305/sodium/aead_xchacha20poly1305.c
             src/main/jni/libsodium/src/libsodium/crypto_core/curve25519/ref10/curve25519_ref10.c
             src/main/jni/libsodium/src/libsodium/crypto_core/hchacha20/core_hchacha20.c
             src/main/jni/libsodium/src/libsodium/crypto_core/salsa/ref/core_salsa_ref.c
             src/main/jni/libsodium/src/libsodium/crypto_generichash/blake2b/ref/blake2b-compress-ref.c
             src/main/jni/libsodium/src/libsodium/crypto_generichash/blake2b/ref/blake2b-ref.c
             src/main/jni/libsodium/src/libsodium/crypto_generichash/blake2b/ref/generichash_blake2b.c
             src/main/jni/libsodium/src/libsodium/crypto_onetimeauth/poly1305/onetimeauth_poly1305.c
             src/main/jni/libsodium/src/libsodium/crypto_onetimeauth/poly1305/donna/poly1305_donna.c
             src/main/jni/libsodium/src/libsodium/crypto_pwhash/crypto_pwhash.c
             src/main/jni/libsodium/src/libsodium/crypto_pwhash/argon2/argon2-core.c
             src/main/jni/libsodium/src/libsodium/crypto_pwhash/argon2/argon2.c
             src/main/jni/libsodium/src/libsodium/crypto_pwhash/argon2/argon2-encoding.c
             src/main/jni/libsodium/src/libsodium/crypto_pwhash/argon2/argon2-fill-block-ref.c
             src/main/jni/libsodium/src/libsodium/crypto_pwhash/argon2/blake2b-long.c
             src/main/jni/libsodium/src/libsodium/crypto_pwhash/argon2/pwhash_argon2i.c
             src/main/jni/libsodium/src/libsodium/crypto_scalarmult/curve25519/scalarmult_curve25519.c
             src/main/jni/libsodium/src/libsodium/crypto_scalarmult/curve25519/ref10/x25519_ref10.c
             src/main/jni/libsodium/src/libsodium/crypto_stream/chacha20/stream_chacha20.c
             src/main/jni/libsodium/src/libsodium/crypto_stream/chacha20/ref/chacha20_ref.c
             src/main/jni/libsodium/src/libsodium/crypto_stream/salsa20/stream_salsa20.c
             src/main/jni/libsodium/src/libsodium/crypto_stream/salsa20/ref/salsa20_ref.c
             src/main/jni/libsodium/src/libsodium/crypto_verify/sodium/verify.c
             src/main/jni/libsodium/src/libsodium/randombytes/randombytes.c
             src/main/jni/libsodium/src/libsodium/randombytes/sysrandom/randombytes_sysrandom.c
             src/main/jni/libsodium/src/libsodium/sodium/core.c
             src/main/jni/libsodium/src/libsodium/sodium/runtime.c
             src/main/jni/libsodium/src/libsodium/sodium/utils.c
             src/main/jni/libsodium/src/libsodium/sodium/version.c )

target_include_directories(libsodium PUBLIC src/main/jni/libsodium/src/libsodium/include)
target_include_directories(libsodium PUBLIC src/main/jni/libsodium/src/libsodium/include/sodium)

include_directories(src/main/jni/include/sodium)
include_directories(src/main/jni/include)

set_target_properties( libsodium
                       PROPERTIES COMPILE_FLAGS
                       "-DPACKAGE_NAME=\"libsodium\" -DPACKAGE_TARNAME=\"libsodium\" \
                        -DPACKAGE_VERSION=\"1.0.15\" -DPACKAGE_STRING=\"libsodium-1.0.15\" \
                        -DPACKAGE_BUGREPORT=\"https://github.com/jedisct1/libsodium/issues\" \
                        -DPACKAGE_URL=\"https://github.com/jedisct1/libsodium\" \
                        -DPACKAGE=\"libsodium\" -DVERSION=\"1.0.15\" \
                        -DHAVE_PTHREAD=1                  \
                        -DSTDC_HEADERS=1                  \
                        -DHAVE_SYS_TYPES_H=1              \
                        -DHAVE_SYS_STAT_H=1               \
                        -DHAVE_STDLIB_H=1                 \
                        -DHAVE_STRING_H=1                 \
                        -DHAVE_MEMORY_H=1                 \
                        -DHAVE_STRINGS_H=1                \
                        -DHAVE_INTTYPES_H=1               \
                        -DHAVE_STDINT_H=1                 \
                        -DHAVE_UNISTD_H=1                 \
                        -D__EXTENSIONS__=1                \
                        -D_ALL_SOURCE=1                   \
                        -D_GNU_SOURCE=1                   \
                        -D_POSIX_PTHREAD_SEMANTICS=1      \
                        -D_TANDEM_SOURCE=1                \
                        -DHAVE_DLFCN_H=1                  \
                        -DLT_OBJDIR=\".libs/\"            \
                        -DHAVE_SYS_MMAN_H=1               \
                        -DNATIVE_LITTLE_ENDIAN=1          \
                        -DASM_HIDE_SYMBOL=.hidden         \
                        -DHAVE_WEAK_SYMBOLS=1             \
                        -DHAVE_ATOMIC_OPS=1               \
                        -DHAVE_ARC4RANDOM=1               \
                        -DHAVE_ARC4RANDOM_BUF=1           \
                        -DHAVE_MMAP=1                     \
                        -DHAVE_MLOCK=1                    \
                        -DHAVE_MADVISE=1                  \
                        -DHAVE_MPROTECT=1                 \
                        -DHAVE_NANOSLEEP=1                \
                        -DHAVE_POSIX_MEMALIGN=1           \
                        -DHAVE_GETPID=1                   \
                        -DCONFIGURED=1" )

add_library( libcork
             STATIC
             src/main/jni/shadowsocks-libev/libcork/src/libcork/cli/commands.c
             src/main/jni/shadowsocks-libev/libcork/src/libcork/core/allocator.c
             src/main/jni/shadowsocks-libev/libcork/src/libcork/core/error.c
             src/main/jni/shadowsocks-libev/libcork/src/libcork/core/gc.c
             src/main/jni/shadowsocks-libev/libcork/src/libcork/core/hash.c
             src/main/jni/shadowsocks-libev/libcork/src/libcork/core/ip-address.c
             src/main/jni/shadowsocks-libev/libcork/src/libcork/core/mempool.c
             src/main/jni/shadowsocks-libev/libcork/src/libcork/core/timestamp.c
             src/main/jni/shadowsocks-libev/libcork/src/libcork/core/u128.c
             src/main/jni/shadowsocks-libev/libcork/src/libcork/core/version.c
             src/main/jni/shadowsocks-libev/libcork/src/libcork/ds/array.c
             src/main/jni/shadowsocks-libev/libcork/src/libcork/ds/bitset.c
             src/main/jni/shadowsocks-libev/libcork/src/libcork/ds/buffer.c
             src/main/jni/shadowsocks-libev/libcork/src/libcork/ds/dllist.c
             src/main/jni/shadowsocks-libev/libcork/src/libcork/ds/file-stream.c
             src/main/jni/shadowsocks-libev/libcork/src/libcork/ds/hash-table.c
             src/main/jni/shadowsocks-libev/libcork/src/libcork/ds/managed-buffer.c
             src/main/jni/shadowsocks-libev/libcork/src/libcork/ds/ring-buffer.c
             src/main/jni/shadowsocks-libev/libcork/src/libcork/ds/slice.c
             src/main/jni/shadowsocks-libev/libcork/src/libcork/posix/directory-walker.c
             src/main/jni/shadowsocks-libev/libcork/src/libcork/posix/env.c
             src/main/jni/shadowsocks-libev/libcork/src/libcork/posix/exec.c
             src/main/jni/shadowsocks-libev/libcork/src/libcork/posix/files.c
             src/main/jni/shadowsocks-libev/libcork/src/libcork/posix/process.c
             src/main/jni/shadowsocks-libev/libcork/src/libcork/posix/subprocess.c
             src/main/jni/shadowsocks-libev/libcork/src/libcork/pthreads/thread.c )

target_compile_definitions(libcork PUBLIC -DCORK_API=CORK_LOCAL)

add_library( libipset
             STATIC
             src/main/jni/shadowsocks-libev/libipset/src/libipset/general.c
             src/main/jni/shadowsocks-libev/libipset/src/libipset/bdd/assignments.c
             src/main/jni/shadowsocks-libev/libipset/src/libipset/bdd/basics.c
             src/main/jni/shadowsocks-libev/libipset/src/libipset/bdd/bdd-iterator.c
             src/main/jni/shadowsocks-libev/libipset/src/libipset/bdd/expanded.c
             src/main/jni/shadowsocks-libev/libipset/src/libipset/bdd/reachable.c
             src/main/jni/shadowsocks-libev/libipset/src/libipset/bdd/read.c
             src/main/jni/shadowsocks-libev/libipset/src/libipset/bdd/write.c
             src/main/jni/shadowsocks-libev/libipset/src/libipset/map/allocation.c
             src/main/jni/shadowsocks-libev/libipset/src/libipset/map/inspection.c
             src/main/jni/shadowsocks-libev/libipset/src/libipset/map/ipv4_map.c
             src/main/jni/shadowsocks-libev/libipset/src/libipset/map/ipv6_map.c
             src/main/jni/shadowsocks-libev/libipset/src/libipset/map/storage.c
             src/main/jni/shadowsocks-libev/libipset/src/libipset/set/allocation.c
             src/main/jni/shadowsocks-libev/libipset/src/libipset/set/inspection.c
             src/main/jni/shadowsocks-libev/libipset/src/libipset/set/ipv4_set.c
             src/main/jni/shadowsocks-libev/libipset/src/libipset/set/ipv6_set.c
             src/main/jni/shadowsocks-libev/libipset/src/libipset/set/iterator.c
             src/main/jni/shadowsocks-libev/libipset/src/libipset/set/storage.c )

add_library( libbloom
             STATIC
             src/main/jni/shadowsocks-libev/libbloom/bloom.c
             src/main/jni/shadowsocks-libev/libbloom/murmur2/MurmurHash2.c )

add_library( libcares
             STATIC
             src/main/jni/c-ares/ares__close_sockets.c
             src/main/jni/c-ares/ares__get_hostent.c
             src/main/jni/c-ares/ares__read_line.c
             src/main/jni/c-ares/ares__timeval.c
             src/main/jni/c-ares/ares_cancel.c
             src/main/jni/c-ares/ares_data.c
             src/main/jni/c-ares/ares_destroy.c
             src/main/jni/c-ares/ares_expand_name.c
             src/main/jni/c-ares/ares_expand_string.c
             src/main/jni/c-ares/ares_fds.c
             src/main/jni/c-ares/ares_free_hostent.c
             src/main/jni/c-ares/ares_free_string.c
             src/main/jni/c-ares/ares_getenv.c
             src/main/jni/c-ares/ares_gethostbyaddr.c
             src/main/jni/c-ares/ares_gethostbyname.c
             src/main/jni/c-ares/ares_getnameinfo.c
             src/main/jni/c-ares/ares_getsock.c
             src/main/jni/c-ares/ares_init.c
             src/main/jni/c-ares/ares_library_init.c
             src/main/jni/c-ares/ares_llist.c
             src/main/jni/c-ares/ares_mkquery.c
             src/main/jni/c-ares/ares_nowarn.c
             src/main/jni/c-ares/ares_options.c
             src/main/jni/c-ares/ares_parse_a_reply.c
             src/main/jni/c-ares/ares_parse_aaaa_reply.c
             src/main/jni/c-ares/ares_parse_mx_reply.c
             src/main/jni/c-ares/ares_parse_ns_reply.c
             src/main/jni/c-ares/ares_parse_ptr_reply.c
             src/main/jni/c-ares/ares_parse_srv_reply.c
             src/main/jni/c-ares/ares_parse_txt_reply.c
             src/main/jni/c-ares/ares_platform.c
             src/main/jni/c-ares/ares_process.c
             src/main/jni/c-ares/ares_query.c
             src/main/jni/c-ares/ares_search.c
             src/main/jni/c-ares/ares_send.c
             src/main/jni/c-ares/ares_strcasecmp.c
             src/main/jni/c-ares/ares_strdup.c
             src/main/jni/c-ares/ares_strerror.c
             src/main/jni/c-ares/ares_timeout.c
             src/main/jni/c-ares/ares_version.c
             src/main/jni/c-ares/ares_writev.c
             src/main/jni/c-ares/bitncmp.c
             src/main/jni/c-ares/inet_net_pton.c
             src/main/jni/c-ares/inet_ntop.c
             src/main/jni/c-ares/windows_port.c )

target_include_directories(libcares PUBLIC src/main/jni/c-ares)
set_target_properties( libcares
                       PROPERTIES COMPILE_FLAGS
                       "-DHAVE_CONFIG_H \
                        -Wall -Werror \
                        -Wno-unused-function \
                        -Wno-unused-variable" )

add_library( mbedtls
             STATIC
             src/main/jni/mbedtls/library/aes.c
             src/main/jni/mbedtls/library/aesni.c
             src/main/jni/mbedtls/library/arc4.c
             src/main/jni/mbedtls/library/asn1parse.c
             src/main/jni/mbedtls/library/asn1write.c
             src/main/jni/mbedtls/library/base64.c
             src/main/jni/mbedtls/library/bignum.c
             src/main/jni/mbedtls/library/blowfish.c
             src/main/jni/mbedtls/library/camellia.c
             src/main/jni/mbedtls/library/ccm.c
             src/main/jni/mbedtls/library/certs.c
             src/main/jni/mbedtls/library/cipher.c
             src/main/jni/mbedtls/library/cipher_wrap.c
             src/main/jni/mbedtls/library/cmac.c
             src/main/jni/mbedtls/library/ctr_drbg.c
             src/main/jni/mbedtls/library/debug.c
             src/main/jni/mbedtls/library/des.c
             src/main/jni/mbedtls/library/dhm.c
             src/main/jni/mbedtls/library/ecdh.c
             src/main/jni/mbedtls/library/ecdsa.c
             src/main/jni/mbedtls/library/ecjpake.c
             src/main/jni/mbedtls/library/ecp.c
             src/main/jni/mbedtls/library/ecp_curves.c
             src/main/jni/mbedtls/library/entropy.c
             src/main/jni/mbedtls/library/entropy_poll.c
             src/main/jni/mbedtls/library/error.c
             src/main/jni/mbedtls/library/gcm.c
             src/main/jni/mbedtls/library/havege.c
             src/main/jni/mbedtls/library/hmac_drbg.c
             src/main/jni/mbedtls/library/md2.c
             src/main/jni/mbedtls/library/md4.c
             src/main/jni/mbedtls/library/md5.c
             src/main/jni/mbedtls/library/md.c
             src/main/jni/mbedtls/library/md_wrap.c
             src/main/jni/mbedtls/library/memory_buffer_alloc.c
             src/main/jni/mbedtls/library/net_sockets.c
             src/main/jni/mbedtls/library/oid.c
             src/main/jni/mbedtls/library/padlock.c
             src/main/jni/mbedtls/library/pem.c
             src/main/jni/mbedtls/library/pk.c
             src/main/jni/mbedtls/library/pkcs11.c
             src/main/jni/mbedtls/library/pkcs12.c
             src/main/jni/mbedtls/library/pkcs5.c
             src/main/jni/mbedtls/library/pkparse.c
             src/main/jni/mbedtls/library/pk_wrap.c
             src/main/jni/mbedtls/library/pkwrite.c
             src/main/jni/mbedtls/library/platform.c
             src/main/jni/mbedtls/library/ripemd160.c
             src/main/jni/mbedtls/library/rsa.c
             src/main/jni/mbedtls/library/sha1.c
             src/main/jni/mbedtls/library/sha256.c
             src/main/jni/mbedtls/library/sha512.c
             src/main/jni/mbedtls/library/ssl_cache.c
             src/main/jni/mbedtls/library/ssl_ciphersuites.c
             src/main/jni/mbedtls/library/ssl_cli.c
             src/main/jni/mbedtls/library/ssl_cookie.c
             src/main/jni/mbedtls/library/ssl_srv.c
             src/main/jni/mbedtls/library/ssl_ticket.c
             src/main/jni/mbedtls/library/ssl_tls.c
             src/main/jni/mbedtls/library/threading.c
             src/main/jni/mbedtls/library/timing.c
             src/main/jni/mbedtls/library/version.c
             src/main/jni/mbedtls/library/version_features.c
             src/main/jni/mbedtls/library/x509.c
             src/main/jni/mbedtls/library/x509_create.c
             src/main/jni/mbedtls/library/x509_crl.c
             src/main/jni/mbedtls/library/x509_crt.c
             src/main/jni/mbedtls/library/x509_csr.c
             src/main/jni/mbedtls/library/x509write_crt.c
             src/main/jni/mbedtls/library/x509write_csr.c
             src/main/jni/mbedtls/library/xtea.c )

target_include_directories(mbedtls PUBLIC src/main/jni/mbedtls/include)
target_compile_definitions(mbedtls
                           PUBLIC
                           -DHAVE_LIBEV_EV_H)

add_library( libpcre
             STATIC
             src/main/jni/pcre/dist/pcre_byte_order.c
             src/main/jni/pcre/dist/pcre_compile.c
             src/main/jni/pcre/dist/pcre_config.c
             src/main/jni/pcre/dist/pcre_dfa_exec.c
             src/main/jni/pcre/dist/pcre_exec.c
             src/main/jni/pcre/dist/pcre_fullinfo.c
             src/main/jni/pcre/dist/pcre_get.c
             src/main/jni/pcre/dist/pcre_globals.c
             src/main/jni/pcre/dist/pcre_jit_compile.c
             src/main/jni/pcre/dist/pcre_maketables.c
             src/main/jni/pcre/dist/pcre_newline.c
             src/main/jni/pcre/dist/pcre_ord2utf8.c
             src/main/jni/pcre/dist/pcre_refcount.c
             src/main/jni/pcre/dist/pcre_string_utils.c
             src/main/jni/pcre/dist/pcre_study.c
             src/main/jni/pcre/dist/pcre_tables.c
             src/main/jni/pcre/dist/pcre_ucd.c
             src/main/jni/pcre/dist/pcre_valid_utf8.c
             src/main/jni/pcre/dist/pcre_version.c
             src/main/jni/pcre/dist/pcre_xclass.c
             src/main/jni/patch/pcre/pcre_chartables.c)

target_include_directories(libpcre PUBLIC src/main/jni/pcre/dist)
target_include_directories(libpcre PUBLIC src/main/jni/pcre)
target_compile_definitions(libpcre
                           PUBLIC
                           -DHAVE_CONFIG_H)

add_library( ss-server
             SHARED
             src/main/jni/shadowsocks-libev/src/ppbloom.c
             src/main/jni/shadowsocks-libev/src/utils.c
             src/main/jni/shadowsocks-libev/src/jconf.c
             src/main/jni/shadowsocks-libev/src/json.c
             src/main/jni/shadowsocks-libev/src/netutils.c
             src/main/jni/shadowsocks-libev/src/udprelay.c
             src/main/jni/shadowsocks-libev/src/cache.c
             src/main/jni/shadowsocks-libev/src/resolv.c
             src/main/jni/shadowsocks-libev/src/server.c
             src/main/jni/shadowsocks-libev/src/crypto.c
             src/main/jni/shadowsocks-libev/src/aead.c
             src/main/jni/shadowsocks-libev/src/stream.c
             src/main/jni/shadowsocks-libev/src/base64.c
             src/main/jni/shadowsocks-libev/src/plugin.c
             src/main/jni/shadowsocks-libev/src/http.c
             src/main/jni/shadowsocks-libev/src/tls.c
             src/main/jni/shadowsocks-libev/src/acl.c
             src/main/jni/shadowsocks-libev/src/rule.c
             src/main/jni/smartsocks-jni/ssproxyserver.c
             src/main/jni/smartsocks-jni/vpninterface.c )

target_include_directories(ss-server PUBLIC src/main/jni/c-ares)
target_include_directories(ss-server PUBLIC src/main/jni)
target_include_directories(ss-server PUBLIC src/main/jni/pcre)
target_compile_definitions(ss-server
                           PUBLIC
                           -DMODULE_REMOTE
                           -DHAVE_CONFIG_H
                           -DHAVE_NET_IF_H
                           -DHAVE_SYS_IOCTL_H
                           -D__linux__
                           -DHAVE_PCRE_H
                           -DCONNECT_IN_PROGRESS=EINPROGRESS)
set_target_properties( ss-server
                       PROPERTIES COMPILE_FLAGS
                       "-g -O0" )

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
                       ss-server

                       # Links the target library to the log library
                       # included in the NDK.
                       libev
                       libcork
                       libipset
                       libbloom
                       libsodium
                       libcares
                       mbedtls
                       libpcre
                       ${log-lib} )