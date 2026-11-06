# SDK demos — behavior reference (from the RN boilerplate)

Companion to `2026-10-31-05-sdk-demos.md`. Extracted from `D:\RN\rn-expo-boilerplate\src\app\(public)\example\sdk\*.tsx`. Labels, numbers and messages are the RN ones (English); **Android adaptations** are marked ➜. The RN app has no Indonesian copy for these screens; Indonesian is authored in the plan's string tables.

## Shared

- **Section** = `DemoSection(title)` + a muted description line, then content.
- **InfoRows** = an outlined card of `InfoRow(label, value)`; a null/empty value renders as "—".
- **Status alert** = `Banner` (info/success/warning/error), closable where noted.
- **PermissionGate**: loading → spinner; granted → content; otherwise an outlined card with the reason and a button "Grant access" (requests). When the system will no longer show the prompt, the text is `reason + " Enable it in Settings to continue."` and the button "Open Settings" opens the app's settings page.
  - Gated: camera, location, calendar (whole screen); media (voice-note section only); contacts (Search section only).

## camera — "Camera & QR Scanner"
- Gate reason: "Camera access is needed to scan QR codes and take photos."
- **Preview**: description "Point at a QR code — the result opens in a sheet" (scan mode) / "Tap Capture to take a photo" (photo mode). 380 dp tall, rounded. Default mode scan, back camera. QR only.
- **Controls** ("Native Picker, Switch, Slider and Button drive the camera" ➜ "Segmented buttons, switch, slider and buttons drive CameraX"): mode "Scan QR" (default) / "Photo"; flash "Flash off" (default) / "Flash on" / "Flash auto"; switch "Torch" (off); zoom slider 0..1 (0, linear zoom); button "Flip" (outlined, back/front); button "Capture" only in photo mode, disabled until the camera is bound; JPEG quality 70.
- **Last photo**: after a capture, 240 dp tall, crop.
- **Sheet** when a code is scanned: title "QR code scanned", body = the raw value (selectable, monospace), caption "Close the sheet to scan again." Scanning only in scan mode and only while no result is shown; closing clears it → one code fires once.

## image-picker — "Image & Document Picker"
- **Image Picker** ("Photo library or camera, with native crop and compression" ➜ "Photo Picker or camera, with a square crop"): source "Images" (default) / "Videos"; switch "Crop (square)" (on; disabled while Multiple is on); switch "Multiple selection" (off); quality slider 0.1..1 step 0.1 default 0.8; buttons "Library", "Camera" (outlined).
- Camera permission is requested on "Camera" press (no gate); denial → error "Camera permission was denied."
- Results replace the list; thumbnails 104×104 dp, crop; InfoRows for the first item: "Type", "Size" (`W × H`), "File" (`KB` to 1 decimal), "MIME".
- **Document Picker** ("Any file from the system picker, filtered by MIME type"): type "Any file" (`*/*`, default) / "PDF" (`application/pdf`) / "Images" (`image/*`); button "Choose files" (multiple); InfoRows name | size.

## location — "Location"
- Gate reason: "Location access is needed to show where you are."
- **Position** ("One-off fix, or live updates every 5 meters"): accuracy "Low accuracy" / "Balanced" (default) / "High accuracy"; switch "Watch position" (off); button "Get current location" / "Locating…" (disabled while loading).
- One-off: current location at the chosen accuracy, then reverse-geocode. Error → "Could not get a location." (or the exception message).
- Watch: while on, updates with a 5 m minimum distance; restarts when accuracy changes; stops when off or the screen closes; no geocoding.
- InfoRows: "Latitude" (6 decimals), "Longitude" (6 decimals), "Accuracy" (`±{round} m`), "Updated" (time).
- **Address** ("Reverse-geocoded from the last one-off fix"): "Street", "District", "City", "Region", "Postal code", "Country".

