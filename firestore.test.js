const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const PROJECT_ID = "demo-no-project";
let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: fs.readFileSync("firestore.rules", "utf8"),
      host: "127.0.0.1",
      port: 8085
    }
  });
});

beforeEach(async () => {
  await testEnv.clearFirestore();
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

test("Denies unauthenticated access to user profile", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc("user_alice").get());
});

test("Denies cross-user access to profile", async () => {
  const bobDb = testEnv.authenticatedContext("user_bob").firestore();
  await assertFails(bobDb.collection("users").doc("user_alice").get());
});

test("Allows user to create and get their own profile", async () => {
  const aliceDb = testEnv.authenticatedContext("user_alice").firestore();
  const aliceDoc = aliceDb.collection("users").doc("user_alice");
  
  await assertSucceeds(
    aliceDoc.set({
      userId: "user_alice",
      email: "alice@example.com",
      displayName: "Alice Scientist",
      institution: "Genomics Institute",
      role: "Geneticist",
      bio: "Focusing on BRCA1 and BRCA2 pathogenic variations.",
      phone: "+1-555-0199",
      avatarUrl: "https://example.com/avatar.png",
      createdAt: new Date(),
      updatedAt: new Date()
    })
  );

  await assertSucceeds(aliceDoc.get());
});

test("Allows user to manage notification settings", async () => {
  const aliceDb = testEnv.authenticatedContext("user_alice").firestore();
  const settingsDoc = aliceDb.collection("users").doc("user_alice").collection("settings").doc("notifications");

  await assertSucceeds(
    settingsDoc.set({
      userId: "user_alice",
      newMessages: true,
      taskReminders: true,
      specialOffers: false,
      modelTrainingAlerts: true,
      variantAlerts: true,
      emailNotifications: true,
      updatedAt: new Date()
    })
  );

  await assertSucceeds(settingsDoc.get());
});

test("Allows user to log activity and prevents unauth/cross-user reads", async () => {
  const aliceDb = testEnv.authenticatedContext("user_alice").firestore();
  const actDoc = aliceDb.collection("users").doc("user_alice").collection("activities").doc("act_1");

  await assertSucceeds(
    actDoc.set({
      id: "act_1",
      userId: "user_alice",
      type: "prediction",
      title: "BRCA1 Variant Analyzed",
      description: "Chr 17:43044295 G>A classified as Pathogenic",
      gene: "BRCA1",
      variantId: "VCV000012345",
      timestamp: new Date()
    })
  );

  const bobDb = testEnv.authenticatedContext("user_bob").firestore();
  await assertFails(bobDb.collection("users").doc("user_alice").collection("activities").doc("act_1").get());
});

test("Allows user to save variant predictions", async () => {
  const aliceDb = testEnv.authenticatedContext("user_alice").firestore();
  const predDoc = aliceDb.collection("users").doc("user_alice").collection("predictions").doc("pred_1");

  await assertSucceeds(
    predDoc.set({
      id: "pred_1",
      userId: "user_alice",
      gene: "TP53",
      assembly: "GRCh38",
      chromosome: "17",
      position: 7673802,
      refAllele: "C",
      altAllele: "T",
      variantType: "single nucleotide variant",
      variationId: "VCV000055555",
      predictedLabel: "Pathogenic",
      riskScore: 0.94,
      confidence: 0.98,
      notes: "Known hotspot mutation in DNA-binding domain.",
      createdAt: new Date()
    })
  );
  
  await assertSucceeds(predDoc.get());
});
