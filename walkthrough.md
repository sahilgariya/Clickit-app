# Security Summary

**Reviewed Files**: 14
**Issues Resolved**: 10

### Issues Resolved
| Area | File | Line | What Was Happening | Impact | Status | How It Was Fixed |
|---|---|---|---|---|---|---|
| Rating & Load Manipulation | `firestore.rules` | 360 | Shop updates permitted non-owners on rating fields and allowed owners to rewrite `ratingAvg`, `ratingCount`, and `activeLoad`. | Medium | Fixed | Restricted `shops/{shopId}` updates strictly to `existing().ownerId == request.auth.uid` and excluded `ratingAvg`, `ratingCount`, and `activeLoad` from editable fields. |
| Booking Price & Owner Spoofing | `firestore.rules` | 419 | Booking creation did not enforce `workerOwnerId` matching the target shop owner or validate `totalMinor` arithmetic and numeric OTPs. | High | Fixed | Required `get(/.../shops/$(incoming().shopId)).data.ownerId == incoming().workerOwnerId`, `platformFeeMinor >= 100`, `totalMinor` sum equality, and `^[0-9]{4,8}$` OTP format. |
| Booking Role & State Bypass | `firestore.rules` | 450 | Customer updates could touch pricing fields or complete unpaid bookings; partners could claim unscheduled tasks. | High | Fixed | Consolidated role-scoped `allow update` branches so customers cannot alter price fields or set `completed` until `paymentStatus == 'paid'`, workers maintain `totalMinor` equality, and partners can claim only `pickup_scheduled` / `return_scheduled` jobs. |
| Cross-Booking Delivery Task Injection | `firestore.rules` | 549 | Creating a `delivery_task` verified the referenced booking existed without checking that `customerId` and `workerOwnerId` matched it. | High | Fixed | Enforced `customerId` and `workerOwnerId` equality against `get(/.../bookings/$(incoming().bookingId)).data`. |
| Unverified Handover OTP Pre-fill | `app/src/main/java/com/example/ui/screens/PartnerScreens.kt` | 288 | The partner pickup/delivery OTP dialog pre-filled and displayed the expected OTP on screen, bypassing customer verification. | Medium | Fixed | Cleared default input (`""`), hid the expected OTP hint, and required matching 4–8 digit customer OTP confirmation before advancing parcel custody. |
| Unpaid Order Completion in UI | `app/src/main/java/com/example/ui/screens/CustomerOrdersProfileScreens.kt` | 290 | Customer order receipt button attempted to mark unpaid orders as completed instead of prompting payment first. | Medium | Fixed | Added `paymentStatus == "paid"` checks in both UI (`Pay to Complete` triggers payment sheet) and `ClickitRepository.customerConfirmDeliveryAndRate` (`1452`). |
| Predictable PRNG for Codes & OTPs | `app/src/main/java/com/example/data/ClickitRepository.kt` | 33 | Parcel codes, pickup OTPs, and delivery OTPs were generated with `kotlin.random.Random`. | Medium | Fixed | Replaced `kotlin.random.Random` with `java.security.SecureRandom` across all generated `parcelCode`, `handoverOtp`, and `deliveryOtp` values. |
| Sensitive Diagnostic Logging & Cleartext/Backup Exposure | `app/src/main/AndroidManifest.xml`, `app/src/main/java/com/example/data/FirestoreErrorHandler.kt` | 12, 20 | `allowBackup` defaulted to enabled without cleartext restriction, and Firestore error logs included full user email addresses. | Medium | Fixed | Set `android:allowBackup="false"`, added `network_security_config.xml` (`cleartextTrafficPermitted="false"`), excluded `sharedpref`/`database` in backup rules, and added `maskEmail()` redaction in `FirestoreErrorHandler.kt`. |
| Stale Profile & Unpersisted Local State | `app/src/main/java/com/example/data/ClickitRepository.kt`, `app/src/main/java/com/example/ui/ClickitViewModel.kt` | 575, 83 | Clearing `phoneMasked` kept the old phone number, Firestore offline persistence wasn't configured explicitly, and partner online status, filters, and quick pickup addresses were lost across restarts. | Medium | Fixed | Fixed `updateUserProfile` to write empty strings cleanly, enabled 100 MB `PersistentCacheSettings`, persisted partner online state and discovery filters in `SharedPreferences` per user, and added PIN + `"Save address"` support in quick checkout. |
| Dialog State Loss on Save Failure | `app/src/main/java/com/example/ui/screens/UserProfileScreen.kt`, `app/src/main/java/com/example/ui/screens/WorkerScreens.kt` | — | Address, support ticket, quote revision, shop editor, and service catalog dialogs dismissed immediately before save validation or Firestore confirmation. | Low | Fixed | Added inline field validation, `isBusy` guards, and `onDone` callbacks so dialogs remain open until validation passes and writes succeed. |

