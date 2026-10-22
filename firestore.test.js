const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = "demo-no-project";

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: fs.readFileSync("firestore.rules", "utf8"),
      host: "127.0.0.1",
      port: 8085,
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

test("Unauthenticated user cannot read or write customer", async () => {
  const unauthContext = testEnv.unauthenticatedContext();
  const db = unauthContext.firestore();
  await assertFails(db.doc("users/user_123/customers/c1").get());
  await assertFails(
    db.doc("users/user_123/customers/c1").set({
      name: "Rahim Store",
      type: "CUSTOMER",
      totalGave: 100,
      totalGot: 50,
      netBalance: 50,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user can write and read their own customer", async () => {
  const authContext = testEnv.authenticatedContext("user_123");
  const db = authContext.firestore();
  await assertSucceeds(
    db.doc("users/user_123/customers/c1").set({
      name: "Rahim Store",
      phone: "01711000000",
      address: "Main Market",
      type: "CUSTOMER",
      totalGave: 100,
      totalGot: 50,
      netBalance: 50,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(db.doc("users/user_123/customers/c1").get());
});

test("Other authenticated user cannot read or write another user's customer", async () => {
  const authContext = testEnv.authenticatedContext("user_123");
  const db = authContext.firestore();
  await assertSucceeds(
    db.doc("users/user_123/customers/c1").set({
      name: "Rahim Store",
      type: "CUSTOMER",
      totalGave: 100,
      totalGot: 0,
      netBalance: 100,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );

  const bobContext = testEnv.authenticatedContext("user_456");
  const bobDb = bobContext.firestore();
  await assertFails(bobDb.doc("users/user_123/customers/c1").get());
  await assertFails(
    bobDb.doc("users/user_123/customers/c2").set({
      name: "Hacker Party",
      type: "CUSTOMER",
      totalGave: 100,
      totalGot: 0,
      netBalance: 100,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Invalid customer data is rejected (e.g. invalid type or negative totalGave)", async () => {
  const authContext = testEnv.authenticatedContext("user_123");
  const db = authContext.firestore();
  await assertFails(
    db.doc("users/user_123/customers/c1").set({
      name: "Invalid Customer",
      type: "UNKNOWN_TYPE",
      totalGave: -50,
      totalGot: 0,
      netBalance: -50,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user can record valid transaction and cashbook entry", async () => {
  const authContext = testEnv.authenticatedContext("user_123");
  const db = authContext.firestore();

  await assertSucceeds(
    db.doc("users/user_123/transactions/t1").set({
      customerId: "c1",
      customerName: "Rahim Store",
      type: "GAVE",
      amount: 500,
      note: "Flour sacks credit",
      billNumber: "INV-101",
      transactionDate: new Date(),
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );

  await assertSucceeds(
    db.doc("users/user_123/cashbook/entry1").set({
      type: "CASH_IN",
      amount: 1500,
      category: "Daily Sales",
      note: "Evening cash tally",
      entryDate: new Date(),
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Transactions with non-positive amount are rejected", async () => {
  const authContext = testEnv.authenticatedContext("user_123");
  const db = authContext.firestore();
  await assertFails(
    db.doc("users/user_123/transactions/t2").set({
      customerId: "c1",
      type: "GAVE",
      amount: -50,
      transactionDate: new Date(),
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});
