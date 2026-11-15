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

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProjectPayload(uid = ALICE_UID, overrides = {}) {
  const now = new Date();
  return {
    userId: uid,
    title: "پوستر تایپوگرافی سه بعدی",
    canvasWidth: 1080,
    canvasHeight: 1080,
    backgroundType: "GRADIENT",
    backgroundColorHex: "#0F1117",
    backgroundSecondaryHex: "#1E1B4B",
    layerCount: 3,
    layerTitles: ["متن اصلی", "لایه شکل", "استیکر"],
    primaryText: "PixelLab AI",
    fontId: "lalezar",
    accentColorHex: "#38BDF8",
    is3dEnabled: true,
    depth3d: 24,
    curveAngle: 0,
    plpFileName: "project_1.plp",
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

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

test("Unauthenticated user cannot read or create projects", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("projects").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("p1")
      .set(validProjectPayload(ALICE_UID))
  );
});

test("Authenticated owner can create, get, list, update, and delete own project", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("projects")
    .doc("proj_1");

  const initial = validProjectPayload(ALICE_UID);
  await assertSucceeds(docRef.set(initial));
  await assertSucceeds(docRef.get());
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .where("userId", "==", ALICE_UID)
      .get()
  );

  await assertSucceeds(
    docRef.update({
      title: "پوستر ویرایش شده",
      depth3d: 40,
      updatedAt: new Date(),
    })
  );

  await assertSucceeds(docRef.delete());
});

test("Cross-user access is strictly denied", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .set(validProjectPayload(ALICE_UID));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .delete()
  );
});

test("Shadow fields and invalid bounds are rejected on create and update", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("projects")
    .doc("proj_bad");

  // Shadow field on create
  await assertFails(
    docRef.set(validProjectPayload(ALICE_UID, { isVerified: true }))
  );

  // Oversized canvasWidth
  await assertFails(
    docRef.set(validProjectPayload(ALICE_UID, { canvasWidth: 99999 }))
  );

  // Valid create, then shadow field on update
  await assertSucceeds(docRef.set(validProjectPayload(ALICE_UID)));
  await assertFails(
    docRef.update({
      title: "Valid Title",
      ghostField: "hack",
      updatedAt: new Date(),
    })
  );
});