## biometric — "Biometric & Secure Store"
- **Device support** ("What this device can verify with"): "Biometric hardware" (Yes/No), "Enrolled" (Yes/No), "Types" ➜ "Strength" ("Strong", "Weak" or "—"), "Secure storage" ("Available"/"Unavailable").
- **Authenticate** ("Face ID / Touch ID / fingerprint prompt" ➜ "Fingerprint or face unlock prompt"): switch "Allow device passcode fallback" (on); button "Verify it's me" (disabled unless hardware and enrolled). Prompt "Verify it's you", negative "Cancel". Success "Verified." (success); failure `Not verified: {error}` (error).
- **Transaction PIN** ("Saved encrypted; reading it back requires biometrics"): password field, number keyboard, placeholder "6-digit PIN", max 6; buttons "Save", "Reveal" (outlined), "Delete" (text).
- Save: must match `^\d{6}$` else "PIN must be exactly 6 digits."; success clears the field and shows "PIN saved to secure storage." Reveal: prompt "Confirm to reveal your PIN", then info `Stored PIN: {pin}`. Delete: info "PIN removed."
- Status: closable banner at the bottom.

## notifications — "Notifications"
- Channel `default`, name "Default", importance HIGH, created on screen start.
- **Permission** ("Ask once; the OS remembers the answer"): row "Status" (granted / denied / not requested ➜ API < 33 is always granted); button "Allow notifications" when not granted.
- **Local notification** ("Send now or after a delay — lock the device to see it"): title field (placeholder "Title", initial "Payment received"); delay "Now" (0) / "In 5 seconds" (5, default) / "In 30 seconds" / "In 1 minute" (60); switch "Show while app is open" (on); buttons "Schedule" (disabled unless granted), "Cancel all" (outlined); caption `{n} pending`.
- Title blank → "Notification". Body: delay > 0 → `Scheduled {s}s from the SDK example.` ➜ write `Sent {s}s after scheduling from the SDK example.`; delay 0 → "Sent immediately from the SDK example." Log "Scheduled in {s}s" / "Sent now" / "Cancelled all scheduled".
- **Events** ("Received in the foreground, or tapped by the user"): newest first, max 5, `{time} · {line}`: "Received: {title}", "Tapped: {title}", plus the log lines above; empty "No events yet."

## contacts — "Contacts"
- **System picker** ("The OS contact picker — the user shares one contact"): button "Choose recipient"; InfoRows "Name", "Phone" (first number); failure "Contact picker failed."
- **Search** (gated; reason "Contacts access is needed to search your address book."): description "Search by name" until counted, then `Search {n} contacts by name`; field placeholder "Name"; debounce 250 ms; limit 10; rows `name ?: "Unnamed"` | first phone; empty "No contacts found."

## clipboard — "Clipboard & Haptics"
- Referral code `SUPER-7K2Q9`.
- **Copy** ("Copies a referral code and confirms with a success tap"): the code (titleLarge); button "Copy" → set clipboard + success haptic.
- **Paste** ("Reads the clipboard into a text field"): field placeholder "Account number"; button "Paste" (outlined): no text → warning haptic; else fill field, remember "Last pasted", selection haptic. Caption `Clipboard changed {n} times while this screen was open.` (primary-clip listener). InfoRows "Last pasted" after a paste.
- **Haptics** ("Changing a picker ticks with a selection haptic"): impact "Light" / "Medium" (default) / "Heavy" / "Soft" / "Rigid" + button "Impact"; outcome "Success" (default) / "Warning" / "Error" + button "Notify"; changing either picker → selection haptic.

## device — "Device & Network"
- **Network** ("Updates live when connectivity changes"): warning banner when not connected or not validated: "You're offline" / "Transactions will resume once you reconnect."; InfoRows "Type", "Connected", "Internet reachable", "IP address", "Airplane mode" ("On"/"Off"); button "Read IP address" (outlined) reads IP and airplane mode.
- **Device** ("Static constants, read synchronously"): "Brand", "Model", "OS" (`Android {release} (API {sdk})`), "Device type" (Phone/Tablet/TV), "Physical device" ("Yes"/"No (emulator)"), "Memory" (GB, 1 decimal), "Year class" ➜ "Performance class" (API 31+ `Build.VERSION.MEDIA_PERFORMANCE_CLASS`, else "—").

## share — "Share & Print"
- **E-receipt** ("Rendered into a PDF, then shared or printed"): merchant "Kopi Kenangan" (default) / "PLN Token" / "Telkomsel Pulsa"; amount field (number keyboard, placeholder "Amount (IDR)", initial "25000"); buttons "Share PDF", "Print" (outlined).
- Amount = digits only, default 0; reference `TRX-` + base-36 uppercase of now; content: heading "Payment receipt", date/time, rows "Merchant", "Reference", "Total" (bold, currency IDR).
- Share: PDF into cache, share sheet title "Share receipt", type `application/pdf`; then InfoRows "File", "Pages". Print: system print dialog. Errors "Something went wrong."

