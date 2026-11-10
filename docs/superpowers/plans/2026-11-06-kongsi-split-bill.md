# Kongsi Split Bill Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or
> superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Kongsi lets one person photograph a receipt (or type it in), check the items, say who had what, and send
each person what they owe over WhatsApp.

**Architecture:** One feature module pair, `:feature:bill:{api,impl}`, provides the app's Home (`BillHome`, added to
`homeScreens` in `AppNavHost.kt`; the starter's `PlaceholderHomeScreen` stays as the fallback). Bills are stored on the device in Room. There is no backend and no sign-in in v1. The receipt is read on the device by ML Kit
Text Recognition. The split calculation is a pure Kotlin function with no Android types, so most of the logic is
covered by plain unit tests. WhatsApp is reached through standard Android intents, so no SDK or API key is needed.

**Tech Stack:** Kotlin 2.4, Compose + Material 3, Navigation Compose type-safe routes, Hilt, Room 2.8 (already in the
catalog), ML Kit Text Recognition (new dependency), Activity Result APIs for the camera, photo picker and contact picker.

## User Flow

```
Home (bill history)
  └─ New bill ─┬─ Take photo ──┐
               ├─ Pick photo ──┼─> OCR ─> Review items ─> People ─> Assign items ─> Summary ─> Share via WhatsApp
               └─ Type it in ──┘           (edit/add/remove)        (who had what)   (per person)  (per person or group)
```

1. **Home:** list of saved bills (name, date, total, number of people). FAB "New bill".
2. **Capture:** "Take photo" (`ActivityResultContracts.TakePicture` into a `FileProvider` URI, so no CAMERA
   permission is needed), "Choose from gallery" (`PickVisualMedia`, no permission), or "Enter manually".
3. **Review items:** OCR result shown as an editable list: name, qty, price per line, plus subtotal, tax,
   service charge, discount and total. The user can always fix, add or delete lines. A warning shows when the items
   plus charges don't add up to the receipt total.
4. **People:** one search field that adds participants from the phone's contacts (see Contacts Integration below), or
   adds a typed name as a contact-less person. Several people can be added in a row without leaving the screen.
   Phone number is optional. The payer ("Me") is added automatically. People used in earlier bills are suggested first.
5. **Assign:** for each item, tap the people who shared it. An item shared by N people is split equally. Shortcuts:
   "Everyone" on an item, "Split whole bill equally".
6. **Summary:** per person: their items, their share of tax/service/discount, total owed. The payer's own share is
   shown but not requested.
7. **Share:**
   - Per person with a phone number: open `https://wa.me/<msisdn>?text=<message>` → WhatsApp chat with that person,
     message pre-filled.
   - Per person without a number, or "Send to group": `ACTION_SEND` text with `setPackage("com.whatsapp")`
     (falls back to `com.whatsapp.w4b`, then the system share sheet when WhatsApp is not installed).
   - Message includes the bill name, their items, their total and the payer's payment accounts chosen for this bill
     (see Payment Accounts below).

Example message (Indonesian locale):

```
Halo Budi, ini tagihan "Makan Malam Sushi" (6 Nov 2026):
- Salmon Roll x1  Rp45.000
- Ocha (1/3)      Rp6.000
Pajak & servis    Rp7.650
Total: Rp58.650

Bisa transfer ke:
BCA a.n. Ricky
1234567890
GoPay a.n. Ricky
081234567890

Dikirim dari Kongsi
```

## Payment Accounts

- The bill creator saves their own accounts once, in a "Payment accounts" screen reached from Home's top bar and from
  the Summary step ("Add account" when none exist).
- Each account: type (**Bank** or **E-wallet**), provider, account number (or phone number for e-wallets), account
  holder name.
  - Bank providers: a fixed list of common Indonesian banks (BCA, BRI, BNI, Mandiri, BSI, CIMB Niaga, Permata,
    Danamon, BTN, Jago, SeaBank, blu, Jenius) plus "Other" with a free-text name.
  - E-wallet providers: GoPay, OVO, DANA, ShopeePay, LinkAja, plus "Other".
  - The e-wallet number is normalised like contact numbers (`IndonesianPhone`, reused by Task 7) and stored as `08…`.
