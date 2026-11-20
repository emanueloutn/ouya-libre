# OuyaLibre 0.9.1 verification

Current update: 19 Drive parsing/access/range checks and 7 real native playback checks passed on OUYA. A publicly linked RGUKT MP4 was used without session or credentials; native playback advanced, displayed 195 frames, decoded 768 audio buffers and sought successfully. IPTV list count was preserved. No test media or user data are included. Search coverage and Drive folder limitations are documented in RELEASE-NOTES.md.

## Retained 0.8.2 verification

Corrects the 0.8.1 subtitle-surface visual regression and adds bounded live-HLS EOF recovery.

The baseline reproduction showed a black screen while the native engine had
already displayed 468 frames. Clearing only the subtitle surface exposed the
running Discovery picture. In the corrected test build, image was visible before
any manual intervention. The corrected signed app was installed over 0.8.1, and
a real 1080p video was opened through the normal history/source-selection flow.
Different visible scenes were captured and the user confirmed visible playback
without pressing Back. Private channel/movie captures are not distributed.

On a real OUYA, a local HTTP server inside the instrumentation process served
an actual encoded TS fragment through a deliberately finite HLS playlist. No
Internet, PC media server, user playlist modifications or catalog fixtures were required.

- Seven checks: real TS fixture, recovery notice after EOF, initial session ends,
  controls visible, two reconnections, bounded attempts, usable final state.
- Five checks in a separate run: real fixture, recovery notice, initial end,
  visible controls, and no reconnection after leaving the player.

These checks establish the recovery lifecycle, not uninterrupted reception from
every IPTV server. Visual checks above cover the confirmed subtitle-overlay bug,
not every possible decoder/source failure. See HARDWARE-OUYA.md
for previous hardware tests and their limitations.