### Safeguards Verified
| Area | File | How It's Protected |
|---|---|---|
| Google Sign-In & Auth Gating | `app/src/main/java/com/example/MainActivity.kt` | Uses Jetpack `CredentialManager` (`GetSignInWithGoogleOption`) exclusively with zero anonymous or password fallbacks; feature UI and listeners are gated behind signed-in UID. |
| Named Firestore Isolation | `app/src/main/java/com/example/data/ClickitRepository.kt` | Connects strictly to `R.string.firestore_database_id` with explicit 100 MB `PersistentCacheSettings` and `requireUserId()` fail-fast checks on reads and mutations. |
| Network & Backup Hardening | `app/src/main/res/xml/network_security_config.xml`, `backup_rules.xml`, `data_extraction_rules.xml` | Disallows cleartext HTTP traffic (`cleartextTrafficPermitted="false"`), disables cloud/device backups (`allowBackup="false"`), and excludes `sharedpref` and `database` domains. |
| PII Log Redaction | `app/src/main/java/com/example/data/FirestoreErrorHandler.kt` | Masks email local parts via `maskEmail()` (e.g. `sa***@gmail.com`) before emitting structured Logcat error JSON. |
| Rule & Unit Test Coverage | `firestore.test.js`, `app/src/test/java/com/example/ExampleUnitTest.kt` | Verified 5/5 Firestore Emulator rule tests (zero expression-limit errors) and JVM unit tests (`maskEmail` + pricing/domain helpers). |

## PoC Verification

### 1. Booking Price Spoofing, Unpaid Completion & Cross-Shop Owner Injection

#### Summary
| Field | Value |
|---|---|
| Issue Type | Missing Business Integrity & Ownership Cross-Checks in Security Rules |
| Impact | High |
| Affected File | `firestore.rules:419` |
| Test Scenario | Customer attempts to create a booking with `workerOwnerId` pointing to an attacker UID instead of the shop owner, with mismatched `totalMinor` (`serviceSubtotalMinor + deliveryFeeMinor + platformFeeMinor != totalMinor`), or attempts to update `totalMinor` / mark an unpaid (`paymentStatus: 'pending'`) booking as `status: 'completed'`. |

#### How It Was Fixed
`firestore.rules` validates `get(/databases/$(database)/documents/shops/$(incoming().shopId)).data.ownerId == incoming().workerOwnerId`, enforces `platformFeeMinor >= 100` and exact `totalMinor` sum arithmetic on creation and worker updates, removes pricing keys from the customer `affectedKeys()` allowlist, and blocks `status == 'completed'` unless `existing().paymentStatus == 'paid'`.

#### Step-by-Step Trace
| Step | What Happens in the Code | Outcome |
|---|---|---|
| 1 | Test writes spoofed booking (`workerOwnerId: "rogue_uid"`, `totalMinor: 100`, or non-numeric `handoverOtp: "ABCD"`) via authenticated customer context in `firestore.test.js` | Hits `match /bookings/{bookingId}` `allow create` at `firestore.rules:419` |
| 2 | Rules evaluate `get(.../shops/$(incoming().shopId)).data.ownerId == incoming().workerOwnerId`, `totalMinor` sum equality (`446`), and numeric OTP regex (`155`) | Condition evaluates to `false` -> `PERMISSION_DENIED` |
| 3 | Customer tries updating `totalMinor` or setting `status: 'completed'` while `paymentStatus == 'pending'` | Blocked by customer branch `affectedKeys()` (`482`) and payment gate (`468`) -> **Handled Safely** |