- Several accounts can be saved, listed in the order they were added; one or more are marked **default**. The first
  account is default automatically. (Later: drag to reorder, "Use my number" once the app knows the user's number.)
- Per bill, the Summary step shows chips for every saved account; the defaults are pre-selected and the choice is
  saved with the bill. The message lists only the selected accounts, in the user's order.
- In the message the account number is on its own line with no other text, so the recipient can long-press and copy it
  in WhatsApp.
- Validation: number is digits only (spaces and dashes stripped), 5–20 digits for banks, a valid Indonesian mobile
  number for e-wallets; holder name required.
- Stored only on the device. Account numbers are not secret (they are shared in every message), so plain Room storage
  is enough; no encryption needed.
- Deleting an account removes it from future messages and from the selection of existing bills; already-sent messages
  are not affected.

## UX Principles: the math must never confuse

The calculation rules below are precise; the user should never need to know them. Every screen follows these rules.

1. **Defaults do the work.** A normal receipt needs no settings: every item is taxable and serviceable, charges come
   from OCR, shared items split equally. The service/tax toggles sit inside the item's edit sheet (plus a small
   "no tax" / "no service" tag on the row when turned off), not as two icons on every row.
2. **One running total, always visible.** Every step from Review to Summary has a bottom bar:
   "Rp152.460 · matches the receipt ✓" on Review, "Rp96.000 of Rp152.460 assigned" with a progress bar on Assign,
   "Everyone's share adds up to Rp152.460 ✓" on Summary. The user always knows whether things add up.
3. **Every number can explain itself.** Tapping a person's total opens "How was this calculated?" in plain language:
   ```
   Budi                               Rp41.580
   Nasi Goreng                        Rp30.000
   Ocha (shared with Ani, ½)           Rp6.000
   Service 5% of your order            Rp1.800
   Tax 10% of your order + service     Rp3.780
   ```
   No words like "proportional", "basis", "remainder". Rounding is never mentioned unless it changes a total by more
   than Rp1, and then as "Rp1 rounding added to Budi".
4. **Say it as the user would.** "Tax is split by what each person ordered" instead of formulas. Warnings name the cause
   and the fix, and tapping one jumps to the item: "Tax on the receipt is Rp13.860 but we got Rp12.700. Is an item
   tax-free?" → highlights items.
5. **No dead ends.** Every blocking state says what to do ("2 items not assigned yet. Tap to assign"). Every delete has
   undo. Back never loses data (drafts are in Room). A step indicator (Items · People · Assign · Summary) shows where
   the user is and lets them jump back.
6. **Two ways to assign.** Item-first (tap an item, tick people) and person-first ("Budi, what did you have?", tick
   items), switchable with one segmented control. Each person has a colour and initials avatar used on every screen,
   so chips on an item read at a glance.
7. **Typing money is easy.** Numeric keypad, thousands separators while typing (`45.000`), `45rb` / `45k` accepted,
   no decimals for IDR.
8. **Show the receipt.** On Review, the receipt photo is one tap away (bottom sheet, zoomable) and tapping a scanned
   line highlights where it came from, so fixing OCR is comparing, not guessing.
9. **The message is as clear as the app.** The WhatsApp message uses the same breakdown as point 3, so the recipient
   sees why they owe that amount and doesn't ask.
10. **Accessible.** Totals readable at 200% font scale without truncation, TalkBack reads "Budi owes 41 thousand 580
    rupiah", the colour is never the only cue (avatars also carry initials), touch targets ≥ 48 dp.

**Proof that it works** (Task 22): a usability test with at least 5 people who did not build the app, each splitting
2 real receipts (one with a shared item and a tax-free item) without help. Target: each bill done and sent in under
2 minutes, and every tester can explain their own total from the breakdown screen. Any step where a tester hesitates
or asks a question is a bug to fix before release.

## Split Rules (the core invariant)

- Money is stored as `Long` minor units (`amountMinor`) with a currency code (default IDR, 0 decimals). Never `Double`.
- Every person pays for the items they ordered: an item shared by N people gives each `1/N` of it.
- Every item has two flags, **`serviceable`** and **`taxable`**, both `true` by default. The user turns them off on the
  Review screen for items the receipt doesn't charge (mineral water without service, consigned goods, items whose
  price already includes tax, non-PPN goods at a minimarket).
