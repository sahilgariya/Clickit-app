const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_customer_1";
const BOB_UID = "bob_worker_2";
const CHARLIE_UID = "charlie_partner_3";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

const pastDate = () => new Date(Date.now() - 5000);
const futureDate = () => new Date(Date.now() + 120000);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function buildShopPayload(ownerId, overrides = {}) {
  return {
    ownerId,
    shopName: "Royal Steam & Press",
    category: "laundry",
    tagline: "Crisp steam press & organic wash",
    locality: "Indiranagar 12th Main",
    addressSummary: "Shop 4, 12th Main, Indiranagar",
    hoursSummary: "8:00 AM - 9:00 PM",
    turnaroundLabel: "24 hrs",
    openStatus: true,
    published: true,
    verified: true,
    supportsShopDrop: true,
    supportsWorkerDelivery: true,
    supportsClickitBatch: true,
    dailyCapacity: 40,
    activeLoad: 5,
    ratingAvg: 4.9,
    ratingCount: 10,
    startingPriceMinor: 1200,
    startingPriceUnit: "cloth",
    createdAt: pastDate(),
    updatedAt: pastDate(),
    ...overrides,
  };
}

function buildBookingPayload(customerId, workerOwnerId, shopId, overrides = {}) {
  return {
    customerId,
    customerName: "Alice",
    workerOwnerId,
    shopId,
    shopName: "Royal Steam & Press",
    category: "laundry",
    status: "requested",
    quoteStatus: "awaiting_worker_confirmation",
    fulfilmentMethod: "clickit_pickup_return",
    slotWindowLabel: "09:00 AM - 10:00 AM",
    batchEligible: true,
    batchOptIn: true,
    addressLabel: "Home",
    addressSummary: "Flat 402, 12th Main, Indiranagar, Bengaluru 560038",
    parcelCode: "CLK-4821",
    handoverOtp: "1234",
    deliveryOtp: "5678",
    itemsSummary: "5 x Steam Press (₹12/cloth)",
    itemCount: 5,
    weightKg: 2.5,
    serviceSubtotalMinor: 6000,
    deliveryFeeMinor: 1000,
    platformFeeMinor: 100,
    totalMinor: 7100,
    paymentStatus: "pending",
    milestone: "Awaiting worker confirmation",
    ratingStars: 0,
    createdAt: pastDate(),
    updatedAt: pastDate(),
    ...overrides,
  };
}

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("shops").where("published", "==", true).get());
  await assertFails(unauthDb.collection("bookings").where("customerId", "==", ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Hacker",
      activeRole: "customer",
      activeLocality: "Indiranagar",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("User profile & addresses: owner CRUD succeeds, cross-user & future timestamps fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      activeRole: "customer",
      activeLocality: "Indiranagar",
      phoneMasked: "+91 98••• ••123",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  // Reject @DocumentId `id` injection
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("addresses").doc("addr_bad").set({
      id: "addr_bad",
      userId: ALICE_UID,
      label: "Home",
      houseNo: "402",
      street: "12th Main",
      locality: "Indiranagar",
      city: "Bengaluru",
      postalCode: "560038",
      defaultAddress: true,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  // Valid address create & partial update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("addresses").doc("addr_1").set({
      userId: ALICE_UID,
      label: "Home",
      houseNo: "402",
      street: "12th Main",
      locality: "Indiranagar",
      city: "Bengaluru",
      postalCode: "560038",
      defaultAddress: true,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("addresses").doc("addr_1").update({
      label: "Primary Home",
      updatedAt: pastDate(),
    })
  );

  // Owner can update displayName and photoUrl
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Alice Sharma",
      photoUrl: "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD/",
      updatedAt: pastDate(),
    })
  );

  // Reject oversized photoUrl (> 200,000 chars)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      photoUrl: "x".repeat(200001),
      updatedAt: pastDate(),
    })
  );

  // Reject future timestamp tampering
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      activeRole: "worker",
      updatedAt: futureDate(),
    })
  );

  // Reject cross-user read/write
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Tampered",
      updatedAt: pastDate(),
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("addresses").get());
});

