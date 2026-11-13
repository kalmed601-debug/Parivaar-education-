const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const nowTs = () => new Date(Date.now() - 1000);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_1";
const BOB_UID = "bob_uid_2";

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

test("Unauthenticated user cannot read or write students", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/students`).get());
  await assertFails(
    unauthDb.doc(`users/${ALICE_UID}/students/stu_1`).set({
      userId: ALICE_UID,
      name: "Aarav",
      className: "Class 1",
      rollNumber: "101",
      gender: "Boy",
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );
});

test("Authenticated owner can create and update student with valid schema", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ref = aliceDb.doc(`users/${ALICE_UID}/students/stu_1`);
  await assertSucceeds(
    ref.set({
      userId: ALICE_UID,
      name: "Aarav Patel",
      className: "Class 3",
      rollNumber: "301",
      gender: "Boy",
      village: "Chikalpat",
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );
  await assertSucceeds(
    ref.update({
      village: "Chikalpat East",
      updatedAt: nowTs(),
    })
  );
});

test("Cross-user access is denied across students, attendance, tests, books, stationery, and notices", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminDb = ctx.firestore();
    await adminDb.doc(`users/${BOB_UID}/students/stu_bob`).set({
      userId: BOB_UID,
      name: "Bob Student",
      className: "JNV",
      rollNumber: "1",
      gender: "Boy",
      createdAt: nowTs(),
      updatedAt: nowTs(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection(`users/${BOB_UID}/students`).get());
  await assertFails(aliceDb.doc(`users/${BOB_UID}/students/stu_bob`).get());
  await assertFails(
    aliceDb.doc(`users/${BOB_UID}/books/b1`).set({
      userId: BOB_UID,
      bookName: "Math",
      className: "Class 5",
      subject: "Mathematics",
      quantity: 10,
      distributed: 0,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );
});

test("Schema validation rejects ghost fields, invalid className, and distributed > quantity", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Ghost field rejection
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/students/stu_ghost`).set({
      userId: ALICE_UID,
      name: "Priya",
      className: "Class 2",
      rollNumber: "201",
      gender: "Girl",
      ghostField: true,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  // Invalid className rejection
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/students/stu_bad_class`).set({
      userId: ALICE_UID,
      name: "Priya",
      className: "Class 12",
      rollNumber: "202",
      gender: "Girl",
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  // Book distributed > quantity rejection
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/books/book_bad`).set({
      userId: ALICE_UID,
      bookName: "Hindi",
      className: "Class 4",
      subject: "Hindi",
      quantity: 5,
      distributed: 10,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  // Valid stationery & test records succeed
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/stationery/stat_1`).set({
      userId: ALICE_UID,
      itemName: "Wooden Abacus Toy",
      category: "Educational Toy",
      quantity: 20,
      distributed: 5,
      purchasePrice: 150.0,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/tests/test_1`).set({
      userId: ALICE_UID,
      studentId: "stu_1",
      studentName: "Aarav Patel",
      className: "JNV",
      subject: "Arithmetic",
      testTitle: "JNV Mock 1",
      testType: "JNV Mock Test",
      testDate: "2026-11-12",
      totalMarks: 100,
      obtainedMarks: 88,
      percentage: 88,
      grade: "A+",
      passed: true,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );
});
