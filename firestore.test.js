const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

// 1. Unauthenticated checks
test("Unauthenticated: cannot read or write user profile", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set({
    userId: ALICE_UID,
    email: "alice@test.com"
  }));
});

test("Unauthenticated: cannot read user books or transactions", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("books").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("transactions").get());
});

// 2. Cross-user isolation checks
test("Bob cannot read Alice's profile, books, or transactions", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@test.com"
    });
    await context.firestore().collection("users").doc(ALICE_UID).collection("books").doc("book1").set({
      id: "book1",
      userId: ALICE_UID,
      name: "Alice Personal"
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("books").doc("book1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("transactions").get());
});

// 3. Alice can manage her own data with valid schemas
test("Alice can create and read her profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set({
    userId: ALICE_UID,
    email: "alice@test.com",
    displayName: "Alice"
  }));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
});

test("Alice can create a ledger book", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("books").doc("book_001").set({
    id: "book_001",
    userId: ALICE_UID,
    name: "Main Ledger",
    currencySymbol: "৳",
    currencyCode: "BDT",
    colorHex: 4280193160,
    iconName: "wallet",
    isArchived: false,
    customCategories: "Food||Rent"
  }));
});

test("Alice can create a transaction", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_001").set({
    id: "tx_001",
    userId: ALICE_UID,
    bookId: "book_001",
    type: "IN",
    amount: 1500.0,
    category: "Salary",
    timestamp: Date.now(),
    memo: "Monthly Salary",
    paymentMode: "Bank"
  }));
});

test("Negative: Alice cannot create transaction with negative amount or invalid type", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_bad").set({
    id: "tx_bad",
    userId: ALICE_UID,
    bookId: "book_001",
    type: "INVALID",
    amount: -50.0,
    category: "Misc"
  }));
});