test("Shops & Services: published discovery query succeeds, unfiltered query fails, owner-only service edits", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await assertSucceeds(
    bobDb.collection("shops").doc("shop_1").set(buildShopPayload(BOB_UID))
  );

  await assertSucceeds(
    bobDb.collection("shops").doc("shop_1").collection("services").doc("svc_1").set({
      shopId: "shop_1",
      ownerId: BOB_UID,
      name: "Steam Press",
      description: "Crisp press",
      category: "laundry",
      unit: "cloth",
      priceMinor: 1200,
      minQty: 1,
      turnaroundHours: 24,
      active: true,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  // Customer discovery filtered query aligns with rules
  await assertSucceeds(aliceDb.collection("shops").where("published", "==", true).get());
  await assertSucceeds(
    aliceDb.collection("shops").doc("shop_1").collection("services").where("active", "==", true).get()
  );

  // Unfiltered list queries must fail
  await assertFails(aliceDb.collection("shops").get());
  await assertFails(aliceDb.collection("shops").doc("shop_1").collection("services").get());

  // Bootstrap WriteBatch creating shop and nested service atomically succeeds
  const bootstrapBatch = bobDb.batch();
  const batchShopRef = bobDb.collection("shops").doc("shop_batch_1");
  bootstrapBatch.set(batchShopRef, buildShopPayload(BOB_UID));
  bootstrapBatch.set(batchShopRef.collection("services").doc("svc_batch_1"), {
    shopId: "shop_batch_1",
    ownerId: BOB_UID,
    name: "Wash & Fold",
    description: "Organic wash",
    category: "laundry",
    unit: "kg",
    priceMinor: 6500,
    minQty: 1,
    turnaroundHours: 24,
    active: true,
    createdAt: pastDate(),
    updatedAt: pastDate(),
  });
  await assertSucceeds(bootstrapBatch.commit());

  // Customer cannot add or edit Bob's shop services
  await assertFails(
    aliceDb.collection("shops").doc("shop_1").collection("services").doc("svc_hack").set({
      shopId: "shop_1",
      ownerId: ALICE_UID,
      name: "Free Wash",
      description: "Hack",
      category: "laundry",
      unit: "cloth",
      priceMinor: 100,
      minQty: 1,
      turnaroundHours: 24,
      active: true,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("End-to-end Booking & DeliveryTask security boundaries, partner claim, and terminal state lock", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("shops").doc("shop_1").set(buildShopPayload(BOB_UID));
  });

  // Customer creates booking
  await assertSucceeds(
    aliceDb.collection("bookings").doc("bk_1").set(buildBookingPayload(ALICE_UID, BOB_UID, "shop_1"))
  );

  // Unrelated user Charlie cannot list Alice's bookings before assignment
  await assertFails(charlieDb.collection("bookings").where("customerId", "==", ALICE_UID).get());
  await assertFails(charlieDb.collection("bookings").doc("bk_1").get());

  // Worker accepts booking & creates delivery task offer + timeline + customer notification
  await assertSucceeds(
    bobDb.collection("bookings").doc("bk_1").update({
      status: "pickup_scheduled",
      quoteStatus: "confirmed",
      milestone: "Pickup scheduled",
      updatedAt: pastDate(),
    })
  );

  await assertSucceeds(
    bobDb.collection("bookings").doc("bk_1").collection("timeline").doc("ev_1").set({
      customerId: ALICE_UID,
      workerOwnerId: BOB_UID,
      actorId: BOB_UID,
      actorRole: "worker",
      status: "pickup_scheduled",
      title: "Worker Accepted",
      detail: "Confirmed final quote",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  await assertSucceeds(
    bobDb.collection("users").doc(ALICE_UID).collection("notifications").doc("notif_1").set({
      userId: ALICE_UID,
      title: "Royal Steam & Press accepted",
      message: "Pickup scheduled",
      category: "booking",
      bookingId: "bk_1",
      readFlag: false,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  // Unrelated Charlie cannot inject timeline or notification for Alice
  await assertFails(
    charlieDb.collection("users").doc(ALICE_UID).collection("notifications").doc("notif_bad").set({
      userId: ALICE_UID,
      title: "Spam",
      message: "Spam",
      category: "booking",
      bookingId: "bk_1",
      readFlag: false,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  await assertSucceeds(
    bobDb.collection("delivery_tasks").doc("task_1").set({
      bookingId: "bk_1",
      customerId: ALICE_UID,
      workerOwnerId: BOB_UID,
      batchId: "BATCH-09AM",
      taskType: "pickup",
      status: "offered",
      pickupZone: "Indiranagar",
      dropZone: "Royal Steam & Press",
      shopName: "Royal Steam & Press",
      timeWindowLabel: "09:00 AM - 10:00 AM",
      parcelCode: "CLK-4821",
      itemCount: 5,
      estimatedWeightKg: 2.5,
      payoutMinor: 2000,
      batchStopIndex: 1,
      batchTotalStops: 3,
      pickupLat: 12.9716,
      pickupLng: 77.6412,
      dropLat: 12.9784,
      dropLng: 77.6408,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  // Partner queries offered tasks and claims task_1
  await assertSucceeds(charlieDb.collection("delivery_tasks").where("status", "==", "offered").get());
  await assertSucceeds(
    charlieDb.collection("delivery_tasks").doc("task_1").update({
      partnerId: CHARLIE_UID,
      status: "accepted",
      updatedAt: pastDate(),
    })
  );
  await assertSucceeds(
    charlieDb.collection("bookings").doc("bk_1").update({
      assignedPartnerId: CHARLIE_UID,
      milestone: "Partner assigned",
      updatedAt: pastDate(),
    })
  );

  // Customer observes own delivery tasks for live map tracking
  await assertSucceeds(
    aliceDb.collection("delivery_tasks").where("customerId", "==", ALICE_UID).get()
  );

  // Assigned partner streams valid live GPS coordinates
  await assertSucceeds(
    charlieDb.collection("delivery_tasks").doc("task_1").update({
      agentLat: 12.9742,
      agentLng: 77.6410,
      agentBearing: 145.0,
      locationUpdatedAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  // Reject out-of-range coordinates or unauthorized customer GPS spoof
  await assertFails(
    charlieDb.collection("delivery_tasks").doc("task_1").update({
      agentLat: 120.0,
      agentLng: 77.6410,
      locationUpdatedAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
  await assertFails(
    aliceDb.collection("delivery_tasks").doc("task_1").update({
      agentLat: 12.9750,
      agentLng: 77.6411,
      locationUpdatedAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  // Partner cannot change service price on booking
  await assertFails(
    charlieDb.collection("bookings").doc("bk_1").update({
      totalMinor: 99900,
      updatedAt: pastDate(),
    })
  );

  // Customer pays via RuPay before completing order
  await assertSucceeds(
    aliceDb.collection("bookings").doc("bk_1").update({
      paymentStatus: "paid",
      paymentMethod: "rupay_card",
      paymentRef: "RuPay •••• 4521",
      updatedAt: pastDate(),
    })
  );

  // Worker cannot alter customer's paymentMethod/paymentRef
  await assertFails(
    bobDb.collection("bookings").doc("bk_1").update({
      paymentMethod: "tampered",
      updatedAt: pastDate(),
    })
  );

  // Customer completes & submits one-time review; further status edits locked
  await assertSucceeds(
    aliceDb.collection("bookings").doc("bk_1").update({
      status: "completed",
      paymentStatus: "paid",
      updatedAt: pastDate(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("bookings").doc("bk_1").update({
      ratingStars: 5,
      reviewText: "Crisp ironing and right on time!",
      updatedAt: pastDate(),
    })
  );

  // Terminal state lock: second rating or status revert rejected
  await assertFails(
    aliceDb.collection("bookings").doc("bk_1").update({
      ratingStars: 1,
      updatedAt: pastDate(),
    })
  );
  await assertFails(
    bobDb.collection("bookings").doc("bk_1").update({
      status: "in_progress",
      updatedAt: pastDate(),
    })
  );
});

test("Hardened security boundaries: block rating tampering, price spoofing, unpaid completion, premature partner claim, and mismatched delivery_tasks", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("shops").doc("shop_sec").set(buildShopPayload(BOB_UID));
  });

  // 1. Non-owner Alice AND owner Bob cannot tamper with published shop ratingAvg/ratingCount/activeLoad
  await assertFails(
    aliceDb.collection("shops").doc("shop_sec").update({
      ratingAvg: 1.0,
      ratingCount: 999,
      updatedAt: pastDate(),
    })
  );
  await assertFails(
    bobDb.collection("shops").doc("shop_sec").update({
      ratingAvg: 5.0,
      ratingCount: 999,
      updatedAt: pastDate(),
    })
  );
  // Owner can still legitimately update operational shop settings
  await assertSucceeds(
    bobDb.collection("shops").doc("shop_sec").update({
      openStatus: false,
      updatedAt: pastDate(),
    })
  );

  // 2. Booking create blocks workerOwnerId spoofing, mismatched totalMinor, and non-numeric OTP
  await assertFails(
    aliceDb.collection("bookings").doc("bk_spoof_worker").set(
      buildBookingPayload(ALICE_UID, ALICE_UID, "shop_sec")
    )
  );
  await assertFails(
    aliceDb.collection("bookings").doc("bk_bad_total").set(
      buildBookingPayload(ALICE_UID, BOB_UID, "shop_sec", { totalMinor: 100 })
    )
  );
  await assertFails(
    aliceDb.collection("bookings").doc("bk_bad_otp").set(
      buildBookingPayload(ALICE_UID, BOB_UID, "shop_sec", { handoverOtp: "abcd" })
    )
  );

  // Valid booking created
  await assertSucceeds(
    aliceDb.collection("bookings").doc("bk_sec").set(
      buildBookingPayload(ALICE_UID, BOB_UID, "shop_sec")
    )
  );

  // 3. Premature partner claim while booking is still 'requested' must fail
  await assertFails(
    charlieDb.collection("bookings").doc("bk_sec").update({
      assignedPartnerId: CHARLIE_UID,
      milestone: "Premature claim",
      updatedAt: pastDate(),
    })
  );

  // 4. Customer cannot tamper with booking prices
  await assertFails(
    aliceDb.collection("bookings").doc("bk_sec").update({
      totalMinor: 0,
      serviceSubtotalMinor: 0,
      updatedAt: pastDate(),
    })
  );

  // 5. Customer cannot mark unpaid booking as completed directly
  await assertFails(
    aliceDb.collection("bookings").doc("bk_sec").update({
      status: "completed",
      updatedAt: pastDate(),
    })
  );
  await assertFails(
    aliceDb.collection("bookings").doc("bk_sec").update({
      status: "completed",
      paymentStatus: "paid",
      updatedAt: pastDate(),
    })
  );

  // 6. Cross-booking delivery_task customerId/workerOwnerId spoofing must fail
  await assertFails(
    aliceDb.collection("delivery_tasks").doc("task_spoof").set({
      bookingId: "bk_sec",
      customerId: ALICE_UID,
      workerOwnerId: CHARLIE_UID, // mismatched from bk_sec's workerOwnerId (BOB_UID)
      batchId: "BATCH-09AM",
      taskType: "pickup",
      status: "offered",
      pickupZone: "Indiranagar",
      dropZone: "Royal Steam & Press",
      shopName: "Royal Steam & Press",
      timeWindowLabel: "09:00 AM - 10:00 AM",
      parcelCode: "CLK-4821",
      itemCount: 5,
      estimatedWeightKg: 2.5,
      payoutMinor: 2000,
      batchStopIndex: 1,
      batchTotalStops: 3,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  // 7. Partner can transition pickup_scheduled -> picked_up -> received_by_worker cleanly
  await assertSucceeds(
    bobDb.collection("bookings").doc("bk_sec").update({
      status: "pickup_scheduled",
      quoteStatus: "confirmed",
      updatedAt: pastDate(),
    })
  );
  await assertSucceeds(
    charlieDb.collection("bookings").doc("bk_sec").update({
      assignedPartnerId: CHARLIE_UID,
      milestone: "Partner assigned",
      updatedAt: pastDate(),
    })
  );
  await assertSucceeds(
    charlieDb.collection("bookings").doc("bk_sec").update({
      status: "picked_up",
      milestone: "Picked up",
      updatedAt: pastDate(),
    })
  );
  await assertSucceeds(
    charlieDb.collection("bookings").doc("bk_sec").update({
      status: "received_by_worker",
      milestone: "Handed over",
      updatedAt: pastDate(),
    })
  );
  // Partner cannot cancel or complete a booking
  await assertFails(
    charlieDb.collection("bookings").doc("bk_sec").update({
      status: "completed",
      updatedAt: pastDate(),
    })
  );
});

