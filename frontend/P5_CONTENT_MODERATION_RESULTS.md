# P5 — Content Moderation (FR-A07 + FR-A14)

**Status:** implemented; `assembleDebug` and `lintDebug` pass. `verify-skeleton.mjs` is still red on
the repository baseline, with no P5 structural failure.

## Scope

One screen, two lanes in `content_moderationFragment`:

- **Cờ nội dung (A07):** 3 KPI tiles (Cần xử lý / Mức cao / Đã xử lý hôm nay), filters by
  target type / severity / status, flag cards with target, type, relative time, severity, source
  (`RULE`/`AI`), confidence, reason, evidence, `Chưa quét AI`, and Confirm / Dismiss / Rescan.
- **Đánh giá bị báo cáo (A14):** review list with `HELD` / `FLAGGED` / `REPORTED` source badges and
  severity; row click opens a bottom sheet with content, shortened author, account code, event title
  plus event code, spam/toxicity scores and Hide / Keep / Request edit.
- Both lanes: processed rows leave the open queue with the default RecyclerView removal fade,
  followed by a Snackbar with undo and the audit note `Đã ghi nhật ký thao tác`.
- States: skeleton, empty with `Xem đã xử lý`, error + retry.

Dismiss, Hide and Request edit require a reason code where applicable and a note. Keep requires a
note. All runtime copy lives in `strings_huucanh.xml` under `hc_mod_*` / `hc_a14_*`.

## Main files

| File | Role |
|---|---|
| `ui/admin/event/ContentModerationFragment.java` | screen wiring, tabs, filters, states, dialogs, Snackbar |
| `ui/admin/event/ContentModerationViewModel.java` | filters, KPI counts, actions, undo stack, action events |
| `ui/admin/event/ModerationUiModel.java` | A07 row model |
| `ui/admin/event/SampleModerationData.java` | deterministic A07 sample queue |
| `ui/admin/event/ModerationReasonDialog.java` | reason-code + required-note dialog |
| `ui/admin/event/ModerationReviewSheet.java` | A14 detail bottom sheet |
| `ui/common/adapter/ModerationFlagAdapter.java` | A07 list adapter with DiffUtil |
| `ui/common/adapter/ReviewAdapter.java` | shared A14 adapter; moderation mode adds severity + row click |
| `ui/common/view/SeverityChip.java` | shared severity → `StatusChip` mapping |
| `res/layout/fragment_event_content_moderation.xml` | screen layout |
| `res/layout/item_moderation_flag.xml` | A07 card |
| `res/layout/dialog_moderation_reason.xml` | reason dialog |
| `res/layout/dialog_moderation_review.xml` | A14 sheet |
| `res/values/strings_huucanh.xml` | P5 strings |

`fragment_event_content_moderation_v2.xml` remains as a thin `FrameLayout` alias around the
implemented layout; the abandoned untracked v2 file previously broke AAPT.

## Verification

```text
ANDROID_HOME=/home/couterit/Work/01.Tool/AndroidStudio \
  /home/couterit/.gradle/wrapper/dists/gradle-9.6.0-bin/42k10rwplmzkhuboz9kdazi7s/gradle-9.6.0/bin/gradle \
  :app:assembleDebug --console=plain
→ BUILD SUCCESSFUL

ANDROID_HOME=/home/couterit/Work/01.Tool/AndroidStudio \
  /home/couterit/.gradle/wrapper/dists/gradle-9.6.0-bin/42k10rwplmzkhuboz9kdazi7s/gradle-9.6.0/bin/gradle \
  :app:lintDebug --console=plain
→ BUILD SUCCESSFUL (warnings only)
```

`node scripts/verify-skeleton.mjs` exits 1 with 92 errors. The P5-relevant structural checks are OK:
nav graph and destinations resolve, layouts/classes are unique, 23/23 APIs, 23/23 repositories,
95/95 DTOs, manifest and themes resolve, and no BOMs were found. The XML-validity check cannot run on
Linux because the script shells out to PowerShell (`spawnSync powershell ENOENT`); an independent
Python parse of all 255 XML files returned 0 invalid files. The remaining errors are the legacy
"missing `// TODO:`" and "class not public" checks that already fail for the implemented screens;
10 of those entries are the new P5 files and match the same baseline pattern.

## Pre-existing build blockers fixed to unblock verification

These were already broken in HEAD and are not part of the P5 feature:

- `EventFormFragment`: corrected the `MaterialAlertDialogBuilder` import and replaced nonexistent
  `Chip.setStyle(...)` calls with `ContextThemeWrapper`.
- `EventDetailFragment`: added the missing `WriteReviewFragment` import.
- `SimpleBarChartView`: changed the label paint to `TextPaint` for `TextUtils.ellipsize`.
- `nav_organizer.xml`: corrected `android:nullable` to `app:nullable`.

## Notes / risks

- Data is local sample data; no API wiring was added.
- The error state is wired and retryable, but the sample loader never throws, so it is only
  reachable when a future data source sets `error = true`.
- No new dependencies, navigation destinations, or architecture changes were introduced.
- Work is uncommitted.
