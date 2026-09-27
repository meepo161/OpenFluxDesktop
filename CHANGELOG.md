# Changelog

All notable changes to OpenFluxDesktop. Format loosely follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [2.0.1] - 2026-09-27

### Fixed

- Settings → Core → "Выбрать…" filtered the file dialog to `*.exe`
  unconditionally, so on macOS/Linux (where the core binary has no
  extension) it showed nothing and the picker was unusable; the path
  field still took a manually typed/pasted path. Bumps `shared` to
  [OpenFluxClientShared#2](https://github.com/p1neappleXpress/OpenFluxClientShared/pull/2).

## [2.0.0] - 2026-09-27

First release of this app. Replaces the previous independent desktop
client (`tech.p1neapplexpress.openfluxdesktop`, preserved at the
[`legacy-native-app`](../../tree/legacy-native-app) tag) with the Compose
Multiplatform app originally built by [@meepo161](https://github.com/meepo161)
in [OpenFluxClient](https://github.com/meepo161/OpenFluxClient), moved here
with his agreement.

### Added

- Windows/macOS/Linux: full tunnel (Wintun on Windows) or SOCKS5/HTTP
  proxy.
- Multi-transport sessions with automatic failover and priority-based
  switching (including `direct`).
- AES-256-GCM session encryption.
- SmartCaptcha and login handling in a built-in browser (KCEF), including
  a transport check on the exit node, passed through the tunnel from its
  own address.
- A node-deployment wizard: install an exit node on your own VPS over SSH
  from the app.
- `shared/` and `OpenFlux/` as git submodules ([OpenFluxClientShared](https://github.com/p1neappleXpress/OpenFluxClientShared)
  and the [OpenFlux](https://github.com/p1neappleXpress/OpenFlux) core), so
  this app always builds against one pinned, single copy of each instead of
  a vendored one; `prepareCore`'s fallback source now points at that core
  repository instead of a third-party fork.
- `.github/workflows/release.yml`: a `v*` tag builds Windows/Linux/macOS
  packages and publishes them here.

### Credits

- [@meepo161](https://github.com/meepo161) — this app's UI and logic.
- [@p1neappleXpress](https://github.com/p1neappleXpress) — the OpenFlux
  core it bundles.
- [@damnurmum](https://github.com/damnurmum) — `openflux://` links/QR codes
  and the cups.online transport in the core, which this app's share and
  scan screens build on.
