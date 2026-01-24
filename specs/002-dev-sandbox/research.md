# Development Sandbox - Research

## Problem Statement

Component development requires rapid iteration. The traditional edit-save-reload cycle is too slow. Need a system where REPL evaluation immediately updates the browser.

## Requirements

### Functional Requirements

1. Browser displays hiccup rendered from REPL
2. Multiple browser tabs/devices update simultaneously
3. SSE connection persists across REPL reloads
4. Preview helpers for common operations

### Non-Functional Requirements

- Latency: Sub-100ms REPL-to-browser updates
- Reliability: Connection survives code reloads
- Simplicity: Minimal boilerplate for developers

## Options Considered

### Option A: WebSocket-based

**Description:** Use WebSockets for bidirectional communication

**Pros:**
- Full duplex communication
- Lower overhead than SSE

**Cons:**
- More complex server setup
- Overkill for server-push only

### Option B: Server-Sent Events (SSE)

**Description:** Use Datastar's native SSE transport

**Pros:**
- Native Datastar integration
- Simple server implementation
- Automatic reconnection

**Cons:**
- Unidirectional only

## Recommendation

**Option B** - SSE via Datastar. The Datastar/twk ecosystem already provides SSE support, and we only need server-to-client push for preview updates.

## References

- Datastar documentation
- kaiin demo pattern
- browser-sync for multi-device inspiration
