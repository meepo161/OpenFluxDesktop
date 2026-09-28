# Changelog

All notable changes to OpenFluxDesktop. Format loosely follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [2.7.0] - 2026-09-28 (meepo161/OpenFluxDesktop)

### Added

- «Аккаунты»: sign in once to Yandex or Mail.ru in the built-in browser; the
  app keeps only the session (owner-only `accounts.json`, never in backups,
  logs, QR codes or links) and shows each service's status — signed in (with
  the login and when it was last checked), expired, or asking for a check.
  Sessions are rechecked every 30 minutes without the browser; «Войти заново»
  is one button, on the card and in a banner on Home when a profile needs it.
- «Создать документ» makes the channel's document with the saved account, no
  `cookies.txt` export: Yandex — a document on Disk with editing by link;
  Mail.ru — a document in Cloud (`/openflux`), published and switched to
  editing by link, then checked the way the core opens it (anonymous
  `r7/edit`). The same button sits under a document field in the profile
  editor; the node wizard uses the saved Yandex account too.
- The Yandex sign-in goes into the core's cookie store before it connects, and
  to your own node (from the wizard) over the tunnel, again after a fresh
  sign-in. Someone else's node gets it only by hand, after a warning. The
  Mail.ru account is never put into the core: its transport opens the
  document anonymously and Cloud refuses that request with the account's
  cookies.
- Cups.online: «Сгенерировать комнаты» in the profile editor opens four rooms
  without a node; the node given the same string joins them.

### Fixed

- Creating a Yandex document failed with «Failed to fetch»: Disk now
  redirects `/editnew` to `docs.yandex.ru`, where the browser refused a
  readable (CORS) request. Affected the node wizard as well.

## [2.6.0] - 2026-09-28 (meepo161/OpenFluxDesktop)

The first release of the meepo161 fork: the app, its update check, the core
submodule and the node wizard's core come from the fork's repositories
(meepo161/OpenFlux, meepo161/OpenFluxClientShared, meepo161/OpenFluxDesktop).

### Added

- «Своя нода»: step 1 asks whose core the server gets — the fork's
  (`meepo161/OpenFlux`, default) or the original (`p1neappleXpress/OpenFlux`);
  the node's auto-update then follows that repository.
- Settings → Core: besides the bundled core and a file of your own, the
  newest `v*` release of the fork's or the original core, downloaded for
  this OS and checked against the release's `SHA256SUMS.txt`. The node
  wizard always runs the bundled core.

### From p1neappleXpress/OpenFluxDesktop (not yet released there)

### Added

- «Своя нода»: a new channel is no longer Yandex-only. Step 2 picks any mix
  of a Yandex document (Volga), a Mail.ru public document and cups.online
  rooms (created automatically), with direct always on as the backup; the
  link and the saved profile carry all of them. The node gets the Yandex
  sign-in only when the channel has a Yandex document.
- «Автообновление ядра» on the plan step (on by default): the server's
  `openflux-node-update.timer` checks the newest `node-v*` release every
  6 hours, verifies it against the release's `node-install.sh` and
  `SHA256SUMS`, restarts the channels and rolls back if one does not stay
  up.
- Bumps `OpenFlux` to [`1394680`](https://github.com/p1neappleXpress/OpenFlux/commit/1394680027f7d2cf448f17267c13b9f5a44b859b)
  and `shared` to [`abf9c97`](https://github.com/p1neappleXpress/OpenFluxClientShared/commit/abf9c97a75f1fac9e09cbcc81aa74a754ef76aa3).

## [2.0.2] - 2026-09-27

### Fixed

- An exit node deployed by the node wizard never actually connected: a
  `.conf`-only `Role = exit` (every node-wizard deployment) left the core's
  internal exit/client flag stuck at its pre-config value, so the exit
  never answered the handshake and crash-looped instead. Bumps `OpenFlux`
  to [`e8f735a`](https://github.com/p1neappleXpress/OpenFlux/commit/e8f735a98c1ba9e091956416fde5c5ef92d3cd66).
- The startup log always printed `Transport: yandex` for session/multi-transport
  profiles regardless of which transports were actually configured (a stale
  flag default, not a functional bug — the correct transports ran either
  way). Now prints the actual list, e.g. `Transport: boards, direct (session)`.
- Cups.online profiles with no room codes couldn't be saved or connected;
  the node generates its own rooms, so an empty value is valid for this
  transport only.
- An exit node always listened for Direct (TCP on `0.0.0.0:<port>`) and put
  it into the clients' link and QR, even when the profile had no Direct
  transport: choosing two transports gave clients three. It now listens
  only when the profile has Direct, at that transport's priority.
  [OpenFluxClientShared#6](https://github.com/p1neappleXpress/OpenFluxClientShared/pull/6).
- A cups.online exit started without room codes left cups.online out of its
  link and QR, so clients had no rooms to join; the link now carries the
  rooms the node created, and is printed again if they change. Bumps
  `OpenFlux` to [OpenFlux#123](https://github.com/p1neappleXpress/OpenFlux/pull/123).
- After switching profiles the "через …" badge could keep the previous
  profile's carrier (for good if the new profile is classic) and an exit
  its old QR: a stopped core's last status and output no longer overwrite
  the new state.

### Added

- Settings → Ядро OpenFlux: a core log-level picker (Выкл / -d / -dd /
  -ddd, the core's `--debug=N`) in place of the "Подробный журнал ядра"
  switch, which could only turn -dd on. -dd is the level that shows
  sessions, handshakes and the encryption (KDF) context; -ddd adds packet
  hexdumps. Bumps `shared` to
  [OpenFluxClientShared#5](https://github.com/p1neappleXpress/OpenFluxClientShared/pull/5).
- Node-wizard deployment logging: every SSH/RPC call and the wizard's own
  step narration now goes to the Logs tab, so a stuck deployment is
  diagnosable without a debugger.
- Home → Подключение: a "Сейчас через" row for profiles with several
  transports, and carriers named as in the app ("Board 2", not `boards-2`)
  there and in the "через …" badge.

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
