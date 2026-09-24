<div align="center">

# 🌐 BitTorrent

**A peer-to-peer file sharing system built from raw TCP sockets — a tracker, a swarm of peers, and a JSON wire protocol. No networking framework involved.**

[![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Networking](https://img.shields.io/badge/sockets-java.net-4B8BBE?style=flat-square)](https://docs.oracle.com/javase/tutorial/networking/sockets/)
[![Serialization](https://img.shields.io/badge/wire%20format-Gson-red?style=flat-square)](https://github.com/google/gson)
[![Integrity](https://img.shields.io/badge/integrity-MD5-9C27B0?style=flat-square)](https://en.wikipedia.org/wiki/MD5)

</div>

---

## Overview

Two programs that together form a working file-sharing swarm:

- a **tracker** that knows which peers are online and which files each of them holds
- a **peer** that shares a local folder, asks the tracker where a file lives, and then downloads it **directly from another peer** — the tracker never touches file data

Everything runs on hand-managed `Socket` and `ServerSocket` connections with a
thread per peer. Messages are `Message` objects serialized to JSON over the
wire, and every completed download is verified against an MD5 hash the tracker
supplied before the transfer started.

## How a download works

```
   peer A                 tracker                  peer B
     │                       │                       │
     │──── who has X? ──────►│                       │
     │                       │  looks up its index   │
     │◄─── B:port + md5 ─────│                       │
     │                       │                       │
     │════════ download X ══════════════════════════►│
     │◄═══════ file bytes ═══════════════════════════│
     │                       │                       │
     │  verify md5 ✓         │                       │
     │──── report transfer ─►│                       │
```

The tracker is a **directory, not a relay**. It answers "who has this file and
what should it hash to," then steps out of the way. If two peers advertise the
same filename with different hashes, the request is rejected as ambiguous rather
than serving a file that might be the wrong one.

## Design notes

| | |
|---|---|
| 🔌 **Raw sockets, thread-per-connection** | `ListenerThread` accepts, `PeerConnectionThread` / `TorrentP2PThread` handle one conversation each. No Netty, no executor framework — the concurrency is explicit. |
| 📨 **Typed JSON protocol** | A single `Message` type with an enum tag (`command`, `response`, `file_request`, `download_request`) and a free-form body map, serialized by Gson. One envelope carries every interaction. |
| 🔐 **Integrity checking** | The tracker returns the expected MD5 alongside the peer address. A download that hashes differently is reported as corrupted rather than silently accepted. |
| ⚖️ **Ambiguity is an error** | Same filename, different hashes across peers ⇒ `Multiple hashes found!`. The system refuses to guess. |
| 🧩 **Shared `common/` module** | Both executables compile against the same models and utilities, so the protocol cannot drift between the two sides. |
| 🖥️ **Regex-driven CLIs** | Commands are an `enum` implementing a `CLICommands` interface, each carrying its own named-group regex — parsing and dispatch stay in one place. |

## Build

```bash
./build_tracker.sh   # → tracker.jar
./build_peer.sh      # → peer.jar
```

Each script compiles its own module plus `common/`, unpacks the Gson dependency
into the output, and produces a self-contained runnable jar.

## Run

**1 — Start the tracker**

```bash
java -jar tracker.jar 8080
```

**2 — Start one or more peers**, each with its own shared folder:

```bash
java -jar peer.jar 127.0.0.1:9001 127.0.0.1:8080 ./shared_a
java -jar peer.jar 127.0.0.1:9002 127.0.0.1:8080 ./shared_b
```

```
java -jar peer.jar <self-address:port> <tracker-address:port> <shared-folder>
```

### Peer commands

| Command | Does |
|---|---|
| `list` | List the files in your shared folder with their hashes |
| `download <filename>` | Ask the tracker who has it, then fetch it peer-to-peer |
| `exit` | Disconnect and shut down |

### Tracker commands

| Command | Does |
|---|---|
| `list_peers` | Every peer currently connected |
| `list_files <ip>:<port>` | What a given peer is sharing |
| `get_sends <ip>:<port>` | Transfers that peer has served |
| `get_receives <ip>:<port>` | Transfers that peer has pulled |
| `refresh_files` | Re-scan every peer's advertised file list |
| `reset_connections` | Drop and rebuild peer connections |
| `exit` | Shut down |

## Project layout

```
.
├── common/                 # Shared by both executables
│   ├── models/
│   │   ├── Message.java            # The wire envelope
│   │   ├── ConnectionThread.java   # Base socket conversation
│   │   └── CLICommands.java        # Regex command contract
│   └── utils/
│       ├── FileUtils.java          # Shared-folder scanning & listing
│       ├── JSONUtils.java          # Gson wrapper
│       └── MD5Hash.java            # Integrity hashing
├── tracker/                # The directory service
│   ├── app/                        # Listener + per-peer threads
│   └── controllers/                # CLI and connection handling
├── peer/                   # The swarm client
│   ├── app/                        # P2P listener, tracker link, transfer threads
│   └── controllers/                # CLI and tracker protocol
├── lib/gson-2.13.1.jar
└── build_*.sh
```

## Built for

Advanced Programming — Sharif University of Technology. The brief was to
implement the tracker/peer split of BitTorrent's architecture directly on
sockets, without a networking library doing the work.