#### Conclusion
**Fix Verified** - Verified by `firestore.test.js` Subtest 5 against the local Firestore Emulator (`pass 5, fail 0`), confirming price tampering, shop-owner spoofing, non-numeric OTPs, and unpaid completions are rejected at the database boundary.

---

### 2. Shop Rating Tampering & Cross-Booking Delivery Task Injection

#### Summary
| Field | Value |
|---|---|
| Issue Type | Over-Permissive Public/Owner Field Allowlists & Missing Cross-Document Reference Validation |
| Impact | High |
| Affected File | `firestore.rules:360`, `firestore.rules:549` |
| Test Scenario | Non-owner (or shop owner) attempts to write `ratingAvg: 5.0` / `ratingCount: 999` on `/shops/{shopId}`, or an actor tries to create a `/delivery_tasks/{taskId}` linking a victim's `bookingId` with a different `customerId` or `workerOwnerId`. |

#### How It Was Fixed
Removed the non-owner update branch and stripped `ratingAvg`, `ratingCount`, and `activeLoad` from shop owner `affectedKeys()` (`firestore.rules:360-369`), and added `customerId` + `workerOwnerId` equality checks against the referenced booking document in `delivery_tasks` `allow create` (`firestore.rules:570-572`).

#### Step-by-Step Trace
| Step | What Happens in the Code | Outcome |
|---|---|---|
| 1 | Owner or third-party client sends `updateDoc` altering `ratingAvg` on `/shops/{shopId}` | Evaluated at `firestore.rules:360` -> fails `affectedKeys().hasOnly(...)` -> `PERMISSION_DENIED` |
| 2 | Actor sends `setDoc` to `/delivery_tasks/{taskId}` referencing an existing `bookingId` with a forged `customerId` | Evaluated at `firestore.rules:570-571` -> `get(.../bookings/...).data.customerId == incoming().customerId` fails -> `PERMISSION_DENIED` |
| 3 | Legitimate shop operational update (`openStatus`, `shopName`) and valid partner pickup handover (`picked_up -> received_by_worker`) | Succeed cleanly in Subtests 4 and 5 -> **Handled Safely** |

#### Conclusion
**Fix Verified** - Verified by `firestore.test.js` Subtests 4 and 5 against the local Firestore Emulator (`pass 5, fail 0`) with live rules deployed via `DeployRules`.

---

### 3. Delivery Handover OTP Bypass & Logcat Email PII Exposure

#### Summary
| Field | Value |
|---|---|
| Issue Type | Client-Side Verification Bypass & Sensitive Data Logging |
| Impact | Medium |
| Affected File | `PartnerScreens.kt:288`, `FirestoreErrorHandler.kt:20` |
| Test Scenario | Partner opens parcel collection/delivery verification without asking the customer for the OTP, or a Firestore operation fails and writes diagnostic auth metadata to Logcat. |

#### How It Was Fixed
Removed OTP pre-fill and expected-code text hints in `PartnerRouteScreen` (`PartnerScreens.kt:288-330`), switched OTP/parcel generation to `SecureRandom` (`ClickitRepository.kt:33`), and masked email local parts via `maskEmail()` (`FirestoreErrorHandler.kt:20-29`) while hardening `AndroidManifest.xml` (`allowBackup="false"`, `networkSecurityConfig`).

#### Step-by-Step Trace
| Step | What Happens in the Code | Outcome |
|---|---|---|
| 1 | Partner opens `Collect Parcel` / `Deliver` dialog (`PartnerScreens.kt:288`) | `enteredOtp` initializes to `""` with no expected OTP displayed; empty/mismatched input sets inline `otpError` and blocks `onAdvanceTask` |
| 2 | Diagnostic error path calls `handleFirestoreError()` (`FirestoreErrorHandler.kt:50, 56`) | `maskEmail("sahil.gariya@gmail.com")` redacts to `"sa***@gmail.com"` before `Log.e` |
| 3 | Verified via `ExampleUnitTest` (`testDebugUnitTest`) and full `assembleDebug` APK build | Unit tests pass cleanly and `apk/Clickit-debug.apk` compiles with `compile_applet` -> **Handled Safely** |

#### Conclusion
**Fix Verified** - Handover verification strictly requires the customer-provided 4–8 digit OTP generated by `SecureRandom`, and email PII is masked in all diagnostic logs.