## web-browser — "Web Browser & Linking"
- **In-app browser** ("Themed browser sheet that returns to the app when closed" ➜ "Custom Tab themed with the app colors"): page "Terms & Conditions" (https://expo.dev/terms, default) / "Privacy Policy" (https://expo.dev/privacy) / "Help Center" (https://docs.expo.dev); button "Open page". (No "closed with" row: Custom Tabs report no result.)
- **Open other apps** ("wa.me, tel:, mailto: and map URLs"): "WhatsApp" `https://wa.me/6281234567890?text=Halo`, "Call" `tel:+6281234567890`, "Email" `mailto:support@example.com?subject=Help`, "Maps" `geo:0,0?q=Monas%20Jakarta`; button "Open" (outlined); no handler → warning `No app on this device can open {app}.`
- **Deep link** ("A link that opens this app on a given screen"): row "URL" = `{scheme}://example/sdk/payment-code?amount=50000`.

## payment-code — "Payment Code"
- Card: "Show this code to the cashier"; code = 3 groups of 4 random digits, space-separated; caption `Refreshes in {n}s · Balance {Rp 1.250.000}`; 60 s lifetime, ticks every second, new code at 0.
- Warning banner after a screenshot: "Screenshot detected" / "Never share your payment code with anyone." (API 34+).
- **Screen treatment** ("Each switch applies while on, and is undone when you leave"): "Max brightness", "Keep screen on", "Block screenshots" (all on) ➜ window `screenBrightness = 1f`, `FLAG_KEEP_SCREEN_ON`, `FLAG_SECURE`; each restored when switched off or the screen closes.

## media — "Audio & Video"
- **Voice note** (gated mic; reason "Microphone access is needed to record voice notes."; "Record, stop, then play it back"): button "Record" / "Stop"; button "Play" / "Pause" (outlined; disabled until a note exists and while recording); finished note restarts from 0. InfoRows "Recording" (`m:ss` or "Idle"), "Playback" (`m:ss / m:ss`).
- **Promo video** ("Muted, looping banner; picture-in-picture where supported"): `https://d23dyxeqlo5psv.cloudfront.net/big_buck_bunny.mp4`, looping, muted, 200 dp, crop, no controls; button "Play video" / "Pause video" ➜ plus "Picture-in-picture" (outlined, API 26+).

## sqlite — "SQLite" ➜ "Room transactions"
- **Transaction history** ("Stored in a local database that survives restarts"): kind "Top-up" (+, default) / "Payment" (−) / "Transfer" (−); amount 10 000 / 50 000 (default) / 100 000 (shown as IDR); buttons "Add transaction", "Clear" (text).
- Title `Balance {sum}`; latest 20 by created_at DESC, id DESC; row `{Kind} · {dd MMM, HH:mm}` | signed IDR; empty "No transactions yet."

## files — "Image Manipulator & Files" ➜ "Image Compression & Files"
- **Compress before upload** ("Resize to a max width and re-encode as JPEG"): max width slider 480..1920 step 120 default 1080; quality 0.1..1 step 0.1 default 0.6; buttons "Pick photo" (outlined), "Compress" (disabled until picked); caption `Max width {px}px · Quality {x.x}`; preview 200 dp fit; InfoRows "Original" (`W × H`), "Original size", "Compressed" (`W × H`), "Compressed size", "Saving" (`{n}% smaller`). Only scale down.
- **Downloads** ("Saved under the cache directory, which the OS may purge"): sample PDF `https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf` into `cache/downloads`; buttons "Download PDF", "Clear" (text); rows name | size; empty "No downloaded files."; failure "Download failed."

## calendar — "Calendar"
- Gate reason: "Calendar access is needed to add your bookings."
- **Add booking** ("Creates an event, optionally with a reminder an hour before"): booking "Flight CGK → DPS" (Soekarno-Hatta T3, 2 h, default) / "Doctor appointment" (RS Pondok Indah, 1 h) / "Movie tickets" (CGV Grand Indonesia, 3 h); when "Tomorrow 09:00" (+1, default) / "In 3 days 09:00" (+3) / "Next week 09:00" (+7), at 09:00 local; switch "Remind me 1 hour before" (on → reminder 60 min); button "Add to calendar".
- Target = first writable calendar (`CALENDAR_ACCESS_LEVEL >= CAL_ACCESS_CONTRIBUTOR`, visible); none → "No writable calendar on this device." Notes "Added from the SDK example." Success `Added to “{calendar}”.`; error "Could not add the event."
- **Next 7 days** (`Events in “{calendar}”`): title | `EEE dd MMM, HH:mm`, events in [now, now + 8 days].

## app-info — "App Info & Store Review"
- **Application** ("Read from the installed package"): "Name", "Package" (application id), "Version" (name), "Build" (version code), "Installed" (first install time), "Platform" (`Android {release}`).
- **Rate the app** ("In-app review sheet, or the store page as a fallback"): button "Rate us" → Play In-App Review flow → info "Review requested — Google Play may choose not to show it."; failure → info "In-app review is not available here."; button "Open store page" (outlined) → `market://details?id={package}` falling back to `https://play.google.com/store/apps/details?id={package}`. Banner closable.

## background-task — "Background Task"
- **Periodic sync** ("Runs even when the app is closed, when the OS allows"): switch "Background sync" (= enqueued); interval "Every 15 minutes" (default) / "Every 30 minutes" / "Every hour"; changing while on re-enqueues (REPLACE/UPDATE); button "Refresh" (outlined); debug-only button "Run now" (one-time run of the same worker) with caption "Run now is only shown in debug builds."; note "Android runs periodic work at most every 15 minutes."
- **Status** ("The worker records each run"): "Status" (WorkInfo state), "Registered" (Yes/No), "Last run" (date-time or "Never"), "Runs".
- Work name `example-background-sync`; each run stores last run (epoch ms) and run count + 1.

## effects — "Blur & Gradients"
- **Linear gradient** ("A wallet card background at any angle"): 160 dp card, caption "Super Wallet", heading Rp 1.250.000 in white; gradient "Ocean" (#0EA5E9→#6366F1, default) / "Sunset" (#F97316→#EC4899) / "Forest" (#22C55E→#0F766E); angle slider 0..180 step 15 default 45 (0° = left→right).
- **Blur view** ("Hide the balance behind frosted glass"): switch "Hide balance" (on); intensity slider 0..100 step 5 default 60 ➜ blur radius `intensity / 100 × 24 dp`; overlay title "Balance hidden". ➜ API < 31: no blur; a translucent scrim with the caption "Blur needs Android 12; showing a scrim instead." Tint picker dropped (no Android equivalent).
- **Mesh gradient** ("3×3 control points — drag the middle one with the slider"): 200 dp; colors row-major #6366F1, #8B5CF6, #EC4899 / #0EA5E9, #F8FAFC, #F97316 / #22C55E, #14B8A6, #EAB308; middle point (m, m) with slider 0.2..0.8 default 0.5; switch "Smooth colors" (on). ➜ AGSL `RuntimeShader` on API 33+; below: caption "Mesh gradients need Android 13; showing a linear gradient instead."

## live-photo — "Live Photo"
- Android: the whole screen is an info banner "Live Photos are an iOS feature" / "Open this example on an iPhone or iPad to try it." (spec §6: same as RN on other platforms).

## blob — "Blob" ➜ "Binary data"
- Initial `{"trx":"TRX-001","amount":50000,"status":"PAID"}`.
- **Blob** ("Create from text, then read it back as text or bytes"): field placeholder "Content"; buttons "Create" (MIME `application/json`), "Text", "Slice" (bytes 0..8, hex 2-digit lowercase space-separated), "Combine" (`[blob, "\n", blob]`, MIME `text/plain`) — the last three outlined, disabled until created. InfoRows "Size" (`{n} bytes`, UTF-8 byte length), "Type", then "Text" or "Bytes".

## status-bar — "Status Bar" ➜ "System Bars"
- ("Changes apply to this screen only"): style "Auto (follows theme)" (default) / "Light icons" / "Dark icons" / "Inverted"; switch "Hidden" (off). Caption "Go back to see the app's own status bar return." (hide-animation picker dropped: iOS only).