- Charges are the amounts printed on the receipt, distributed **per item**, then summed per person:
  1. **Service** is spread over serviceable items only, in proportion to their price:
     `service_i = service × price_i / Σ price(serviceable)`.
  2. **Tax** is spread over taxable items only, in proportion to their price plus their service (Indonesian PB1 order,
     tax on subtotal + service): `taxBase_i = price_i + service_i`, `tax_i = tax × taxBase_i / Σ taxBase(taxable)`.
  3. **Bill-level discount** is spread over all items in proportion to price. A discount on one item is entered as a
     lower price for that item, not as a bill-level discount.
  4. Person total = Σ over their items of `(price_i + service_i + tax_i − discount_i) / sharers_i`.
- A "%" helper fills a charge from a rate and stores the rate with it: service = rate × Σ price(serviceable),
  tax = rate × Σ taxBase(taxable).
- Intermediate values are exact (`BigDecimal`, no rounding per item). Rounding happens once, at the person totals,
  with the largest-remainder method, so **the sum of all person totals equals the bill total exactly**. This is the
  property the tests must protect.
- Checks shown on the Review screen (warnings, the user can still continue):
  - items + charges don't add up to the receipt total;
  - a charge has a rate and `rate × eligible base` differs from the printed amount by more than 1%
    ("Tax on the receipt is Rp13.860, calculated Rp12.700. Check items without tax.");
  - a charge is above zero but no item is eligible for it (blocks the Summary step: nothing to spread it over).
- Items nobody is assigned to block the Summary step ("2 items not assigned yet").

Worked example (service 5%, then PB1 10% on subtotal + service), used as a unit test:

| Item | Ordered by | Price |
|---|---|---|
| Nasi Goreng | Budi | 30.000 |
| Steak | Ani | 90.000 |
| Ocha | Budi + Ani | 12.000 |
| Service 5% | | 6.600 |
| PB1 10% | | 13.860 |
| **Total** | | **152.460** |

Budi pays 30.000 + 6.000 + 1.800 service + 3.780 tax = **41.580**; Ani pays 90.000 + 6.000 + 4.800 + 10.080 =
**110.880**; together 152.460. If the Ocha is marked not serviceable, its share of service moves to the other items
and both totals change while the sum stays 152.460.

## Contacts Integration

- **With READ_CONTACTS granted:** typing in the People search field queries `ContactsContract.CommonDataKinds.Phone`
  (`CONTENT_FILTER_URI` with the typed text, matching name and number) and shows matches inline: name, number and
  photo thumbnail. Tapping one adds that person with their number. A contact with several numbers shows each number
  as its own row (mobile numbers first, `Phone.TYPE_MOBILE`).
- **Asking for the permission:** only when the user first taps the search field, with a short explanation ("to find
  friends' WhatsApp numbers"), using `PermissionGate` from `core:ui` (see the `add-permission-feature` skill).
- **Permission denied or "Don't ask again":** the screen keeps working. A "Choose from contacts" button opens the
  system picker (`ACTION_PICK` on `Phone.CONTENT_TYPE`), which returns one number with temporary read access and no
  permission. Typed names still work.
- **Remembered people:** a person added to a bill is also stored in a local `FrequentPersonEntity` (name, normalised
  number, last used, use count). These show first in suggestions, also when the permission is denied.
- **WhatsApp hint (optional):** a contact that has a WhatsApp raw contact (`RawContacts.ACCOUNT_TYPE = "com.whatsapp"`)
  gets a small WhatsApp badge, so the user picks the number that has WhatsApp.
- Contacts are read only, queried on demand (no copy of the address book is stored), and queried on `Dispatchers.IO`
  through a `ContactsRepository` interface so the ViewModel and tests use a fake.

## Receipt OCR

- Library: ML Kit Text Recognition v2 (Latin). Use the **bundled** artifact `com.google.mlkit:text-recognition`
  (about 4 MB per ABI, works offline right after install), not the unbundled Play Services one that downloads its
  model on first use.
- Parser (`ReceiptParser`, pure Kotlin, input = recognized lines with bounding boxes):
  - Group text blocks into rows by vertical position; a row ending in a price is an item candidate.
  - Price formats: `45.000`, `45,000`, `45000`, `Rp 45.000`, `45.000,00`.
  - Optional leading quantity: `2 x`, `2x`, `2 `.
  - Keywords mark charge and total rows instead of items: `subtotal`, `total`, `pajak`, `ppn`, `pb1`, `tax`,
    `service`, `servis`, `sc`, `diskon`, `discount`, `tunai`, `cash`, `kembali`, `change`.
  - Lines after `total` (payment, change) are ignored.
- OCR is a starting point and never final: the Review screen is always shown. The parser is tested against a folder
  of anonymised sample receipts saved as text fixtures (`src/test/resources/receipts/*.txt` + expected JSON).
- Images stay in the app's cache dir and are deleted after parsing. Only the parsed data is saved.

## Data Model (Room, `feature/bill/impl/.../data`)

```
BillEntity(id, title, createdAt, currency, payerPersonId, taxMinor, taxRateBps?, serviceMinor, serviceRateBps?,
           discountMinor, receiptTotalMinor?)                -- rates in basis points (10% = 1000), null when typed as amount
ItemEntity(id, billId, name, quantity, totalMinor, serviceable = true, taxable = true, position)
PersonEntity(id, billId, name, phone?)
ItemShareEntity(itemId, personId)            -- many-to-many
FrequentPersonEntity(id, name, phone?, lastUsedAt, useCount)   -- suggestions across bills
PaymentAccountEntity(id, type, provider, number, holderName, isDefault, createdAt)   -- built in v1 of kongsi.db
BillPaymentAccountEntity(billId, accountId)  -- accounts chosen for a bill (cascade on both sides)
```

Mapped to models `Bill`, `BillItem`, `Person`, `PaymentAccount` before the ViewModel sees them. `BillRepository` and
`PaymentAccountRepository` interfaces + `Default…` implementations. Payment accounts live in Room (not DataStore)
because bills reference them.

A bill is saved as a draft from the Review step on, so leaving the app mid-flow loses nothing.

## Module Layout

```
feature/bill/api     routes: BillCaptureRoute, BillEditRoute(billId), BillPeopleRoute(billId),
                     BillAssignRoute(billId), BillSummaryRoute(billId), PaymentAccountsRoute, PaymentAccountEditRoute(id?)
feature/bill/impl
  data/              Room DB, DAO, entities, mappers, DefaultBillRepository, DefaultPaymentAccountRepository, BillModule (Hilt)
  contacts/          ContactsRepository (interface) + DefaultContactsRepository (ContentResolver queries), PhoneNumbers (normalisation)
  model/             Bill, BillItem, Person, PersonShare, SplitCalculator (pure), Money formatting via core.util
  ocr/               ReceiptScanner (ML Kit wrapper, suspend fun scan(uri): List<TextLine>), ReceiptParser (pure)
  share/             BillMessageBuilder (pure), WhatsAppSharer (intents)
  ui/                one stateless Screen + ViewModel per step, NavGraphBuilder.billDestinations()
```

`:app` shows `BillHome` on its `HomeRoute` (one tab is enough for v1).

## Global Constraints

- Follow `AGENTS.md` layer rules and the `add-feature` / `add-screen-route` skills. Look at `.examples/feature/todos`
  (Room) and `.examples/feature/devices` (share intents, photo picker) before writing those parts.
- Form controls only from `core:designsystem`. Every screen file ends with a `@ThemePreviews` preview.
- Strings in `values/` (en) and `values-in/` (id), prefixed `bill_`. Indonesian is the primary audience: write `id` first-class.
- Manifest entries (`READ_CONTACTS`, the `FileProvider` for camera output, the `<queries>` for `com.whatsapp` /
  `com.whatsapp.w4b` package visibility) go in `feature/bill/impl/src/main/AndroidManifest.xml`.
- Proof command: `gradlew.bat test ktlintCheck lint assembleDevDebug`.

## Tasks

### Phase 0: App shell and Profile (done)

- [x] **Swipeable tabs.** Home and Profile are pages of a `HorizontalPager` with the bottom bar (rail on wide
      windows); other pages open full screen over them (docs/architecture/navigation.md).
- [x] **Profile tab** (`:feature:profile`): own name and mobile number (DataStore, `UserProfileRepository` in
      `core:datastore`), Bank accounts & e-wallets, Permissions (Contacts with allow / open settings; Camera listed as
      not needed because receipts use the system camera app), theme, language, app version. Home's overflow menu was
      removed. E-wallet accounts offer "Use my number" from the profile.
- [ ] Privacy policy link in Profile > About once the policy is published (needed before the Play Store release).

### Phase 1: Manual bill end-to-end (no OCR)

- [x] **1. Module skeleton + Home.** `:feature:bill:{api,impl}`, `kongsi.db`, `BillHome` wired into `:app`: empty
      bill history, "New bill" FAB (shows "coming soon" until Task 6), overflow menu → payment accounts, banner
      suggesting an account while none is saved.
- [x] **2. SplitCalculator.** Pure function `split(bill): SplitResult` (per-person shares with item, service, tax and
      discount parts, plus warnings), following Split Rules. Tests: the Budi/Ani worked example; equal split; shared
      item among 3 with remainder; item not serviceable; item not taxable; item serviceable but not taxable (its
      service is not taxed); all items tax-free with tax 0; tax above zero with no taxable item (blocking error);
      rate vs. printed amount mismatch warning; bill-level discount; payer included; unassigned item reported; the
      sum of shares always equals the total (property-style loop over random bills and random flags).
- [x] **3. Room data layer.** Entities, DAO, DB, mappers, repository, Hilt module. Tests: DAO round-trip incl.
      item shares, cascade delete of a bill.
- [x] **4. Bill list screen.** Empty state, list, delete with undo. ViewModel + screen tests.
- [x] **5. Shared flow chrome.** Step indicator, running-total bottom bar, person colour/initials avatar, money input
      field (`45.000`, `45rb`, `45k`) in `core:designsystem` if reusable. Tests: money input parsing table, bottom bar
      states (matches / doesn't match / partly assigned).
- [x] **6. Review/edit items screen.** Add/edit/delete item with undo, "service" and "tax" toggles inside the item edit
      sheet with a tag on the row when off, charges with "%" helper, receipt photo sheet, the warnings from Split
      Rules worded per UX Principles and jumping to the item they concern. Tests: validation (empty name, zero
      price), toggling flags updates the warnings, total mismatch warning, tax mismatch warning, blocking error when a
      charge has no eligible item, tapping a warning focuses the item.
- [x] **7. Phone numbers.** Done as `IndonesianPhone.international` in `core.util` (next to `local` and `subscriber`, which Profile and e-wallet accounts already used), instead of a separate `PhoneNumbers.normalise` (`08…`, `+62 8…`, `62-8…` → `628…`; strip spaces, `-`, `(`, `)`),
      used by contacts, frequent people and WhatsApp. Tests: normalisation table, invalid input returns null.
- [x] **8. ContactsRepository.** `search(query)` and `fromPickerResult(uri)` over `ContactsContract`, mobile numbers
      first, one row per number, WhatsApp badge. Tests: Robolectric `ContentProvider` fixture with several contacts,
      multiple numbers, no number, Indonesian names.
- [x] **9. People screen.** (Frequent people are saved by "Done" on Summary since Task 11.) Search field with inline contact results (permission granted), picker button
      (denied), typed name, frequent people first, remove; "Me" fixed. Permission asked on first focus via
      `PermissionGate`. Tests: ViewModel with fake `ContactsRepository` for granted/denied, adding the same number twice
      is blocked, duplicate names allowed but distinguished, frequent people saved after a bill is finished.
- [x] **10. Assign screen.** Item-first and person-first modes, person chips with avatars, "Everyone", "Split
      equally", progress in the bottom bar. Blocks next step while items are unassigned, with a tap-to-fix message.
      Tests: toggling shares in both modes gives the same result, blocking state, progress amount.
- [x] **11. Summary screen.** (Item lines are each rounded on their own; the difference from the total is a line only above Rp1. "Done" remembers the people and returns Home; Task 16 adds the share actions.) Per-person totals and the "How was this calculated?" breakdown sheet in plain language
      (UX Principles point 3), payer marked, "adds up to the receipt ✓". Tests: breakdown lines for the Budi/Ani
      example, shared-item fraction text, rounding line only when above Rp1, TalkBack content descriptions.

### Phase 2: Share to WhatsApp

- [x] **12. Payment accounts** (built early, with Task 1; reorder not yet). Entities, DAO, repository; list screen (reorder, mark default, delete with undo) and
      edit screen (type, provider picker, number, holder name, note) reached from Home's top bar. Tests: validation
      for bank and e-wallet numbers, default selection, deleting an account removes it from bill selections.
- [x] **13. Account selection on Summary.** (Stored in `bill_payment_accounts` plus `bills.accountsChosen`, kongsi.db v4; defaults are applied once, as soon as an account exists.) Chips for saved accounts, defaults pre-selected for a new bill, choice
      saved with the bill, "Add account" when none exist. Tests: defaults applied once, choice survives reopening.
- [x] **14. BillMessageBuilder.** (`share/BillMessageBuilder.forPerson`; the breakdown moved to `model/Breakdown.kt` so Summary and the message share it. The group summary comes with Task 16.) Pure, locale-aware (en/id), currency formatting from `core.util`. Tests: golden
      strings for en and id, with zero, one and several accounts (bank and e-wallet), number on its own line,
      shared-item fraction text.
- [ ] **15. WhatsAppSharer.** `wa.me` link from `IndonesianPhone.international` (Task 7), `ACTION_SEND` with WhatsApp →
      WhatsApp Business → chooser fallback, `<queries>` in manifest. Tests: intent built for each fallback case
      (Robolectric `ShadowPackageManager`).
- [ ] **16. Share actions on Summary.** "Send" per person, "Send summary to group". A person without a number gets an
      "Add number" action that opens the same contact search.

### Phase 3: Receipt scanning

- [ ] **17. Capture screen.** Take photo (FileProvider + `TakePicture`), pick photo (`PickVisualMedia`), manual entry.
- [ ] **18. ReceiptParser.** Pure parser + fixture tests for at least 8 real receipt layouts (restaurant with PB1 +
      service, coffee shop, minimarket, receipt with discount, qty-prefixed lines, `45.000,00` format).
- [ ] **19. ReceiptScanner.** ML Kit wrapper, add the dependency to `gradle/libs.versions.toml`, loading/error
      states ("Couldn't read the receipt, enter it manually"), delete the image after scanning.
- [ ] **20. Wire capture → OCR → Review.** Prefill the Review screen; a manual test on a device with real receipts.

### Phase 4: Polish

- [ ] **21.** Rename/duplicate a bill, "mark as paid" per person, release build (`release-build` skill).
- [ ] **22. Usability test.** Run the test described under UX Principles on a release build, write findings to
      `docs/ux/2026-usability-findings.md`, fix every hesitation point, repeat with 2 new testers.

## Review Focus

- **Totals always add up:** Σ person totals == bill total for every input, including any mix of service and tax
  flags (Task 2 property test).
- **Fair tax:** a person who only ordered tax-free or service-free items pays none of that charge.
- **Money never uses floating point** anywhere between parsing and display.
- **No data loss:** a draft survives process death and leaving mid-flow (Room is the source of truth, not ViewModel state).
- **Privacy:** receipt photos are deleted after scanning. Contacts are read only, on demand, and never copied in bulk;
  only people the user adds to a bill are stored. Nothing leaves the device except the message the user chooses to
  send. The Play Store listing needs a privacy policy that declares contacts access.
- **Contacts permission denied:** the People step still works through the picker and typed names.
- **WhatsApp missing:** sharing still works through the system share sheet.

## Out of Scope (v1)

Accounts, sync between devices, group ledgers / running balances across bills, in-app payments (QRIS), multi-currency
conversion, iOS.

## Open Decisions

1. **OCR quality vs. cost.** On-device ML Kit is free and offline but reads crumpled or thermal receipts poorly, and the
   rule-based parser will miss layouts. The alternative is sending the photo to a vision LLM (e.g. Claude) through a
   small backend, which is far more accurate but needs a server, an API key kept off the device, per-scan cost and
   a privacy notice. Plan: ship ML Kit, and keep `ReceiptScanner` swappable if accuracy proves too low.
2. **Bundled vs. unbundled ML Kit:** bundled is chosen above (+~4 MB per ABI) for reliability without Play Services
   download. Revisit if APK size matters.
3. **Currency:** IDR only in v1, but the model already stores a currency code.
